package net.aufdemrand.denizencore.events;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.core.ReloadScriptsScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ScriptEvent {

    public static void registerCoreEvents() {
        registerScriptEvent(new ReloadScriptsScriptEvent());
    }

    public static void registerScriptEvent(ScriptEvent event) {
        event.reset();
        events.add(event);
    }

    public static class ScriptPath {

        ScriptContainer container;
        String event;

        public ScriptPath(ScriptContainer container, String event) {
            this.container = container;
            this.event = event;
        }
    }

    public static ArrayList<ScriptContainer> worldContainers = new ArrayList<ScriptContainer>();

    public static ArrayList<ScriptEvent> events = new ArrayList<ScriptEvent>();

    public static void reload() {
        dB.log("Reloading script events...");
        for (ScriptEvent event: events) {
            event.destroy();
            event.eventPaths.clear();
            boolean matched = false;
            for (ScriptContainer container: worldContainers) {
                for (String evt: container.getConfigurationSection("events").getKeys(false)) {
                    evt = evt.substring(3);
                    if (couldMatchScript(event, container, evt)) {
                        event.eventPaths.add(new ScriptPath(container, evt));
                        dB.log("Event match, " + event.getName() + " matched for '" + evt + "'!");
                        matched = true;
                    }
                }
            }
            if (matched) {
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
        List<String> determinations = DetermineCommand.getOutcome(id);
        if (determinations != null) {
            for (String determination : determinations) {
                applyDetermination(script, determination);
            }
        }
    }
}
