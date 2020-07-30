package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.List;

public class InjectCommand extends AbstractCommand {

    public InjectCommand() {
        setName("inject");
        setSyntax("inject (locally) [<script>] (path:<name>) (instantly)");
        setRequiredArguments(1, 4);
        isProcedural = true;
    }

    // <--[command]
    // @Name Inject
    // @Syntax inject (locally) [<script>] (path:<name>) (instantly)
    // @Required 1
    // @Maximum 4
    // @Short Runs a script in the current queue.
    // @Guide https://guide.denizenscript.com/guides/basics/run-options.html
    // @Group queue
    //
    // @Description
    // Injects a script into the current queue.
    // This means this task will run with all of the original queue's definitions and tags.
    // It will also now be part of the queue, so any delays or definitions used in the injected script will be
    // accessible in the original queue.
    //
    // @Tags
    // None
    //
    // @Usage
    // Injects the InjectedTask task into the current queue
    // - inject InjectedTask
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (arg.matches("instant", "instantly")) {
                scriptEntry.addObject("instant", new ElementTag(true));
            }
            else if (arg.matches("local", "locally")) {
                scriptEntry.addObject("local", new ElementTag(true));
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

        if (!scriptEntry.hasObject("script") && !scriptEntry.hasObject("local")) {
            throw new InvalidArgumentsException("Must define a SCRIPT to be injected.");
        }

        if (scriptEntry.hasObject("local") && !scriptEntry.hasObject("path") && !scriptEntry.hasObject("script")) {
            throw new InvalidArgumentsException("Must specify a PATH.");
        }

    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        ScriptTag script = scriptEntry.getObjectTag("script");
        if (script == null) {
            script = scriptEntry.getScript();
        }

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), script.debug()
                            + (scriptEntry.hasObject("instant") ? scriptEntry.getObjectTag("instant").debug() : "")
                            + (scriptEntry.hasObject("path") ? scriptEntry.getElement("path").debug() : "")
                            + (scriptEntry.hasObject("local") ? scriptEntry.getElement("local").debug() : ""));
        }

        List<ScriptEntry> entries;
        if (scriptEntry.hasObject("local")) {
            String pathName = scriptEntry.hasObject("path") ? scriptEntry.getElement("path").asString() : script.getName();
            entries = scriptEntry.getScript().getContainer().getEntries(scriptEntry.entryData.clone(), pathName);
        }
        else if (scriptEntry.hasObject("path")) {
            entries = script.getContainer().getEntries(scriptEntry.entryData.clone(), scriptEntry.getElement("path").asString());
        }
        else {
            entries = script.getContainer().getBaseEntries(scriptEntry.entryData.clone());
        }

        if (entries == null) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Script inject failed (invalid path or script name)!");
            return;
        }

        if (scriptEntry.hasObject("instant")) {
            scriptEntry.getResidingQueue().runNow(entries);
        }
        else {
            scriptEntry.getResidingQueue().injectEntries(entries, 0);
        }
    }
}
