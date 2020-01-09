package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;

import java.util.List;

public class RunCommand extends AbstractCommand implements Holdable {

    // <--[command]
    // @Name Run
    // @Syntax run [<script>/locally] (path:<name>) (def:<element>|...) (id:<name>) (speed:<value>/instantly) (delay:<value>)
    // @Required 1
    // @Short Runs a script in a new ScriptQueue.
    // @Guide https://guide.denizenscript.com/guides/basics/run-options.html
    // @Group queue
    //
    // @Description
    // Runs a script in a new ScriptQueue.
    //
    // You can specify either a script object to run, or "locally" to use a path within the same script.
    //
    // Optionally, use the "path:" argument to choose a specific sub-path within a script (works well with the "locally" argument).
    //
    // Optionally, use the "def:" argument to specify definition values to pass to the script,
    // the definitions will be named via the "definitions:" script key on the script being ran,
    // or numerically in order if that isn't specified (starting with <[1]>).
    // To pass a list value in here as a single definition, use <@link tag ElementTag.escaped> in the run command line,
    // and <@link tag ElementTag.unescaped> in the final task script to read it back.
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
    // Use to run 'MyTask' and pass a list as a single definition.
    // - run MyTask def:<list[a|big|list|here].escaped>
    // # MyTask can then get the list back by doing:
    // - define mylist:<[1].unescaped>
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (arg.matchesPrefix("i", "id")) {
                scriptEntry.addObject("id", arg.asElement());
            }
            else if (arg.matchesPrefix("d", "def", "define", "c", "context")) {
                scriptEntry.addObject("definitions", arg.asElement());
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
            else if (!scriptEntry.hasObject("script")
                    && arg.matchesArgumentType(ScriptTag.class)
                    && !arg.matchesPrefix("p", "path")) {
                scriptEntry.addObject("script", arg.asType(ScriptTag.class));
            }
            else if (!scriptEntry.hasObject("speed") && arg.matchesPrefix("speed")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("speed", arg.asType(DurationTag.class));
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

    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(),
                    (scriptEntry.hasObject("script") ? scriptEntry.getObjectTag("script").debug() : scriptEntry.getScript().debug())
                            + (scriptEntry.hasObject("instant") ? scriptEntry.getObjectTag("instant").debug() : "")
                            + (scriptEntry.hasObject("path") ? scriptEntry.getElement("path").debug() : "")
                            + (scriptEntry.hasObject("local") ? scriptEntry.getElement("local").debug() : "")
                            + (scriptEntry.hasObject("delay") ? scriptEntry.getObjectTag("delay").debug() : "")
                            + (scriptEntry.hasObject("id") ? scriptEntry.getObjectTag("id").debug() : "")
                            + (scriptEntry.hasObject("definitions") ? scriptEntry.getObjectTag("definitions").debug() : "")
                            + (scriptEntry.hasObject("speed") ? scriptEntry.getObjectTag("speed").debug() : ""));
        }

        // Get the script
        ScriptTag script = scriptEntry.getObjectTag("script");

        // Get the entries
        List<ScriptEntry> entries;
        // If it's local
        if (scriptEntry.hasObject("local")) {
            entries = scriptEntry.getScript().getContainer().getEntries(scriptEntry.entryData.clone(),
                    scriptEntry.getElement("path").asString());
            script = scriptEntry.getScript();
        }

        // If it has a path
        else if (scriptEntry.hasObject("path") && scriptEntry.getObject("path") != null) {
            entries = script.getContainer().getEntries(scriptEntry.entryData.clone(),
                    scriptEntry.getElement("path").asString());
        }

        // Else, assume standard path
        else {
            entries = script.getContainer().getBaseEntries(scriptEntry.entryData.clone());
        }

        if (entries == null) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Script run failed (invalid path or script name)!");
            return;
        }

        // Get the 'id' if specified
        String id = (scriptEntry.hasObject("id") ?
                "FORCE:" + (scriptEntry.getElement("id")).asString() : script.getContainer().getName());

        // Build the queue
        ScriptQueue queue;
        if (scriptEntry.hasObject("instant")) {
            queue = new InstantQueue(id).addEntries(entries);
        }
        else {

            DurationTag speed;
            if (scriptEntry.hasObject("speed")) {
                speed = scriptEntry.getObjectTag("speed");
            }
            else if (script != null && script.getContainer().contains("SPEED")) {
                speed = DurationTag.valueOf(script.getContainer().getString("SPEED", "0"));
            }
            else {
                speed = DurationTag.valueOf(DenizenCore.getImplementation().scriptQueueSpeed());
            }
            if (speed.getTicks() > 0) {
                queue = new TimedQueue(id).setSpeed(speed.getTicks()).addEntries(entries);
            }
            else {
                queue = new InstantQueue(id).addEntries(entries);
            }
        }

        // Set any delay
        if (scriptEntry.hasObject("delay")) {
            queue.delayUntil(DenizenCore.serverTimeMillis + ((DurationTag) scriptEntry.getObject("delay")).getMillis());
        }

        // Set any definitions
        if (scriptEntry.hasObject("definitions")) {
            int x = 1;
            ElementTag raw_defintions = scriptEntry.getElement("definitions");
            ListTag definitions = ListTag.valueOf(raw_defintions.asString());
            String[] definition_names = null;
            try {
                if (script != null && script.getContainer() != null) {
                    String str = script.getContainer().getString("definitions");
                    if (str != null) {
                        definition_names = str.split("\\|");
                    }
                }
            }
            catch (Exception e) {
                // TODO: less lazy handling
            }
            for (String definition : definitions) {
                String name = definition_names != null && definition_names.length >= x ?
                        definition_names[x - 1].trim() : String.valueOf(x);
                queue.addDefinition(name, definition);
                Debug.echoDebug(scriptEntry, "Adding definition '" + name + "' as " + definition);
                x++;
            }
            queue.addDefinition("raw_context", raw_defintions.asString());
        }

        // Setup a callback if the queue is being waited on
        if (scriptEntry.shouldWaitFor()) {
            // Record the ScriptEntry
            final ScriptEntry se = scriptEntry;
            queue.callBack(new Runnable() {
                @Override
                public void run() {
                    se.setFinished(true);
                }
            });
        }

        // Save the queue for script referencing
        scriptEntry.addObject("created_queue", new QueueTag(queue));

        // OK, GO!
        queue.start();
    }
}
