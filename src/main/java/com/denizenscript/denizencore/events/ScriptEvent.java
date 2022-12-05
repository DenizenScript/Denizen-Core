package com.denizenscript.denizencore.events;

import com.denizenscript.denizencore.events.core.*;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.containers.core.WorldScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.ScriptEntrySet;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.denizenscript.denizencore.DenizenCore;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

/**
 * Core handler for script events (in world script containers).
 * Instances of this class are often duplicated as part of the core engine system.
 */
public abstract class ScriptEvent implements ContextSource, Cloneable {

    @Override
    public ScriptEvent clone() {
        try {
            return (ScriptEvent) super.clone();
        }
        catch (CloneNotSupportedException e) {
            Debug.echoError("Clone not supported for script events?!");
            return this;
        }
    }

    /**
     * (Called by DenizenCore.init) registers primary core events.
     */
    public static void registerCoreEvents() {
        registerScriptEvent(ConsoleOutputScriptEvent.class);
        registerScriptEvent(CustomScriptEvent.class);
        registerScriptEvent(DeltaTimeScriptEvent.class);
        registerScriptEvent(RedisPubSubMessageScriptEvent.class);
        registerScriptEvent(PreScriptReloadScriptEvent.class);
        registerScriptEvent(ReloadScriptsScriptEvent.class);
        registerScriptEvent(ScriptGeneratesErrorScriptEvent.class);
        registerScriptEvent(ServerGeneratesExceptionScriptEvent.class);
        registerScriptEvent(ScriptsLoadedScriptEvent.class);
        registerScriptEvent(ShutdownScriptEvent.class);
        registerScriptEvent(SystemTimeScriptEvent.class);
        registerScriptEvent(TickScriptEvent.class);
        registerScriptEvent(WebserverWebRequestScriptEvent.class);
    }

    /**
     * Register a new script event to the system. All events must be registered to function in scripts, even if not currently used.
     */
    public static void registerScriptEvent(Class<? extends ScriptEvent> eventClass) {
        try {
            ScriptEvent event = eventClass.getConstructor().newInstance();
            registerScriptEvent(event);
        }
        catch (Throwable ex) {
            Debug.echoError("Failed to register script event '" + eventClass.getName() + "':");
            Debug.echoError(ex);
        }
    }

    public static List<String> notNameParts = new ArrayList<>(Collections.singleton("ScriptEvent"));

    public static void registerScriptEvent(ScriptEvent event) {
        String name = DebugInternals.getClassNameOpti(event.getClass());
        for (String suffix : notNameParts) {
            if (name.endsWith(suffix)) {
                name = name.substring(0, name.length() - suffix.length());
            }
        }
        event.eventData.name = name;
        events.add(event);
        eventLookup.put(CoreUtilities.toLowerCase(event.getName()), event);
        if (event.eventData.couldMatchers.isEmpty() || event.eventData.needsLegacy) {
            legacyCouldMatchEvents.add(event);
        }
    }

    /**
     * A list of all world script containers, automatically populated during script reload.
     */
    public static ArrayList<WorldScriptContainer> worldContainers = new ArrayList<>();

    /**
     * A list of all registered script events.
     */
    public static ArrayList<ScriptEvent> events = new ArrayList<>();

    /**
     * A special lookup table of script events to help optimize couldMatch calls.
     */
    public static HashMap<String, ArrayList<ScriptEvent>> couldMatchOptimizer = new HashMap<>();

    /**
     * How many paths are processed.
     */
    public static int totalPaths = 0;

    public static ArrayList<ScriptEvent> legacyCouldMatchEvents = new ArrayList<>();

    /**
     * Lookup table from script event names to their instances.
     */
    public static HashMap<String, ScriptEvent> eventLookup = new HashMap<>();

    public static class InternalEventData {

        /**
         * Statistics about an event firing.
         */
        public long stats_fires = 0, stats_scriptFires = 0, stats_nanoTimes = 0;

        /**
         * Built-in could-matchers for this event.
         */
        public ArrayList<ScriptEventCouldMatcher> couldMatchers = new ArrayList<>();

        /**
         * Switches available to this event.
         */
        public HashSet<String> localSwitches = new HashSet<>();

        /**
         * If true, this event needs to be in legacy event couldMatcher.
         */
        public boolean needsLegacy = false;

        /**
         * Cached name.
         */
        public String name;
    }

    /**
     * Known paths that fire this event.
     * Note: cannot be inside InternalEventData as it's uniquely modified for 'bukkit_priority'.
     */
    public ArrayList<ScriptPath> eventPaths = new ArrayList<>();

    /**
     * This ScriptEvent object's base data (separate from the firing-related data of an event happening). Stored in a separate instance to avoid duplication issues.
     */
    public InternalEventData eventData = new InternalEventData();

    /**
     * Whether this event has been cancelled.
     */
    public boolean cancelled = false;

    /**
     * Represents a single path for an event within a world container, based on raw text of a script.
     */
    public static class ScriptPath {

        public ScriptContainer container;
        public String event;
        public String eventLower;
        public String rawContainerPath;
        public int priority = 0;
        public ScriptEntrySet set;
        public Boolean switch_cancelled;
        public Boolean switch_ignoreCancelled;
        public HashMap<String, String> switches = new HashMap<>();
        public List<String> rawSwitches = new ArrayList<>();
        public String[] eventArgs;
        public String[] eventArgsLower;
        public String[] rawEventArgs;
        public List<ScriptEvent> matches = new ArrayList<>();
        public boolean fireAfter = false;
        public List<String> matchFailReasons = null;
        public double switch_chance;
        public List<String> switch_serverFlagged;

        public String rawEventArgAt(int index) {
            return index < rawEventArgs.length ? rawEventArgs[index] : "";
        }

        public String eventArgAt(int index) {
            return index < eventArgs.length ? eventArgs[index] : "";
        }

        public String eventArgLowerAt(int index) {
            return index < eventArgsLower.length ? eventArgsLower[index] : "";
        }

        @Deprecated
        public final boolean eventArgsLowEqualStartingAt(int index, String a, String b) {
            return eventArgLowerAt(index).equals(a) && eventArgLowerAt(index + 1).equals(b);
        }

        // <--[language]
        // @name Script Event Switches
        // @group Script Events
        // @description
        // Modern script events support the concept of 'switches'.
        // A switch is a specification of additional requirements in an event line other than what's in the event label it.
        //
        // A switch consists of a name and a value input, and are can be added anywhere in an event line as "name:<value>".
        // For example, "on delta time secondly every:5:" is a valid event, where "delta time secondly" is the event itself, and "every:<#>" is a switch available to the event.
        //
        // A traditional Denizen event might look like "on <entity> damaged",
        // where "<entity>" can be filled with "entity" or any entity type (like "player").
        // A switch-using event would instead take the format "on entity damaged" with switch "type:<entity type>"
        // meaning you can do "on entity damaged" for any entity, or "on entity damaged type:player:" for players specifically.
        // This is both more efficient to process and more explicit in what's going on, however it is less clear/readable to the average user, so it is not often used.
        // Some events may have switches for less-often specified data, and use the event line for other options.
        //
        // There are also some standard switches available to every script event, and some available to an entire category of script events.
        //
        // One switch available to every event is "server_flagged:<flag_name>", which requires that there be a server flag under the given name.
        // For example, "on console output server_flagged:recording:" will only run the handler for console output when the "recording" flag is set on the server.
        // This can also be used to require the server does NOT have a flag with "server_flagged:!<flag_name>"
        //
        // "chance:<percent>" is also a globally available switch.
        // For example, "on player breaks diamond_ore chance:25:" will only fire on average one in every four times that a player breaks a diamond ore block.
        //
        // Events that have a player linked have the "flagged" and "permission" switches available.
        //
        // If the switch is specified, and an event doesn't have a linked player, the event will automatically fail to match.
        // The "flagged:<flag_name>" switch will limit the event to only fire when the player has the flag with the specified name.
        // It can be used like "on player breaks block flagged:nobreak:" (that would be used alongside "- flag player nobreak").
        // You can also use "flagged:!<flag_name>" to require the player does NOT have the flag, like "on player breaks block flagged:!griefbypass:"
        //
        // The "permission:<perm key>" will limit the event to only fire when the player has the specified permission key.
        // It can be used like "on player breaks block permission:denizen.my.perm:"
        // For multiple flag or permission requirements, just list them separated by '|' pipes, like "flagged:a|b|c". This will require all named flags/permissions to be present, not just one.
        //
        // Events that have an NPC linked have the "assigned" switch available.
        // If the switch is specified, and an event doesn't have a linked NPC, the event will automatically fail to match.
        // The "assigned:<script name>" switch will limit the event to only fire when the NPC has an assignment script that matches the given advanced matcher.
        //
        // Events that occur at a specific location have the "in:<area>" and "location_flagged" switches.
        // This switches will be ignored (not counted one way or the other) for events that don't have a known location.
        // For "in:<area>" switches, 'area' is any area-defining tag type - refer to <@link language Advanced Object Matchables>.
        // "location_flagged:<flag name>" works just like "server_flagged" or the player "flagged" switches, but for locations.
        //
        // All script events have priority switches (see <@link language script event priority>),
        // All Bukkit events have bukkit priority switches (see <@link language bukkit event priority>),
        // All cancellable script events have cancellation switches (see <@link language script event cancellation>).
        //
        // See also <@link language Advanced Object Matching>.
        // -->

        public boolean checkSwitch(String key, String value) {
            String pathValue = switches.get(key);
            if (pathValue == null) {
                return true;
            }
            return CoreUtilities.equalsIgnoreCase(pathValue, value);
        }

        public boolean tryObjectSwitch(String key, ObjectTag obj) {
            String val = switches.get(key);
            if (val == null) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            return obj.tryAdvancedMatcher(val);
        }

        public boolean tryArgObject(int argIndex, ObjectTag obj) {
            if (obj == null) {
                return false;
            }
            return obj.tryAdvancedMatcher(eventArgAt(argIndex));
        }

        // <--[data]
        // @name not_switches
        // @values regex
        // -->

        /**
         * List of all prefixed keys that should not be interpreted as switches.
         */
        public static HashSet<String> notSwitches = new HashSet<>(Collections.singleton("regex"));


        public ScriptPath(ScriptContainer container, String event, String rawContainerPath) {
            this.event = event;
            this.rawContainerPath = rawContainerPath;
            rawEventArgs = CoreUtilities.split(event, ' ').toArray(new String[0]);
            this.container = container;
            List<String> eventLabel = new ArrayList<>();
            for (String possible : CoreUtilities.split(event, ' ').toArray(new String[0])) {
                List<String> split = CoreUtilities.split(possible, ':', 2);
                String low = CoreUtilities.toLowerCase(split.get(0));
                if (split.size() > 1 && !ArgumentHelper.matchesInteger(split.get(0)) && !notSwitches.contains(low)) {
                    switches.put(low, split.get(1));
                    rawSwitches.add(low + ":" + split.get(1));
                }
                else {
                    eventLabel.add(possible);
                }
            }
            eventLower = CoreUtilities.toLowerCase(String.join(" ", eventLabel));
            eventArgs = eventLabel.toArray(new String[0]);
            eventArgsLower = CoreUtilities.split(eventLower, ' ').toArray(new String[0]);
            switch_cancelled = switches.containsKey("cancelled") ? CoreUtilities.equalsIgnoreCase(switches.get("cancelled"), "true") : null;
            switch_ignoreCancelled = switches.containsKey("ignorecancelled") ? CoreUtilities.equalsIgnoreCase(switches.get("ignorecancelled"), "true") : null;
            switch_serverFlagged = switches.containsKey("server_flagged") ? CoreUtilities.split(switches.get("server_flagged"), '|') : null;
            switch_chance = switches.containsKey("chance") ? new ElementTag(switches.get("chance")).asDouble() : 0;
            set = container.getSetFor("events." + rawContainerPath);
            if (set == null || set.entries == null) {
                Debug.echoError("Invalid script (formatting error?) in container '" + container.getName() + " at event '" + rawContainerPath + "'.");
            }
        }

        @Override
        public String toString() {
            return container.getName() + ".events.on " + event;
        }
    }
    // <--[language]
    // @name Script Event After vs On
    // @group Script Events
    // @description
    // Modern ScriptEvents let you choose between "on" and "after".
    // An "on" event looks like "on player breaks block:" while an "after" event looks like "after player breaks block:".
    //
    // An "on" event fires *before* the event actually happens in the world. This means some relevant data won't be updated
    // (for example, "<context.location.material>" would still show the block type that is going to be broken)
    // and the result of the event can be changed (eg the event can be cancelled to stop it from actually going through).
    //
    // An "after" event, as the name implies, fires *after* the event actually happens. This means data will be already updated to the new state
    // (so "<context.location.material>" would now show air) but could potentially contain an arbitrary new state from unrelated changes
    // (for example "<context.location.material>" might now show a different block type, or the original one, if the event was changed,
    // or another thing happened right after the event but before the 'after' event ran).
    // This also means you cannot affect the outcome of the event at all (you can't cancel it or anything else - the "determine" command does nothing).
    //
    // See also <@link language Safety In Events>
    // -->

    public static void reload() {
        if (CoreConfiguration.debugLoadingInfo) {
            Debug.log("Reloading script events...");
        }
        reloadPreClear();
        totalPaths = 0;
        for (ScriptContainer container : worldContainers) {
            try {
                if (!container.shouldEnable()) {
                    continue;
                }
                YamlConfiguration config = container.getConfigurationSection("events");
                if (config == null) {
                    Debug.echoError("Missing or invalid events block for <Y>" + container.getName());
                    continue;
                }
                for (StringHolder evt1 : config.getKeys(false)) {
                    if (evt1 == null || evt1.str == null) {
                        Debug.echoError("Missing or invalid events block for <Y>" + container.getName());
                        continue;
                    }
                    totalPaths++;
                    loadSinglePath(evt1, container);
                }
            }
            catch (Exception ex) {
                Debug.echoError("Failed to load world script container '<Y>" + container.getName() + "<W>':");
                Debug.echoError(ex);
            }
        }
        reloadPostLoad();
        Debug.log("Processed <A>" + totalPaths + "<W> script event paths.");
    }

    private static void reloadPreClear() {
        for (ScriptEvent event : events) {
            try {
                event.destroy();
                event.eventPaths.clear();
            }
            catch (Throwable ex) {
                Debug.echoError("Failed to unload event '<Y>" + event.getName() + "<W>':");
                Debug.echoError(ex);
            }
        }
    }

    private static void loadSinglePath(StringHolder evt1, ScriptContainer container) {
        if (CoreUtilities.contains(evt1.str, '@')) {
            Debug.echoError("Script '<Y>" + container.getName() + "<W>' has event '<Y>" + evt1.str.replace("@", "<LR>@<Y>")
                    + "<W>' which contains object notation, which is deprecated for use in world events. Please remove it.");
        }
        String evt;
        boolean after = false;
        if (evt1.low.startsWith("on ")) {
            evt = evt1.str.substring("on ".length());
        }
        else if (evt1.low.startsWith("after ")) {
            evt = evt1.str.substring("after ".length());
            after = true;
        }
        else {
            Debug.echoError("Script path '<Y>" + evt1.str + "<W>' is invalid (missing 'on' or 'after').");
            return;
        }
        evt = evt.replace("&dot", ".").replace("&amp", "&");
        ScriptPath path = new ScriptPath(container, evt, evt1.str);
        path.fireAfter = after;
        tryLoadDirect(path);
    }

    private static boolean tryLoadDirect(ScriptPath path) {
        if (path.set == null) {
            Debug.echoError("Script path '<Y>" + path + "<W>' is invalid (empty or misconfigured).");
            return false;
        }
        tryingToBuildPath = path;
        ArrayList<ScriptEvent> toScan = couldMatchOptimizer.get(path.eventArgLowerAt(0));
        if (toScan != null) {
            tryLoadForSet(path, toScan);
        }
        tryLoadForSet(path, legacyCouldMatchEvents);
        if (path.matches.size() > 1) {
            Debug.log("Event <Y>" + path + "<W> is matched to multiple ScriptEvents: <Y>" + CoreUtilities.join("<W>,<Y> ", path.matches));
        }
        else if (path.matches.isEmpty()) {
            if (path.eventArgsLower.length > 2 && path.eventArgLowerAt(path.eventArgsLower.length - 2).equals("in")) {
                ScriptPath legacy = new ScriptPath(path.container, path.event.substring(0, path.eventLower.lastIndexOf(" in ")), path.rawContainerPath);
                if (tryLoadDirect(legacy)) {
                    Deprecations.inAreaSwitchFormat.warn(path.container);
                    return true;
                }
            }
            if (toScan == null) {
                Debug.echoError("Event <Y>" + path + "<W> is not matched to any ScriptEvents.");
            }
            else {
                int sizedRight = 0;
                for (ScriptEvent evt : toScan) {
                    for (ScriptEventCouldMatcher matcher : evt.eventData.couldMatchers) {
                        if (matcher.validators.length == path.eventArgsLower.length) {
                            sizedRight++;
                            break;
                        }
                    }
                }
                Debug.echoError("Event <Y>" + path + "<W> is not matched to any ScriptEvents.  First word '<Y>"
                        + path.eventArgLowerAt(0) + "<W>' matched lookup table, of which <Y>" + sizedRight + "<W> are correct length, but specific matching failed.");
            }
            if (path.matchFailReasons != null) {
                for (String reason : path.matchFailReasons) {
                    Debug.log(reason);
                }
            }
            path.matchFailReasons = null;
            return false;
        }
        path.matchFailReasons = null;
        return true;
    }

    private static void tryLoadForSet(ScriptPath path, ArrayList<ScriptEvent> events) {
        for (ScriptEvent event : events) {
            tryingToBuildEvent = event;
            if (event.couldMatch(path) && !path.matches.contains(event)) {
                event.eventPaths.add(path);
                path.matches.add(event);
                if (CoreConfiguration.debugLoadingInfo) {
                    Debug.log("Event match, <Y>" + event.getName() + "<W> matched for '<Y>" + path + "<W>'!");
                }
            }
        }
    }

    private static void reloadPostLoad() {
        for (ScriptEvent event : events) {
            try {
                if (event.eventPaths.isEmpty()) {
                    continue;
                }
                event.sort();
                event.init();
            }
            catch (Throwable ex) {
                Debug.echoError("Failed to load event '<Y>" + event.getName() + "<W>':");
                Debug.echoError(ex);
            }
        }
    }

    public static ScriptPath tryingToBuildPath = null;
    public static ScriptEvent tryingToBuildEvent = null;

    /**
     * Adds a reason a 'couldMatch' call failed, to try to help end users figure out why their event isn't recognized.
     */
    public static void addPossibleCouldMatchFailReason(String reason, String example) {
        if (tryingToBuildPath == null || tryingToBuildEvent == null) {
            return;
        }
        if (tryingToBuildPath.matchFailReasons == null) {
            tryingToBuildPath.matchFailReasons = new ArrayList<>();
        }
        String baseText = "Almost matched: <Y>" + tryingToBuildEvent.getName();
        String reasonText = "<W>, but failed because: <Y>" + reason + "<W>: '<LR>" + example + "<W>'";
        if (currentCouldMatcher == null) {
            tryingToBuildPath.matchFailReasons.add(baseText + reasonText);
        }
        else {
            tryingToBuildPath.matchFailReasons.add(baseText + "<W> as <Y>" + currentCouldMatcher.format + reasonText);
        }
    }

    // <--[language]
    // @name Script Event Cancellation
    // @group Script Events
    // @description
    // Any modern ScriptEvent can take a "cancelled:<true/false>" argument and a "ignorecancelled:true" argument.
    // For example: "on object does something ignorecancelled:true:"
    // Or, "on object does something cancelled:true:"
    // If you set 'ignorecancelled:true', the event will fire regardless of whether it was cancelled.
    // If you set 'cancelled:true', the event will fire /only/ when it was cancelled.
    // By default, only non-cancelled events will fire. (Effectively acting as if you had set "cancelled:false").
    //
    // Any modern script event can take the determinations "cancelled" and "cancelled:false".
    // These determinations will set whether the script event is 'cancelled' in the eyes of following script events,
    // and, in some cases, can be used to stop the event itself from continuing.
    // A script event can at any time check the cancellation state of an event by accessing "<context.cancelled>".
    // -->

    public static boolean matchesScript(ScriptEvent sEvent, ScriptPath path) {
        if (path.switch_cancelled != null) {
            if (path.switch_cancelled != sEvent.cancelled) {
                return false;
            }
        }
        else if (path.switch_ignoreCancelled != null) {
            if (!path.switch_ignoreCancelled && sEvent.cancelled) {
                return false;
            }
        }
        else { // No cancelled status switches given
            if (sEvent.cancelled) {
                return false;
            }
        }
        if (path.switch_serverFlagged != null) {
            for (String flag : path.switch_serverFlagged) {
                if (flag.startsWith("!")) {
                    if (DenizenCore.serverFlagMap.hasFlag(flag.substring(1))) {
                        return false;
                    }
                }
                else if (!DenizenCore.serverFlagMap.hasFlag(flag)) {
                    return false;
                }
            }
        }
        if (path.switch_chance != 0) {
            if (CoreUtilities.getRandom().nextDouble() * 100 > path.switch_chance) {
                return false;
            }
        }
        for (BiFunction<ScriptEvent, ScriptPath, Boolean> matcher : extraMatchers) {
            if (!matcher.apply(sEvent, path)) {
                return false;
            }
        }
        return sEvent.matches(path);
    }

    /**
     * Additional basic matchers added for all events to use.
     */
    public static List<BiFunction<ScriptEvent, ScriptPath, Boolean>> extraMatchers = new ArrayList<>();

    // <--[language]
    // @name Script Event Priority
    // @group Script Events
    // @description
    // Any modern ScriptEvent can take a "priority:#" argument.
    // For example: "on object does something priority:3:"
    // The priority indicates which order the events will fire in.
    // Lower numbers fire earlier. EG, -1 fires before 0 fires before 1.
    // Any integer number, within reason, is valid. (IE, -1 is fine, 100000 is fine,
    // but 200000000000 is not, and 1.5 is not as well)
    // The default priority is 0.
    // -->
    public void sort() {
        try {
            for (ScriptPath path : eventPaths) {
                String gotten = path.switches.get("priority");
                path.priority = gotten == null ? 0 : Integer.parseInt(gotten);
            }
        }
        catch (NumberFormatException ex) {
            Debug.echoError("Failed to sort events: not-a-number priority value! " + ex.getMessage());
        }
        eventPaths.sort((scriptPath, t1) -> {
            int rel = scriptPath.priority - t1.priority;
            return Integer.compare(rel, 0);
        });
    }

    public void init() {
    }

    public void destroy() {
    }

    public void cancellationChanged() {
    }

    @Deprecated
    public static boolean isDefaultDetermination(ObjectTag determination) {
        Debug.echoError("VERSION MISMATCH, UPDATE ALL DENIZEN-RELATED SUBPROJECTS.");
        return false;
    }

    public boolean applyDetermination(ScriptPath path, ObjectTag determination) {
        Debug.echoError("Unknown determination '" + determination + "'");
        return false;
    }

    public boolean handleBaseDetermination(ScriptPath path, ObjectTag determination) {
        if (determination instanceof ElementTag) {
            String text = determination.toString();
            if (text.length() <= "cancelled:false".length()) {
                String low = CoreUtilities.toLowerCase(text);
                switch (low) {
                    case "cancelled:true":
                    case "cancelled":
                        Debug.echoDebug(path.container, "Event cancelled!");
                        cancelled = true;
                        cancellationChanged();
                        return true;
                    case "cancelled:false":
                        Debug.echoDebug(path.container, "Event uncancelled!");
                        cancelled = false;
                        cancellationChanged();
                        return true;
                }
            }
        }
        return applyDetermination(path, determination);
    }

    public ScriptEntryData getScriptEntryData() {
        return DenizenCore.implementation.getEmptyScriptEntryData();
    }

    public final void registerSwitches(String... switches) {
        eventData.localSwitches.addAll(Arrays.asList(switches));
    }

    /**
     * Registers a new couldMatcher format for this event. Usually called by a constructor.
     */
    public final void registerCouldMatcher(String format) {
        int paren = format.indexOf('(');
        if (paren == -1) {
            ScriptEventCouldMatcher matcher = new ScriptEventCouldMatcher(format);
            eventData.couldMatchers.add(matcher);
            if (matcher.validators[0] instanceof ScriptEventCouldMatcher.StringBasedValidator) {
                String text = ((ScriptEventCouldMatcher.StringBasedValidator) matcher.validators[0]).word;
                ArrayList<ScriptEvent> list = couldMatchOptimizer.computeIfAbsent(text, k -> new ArrayList<>());
                if (!list.contains(this)) {
                    list.add(this);
                }
            }
            else {
                eventData.needsLegacy = true;
            }
            return;
        }
        int endParen = format.indexOf(')', paren);
        if (endParen == -1) {
            Debug.echoError("Invalid couldMatcher registration '" + format + "': inconsistent parens");
            return;
        }
        String base = paren == 0 ? "" : format.substring(0, paren - 1);
        String afterText = endParen + 2 >= format.length() ? "" : format.substring(endParen + 2);
        String optional = format.substring(paren + 1, endParen);
        registerCouldMatcher(base + (afterText.isEmpty() || base.isEmpty() ? afterText : (" " + afterText)));
        registerCouldMatcher((base.isEmpty() ? "" : (base + " ")) + optional + (afterText.isEmpty() ? "" : (" " + afterText)));
    }

    private static ScriptEventCouldMatcher currentCouldMatcher = null;

    // <--[data]
    // @name global_switches
    // @values cancelled, ignorecancelled, priority, server_flagged, in, chance
    // -->

    /**
     * Switches that are globally available.
     */
    public static HashSet<String> globalSwitches = new HashSet<>(Arrays.asList("cancelled", "ignorecancelled", "priority", "server_flagged", "in", "chance"));

    private boolean couldMatchSwitches(ScriptPath path) {
        for (String switchName : path.switches.keySet()) {
            if (!globalSwitches.contains(switchName) && !eventData.localSwitches.contains(switchName)) {
                ScriptEvent.addPossibleCouldMatchFailReason("unrecognized switch name", switchName);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if the event could possibly match the given path (for init/loading).
     */
    public boolean couldMatch(ScriptPath path) {
        if (eventData.couldMatchers.isEmpty()) {
            throw new UnsupportedOperationException("CouldMatch not implemented for event '" + getName() + "'! Report this error to the Denizen developers!");
        }
        try {
            for (ScriptEventCouldMatcher matcher : eventData.couldMatchers) {
                currentCouldMatcher = matcher;
                if (matcher.doesMatch(path)) {
                    return couldMatchSwitches(path);
                }
            }
        }
        finally {
            currentCouldMatcher = null;
        }
        return false;
    }

    /**
     * Returns true if the current state of the event matches the specific data within the path.
     */
    public boolean matches(ScriptPath path) {
        return true;
    }

    /**
     * Gets the name of the event class.
     */
    public String getName() {
        return eventData.name;
    }

    /**
     * Makes a copy of this event object, fires it, and returns the copy.
     */
    public ScriptEvent fire() {
        ScriptEvent copy = clone();
        eventData.stats_fires++;
        for (ScriptPath path : eventPaths) {
            try {
                if (matchesScript(copy, path)) {
                    if (path.fireAfter) {
                        final ScriptPath finalPath = path;
                        DenizenCore.schedule(new OneTimeSchedulable(() -> copy.run(finalPath), 0.01f));
                    }
                    else {
                        copy.run(path);
                    }
                }
            }
            catch (Exception e) {
                Debug.echoError("Matching script " + path.container.getName() + " event path:" + path.event + ":::");
                Debug.echoError(e);
            }
        }
        return copy;
    }

    public void run(ScriptPath path) {
        try {
            eventData.stats_scriptFires++;
            if (path.container.shouldDebug()) {
                Debug.echoDebug(path.container, "<Y>Running script event '<A>" + getName() + "<Y>', event='<A>" + (path.fireAfter ? "after " : "on ") + path.event + "<Y>'"
                        + " for script '<A>" + path.container.getName() + "<Y>'");
            }
            if (path.set == null) {
                return;
            }
            List<ScriptEntry> entries = ScriptContainer.cleanDup(getScriptEntryData(), path.set);
            ScriptQueue queue = new InstantQueue(path.container.getName());
            queue.addEntries(entries);
            queue.setContextSource(this);
            if (!path.fireAfter) {
                queue.determinationTarget = (o) -> handleBaseDetermination(path, o);
            }
            queue.start(true);
            eventData.stats_nanoTimes += System.nanoTime() - queue.startTime;
        }
        catch (Exception e) {
            Debug.echoError("Handling script " + path.container.getName() + " path:" + path.event + ":::");
            Debug.echoError(e);
        }
    }

    public TagContext getTagContext(ScriptPath path) {
        TagContext context = getScriptEntryData().getTagContext().clone();
        context.script = new ScriptTag(path.container);
        context.debug = path.container.shouldDebug();
        return context;
    }

    // <--[language]
    // @name Script Event Special Contexts
    // @group Script Events
    // @description
    // Every modern ScriptEvent has some special context tags available.
    // The most noteworthy is "context.cancelled", which tracks whether the script event has been cancelled.
    // That returns, for example, "on player breaks stone".
    // The context 'reflect_event' is available in some events (eg Bukkit events) to get a JavaReflectedObjectTag of the raw internal event.
    // -->

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "cancelled":
                return new ElementTag(cancelled);
            case "event_name": // Intentionally undocumented, can be removed without harm
                return new ElementTag(getName());
        }
        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    // <--[language]
    // @name Advanced Object Matching
    // @group Object System
    // @description
    // Script event lines often include specific 'matchable' keywords.
    // For example, while you can write "on player breaks block:" as a script event line,
    // you can also instead write "on player breaks stone:" to listen to a much more specific event.
    // This is general in-line matching.
    // This is made available to avoid needing to do things like "- if <context.material.name> == stone"
    // just to validate whether an event is even relevant to you.
    //
    // Of course, there are times when you want to more than one specific thing to be handled by the event, so what do you do?
    // The Denizen script event system provides a few 'advanced' options to get more detailed matching.
    //
    // One option is to use wildcards.
    // For example, there are several 'log' materials, such as 'oak_log', 'birch_log', and more for the rest of the tree types.
    // So how can you match a player breaking any of these? Use "on player breaks *_log:"
    // The asterisk is a generic wildcard, it means any text at all will match. So an asterisk followed by '_log' means
    // any material at all that has a name ending with '_log', including 'birch_log' and the rest.
    //
    // Note that you can also use multiple wildcards at once, like "on player breaks block with:my_*_script_*:"
    // That example will work for item scripts named "my_item_script_1" and "my_first_script_of_items" or any similar name.
    // Note also that wildcards still match for blanks, so "my_item_script_" would still work for that example.
    //
    // You can also specify lists. For example, if you want an event to work with certain tool types,
    // the 'on player breaks block:' event supports a switch named 'with', like 'on player breaks block with:iron_pickaxe:'
    // So lets match multiple tools for our event...
    // 'on player breaks block with:iron_pickaxe|gold_pickaxe|diamond_axe|wooden_shovel:'
    //
    // You can also combine wildcards and lists... note that lists are the 'wider' option.
    // That is, if you have wildcards and lists together, you will have a list of possible matches, where each entry may contain wildcards, you will not have a wildcard match with a list.
    // As a specific example,
    // '*_pickaxe|*_axe' will match any pickaxe or any axe.
    // '*_pickaxe|stone' will match any pickaxe or specifically stone. It will NOT match other types of stone, as it interprets
    // the match to be a list of "*_pickaxe" and "stone", NOT "*" followed by a list of "pickaxe" or "stone".
    //
    // Additionally, when you're really desperate for a good matcher, you may use 'regex:'
    // For example, "on player breaks regex:(?i)\d+_customitem:"
    // Note that generally regex should be avoided whenever you can, as it's inherently hard to track exactly what it's doing at-a-glance, and may have unexpected edge case errors.
    //
    // If you want to match anything *except* a specific value, just prefix the matcher with '!'
    // For example, on player breaks !stone:" will fire for a player breaking any block type OTHER THAN stone.
    // This can be combined with other match modes, like "on player breaks !*wood|*planks|*log:" will fire for any block break other than any wood variant.
    //
    // Object types have their own special supported matchable inputs, refer to <@link language Advanced Object Matchables>.
    //
    // These advanced matchers are also used in some commands and tags, such as <@link tag ObjectTag.advanced_matches>, or in <@link command if> with the 'matches' operator.
    // -->

    /**
     * Entry point of advanced matching tools, refer to 'Advanced Object Matching' meta docs.
     */
    public static abstract class MatchHelper {

        public abstract boolean doesMatch(String input);

        @FunctionalInterface
        public interface ExactCheckerInterface {
            boolean check(String text);
        }

        public boolean doesMatch(String input, ExactCheckerInterface exactChecker) {
            return doesMatch(input);
        }
    }

    public static class AlwaysMatchHelper extends MatchHelper {

        @Override
        public boolean doesMatch(String input) {
            return true;
        }
    }

    public static class ExactMatchHelper extends MatchHelper {

        public ExactMatchHelper(String text) {
            this.text = CoreUtilities.toLowerCase(text);
        }

        public String text;

        @Override
        public boolean doesMatch(String input) {
            return CoreUtilities.equalsIgnoreCase(text, input);
        }

        @Override
        public boolean doesMatch(String input, ExactCheckerInterface exactChecker) {
            return CoreUtilities.equalsIgnoreCase(text, input) || exactChecker.check(text);
        }
    }

    public static class PrefixAsteriskMatchHelper extends MatchHelper {

        public PrefixAsteriskMatchHelper(String text) {
            this.text = CoreUtilities.toLowerCase(text);
        }

        public String text;

        @Override
        public boolean doesMatch(String input) {
            return CoreUtilities.toLowerCase(input).endsWith(text);
        }
    }

    public static class PostfixAsteriskMatchHelper extends MatchHelper {

        public PostfixAsteriskMatchHelper(String text) {
            this.text = CoreUtilities.toLowerCase(text);
        }

        public String text;

        @Override
        public boolean doesMatch(String input) {
            return CoreUtilities.toLowerCase(input).startsWith(text);
        }
    }

    public static class MultipleAsteriskMatchHelper extends MatchHelper {

        public MultipleAsteriskMatchHelper(String[] texts) {
            this.texts = texts;
        }

        public String[] texts;

        @Override
        public boolean doesMatch(String input) {
            int index = 0;
            input = CoreUtilities.toLowerCase(input);
            if (!input.startsWith(texts[0]) || !input.endsWith(texts[texts.length - 1])) {
                return false;
            }
            for (String text : texts) {
                if (text.isEmpty()) {
                    continue;
                }
                index = input.indexOf(text, index);
                if (index == -1) {
                    return false;
                }
                index += text.length();
            }
            return true;
        }
    }

    public static class RegexMatchHelper extends MatchHelper {

        public RegexMatchHelper(String regex) {
            this.regex = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        }

        public Pattern regex;

        @Override
        public boolean doesMatch(String input) {
            return regex.matcher(input).matches();
        }
    }

    public static class MultipleMatchesHelper extends MatchHelper {

        public MultipleMatchesHelper(MatchHelper[] matches) {
            this.matches = matches;
        }

        public MatchHelper[] matches;

        @Override
        public boolean doesMatch(String input) {
            for (MatchHelper match : matches) {
                if (match.doesMatch(input)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean doesMatch(String input, ExactCheckerInterface checker) {
            for (MatchHelper match : matches) {
                if (match.doesMatch(input, checker)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class InverseMatchHelper extends MatchHelper {

        public InverseMatchHelper(MatchHelper matcher) {
            this.matcher = matcher;
        }

        public MatchHelper matcher;

        @Override
        public boolean doesMatch(String input) {
            return !matcher.doesMatch(input);
        }

        @Override
        public boolean doesMatch(String input, ExactCheckerInterface checker) {
            return !matcher.doesMatch(input, checker);
        }
    }

    public static final HashMap<String, MatchHelper> knownMatchers = new HashMap<>();

    public static boolean isAdvancedMatchable(String input) {
        return input.startsWith("regex:") || CoreUtilities.contains(input, '|') || CoreUtilities.contains(input, '*') || input.startsWith("!");
    }

    public static MatchHelper createMatcher(String input) {
        MatchHelper result = knownMatchers.get(input);
        if (result != null) {
            return result;
        }
        int asterisk;
        if (input.startsWith("!")) {
            result = new InverseMatchHelper(createMatcher(input.substring(1)));
        }
        else if (input.startsWith("regex:")) {
            result = new RegexMatchHelper(input.substring("regex:".length()));
        }
        else if (CoreUtilities.contains(input, '|')) {
            String toSplit = input;
            if (toSplit.startsWith("li@")) {
                toSplit = toSplit.substring("li@".length());
            }
            toSplit = CoreUtilities.replace(toSplit, "el@", "");
            List<String> split = CoreUtilities.split(toSplit, '|');
            MatchHelper[] matchers = new MatchHelper[split.size()];
            for (int i = 0; i < split.size(); i++) {
                matchers[i] = createMatcher(split.get(i));
            }
            result = new MultipleMatchesHelper(matchers);
        }
        else if ((asterisk = input.indexOf('*')) != -1) {
            if (input.length() == 1) {
                result = new AlwaysMatchHelper();
            }
            else if (asterisk == 0 && input.indexOf('*', 1) == -1) {
                result = new PrefixAsteriskMatchHelper(input.substring(1));
            }
            else if (asterisk == input.length() - 1) {
                result = new PostfixAsteriskMatchHelper(input.substring(0, input.length() - 1));
            }
            else {
                result = new MultipleAsteriskMatchHelper(CoreUtilities.split(CoreUtilities.toLowerCase(input), '*').toArray(new String[0]));
            }
        }
        else {
            result = new ExactMatchHelper(input);
        }
        knownMatchers.put(input, result);
        return result;
    }

    public static boolean runGenericCheck(String matchableValue, String trueValue) {
        if (matchableValue == null) {
            return false;
        }
        trueValue = CoreUtilities.toLowerCase(trueValue);
        MatchHelper matcher = createMatcher(matchableValue);
        return matcher.doesMatch(trueValue);
    }

    public static boolean runGenericSwitchCheck(ScriptPath path, String switchName, String value) {
        String with = path.switches.get(switchName);
        if (with == null) {
            return true;
        }
        if (value == null) {
            return false;
        }
        value = CoreUtilities.toLowerCase(value);
        MatchHelper matcher = createMatcher(with);
        return matcher.doesMatch(value);
    }

    public static boolean coreFlaggedCheck(String flagged, AbstractFlagTracker tracker) {
        if (flagged == null) {
            return true;
        }
        if (tracker == null) {
            return false;
        }
        for (String flag : CoreUtilities.split(flagged, '|')) {
            if (flag.startsWith("!")) {
                if (tracker.hasFlag(flag.substring(1))) {
                    return false;
                }
            }
            else if (!tracker.hasFlag(flag)) {
                return false;
            }
        }
        return true;
    }
}
