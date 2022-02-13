package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.containers.core.TaskScriptContainer;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.List;

public class InjectCommand extends AbstractCommand {

    public InjectCommand() {
        setName("inject");
        setSyntax("inject [<script>] (path:<name>) (instantly)");
        setRequiredArguments(1, 3);
        isProcedural = true;
    }

    // <--[command]
    // @Name Inject
    // @Syntax inject [<script>] (path:<name>) (instantly)
    // @Required 1
    // @Maximum 3
    // @Short Runs a script in the current queue.
    // @Guide https://guide.denizenscript.com/guides/basics/run-options.html
    // @Group queue
    //
    // @Description
    // Injects a script into the current queue.
    // This means this task will run with all of the original queue's definitions and tags.
    // It will also now be part of the queue, so any delays or definitions used in the injected script will be accessible in the original queue.
    //
    // @Tags
    // None
    //
    // @Usage
    // Injects the InjectedTask task into the current queue
    // - inject InjectedTask
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addScriptsOfType(TaskScriptContainer.class);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (arg.matches("instant", "instantly")) {
                scriptEntry.addObject("instant", new ElementTag(true));
            }
            else if (arg.matches("local", "locally")) {
                Deprecations.locallyArgument.warn(scriptEntry);
                scriptEntry.addObject("script", scriptEntry.getScript());
            }
            else if (!scriptEntry.hasObject("script")
                    && arg.matchesArgumentType(ScriptTag.class)
                    && arg.limitToOnlyPrefix("script")) {
                scriptEntry.addObject("script", arg.asType(ScriptTag.class));
            }
            else if (!scriptEntry.hasObject("script") && !scriptEntry.hasObject("path")
                    && !arg.hasPrefix() && arg.asElement().asString().contains(".")) {
                String path = arg.asElement().asString();
            int dotIndex = path.indexOf('.');
                ScriptTag script = new ScriptTag(path.substring(0, dotIndex));
                if (!script.isValid()) {
                    arg.reportUnhandled();
                }
                else {
                    scriptEntry.addObject("script", script);
                    scriptEntry.addObject("path", new ElementTag(path.substring(dotIndex + 1)));
                }
            }
            else if (!scriptEntry.hasObject("path")
                    && arg.limitToOnlyPrefix("path")) {
                if (!arg.hasPrefix() && scriptEntry.hasObject("script")) { // TODO: Temporarily allow missing prefix due to common mistake
                    Debug.echoError("Inject command path is missing required 'path:' prefix.");
                }
                scriptEntry.addObject("path", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("script")) {
            throw new InvalidArgumentsException("Must define a SCRIPT to be injected.");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ScriptTag script = scriptEntry.getObjectTag("script");
        if (script == null) {
            script = scriptEntry.getScript();
        }
        ElementTag instant = scriptEntry.getElement("instant");
        ElementTag path = scriptEntry.getElement("path");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), script, instant, path);
        }
        List<ScriptEntry> entries;
        if (path != null) {
            entries = script.getContainer().getEntries(scriptEntry.entryData.clone(), path.asString());
        }
        else {
            entries = script.getContainer().getBaseEntries(scriptEntry.entryData.clone());
        }
        if (entries == null) {
            Debug.echoError(scriptEntry, "Script inject failed (invalid script path '" + path + "')!");
            return;
        }
        if (instant != null && instant.asBoolean()) {
            scriptEntry.getResidingQueue().runNow(entries);
        }
        else {
            scriptEntry.getResidingQueue().injectEntriesAtStart(entries);
        }
    }
}
