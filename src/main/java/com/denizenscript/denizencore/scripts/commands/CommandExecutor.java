package com.denizenscript.denizencore.scripts.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagManager;

public class CommandExecutor {

    public static ScriptQueue currentQueue;

    public static void debugSingleExecution(ScriptEntry scriptEntry) {
        StringBuilder output = new StringBuilder();
        output.append("<G>(line ").append(scriptEntry.internal.lineNumber).append(")<W> ");
        if (scriptEntry.internal.waitfor) {
            output.append("~");
        }
        output.append(scriptEntry.getCommandName());
        if (scriptEntry.getOriginalArguments() == null) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Original Arguments null for " + scriptEntry.getCommandName());
        }
        else {
            for (String arg : scriptEntry.getOriginalArguments()) {
                if (CoreUtilities.contains(arg, ' ')) {
                    output.append(" \"").append(arg).append("\"");
                }
                else {
                    output.append(" ").append(arg);
                }
            }
        }
        DenizenCore.getImplementation().debugQueueExecute(scriptEntry, scriptEntry.getResidingQueue().debugId, output.toString());
    }

    // <--[language]
    // @name The Save Argument
    // @group Script Command System
    // @description
    // The "save:<name>" argument is a special meta-argument that is available for all commands, but is only useful for some.
    // It is written like:
    // - run MyScript save:mysave
    //
    // When the save argument is used, the results of the command will be saved on the queue, for later usage by the "entry" tag.
    //
    // The useful entry keys available for any command are listed in the "Tags" documentation section for any command.
    // For example, the "run" command lists "<entry[saveName].created_queue>".
    // The "saveName" part should be replaced with whatever name you gave to the "save" argument,
    // and the "created_queue" part changes between commands.
    // Some commands have multiple save entry keys, some have just one, most don't have any.
    // -->

    public boolean execute(ScriptEntry scriptEntry) {
        if (scriptEntry.dbCallShouldDebug()) {
            debugSingleExecution(scriptEntry);
        }
        TagManager.recentTagError = false;
        AbstractCommand command = scriptEntry.internal.actualCommand;
        currentQueue = scriptEntry.getResidingQueue();
        String saveName = null;
        try {
            scriptEntry.generateAHArgs();
            TagContext context = scriptEntry.getContext();
            for (Argument arg : scriptEntry.internal.preprocArgs) {
                if (DenizenCore.getImplementation().handleCustomArgs(scriptEntry, arg, false)) {
                    // Do nothing
                }
                else if (arg.matchesPrefix("save")) {
                    saveName = TagManager.tag(arg.getValue(), context);
                    if (scriptEntry.dbCallShouldDebug()) {
                        Debug.echoDebug(scriptEntry, "...remembering this script entry as '" + saveName + "'!");
                    }
                }
            }
            if (scriptEntry.internal.actualCommand.shouldPreParse()) {
                TagManager.fillArgumentsObjects(scriptEntry.processed_arguments, scriptEntry.internal.args_ref, scriptEntry.aHArgs, context, scriptEntry.internal.processArgs);
            }
            command.parseArgs(scriptEntry);
            command.execute(scriptEntry);
            if (saveName != null) {
                scriptEntry.getResidingQueue().holdScriptEntry(saveName, scriptEntry);
            }
            currentQueue = null;
            return true;
        }
        catch (InvalidArgumentsException e) {
            // Give usage hint if InvalidArgumentsException was called.
            Debug.echoError(scriptEntry.getResidingQueue(), "Woah! Invalid arguments were specified!");
            if (e.getMessage() != null && e.getMessage().length() > 0) {
                Debug.log("+> MESSAGE follows: " + "'" + e.getMessage() + "'");
            }
            Debug.log("Usage: " + command.getUsageHint());
            Debug.log("(Attempted: " + scriptEntry.toString() + ")");
            Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer);
            scriptEntry.setFinished(true);
            currentQueue = null;
            return false;
        }
        catch (Exception e) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Woah! An exception has been called with this command!");
            Debug.echoError(scriptEntry.getResidingQueue(), e);
            Debug.log("(Attempted: " + scriptEntry.toString() + ")");
            Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer);
            scriptEntry.setFinished(true);
            currentQueue = null;
            return false;
        }
    }
}
