package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.TaskScriptContainer;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.Map;
import java.util.function.Consumer;

public class RunCommand extends AbstractCommand implements Holdable {

    public RunCommand() {
        setName("run");
        setSyntax("run [<script>/locally] (path:<name>) (def:<element>|.../defmap:<map>/def.<name>:<value>) (id:<name>) (speed:<value>/instantly) (delay:<value>)");
        setRequiredArguments(1, -1);
        isProcedural = true;
    }

    // <--[command]
    // @Name Run
    // @Syntax run [<script>/locally] (path:<name>) (def:<element>|.../defmap:<map>/def.<name>:<value>) (id:<name>) (speed:<value>/instantly) (delay:<value>)
    // @Required 1
    // @Maximum -1
    // @Short Runs a script in a new queue.
    // @Guide https://guide.denizenscript.com/guides/basics/run-options.html
    // @Group queue
    //
    // @Description
    // Runs a script in a new queue.
    //
    // You can specify either a script object to run, or "locally" to use a path within the same script.
    //
    // Optionally, use the "path:" argument to choose a specific sub-path within a script (works well with the "locally" argument).
    //
    // Optionally, use the "def:" argument to specify definition values to pass to the script,
    // the definitions will be named via the "definitions:" script key on the script being ran,
    // or numerically in order if that isn't specified (starting with <[1]>).
    //
    // Alternately, use "defmap:<map>" to specify definitions to pass as a MapTag, where the keys will be definition names and the values will of course be definition values.
    //
    // Alternately, use "def.<name>:<value>" to define one or more  named definitions individually.
    //
    // Optionally, use the "speed:" argument to specify the queue command-speed to run the target script at,
    // or use the "instantly" argument to use an instant speed (no command delay applied).
    // If neither argument is specified, the default queue speed applies (normally instant, refer to the config file).
    // Generally, prefer to set the "speed:" script key on the script to be ran, rather than using this argument.
    //
    // Optionally, use the "delay:" argument to specify a delay time before the script starts running.
    //
    // Optionally, specify the "id:" argument to choose a custom queue ID to be used.
    // If none is specified, a randomly generated one will be used. Generally, don't use this argument.
    //
    // The run command is ~waitable. Refer to <@link language ~waitable>.
    //
    // @Tags
    // <entry[saveName].created_queue> returns the queue that was started by the run command.
    //
    // @Usage
    // Use to run a task script named 'MyTask'.
    // - run MyTask
    //
    // @Usage
    // Use to run a task script named 'MyTask' that isn't normally instant, instantly.
    // - run MyTask instantly
    //
    // @Usage
    // Use to run a local subscript named 'alt_path'.
    // - run locally path:alt_path
    //
    // @Usage
    // Use to run 'MyTask' and pass 3 definitions to it.
    // - run MyTask def:A|Second_Def|Taco
    //
    // @Usage
    // Use to run 'MyTask' and pass 3 named definitions to it.
    // - run MyTask def.count:5 def.type:Taco def.smell:Tasty
    //
    // @Usage
    // Use to run 'MyTask' and pass a list as a single definition.
    // - run MyTask def:<list_single[<list[a|big|list|here]>]>
    // # MyTask can then get the list back by doing:
    // - define mylist <[1]>
    //
    // -->

    @Override
    public void addCustomTabCompletions(String arg, Consumer<String> addOne) {
        for (ScriptContainer script : ScriptRegistry.scriptContainers.values()) {
            if (script instanceof TaskScriptContainer) {
                addOne.accept(script.getName());
            }
        }
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        MapTag defMap = new MapTag();
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (arg.matchesPrefix("i", "id")) {
                scriptEntry.addObject("id", arg.asElement());
            }
            else if (arg.matchesPrefix("d", "def", "define", "c", "context")) {
                scriptEntry.addObject("definitions", arg.asType(ListTag.class));
            }
            else if (arg.matchesPrefix("defmap")
                    && arg.matchesArgumentType(MapTag.class)) {
                defMap.map.putAll(arg.asType(MapTag.class).map);
            }
            else if (arg.matches("instant", "instantly")) {
                scriptEntry.addObject("instant", new ElementTag(true));
            }
            else if (arg.matchesPrefix("delay")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("delay", arg.asType(DurationTag.class));
            }
            else if (arg.matches("local", "locally")) {
                scriptEntry.addObject("local", new ElementTag("true"));
                scriptEntry.addObject("script", scriptEntry.getScript());
            }
            else if (!scriptEntry.hasObject("speed")
                    && arg.matchesPrefix("speed")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("speed", arg.asType(DurationTag.class));
            }
            else if (arg.hasPrefix()
                    && arg.getPrefix().getRawValue().startsWith("def.")) {
                defMap.putObject(arg.getPrefix().getRawValue().substring("def.".length()), arg.object);
            }
            else if (!scriptEntry.hasObject("script")
                    && arg.matchesArgumentType(ScriptTag.class)
                    && !arg.matchesPrefix("p", "path")) {
                scriptEntry.addObject("script", arg.asType(ScriptTag.class));
            }
            else if (!scriptEntry.hasObject("path")) {
                String path = arg.asElement().asString();
                if (!scriptEntry.hasObject("script")) {
                    int dotIndex = path.indexOf('.');
                    if (dotIndex > 0) {
                        ScriptTag script = new ScriptTag(path.substring(0, dotIndex));
                        if (script.isValid()) {
                            scriptEntry.addObject("script", script);
                            path = path.substring(dotIndex + 1);
                        }
                    }
                }
                scriptEntry.addObject("path", new ElementTag(path));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("script") && (!scriptEntry.hasObject("local") || scriptEntry.getScript() == null)) {
            throw new InvalidArgumentsException("Must define a SCRIPT to be run.");
        }
        if (!scriptEntry.hasObject("path") && scriptEntry.hasObject("local")) {
            throw new InvalidArgumentsException("Must specify a PATH.");
        }
        if (!defMap.map.isEmpty()) {
            scriptEntry.addObject("def_map", defMap);
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag pathElement = scriptEntry.getElement("path");
        ScriptTag script = scriptEntry.getObjectTag("script");
        ElementTag local = scriptEntry.getElement("local");
        ElementTag instant = scriptEntry.getElement("instant");
        ElementTag id = scriptEntry.getElement("id");
        DurationTag speed = scriptEntry.getObjectTag("speed");
        DurationTag delay = scriptEntry.getObjectTag("delay");
        MapTag defMap = scriptEntry.getObjectTag("def_map");
        if (local != null && local.asBoolean()) {
            script = scriptEntry.getScript();
        }
        String path = pathElement != null ? pathElement.asString() : null;
        if (script == null) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Script run failed (invalid script name)!");
            return;
        }
        if (path != null && (!script.getContainer().contains(path) || !script.getContainer().getContents().isList(path))) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Script run failed (invalid path)!");
            return;
        }
        if (instant != null && instant.asBoolean()) {
            speed = new DurationTag(0);
        }
        ListTag definitions = scriptEntry.getObjectTag("definitions");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(),
                    (script.debug())
                            + (pathElement != null ? pathElement.debug() : "")
                            + (local != null ? local.debug() : "")
                            + (instant != null ? instant.debug() : "")
                            + (speed != null ? speed.debug() : "")
                            + (delay != null ? delay.debug() : "")
                            + (id != null ? id.debug() : "")
                            + (defMap != null ? defMap.debug() : "")
                            + (definitions != null ? definitions.debug() : ""));
        }
        Consumer<ScriptQueue> configure = (queue) -> {
            // Set any delay
            if (delay != null) {
                queue.delayUntil(DenizenCore.serverTimeMillis + delay.getMillis());
            }
            // Setup a callback if the queue is being waited on
            if (scriptEntry.shouldWaitFor()) {
                queue.callBack(() -> scriptEntry.setFinished(true));
            }
            if (defMap != null) {
                for (Map.Entry<StringHolder, ObjectTag> val : defMap.map.entrySet()) {
                    queue.addDefinition(val.getKey().str, val.getValue());
                }
            }
            // Save the queue for script referencing
            scriptEntry.addObject("created_queue", new QueueTag(queue));
            // Preserve procedural status
            queue.procedural = scriptEntry.getResidingQueue().procedural;
        };
        String idString = id != null ? "FORCE:" + id.asString() : null;
        ScriptQueue result = ScriptUtilities.createAndStartQueue(script.getContainer(), path, scriptEntry.entryData, null, configure, speed, idString, definitions, scriptEntry);
        if (result == null) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Script run failed!");
            return;
        }
    }
}
