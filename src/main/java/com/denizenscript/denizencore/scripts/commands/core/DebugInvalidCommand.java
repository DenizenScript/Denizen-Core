package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.debugging.Debug;

/**
 * This command exists for technical reasons, to handle invalid commands.
 */
public class DebugInvalidCommand extends AbstractCommand {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "Executing command: " + scriptEntry.getCommandName());
        if (scriptEntry.internal.brokenArgs) {
            Debug.echoError(scriptEntry.getResidingQueue(), scriptEntry.toString() + " cannot be executed! Is the number of arguments given correct?");
        }
        else {
            AbstractCommand command = DenizenCore.getCommandRegistry().get(scriptEntry.internal.command);
            if (command != null) {
                if (command.getOptions().requiredArgs > scriptEntry.getArguments().size()) {
                    scriptEntry.internal.brokenArgs = true;
                }
                else {
                    scriptEntry.internal.actualCommand = command;
                }
                scriptEntry.getResidingQueue().injectEntry(scriptEntry, 0);
                return;
            }
            Debug.echoError(scriptEntry.getResidingQueue(), scriptEntry.getCommandName() + " is an invalid command! Are you sure it loaded?");
        }
        Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer);
    }
}
