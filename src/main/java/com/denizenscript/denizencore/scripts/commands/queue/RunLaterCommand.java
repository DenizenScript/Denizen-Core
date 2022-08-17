package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.containers.core.TaskScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class RunLaterCommand extends AbstractCommand {

    public RunLaterCommand() {
        setName("runlater");
        setSyntax("runlater [<script>] (path:<name>) [delay:<duration>] (id:<id>) (def:<element>|.../defmap:<map>/def.<name>:<value>)");
        setRequiredArguments(2, -1);
        setPrefixesHandled("id");
        allowedDynamicPrefixes = true;
    }

    // <--[command]
    // @Name RunLater
    // @Syntax runlater [<script>] (path:<name>) [delay:<duration>] (id:<id>) (def:<element>|.../defmap:<map>/def.<name>:<value>)
    // @Required 2
    // @Maximum -1
    // @Short Causes a task to run sometime in the future, even if the server restarts.
    // @Group queue
    //
    // @Description
    // Causes a task to run sometime in the future, even if the server restarts.
    //
    // Script, path, and definition inputs work the exact same as with <@link command run>.
    //
    // This command will store intended script runs to a file, so that even if the server restarts, they will still run.
    // Script runs are guaranteed to happen after the time is up - if the server is turned off at the scheduled time, they will run at next startup.
    // The guarantee can be broken if the server crashes or other errors occur.
    //
    // The delay input is a DurationTag instance, that is relative to system time (not server delta time).
    //
    // Definitions and queue object links will be preserved, so long as they remain valid at time of execution.
    // Objects that are lost before the delay is up (such as a linked NPC that is removed) may cause errors.
    //
    // You can optionally specify the "id" argument to provide a unique tracking ID for the intended future run, which can then also be used to cancel it via <@link mechanism system.cancel_runlater>.
    // If you use IDs, they must be unique - you cannot have the same ID scheduled twice. Use <@link tag util.runlater_ids> if you need to dynamically check if an ID is in use.
    //
    // Implementation note: the system that tracks when scripts should be ran is a fair bit more optimized than 'wait' commands or the 'run' command with a delay,
    // specifically for the case of very large delays (hours or more) - in the short term, 'wait' or 'run' with a delay will be better.
    //
    // @Tags
    // <util.runlater_ids>
    //
    // @Usage
    // Use to run a task script named 'example' 3 days later.
    // - runlater example delay:3d
    //
    // @Usage
    // Use to run a task script named 'my_task' 5 hours later with definition 'targets' set to a list of all the players that were near the player before the delay.
    // - runlater my_task delay:5h def.targets:<player.location.find_players_within[50]>
    //
    // @Usage
    // Use to plan to go for a jog tomorrow then change your mind after 5 seconds.
    // - runlater go_for_jog id:healthy_plan delay:1d
    // - wait 5s
    // - adjust system cancel_runlater:healthy_plan
    //
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addScriptsOfType(TaskScriptContainer.class);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        MapTag defMap = new MapTag();
        for (Argument arg : scriptEntry) {
            if (arg.matchesPrefix("def")) {
                scriptEntry.addObject("definitions", arg.asType(ListTag.class));
            }
            else if (arg.matchesPrefix("defmap")
                    && arg.matchesArgumentType(MapTag.class)) {
                defMap.map.putAll(arg.asType(MapTag.class).map);
            }
            else if (arg.matchesPrefix("delay")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("delay", arg.asType(DurationTag.class));
            }
            else if (arg.hasPrefix()
                    && arg.getPrefix().getRawValue().startsWith("def.")) {
                defMap.putObject(arg.getPrefix().getRawValue().substring("def.".length()), arg.object);
            }
            else if (!scriptEntry.hasObject("script")
                    && arg.matchesArgumentType(ScriptTag.class)
                    && arg.limitToOnlyPrefix("script")) {
                scriptEntry.addObject("script", arg.asType(ScriptTag.class));
            }
            else if (!scriptEntry.hasObject("path")
                    && arg.matchesPrefix("path", "p")) {
                scriptEntry.addObject("path", arg.asElement());
            }
            else if (!scriptEntry.hasObject("script") && !scriptEntry.hasObject("path")
                    && !arg.hasPrefix() && arg.asElement().asString().contains(".")) {
                String path = arg.asElement().asString();
                int dotIndex = path.indexOf('.');
                ScriptTag script = ScriptTag.valueOf(path.substring(0, dotIndex), CoreUtilities.noDebugContext);
                if (script == null) {
                    arg.reportUnhandled();
                }
                else {
                    scriptEntry.addObject("script", script);
                    scriptEntry.addObject("path", new ElementTag(path.substring(dotIndex + 1)));
                }
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("script")) {
            throw new InvalidArgumentsException("Must define a SCRIPT to be run.");
        }
        if (!scriptEntry.hasObject("delay")) {
            throw new InvalidArgumentsException("Must specify a DELAY.");
        }
        if (!defMap.map.isEmpty()) {
            scriptEntry.addObject("def_map", defMap);
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag pathElement = scriptEntry.getElement("path");
        ScriptTag script = scriptEntry.getObjectTag("script");
        DurationTag delay = scriptEntry.getObjectTag("delay");
        MapTag defMap = scriptEntry.getObjectTag("def_map");
        ElementTag id = scriptEntry.argForPrefixAsElement("id", null);
        String path = pathElement != null ? pathElement.asString() : null;
        if (script == null) {
            Debug.echoError(scriptEntry, "Script RunLater failed (invalid script name)!");
            return;
        }
        if (path != null && !script.getContainer().containsScriptSection(path)) {
            Debug.echoError(scriptEntry, "Script RunLater failed (invalid path)!");
            return;
        }
        ListTag definitions = scriptEntry.getObjectTag("definitions");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), script, pathElement, delay, id, defMap, definitions);
        }
        FutureRunData runData = new FutureRunData();
        runData.definitionList = definitions;
        runData.defMap = defMap;
        runData.scriptName = script.getName();
        runData.path = path;
        runData.entryData = scriptEntry.entryData.clone();
        runData.executeAt = System.currentTimeMillis() + delay.getMillis();
        runData.id = id == null ? null : id.asLowerString();
        if (id != null && trackedById.containsKey(runData.id)) {
            Debug.echoError("Cannot add new RunLater with the given id '" + runData.id + "': there is already a scheduled task with that ID.");
            return;
        }
        addNewRunnable(runData);
    }

    public static class FutureRunData {

        public ScriptEntryData entryData;

        public String scriptName;

        public String path;

        public ListTag definitionList;

        public MapTag defMap;

        public long executeAt;

        public YamlConfiguration savedData;

        public String id;

        public boolean cancelled = false;

        public void load(YamlConfiguration config) {
            scriptName = config.getString("script_name");
            path = config.getString("path", null);
            definitionList = config.contains("definition_list") ? ListTag.valueOf(config.getString("definition_list"), CoreUtilities.errorButNoDebugContext) : null;
            defMap = config.contains("definitions") ? MapTag.valueOf(config.getString("definitions"), CoreUtilities.errorButNoDebugContext) : null;
            entryData = DenizenCore.implementation.getEmptyScriptEntryData().clone();
            entryData.load(config.getConfigurationSection("entry_data"));
        }

        public YamlConfiguration save() {
            if (savedData != null) {
                return savedData;
            }
            YamlConfiguration out = new YamlConfiguration();
            out.set("execute_at", String.valueOf(executeAt));
            out.set("script_name", scriptName);
            if (path != null) {
                out.set("path", path);
            }
            if (definitionList != null) {
                out.set("definition_list", definitionList.savable());
            }
            if (defMap != null) {
                out.set("definitions", defMap.savable());
            }
            out.set("entry_data", entryData.save());
            if (id != null) {
                out.set("id", id);
            }
            return out;
        }

        public void run() {
            try {
                if (cancelled) {
                    return;
                }
                if (savedData != null) {
                    load(savedData);
                    savedData = null;
                }
                if (id != null) {
                    trackedById.remove(id);
                }
                ScriptTag script = ScriptTag.valueOf(scriptName, entryData.getTagContext());
                if (script == null) {
                    Debug.echoError("Script RunLater failed (invalid script name)!");
                    return;
                }
                if (path != null && !script.getContainer().containsScriptSection(path)) {
                    Debug.echoError("Script RunLater failed (invalid path)!");
                    return;
                }
                Consumer<ScriptQueue> configure = (queue) -> {
                    if (defMap != null) {
                        for (Map.Entry<StringHolder, ObjectTag> val : defMap.map.entrySet()) {
                            queue.addDefinition(val.getKey().str, val.getValue());
                        }
                    }
                };
                ScriptQueue result = ScriptUtilities.createAndStartQueue(script.getContainer(), path, entryData, null, configure, null, null, definitionList, script.getContainer());
                if (result == null) {
                    Debug.echoError("RunLater: script run failed!");
                    return;
                }
            }
            catch (Throwable ex) {
                Debug.echoError("Error in RunLater...");
                Debug.echoError(ex);
            }
        }
    }

    public static void addNewRunnable(FutureRunData runData) {
        hasChanged = true;
        long timeNow = System.currentTimeMillis();
        if (runData.executeAt < timeNow + MS_PER_MINUTE * 2) {
            nextMinuteFutureRuns.add(runData);
        }
        else if (runData.executeAt < timeNow + MS_PER_HOUR * 2) {
            nextHourFutureRuns.add(runData);
        }
        else {
            farFutureRuns.add(runData);
        }
        if (runData.id != null) {
            trackedById.put(runData.id, runData);
        }
    }

    public static List<FutureRunData> nextMinuteFutureRuns = new ArrayList<>();

    public static List<FutureRunData> nextHourFutureRuns = new ArrayList<>();

    public static List<FutureRunData> farFutureRuns = new ArrayList<>();

    public static HashMap<String, FutureRunData> trackedById = new HashMap<>();

    public static long timeMinuteReorg = 0, timeHourReorg = 0, timeLastSave = 0;

    public static final long MS_PER_MINUTE = 60 * 1000, MS_PER_HOUR = 60 * MS_PER_MINUTE;

    public static boolean hasAny() {
        return !nextMinuteFutureRuns.isEmpty() || !nextHourFutureRuns.isEmpty() || !farFutureRuns.isEmpty();
    }

    public static String persistFilePath;

    public static boolean isSaving = false;

    public static boolean hasChanged = false;

    public static void init(String path) {
        nextMinuteFutureRuns.clear();
        nextHourFutureRuns.clear();
        farFutureRuns.clear();
        trackedById.clear();
        persistFilePath = path;
        String stored = CoreUtilities.journallingLoadFile(path);
        if (stored != null) {
            load(YamlConfiguration.load(stored));
        }
        timeLastSave = System.currentTimeMillis();
    }

    public static void saveToFile(boolean async) {
        if (!hasChanged) {
            return;
        }
        hasChanged = false;
        final YamlConfiguration toSave = saveAll();
        isSaving = true;
        Runnable doSave = () -> {
            try {
                if (toSave == null) {
                    File fileObj = new File(persistFilePath);
                    if (fileObj.exists()) {
                        fileObj.delete();
                    }
                    fileObj = new File(persistFilePath + "~2");
                    if (fileObj.exists()) {
                        fileObj.delete();
                    }
                    return;
                }
                CoreUtilities.journallingFileSave(persistFilePath, toSave.saveToString(false));
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
            finally {
                isSaving = false;
            }
        };
        if (async) {
            DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(doSave, 0)));
        }
        else {
            doSave.run();
        }
    }

    public static void tickFutureRuns() {
        if (!hasAny()) {
            if (hasChanged) {
                saveToFile(true);
            }
            return;
        }
        long timeNow = System.currentTimeMillis();
        for (int i = 0; i < nextMinuteFutureRuns.size(); i++) {
            FutureRunData data = nextMinuteFutureRuns.get(i);
            if (data.executeAt < timeNow) {
                nextMinuteFutureRuns.remove(i--);
                hasChanged = true;
                data.run();
            }
        }
        if (timeNow > timeMinuteReorg + MS_PER_MINUTE) {
            timeMinuteReorg = timeNow;
            if (timeNow > timeHourReorg + MS_PER_HOUR) {
                timeHourReorg = timeNow;
                for (int i = 0; i < farFutureRuns.size(); i++) {
                    FutureRunData data = farFutureRuns.get(i);
                    if (data.executeAt < timeNow + (2 * MS_PER_HOUR)) {
                        nextHourFutureRuns.add(data);
                        farFutureRuns.remove(i--);
                    }
                }
            }
            for (int i = 0; i < nextHourFutureRuns.size(); i++) {
                FutureRunData data = nextHourFutureRuns.get(i);
                if (data.executeAt < timeNow + (2 * MS_PER_MINUTE)) {
                    nextMinuteFutureRuns.add(data);
                    nextHourFutureRuns.remove(i--);
                }
            }
            if (timeNow > timeLastSave + (30 * MS_PER_MINUTE)) {
                timeLastSave = timeNow;
                saveToFile(true);
            }
        }
    }

    public static YamlConfiguration saveAll() {
        if (!hasAny()) {
            return null;
        }
        YamlConfiguration out = new YamlConfiguration();
        int id = 0;
        for (FutureRunData runData : nextMinuteFutureRuns) {
            if (!runData.cancelled) {
                out.set("minute_" + (id++), runData.save());
            }
        }
        for (FutureRunData runData : nextHourFutureRuns) {
            if (!runData.cancelled) {
                out.set("hour_" + (id++), runData.save());
            }
        }
        for (FutureRunData runData : farFutureRuns) {
            if (!runData.cancelled) {
                out.set("future_" + (id++), runData.save());
            }
        }
        return out;
    }

    public static void load(YamlConfiguration config) {
        if (config == null) {
            Debug.echoError("RunLater load failed due to an invalid YAML file!");
            return;
        }
        for (StringHolder key : config.getKeys(false)) {
            YamlConfiguration subConfig = config.getConfigurationSection(key.str);
            FutureRunData runData = new FutureRunData();
            runData.savedData = subConfig;
            runData.executeAt = Long.parseLong(subConfig.getString("execute_at"));
            runData.id = subConfig.getString("id");
            addNewRunnable(runData);
        }
    }
}
