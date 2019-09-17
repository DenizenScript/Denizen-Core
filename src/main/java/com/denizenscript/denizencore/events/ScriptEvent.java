package com.denizenscript.denizencore.events;

import com.denizenscript.denizencore.events.core.*;
import com.denizenscript.denizencore.scripts.containers.core.WorldScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.ScriptEntrySet;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.denizenscript.denizencore.DenizenCore;

import java.util.*;
import java.util.regex.Pattern;

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

    public static void registerCoreEvents() {
        registerScriptEvent(new ConsoleOutputScriptEvent());
        registerScriptEvent(new DeltaTimeScriptEvent());
        registerScriptEvent(new ReloadScriptsScriptEvent());
        registerScriptEvent(new SystemTimeScriptEvent());
        registerScriptEvent(new TickScriptEvent());
    }

    public static void registerScriptEvent(ScriptEvent event) {
        event.reset();
        events.add(event);
        eventLookup.put(CoreUtilities.toLowerCase(event.getName()), event);
    }

    public static ArrayList<WorldScriptContainer> worldContainers = new ArrayList<>();

    public static ArrayList<ScriptEvent> events = new ArrayList<>();

    public static HashMap<String, ScriptEvent> eventLookup = new HashMap<>();

    public static class StatData {
        public long fires = 0;
        public long scriptFires = 0;
        public long nanoTimes = 0;
    }

    public StatData stats = new StatData();

    public static class ScriptPath {

        public ScriptContainer container;
        public String event;
        public String eventLower;
        public int priority = 0;
        public ScriptEntrySet set;
        public Boolean switch_cancelled;
        public Boolean switch_ignoreCancelled;
        public HashMap<String, String> switches = new HashMap<>();
        public String[] eventArgs;
        public String[] eventArgsLower;
        public String[] rawEventArgs;
        public int matches = 0;

        public String rawEventArgAt(int index) {
            return index < rawEventArgs.length ? rawEventArgs[index] : "";
        }

        public String eventArgAt(int index) {
            return index < eventArgs.length ? eventArgs[index] : "";
        }

        public String eventArgLowerAt(int index) {
            return index < eventArgsLower.length ? eventArgsLower[index] : "";
        }

        // <--[language]
        // @name Script Event Switches
        // @group Script Events
        // @description
        // Modern script events support the concept of 'switches'.
        // A switch is a specification of additional requirements in an event line other than what's in the event label it.
        //
        // A switch consists of a name and a value input, and are can be added anywhere in an event line as "name:<value>"
        // For example, "on delta time secondly every:5:" is a valid event, where "delta time secondly" is the event itself,
        // and "every:<#>" is a switch available to the event.
        //
        // A traditional Denizen 1 event might look like "on <entity> damaged",
        // where "<entity>" can be filled with "entity" or any entity type (like "player").
        // A switch-using event would instead take the format "on entity damaged" with switch "type:<entity type>"
        // meaning you can do "on entity damaged" for any entity, or "on entity damaged type:player:" for players specifically.
        // This is both more efficient to process and more explicit in what's going on, however it is less
        // clear/readable to the average user, so it is not often used.
        // Some events may have switches for less-often specified data, and use the event line for other options.
        // -->

        public boolean checkSwitch(String key, String value) {
            String pathValue = switches.get(key);
            if (pathValue == null) {
                return true;
            }
            return CoreUtilities.toLowerCase(pathValue).equals(value);
        }

        public ScriptPath(ScriptContainer container, String event) {
            this.event = event;
            rawEventArgs = CoreUtilities.split(event, ' ').toArray(new String[0]);
            this.container = container;
            List<String> eventLabel = new ArrayList<>();
            for (String possible : CoreUtilities.split(event, ' ').toArray(new String[0])) {
                List<String> split = CoreUtilities.split(possible, ':', 2);
                if (split.size() > 1 && !split.get(0).equalsIgnoreCase("regex")) {
                    switches.put(CoreUtilities.toLowerCase(split.get(0)), split.get(1));
                }
                else {
                    eventLabel.add(possible);
                }
            }
            eventLower = CoreUtilities.toLowerCase(String.join(" ", eventLabel));
            eventArgs = eventLabel.toArray(new String[0]);
            eventArgsLower = CoreUtilities.split(eventLower, ' ').toArray(new String[0]);
            switch_cancelled = switches.containsKey("cancelled") ? switches.get("cancelled").equalsIgnoreCase("true") : null;
            switch_ignoreCancelled = switches.containsKey("ignorecancelled") ? switches.get("ignorecancelled").equalsIgnoreCase("true") : null;
        }

        @Override
        public String toString() {
            return container.getName() + ".events.on " + event;
        }
    }

    public static void reload() {
        Debug.log("Reloading script events...");
        for (ScriptContainer container : worldContainers) {
            if (!container.getContents().getString("enabled", "true").equalsIgnoreCase("true")) {
                continue;
            }
            YamlConfiguration config = container.getConfigurationSection("events");
            if (config == null) {
                Debug.echoError("Missing or invalid events block for " + container.getName());
                continue;
            }
            for (StringHolder evt : config.getKeys(false)) {
                if (evt == null || evt.str == null) {
                    Debug.echoError("Missing or invalid events block for " + container.getName());
                }
                else if (evt.str.contains("@")) {
                    Debug.echoError("Script '" + container.getName() + "' has event '" + evt.str.replace("@", "<R>@<W>")
                            + "' which contains object notation, which is deprecated for use in world events. Please remove it.");
                }
            }
        }
        List<ScriptPath> paths = new ArrayList<>(worldContainers.size() * 3);
        for (ScriptContainer container : worldContainers) {
            YamlConfiguration config = container.getConfigurationSection("events");
            if (config == null) {
                continue;
            }
            for (StringHolder evt1 : config.getKeys(false)) {
                String evt = evt1.str.substring(3);
                paths.add(new ScriptPath(container, evt));
            }
        }
        for (ScriptEvent event : events) {
            try {
                event.destroy();
                event.eventPaths.clear();
                boolean matched = false;
                for (ScriptPath path : paths) {
                    if (event.couldMatch(path)) {
                        event.eventPaths.add(path);
                        path.matches++;
                        Debug.log("Event match, " + event.getName() + " matched for '" + path + "'!");
                        matched = true;
                    }
                }
                if (matched) {
                    event.sort();
                    event.init();
                }
            }
            catch (Throwable ex) {
                Debug.echoError("Failed to reload event '" + event.getName() + "':");
                Debug.echoError(ex);
            }
        }
        for (ScriptPath path : paths) {
            if (path.matches > 1) {
                Debug.log("Event " + path + " is matched to multiple ScriptEvents.");
            }
            else if (path.matches == 0) {
                Debug.log("Event " + path + " is not matched to any ScriptEvents.");
            }
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
        return sEvent.matches(path);
    }

    public ArrayList<ScriptPath> eventPaths = new ArrayList<>();

    public boolean cancelled = false;

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
        for (ScriptPath path : eventPaths) {
            String gotten = path.switches.get("priority");
            path.priority = gotten == null ? 0 : ArgumentHelper.getIntegerFrom(gotten);
        }
        Collections.sort(eventPaths, new Comparator<ScriptPath>() {
            @Override
            public int compare(ScriptPath scriptPath, ScriptPath t1) {
                int rel = scriptPath.priority - t1.priority;
                return rel < 0 ? -1 : (rel > 0 ? 1 : 0);
            }
        });
    }

    public void init() {
    }

    public void destroy() {
    }

    @Deprecated
    public static boolean checkSwitch(String event, String switcher, String value) {
        for (String possible : CoreUtilities.split(event, ' ')) {
            List<String> split = CoreUtilities.split(possible, ':', 2);
            if (split.get(0).equalsIgnoreCase(switcher) && split.size() > 1 && !split.get(1).equalsIgnoreCase(value)) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public static String getSwitch(String event, String switcher) {
        for (String possible : CoreUtilities.split(event, ' ')) {
            List<String> split = CoreUtilities.split(possible, ':', 2);
            if (split.get(0).equalsIgnoreCase(switcher) && split.size() > 1) {
                return split.get(1);
            }
        }
        return null;
    }

    public static HashSet<String> defaultDeterminations = new HashSet<>(Arrays.asList("cancelled", "cancelled:true", "cancelled:false"));

    public static boolean isDefaultDetermination(ObjectTag determination) {
        if (!(determination instanceof ElementTag)) {
            return false;
        }
        String low = CoreUtilities.toLowerCase(determination.toString());
        return defaultDeterminations.contains(low);
    }

    public boolean applyDetermination(ScriptPath path, ObjectTag determination) {
        String low = CoreUtilities.toLowerCase(determination.toString());
        if (low.equals("cancelled")) {
            Debug.echoDebug(path.container, "Event cancelled!");
            cancelled = true;
            return true;
        }
        else if (low.equals("cancelled:true")) {
            Debug.echoDebug(path.container, "Event cancelled!");
            cancelled = true;
            return true;
        }
        else if (low.equals("cancelled:false")) {
            Debug.echoDebug(path.container, "Event uncancelled!");
            cancelled = false;
            return true;
        }
        else {
            Debug.echoError("Unknown determination '" + determination + "'");
            return false;
        }
    }

    public HashMap<String, ObjectTag> getContext() { // TODO: Delete
        return new HashMap<>();
    }

    public ScriptEntryData getScriptEntryData() {
        return DenizenCore.getImplementation().getEmptyScriptEntryData();
    }

    public boolean couldMatch(ScriptPath path) {
        return couldMatch(path.container, path.event);
    }

    public boolean couldMatch(ScriptContainer script, String event) {
        return false;
    }

    public boolean matches(ScriptPath path) {
        return matches(path.container, path.event);
    }

    public boolean matches(ScriptContainer script, String event) {
        throw new UnsupportedOperationException("Matches not implemented for event '" + getName() + "'! Report this error to the Denizen developers!");
    }

    public abstract String getName();

    public void reset() {
        cancelled = false;
    }

    Runnable resetRunnable = new Runnable() {
        @Override
        public void run() {
            reset();
        }
    };

    public void fire() {
        stats.fires++;
        for (ScriptPath path : eventPaths) {
            try {
                if (matchesScript(this, path)) {
                    run(path);
                }
            }
            catch (Exception e) {
                Debug.echoError("Handling script " + path.container.getName() + " path:" + path.event + ":::");
                Debug.echoError(e);
            }
        }
        if (cancelled) {
            DenizenCore.schedule(new OneTimeSchedulable(resetRunnable, 0.01f));
        }
    }

    private String currentEvent;

    public void run(ScriptPath path) {
        stats.scriptFires++;
        HashMap<String, ObjectTag> context = getContext();
        if (path.container.shouldDebug()) {
            Debug.echoDebug(path.container, "<Y>Running script event '<A>" + getName() + "<Y>', event='<A>" + path.event + "<Y>'"
                    + " for script '<A>" + path.container.getName() + "<Y>'");
            for (Map.Entry<String, ObjectTag> obj : context.entrySet()) {
                Debug.echoDebug(path.container, "<Y>Context '<A>" + obj.getKey() + "<Y>' = '<A>" + obj.getValue().identify() + "<Y>'");
            }
        }
        if (path.set == null) {
            path.set = path.container.getSetFor("events.on " + path.event);
        }
        List<ScriptEntry> entries = ScriptContainer.cleanDup(getScriptEntryData(), path.set);
        ScriptQueue queue = new InstantQueue(path.container.getName()).addEntries(entries);
        HashMap<String, ObjectTag> oldStyleContext = getContext();
        currentEvent = path.event;
        if (oldStyleContext.size() > 0) {
            OldEventManager.OldEventContextSource oecs = new OldEventManager.OldEventContextSource();
            oecs.contexts = oldStyleContext;
            oecs.contexts.put("cancelled", new ElementTag(cancelled));
            oecs.contexts.put("event_header", new ElementTag(currentEvent));
            queue.setContextSource(oecs);
        }
        else {
            queue.setContextSource(this.clone());
        }
        queue.start();
        stats.nanoTimes += System.nanoTime() - queue.startTime;
        ListTag outList = queue.determinations;
        if (outList != null && !outList.isEmpty()) {
            List<ObjectTag> determinations = outList.objectForms;
            for (ObjectTag determination : determinations) {
                applyDetermination(path, determination);
            }
        }
    }

    @Override
    public boolean getShouldCache() {
        return false;
    }

    // <--[language]
    // @name Script Event Special Contexts
    // @group Script Events
    // @description
    // Every modern ScriptEvent has some special context tags available.
    // The most noteworthy is "context.cancelled", which tracks whether the script event has been cancelled.
    // You can also use "context.event_header", which returns the exact event header text that fired (which may be useful for some types of dynamic script).
    // That returns, for example, "on player breaks stone".
    // You can also use "context.event_name", which returns the internal name of the script event that fired (which may be useful for some debugging techniques).
    // That returns, for example, "PlayerBreaksBlock".
    // -->

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("cancelled")) {
            return new ElementTag(cancelled);
        }
        else if (name.equals("event_header")) {
            return new ElementTag(currentEvent);
        }
        else if (name.equals("event_name")) {
            return new ElementTag(getName());
        }
        return null;
    }

    // <--[language]
    // @name Advanced Script Event Matching
    // @group Script Events
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
    // You can also specify lists. For example, if you want an event to work with certain tool types,
    // the 'on player breaks block:' event supports a switch named 'with', like 'on player breaks block with:iron_pickaxe:'
    // So lets match multiple tools for our event...
    // 'on player breaks block with:iron_pickaxe|gold_pickaxe|diamond_axe|wood_shovel:'
    //
    // You can also combine wildcards and lists... note that lists are the 'wider' option.
    // That is, if you have wildcards and lists together, you will have a list of possible matches, where each entry
    // may contain wildcards. You do not have a a wildcard match with a list.
    // As a specific example,
    // '*_pickaxe|*_axe' will match any pickaxe or any axe.
    // '*_pickaxe|stone' will match any pickaxe or specifically stone. It will NOT match other types of stone, as it interprets
    // the match to be a list of "*_pickaxe" and "stone", NOT "*" followed by a list of "pickaxe" or "stone".
    //
    // Additionally, when you're really deseparate for a good matcher, you may use 'regex:'
    // For example, "on player breaks regex:(?i)\d+_customitem:"
    // Note that generally regex should be avoided whenever you can, as it's inherently hard to track exactly what it's doing at-a-glance.
    // -->

    public static final HashMap<String, Pattern> knownPatterns = new HashMap<>();

    public static String quotify(String input) {
        StringBuilder output = new StringBuilder(input.length());
        int last = 0;
        int index = input.indexOf('*');
        while (index >= 0) {
            output.append(Pattern.quote(input.substring(last, index))).append("(.*)");
            last = index + 1;
            index = input.indexOf('*', last);
        }
        output.append(Pattern.quote(input.substring(last)));
        return output.toString();
    }

    public static boolean isRegexMatchable(String input) {
        return input.startsWith("regex:") || input.contains("|") || input.contains("*");
    }

    public static Pattern regexHandle(String input) {
        Pattern result = knownPatterns.get(input);
        if (result != null) {
            return result;
        }
        String output;
        if (input.startsWith("regex:")) {
            output = input.substring("regex:".length());
        }
        else if (input.contains("|")) {
            String[] split = input.split("\\|");
            for (int i = 0; i < split.length; i++) {
                split[i] = quotify(split[i]);
            }
            output = String.join("|", split);
        }
        else if (input.contains("*")) {
            output = quotify(input);
        }
        else {
            return null;
        }
        if (Debug.verbose) {
            Debug.log("Event regex compile: " + output);
        }
        result = Pattern.compile(output);
        knownPatterns.put(input, result);
        return result;
    }

    public static boolean equalityCheck(String input, String compared, Pattern regexed) {
        input = CoreUtilities.toLowerCase(input);
        return input.equals(compared) || (regexed != null && regexed.matcher(input).matches());
    }
}
