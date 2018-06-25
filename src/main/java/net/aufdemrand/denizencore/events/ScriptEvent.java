package net.aufdemrand.denizencore.events;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.core.ReloadScriptsScriptEvent;
import net.aufdemrand.denizencore.events.core.SystemTimeScriptEvent;
import net.aufdemrand.denizencore.interfaces.ContextSource;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.ScriptEntrySet;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.text.StringHolder;

import java.util.*;

public abstract class ScriptEvent implements ContextSource, Cloneable {

    @Override
    public ScriptEvent clone() {
        try {
            return (ScriptEvent) super.clone();
        }
        catch (CloneNotSupportedException e) {
            dB.echoError("Clone not supported for script events?!");
            return this;
        }
    }

    public static void registerCoreEvents() {
        registerScriptEvent(new ReloadScriptsScriptEvent());
        registerScriptEvent(new SystemTimeScriptEvent());
    }

    public static void registerScriptEvent(ScriptEvent event) {
        event.reset();
        events.add(event);
    }

    public long fires = 0;
    public long scriptFires = 0;
    public long nanoTimes = 0;

    public static class ScriptPath {

        ScriptContainer container;
        String event;
        int priority = 0;
        ScriptEntrySet set;

        public ScriptPath(ScriptContainer container, String event) {
            this.container = container;
            this.event = event;
        }
    }

    public static ArrayList<ScriptContainer> worldContainers = new ArrayList<ScriptContainer>();

    public static ArrayList<ScriptEvent> events = new ArrayList<ScriptEvent>();

    public static void reload() {
        dB.log("Reloading script events...");
        for (ScriptContainer container : worldContainers) {
            if (!container.getContents().getString("enabled", "true").equalsIgnoreCase("true")) {
                continue;
            }
            YamlConfiguration config = container.getConfigurationSection("events");
            if (config == null) {
                dB.echoError("Missing or invalid events block for " + container.getName());
                continue;
            }
            for (StringHolder evt : config.getKeys(false)) {
                if (evt == null || evt.str == null) {
                    dB.echoError("Missing or invalid events block for " + container.getName());
                }
                else if (evt.str.contains("@")) {
                    dB.echoError("Script '" + container.getName() + "' has event '" + evt.str.replace("@", "<R>@<W>")
                            + "' which contains object notation, which is deprecated for use in world events. Please remove it.");
                }
            }
        }
        for (ScriptEvent event : events) {
            event.destroy();
            event.eventPaths.clear();
            boolean matched = false;
            for (ScriptContainer container : worldContainers) {
                YamlConfiguration config = container.getConfigurationSection("events");
                if (config == null) {
                    continue;
                }
                for (StringHolder evt1 : config.getKeys(false)) {
                    String evt = evt1.str.substring(3);
                    if (couldMatchScript(event, container, evt)) {
                        event.eventPaths.add(new ScriptPath(container, evt));
                        dB.log("Event match, " + event.getName() + " matched for '" + evt + "'!");
                        matched = true;
                    }
                }
            }
            if (matched) {
                event.sort();
                event.init();
            }
        }
    }

    // <--[language]
    // @name script event cancellation
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
    public static boolean matchesScript(ScriptEvent sEvent, ScriptContainer script, String event) {
        String cancelmode = getSwitch(event, "cancelled");
        if (cancelmode != null && cancelmode.equalsIgnoreCase("false") && sEvent.cancelled) {
            return false;
        }
        if (cancelmode != null && cancelmode.equalsIgnoreCase("true") && !sEvent.cancelled) {
            return false;
        }
        if (checkSwitch(event, "ignorecancelled", "false") && sEvent.cancelled) {
            return false;
        }
        return sEvent.matches(script, event);
    }

    public static boolean couldMatchScript(ScriptEvent sEvent, ScriptContainer script, String event) {
        return sEvent.couldMatch(script, event);
    }

    public ArrayList<ScriptPath> eventPaths = new ArrayList<ScriptPath>();

    public boolean cancelled = false;

    // <--[language]
    // @name script event priority
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
            String gotten = getSwitch(path.event, "priority");
            path.priority = gotten == null ? 0 : aH.getIntegerFrom(gotten);
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

    public static boolean checkSwitch(String event, String switcher, String value) {
        for (String possible : CoreUtilities.split(event, ' ')) {
            List<String> split = CoreUtilities.split(possible, ':', 2);
            if (split.get(0).equalsIgnoreCase(switcher) && split.size() > 1 && !split.get(1).equalsIgnoreCase(value)) {
                return false;
            }
        }
        return true;
    }

    public static String getSwitch(String event, String switcher) {
        for (String possible : CoreUtilities.split(event, ' ')) {
            List<String> split = CoreUtilities.split(possible, ':', 2);
            if (split.get(0).equalsIgnoreCase(switcher) && split.size() > 1) {
                return split.get(1);
            }
        }
        return null;
    }

    public boolean applyDetermination(ScriptContainer container, dObject determination) {
        return applyDetermination(container, determination.identify());
    }

    public boolean applyDetermination(ScriptContainer container, String determination) {
        String low = CoreUtilities.toLowerCase(determination);
        if (low.equals("cancelled")) {
            dB.echoDebug(container, "Event cancelled!");
            cancelled = true;
            return true;
        }
        else if (low.equals("cancelled:true")) {
            dB.echoDebug(container, "Event cancelled!");
            cancelled = true;
            return true;
        }
        else if (low.equals("cancelled:false")) {
            dB.echoDebug(container, "Event uncancelled!");
            cancelled = false;
            return true;
        }
        else {
            dB.echoError("Unknown determination '" + determination + "'");
            return false;
        }
    }

    public HashMap<String, dObject> getContext() { // TODO: Delete
        return new HashMap<String, dObject>();
    }

    public ScriptEntryData getScriptEntryData() {
        return DenizenCore.getImplementation().getEmptyScriptEntryData();
    }

    public abstract boolean couldMatch(ScriptContainer script, String event);

    public abstract boolean matches(ScriptContainer script, String event);

    public abstract String getName();

    public void reset() {
        cancelled = false;
    }

    public void fire() {
        fires++;
        ScriptEvent dupd = null;
        for (ScriptPath path : eventPaths) {
            try {
                if (matchesScript(this, path.container, path.event)) {
                    if (dupd == null) {
                        dupd = clone();
                    }
                    dupd.run(path.container, path.event);
                }
            }
            catch (Exception e) {
                dB.echoError("Handling script " + path.container.getName() + " path:" + path.event + ":::");
                dB.echoError(e);
            }
        }
    }

    private String currentEvent;

    public void run(ScriptContainer script, String event) throws CloneNotSupportedException {
    }

    public void run(ScriptPath path) {
        scriptFires++;
        HashMap<String, dObject> context = getContext();
        if (path.container.shouldDebug()) {
            dB.echoDebug(path.container, "<Y>Running script event '<A>" + getName() + "<Y>', event='<A>" + path.event + "<Y>'"
                    + " for script '<A>" + path.container.getName() + "<Y>'");
            for (Map.Entry<String, dObject> obj : context.entrySet()) {
                dB.echoDebug(path.container, "<Y>Context '<A>" + obj.getKey() + "<Y>' = '<A>" + obj.getValue().identify() + "<Y>'");
            }
        }
        if (path.set == null) {
            path.set = path.container.getSetFor("events.on " + path.event);
        }
        List<ScriptEntry> entries = ScriptContainer.cleanDup(getScriptEntryData(), path.set);
        long id = DetermineCommand.getNewId();
        ScriptBuilder.addObjectToEntries(entries, "reqid", id);
        ScriptQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId(path.container.getName())).addEntries(entries).setReqId(id);
        HashMap<String, dObject> oldStyleContext = getContext();
        currentEvent = path.event;
        if (oldStyleContext.size() > 0) {
            OldEventManager.OldEventContextSource oecs = new OldEventManager.OldEventContextSource();
            oecs.contexts = oldStyleContext;
            oecs.contexts.put("cancelled", new Element(cancelled));
            oecs.contexts.put("event_header", new Element(currentEvent));
            queue.setContextSource(oecs);
        }
        else {
            queue.setContextSource(this);
        }
        queue.start();
        nanoTimes += System.nanoTime() - queue.startTime;
        List<dObject> determinations = DetermineCommand.getOutcome(id).objectForms;
        if (determinations != null) {
            for (dObject determination : determinations) {
                applyDetermination(path.container, determination);
            }
        }
    }

    @Override
    public boolean getShouldCache() {
        return false;
    }

    @Override
    public dObject getContext(String name) {
        if (name.equals("cancelled")) {
            return new Element(cancelled);
        }
        else if (name.equals("event_header")) {
            return new Element(currentEvent);
        }
        return null;
    }
}
