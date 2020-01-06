package com.denizenscript.denizencore.events;

import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.core.WorldScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OldEventManager {

    ///////////////////
    //  MAPS
    ////////////


    // Map for keeping the WorldScriptContainers
    public static Map<String, WorldScriptContainer> world_scripts =
            new ConcurrentHashMap<>(8, 0.9f, 1);

    // Map for keeping the names of events
    public static Map<String, List<WorldScriptContainer>> events =
            new HashMap<>();

    // Map for keeping track of registered smart_events
    public static Set<OldSmartEvent> smart_events = new HashSet<>();

    //////////////////
    // PERFORMANCE
    ///////////


    public static void scanWorldEvents() {
        try {
            // Build a Map of scripts keyed by 'world events name'.

            // Loop through each world script
            if (Debug.showLoading) {
                Debug.log("Scanning " + world_scripts.size() + " world scripts...");
            }
            for (WorldScriptContainer script : world_scripts.values()) {
                if (script == null) {
                    Debug.echoError("Null world script?!");
                    continue;
                }

                // ...and through each event inside the script.
                if (script.contains("EVENTS")) {
                    YamlConfiguration configSection = script.getConfigurationSection("EVENTS");
                    if (configSection == null) {
                        Debug.echoError("Script '" + script.getName() + "' has an invalid events block!");
                        break;
                    }
                    Set<StringHolder> keys = configSection.getKeys(false);
                    if (keys == null) {
                        Debug.echoError("Script '" + script.getName() + "' has an empty events block!");
                        break;
                    }
                    for (StringHolder eventName1 : keys) {
                        String eventName = eventName1.str.toUpperCase();
                        List<WorldScriptContainer> list;
                        if (events.containsKey(eventName)) {
                            list = events.get(eventName);
                        }
                        else {
                            list = new ArrayList<>();
                        }
                        list.add(script);
                        events.put(eventName, list);
                    }
                }
                else {
                    Debug.echoError("Script '" + script.getName() + "' does not have an events block!");
                }
            }
            // dB.echoApproval("Built events map: " + events);

            // Breakdown all SmartEvents (if still being used, they will reinitialize next)
            for (OldSmartEvent smartEvent : smart_events) {
                smartEvent.breakDown();
            }

            // Pass these along to each SmartEvent so they can determine whether they can be enabled or not
            for (OldSmartEvent smartEvent : smart_events) {
                // If it should initialize, run _initialize!
                if (smartEvent.shouldInitialize(events.keySet())) {
                    smartEvent._initialize();
                }
            }
        }
        catch (Exception e) {
            Debug.echoError(e);
        }
    }

    public static List<String> trimEvents(List<String> original) {
        List<String> event = new ArrayList<>(original);
        List<String> parsed = new ArrayList<>();

        if (Debug.showEventsTrimming) {
            Debug.echoApproval("Trimming world events '" + event.toString() + '\'');
        }

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
        for (String e : event) {
            if (events.containsKey("ON " + e.toUpperCase())) {
                parsed.add(e);
            }
        }

        return parsed;
    }


    public static boolean eventExists(String original) {
        return events.containsKey("ON " + original.toUpperCase());
    }


    public static List<String> addAlternates(List<String> events) {

        // Strip object identifiers from world event names and add the results
        // to the original list of world event names without any duplicates

        Set<String> newEvents = new HashSet<>();

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

        List<String> finalEvents = new ArrayList<>();
        finalEvents.addAll(events);
        finalEvents.addAll(newEvents);
        return finalEvents;
    }


    public static String StripIdentifiers(String original) {
        if (original.matches(".*?[a-z]+@[\\w ]+")) {
            return original.replaceAll("[a-z]+@", "");
        }
        else {
            return original;
        }
    }

    ///////////////////
    //  MECHANICS
    ///////////////

    public static List<String> doEvents(List<String> eventNames, ScriptEntryData data, Map<String, ObjectTag> context, boolean strip_ids) {
        return doEvents(addAlternates(eventNames), data, context);
    }

    public static List<String> doEvents(List<String> eventNames, ScriptEntryData data, Map<String, ObjectTag> context) {

        try {
            List<String> determinations = new ArrayList<>();

            // Trim again to catch events that don't trim internally.
            eventNames = trimEvents(eventNames);

            for (String eventName : eventNames) {

                if (events.containsKey("ON " + eventName.toUpperCase()))

                {
                    for (WorldScriptContainer script : events.get("ON " + eventName.toUpperCase())) {

                        if (script == null) {
                            continue;
                        }

                        // Fetch script from Event
                        List<ScriptEntry> entries = script.getEntries(data, "events.on " + eventName);

                        if (entries.isEmpty()) {
                            continue;
                        }

                        Debug.report(script, "Event",
                                ArgumentHelper.debugObj("Type", "on " + eventName)
                                        + script.getAsScriptArg().debug()
                                        + data.toString()
                                        + (context != null ? ArgumentHelper.debugObj("Context", context.toString()) : ""));

                        Debug.echoDebug(script, Debug.DebugElement.Header, "Building event 'ON " + eventName.toUpperCase()
                                + "' for " + script.getName());

                        // Add entries and context to the queue
                        ScriptQueue queue = new InstantQueue(script.getName()).addEntries(entries);

                        if (context != null) {
                            OldEventContextSource oecs = new OldEventContextSource();
                            oecs.contexts = context;
                            queue.setContextSource(oecs);
                        }

                        // Start the queue!
                        queue.start();

                        // Check the determination
                        if (queue.determinations != null) {
                            determinations = queue.determinations;
                        }
                    }
                }
            }

            return determinations;
        }
        catch (Exception e) {
            Debug.echoError(e);
            return new ArrayList<>();
        }
    }

    public static class OldEventContextSource implements ContextSource {

        public Map<String, ObjectTag> contexts;

        @Override
        public boolean getShouldCache() {
            return true;
        }

        @Override
        public ObjectTag getContext(String name) {
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
        if (event != null) {
            smart_events.add(event);
        }
    }
}
