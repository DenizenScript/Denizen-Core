package net.aufdemrand.denizencore.events;

import net.aufdemrand.denizencore.interfaces.ContextSource;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.containers.core.WorldScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.debugging.dB.DebugElement;
import net.aufdemrand.denizencore.utilities.text.StringHolder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OldEventManager {

    ///////////////////
    //  MAPS
    ////////////


    // Map for keeping the WorldScriptContainers
    public static Map<String, WorldScriptContainer> world_scripts =
            new ConcurrentHashMap<String, WorldScriptContainer>(8, 0.9f, 1);

    // Map for keeping the names of events
    public static Map<String, List<WorldScriptContainer>> events =
            new HashMap<String, List<WorldScriptContainer>>();

    // Map for keeping track of registered smart_events
    public static Set<OldSmartEvent> smart_events = new HashSet<OldSmartEvent>();

    //////////////////
    // PERFORMANCE
    ///////////


    public static void scanWorldEvents() {
        try {
            // Build a Map of scripts keyed by 'world events name'.

            // Loop through each world script
            dB.log("Scanning " + world_scripts.size() + " world scripts...");
            for (WorldScriptContainer script : world_scripts.values()) {
                if (script == null) {
                    dB.echoError("Null world script?!");
                    continue;
                }

                // ...and through each event inside the script.
                if (script.contains("EVENTS")) {
                    YamlConfiguration configSection = script.getConfigurationSection("EVENTS");
                    if (configSection == null) {
                        dB.echoError("Script '" + script.getName() + "' has an invalid events block!");
                        break;
                    }
                    Set<StringHolder> keys = configSection.getKeys(false);
                    if (keys == null) {
                        dB.echoError("Script '" + script.getName() + "' has an empty events block!");
                        break;
                    }
                    for (StringHolder eventName1 : keys) {
                        String eventName = eventName1.str;
                        List<WorldScriptContainer> list;
                        if (events.containsKey(eventName))
                            list = events.get(eventName);
                        else
                            list = new ArrayList<WorldScriptContainer>();
                        list.add(script);
                        events.put(eventName, list);
                    }
                }
                else {
                    dB.echoError("Script '" + script.getName() + "' does not have an events block!");
                }
            }
            // dB.echoApproval("Built events map: " + events);

            // Breakdown all SmartEvents (if still being used, they will reinitialize next)
            for (OldSmartEvent smartEvent : smart_events)
                smartEvent.breakDown();

            // Pass these along to each SmartEvent so they can determine whether they can be enabled or not
            for (OldSmartEvent smartEvent : smart_events) {
                // If it should initialize, run _initialize!
                if (smartEvent.shouldInitialize(events.keySet()))
                    smartEvent._initialize();
            }
        }
        catch (Exception e) {
            dB.echoError(e);
        }
    }

    public static List<String> trimEvents(List<String> original) {
        List<String> event = new ArrayList<String>();
        event.addAll(original);
        List<String> parsed = new ArrayList<String>();

        if (dB.showEventsTrimming) dB.echoApproval("Trimming world events '" + event.toString() + '\'');

        // Remove any duplicate event names
        for (int i = 0; i < event.size(); i++) {
            for (int x = 0; x < event.size(); x++) {
                if (i != x && event.get(i).equalsIgnoreCase(event.get(x))) {
                    event.remove(i);
                    i--;
                    break;
                }
            }
        }

        // Create a new list, only containing events that have existing scripts
        for (String e : event)
            if (events.containsKey("ON " + e.toUpperCase()))
                parsed.add(e);

        return parsed;
    }


    public static boolean eventExists(String original) {
        return events.containsKey("ON " + original.toUpperCase());
    }


    public static List<String> addAlternates(List<String> events) {

        // Strip object identifiers from world event names and add the results
        // to the original list of world event names without any duplicates

        Set<String> newEvents = new HashSet<String>();

        for (String event : events) {
            // NOTE: The below code deletes [a-z]{1,2}\@
            if (event.indexOf('@') != -1) {
                StringBuilder sb = new StringBuilder();
                int len = event.length();
                char[] data = event.toCharArray();
                for (int i = 0; i < len; i++) {
                    if (data[i] >= 'a' && data[i] <= 'z' && i + 1 < len) {
                        if (i + 2 < len && data[i + 2] == '@') {
                            i += 2;
                            continue;
                        }
                        if (data[i + 1] == '@') {
                            i++;
                            continue;
                        }
                    }
                    sb.append(data[i]);
                }
                newEvents.add(sb.toString());
            }
        }

        List<String> finalEvents = new ArrayList<String>();
        finalEvents.addAll(events);
        finalEvents.addAll(newEvents);
        return finalEvents;
    }


    public static String StripIdentifiers(String original) {
        if (original.matches(".*?[a-z]+@[\\w ]+"))
            return original.replaceAll("[a-z]+@", "");
        else
            return original;
    }

    ///////////////////
    //  MECHANICS
    ///////////////

    public static List<String> doEvents(List<String> eventNames, ScriptEntryData data, Map<String, dObject> context, boolean strip_ids) {
        return doEvents(addAlternates(eventNames), data, context);
    }

    public static List<String> doEvents(List<String> eventNames, ScriptEntryData data, Map<String, dObject> context) {

        try {
            List<String> determinations = new ArrayList<String>();

            // Trim again to catch events that don't trim internally.
            eventNames = trimEvents(eventNames);

            for (String eventName : eventNames) {

                if (events.containsKey("ON " + eventName.toUpperCase()))

                    for (WorldScriptContainer script : events.get("ON " + eventName.toUpperCase())) {

                        if (script == null) continue;

                        // Fetch script from Event
                        List<ScriptEntry> entries = script.getEntries(data, "events.on " + eventName);

                        if (entries.isEmpty()) continue;

                        dB.report(script, "Event",
                                aH.debugObj("Type", "on " + eventName)
                                        + script.getAsScriptArg().debug()
                                        + data.toString()
                                        + (context != null ? aH.debugObj("Context", context.toString()) : ""));

                        dB.echoDebug(script, DebugElement.Header, "Building event 'ON " + eventName.toUpperCase()
                                + "' for " + script.getName());

                        // Create new ID -- this is what we will look for when determining an outcome
                        long id = DetermineCommand.getNewId();

                        // Add the reqId to each of the entries for the determine command
                        ScriptBuilder.addObjectToEntries(entries, "ReqId", id);

                        // Add entries and context to the queue
                        ScriptQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId(script.getName())).addEntries(entries).setReqId(id);

                        if (context != null) {
                            OldEventContextSource oecs = new OldEventContextSource();
                            oecs.contexts = context;
                            queue.setContextSource(oecs);
                        }

                        // Start the queue!
                        queue.start();

                        // Check the determination
                        if (DetermineCommand.hasOutcome(id))
                            determinations = DetermineCommand.getOutcome(id);
                    }
            }

            return determinations;
        }
        catch (Exception e) {
            dB.echoError(e);
            return new ArrayList<String>();
        }
    }

    public static class OldEventContextSource implements ContextSource {

        public Map<String, dObject> contexts;

        @Override
        public boolean getShouldCache() {
            return true;
        }

        @Override
        public dObject getContext(String name) {
            return contexts.get(name);
        }
    }

    ////////////////////
    //  REGISTRATION
    //////////////


    public void registerCoreMembers() {
    }


    public static void registerSmartEvent(OldSmartEvent event) {
        // Seems simple enough
        if (event != null)
            smart_events.add(event);
    }
}
