package net.aufdemrand.denizencore.events;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.core.ReloadScriptsScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.text.StringHolder;

import java.util.*;

public abstract class ScriptEvent {

    public static void registerCoreEvents() {
        registerScriptEvent(new ReloadScriptsScriptEvent());
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

        public ScriptPath(ScriptContainer container, String event) {
            this.container = container;
            this.event = event;
        }
    }

    public static ArrayList<ScriptContainer> worldContainers = new ArrayList<ScriptContainer>();

    public static ArrayList<ScriptEvent> events = new ArrayList<ScriptEvent>();

    public static void reload() {
        dB.log("Reloading script events...");
        for (ScriptContainer container: worldContainers) {
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
        for (ScriptEvent event: events) {
            event.destroy();
            event.eventPaths.clear();
            boolean matched = false;
            for (ScriptContainer container: worldContainers) {
                for (StringHolder evt1: container.getConfigurationSection("events").getKeys(false)) {
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

    public static boolean matchesScript(ScriptEvent sEvent, ScriptContainer script, String event) {
        if (event.endsWith(" cancelled:false")) {
            if (sEvent.cancelled) {
                return false;
            }
            event = event.substring(0, event.length() - " cancelled:false".length());
        }
        if (event.endsWith(" cancelled:true")) {
            if (!sEvent.cancelled) {
                return false;
            }
            event = event.substring(0, event.length() - " cancelled:true".length());
        }
        return sEvent.matches(script, event);
    }

    public static boolean couldMatchScript(ScriptEvent sEvent, ScriptContainer script, String event) {
        if (event.endsWith(" cancelled:false")) {
            if (sEvent.cancelled) {
                return false;
            }
            event = event.substring(0, event.length() - "cancelled:false".length());
        }
        if (event.endsWith(" cancelled:true")) {
            if (!sEvent.cancelled) {
                return false;
            }
            event = event.substring(0, event.length() - "cancelled:true".length());
        }
        return sEvent.couldMatch(script, event);
    }

    public ArrayList<ScriptPath> eventPaths = new ArrayList<ScriptPath>();

    public boolean cancelled = false;

    // <--[language]
    // @name script event priority
    // @description
    // Any modern ScriptEvent can take a "priority:#" argument.
    // EG, "on object does something priority:3:"
    // The priority indicates which order the events will fire in.
    // Lower numbers fire earlier. EG, -1 fires before 0 fires before 1.
    // Any integer number, within reason, is valid. (IE, -1 is fine, 100000 is fine,
    // but 200000000000 is not, and 1.5 is not as well)
    // The default priority is 0.
    // -->
    public void sort() {
        for (ScriptPath path: eventPaths) {
            String gotten = getSwitch(path.event, "priority");
            path.priority = gotten == null ? 0: aH.getIntegerFrom(gotten);
        }
        Collections.sort(eventPaths, new Comparator<ScriptPath>() {
            @Override
            public int compare(ScriptPath scriptPath, ScriptPath t1) {
                int rel = scriptPath.priority - t1.priority;
                return rel < 0 ? -1: (rel > 0 ? 1: 0);
            }
        });
    }

    public void init() {
    }

    public void destroy() {
    }

    public boolean checkSwitch(String event, String switcher, String value) {
        for (String possible: CoreUtilities.split(event, ' ')) {
            List<String> split = CoreUtilities.split(possible, ':', 2);
            if (dB.verbose) dB.log("TEST: " + split.size() + ", " + split.get(0) + " && "
                    + (split.size() > 1 ? split.get(1): "") + " comp " + switcher + ":" + value);
            if (split.get(0).equalsIgnoreCase(switcher) && split.size() > 1 && !split.get(1).equalsIgnoreCase(value)) {
                return false;
            }
        }
        return true;
    }

    public String getSwitch(String event, String switcher) {
        for (String possible: CoreUtilities.split(event, ' ')) {
            List<String> split = CoreUtilities.split(possible, ':', 2);
            if (split.get(0).equalsIgnoreCase(switcher) && split.size() > 1) {
                return split.get(1);
            }
        }
        return null;
    }

    public boolean applyDetermination(ScriptContainer container, String determination) {
        if (determination.equalsIgnoreCase("CANCELLED")) {
            dB.echoDebug(container, "Event cancelled!");
            cancelled = true;
        }
        else if (determination.equalsIgnoreCase("CANCELLED:FALSE")) {
            dB.echoDebug(container, "Event uncancelled!");
            cancelled = false;
        }
        else {
            dB.echoError("Unknown determination '" + determination + "'");
            return false;
        }
        return true;
    }

    public HashMap<String, dObject> getContext() {
        HashMap<String, dObject> context = new HashMap<String, dObject>();
        context.put("cancelled", new Element(cancelled));
        return context;
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
        for (ScriptPath path: eventPaths) {
            if (matchesScript(this, path.container, path.event)) {
                try {
                    run(path.container, path.event);
                }
                catch (Exception e) {
                    dB.echoError(e);
                }
            }
        }
    }

    public void run(ScriptContainer script, String event) {
        scriptFires++;
        HashMap<String, dObject> context = getContext();
        dB.echoDebug(script, "<Y>Running script event '<A>" + getName() + "<Y>', event='<A>" + event + "<Y>'"
                + " for script '<A>" + script.getName() + "<Y>'");
        for (Map.Entry<String, dObject> obj: context.entrySet()) {
            dB.echoDebug(script, "<Y>Context '<A>" + obj.getKey() + "<Y>' = '<A>" + obj.getValue().identify() + "<Y>'");
        }
        List<ScriptEntry> entries = script.getEntries(getScriptEntryData(), "events.on " + event);
        long id = DetermineCommand.getNewId();
        ScriptBuilder.addObjectToEntries(entries, "ReqId", id);
        ScriptQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId(script.getName())).addEntries(entries).setReqId(id);
        for (Map.Entry<String, dObject> entry : context.entrySet()) {
            queue.addContext(entry.getKey(), entry.getValue());
        }
        queue.start();
        nanoTimes += System.nanoTime() - queue.startTime;
        List<String> determinations = DetermineCommand.getOutcome(id);
        if (determinations != null) {
            for (String determination : determinations) {
                applyDetermination(script, determination);
            }
        }
    }
}
