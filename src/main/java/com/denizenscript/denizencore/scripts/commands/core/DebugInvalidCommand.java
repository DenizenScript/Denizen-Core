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

    public DebugInvalidCommand() {
        setName("debug-invalid-command");
        setSyntax("");
        setRequiredArguments(0, Integer.MAX_VALUE);
        isProcedural = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "Executing command: " + scriptEntry.getCommandName());
        AbstractCommand command = DenizenCore.getCommandRegistry().get(scriptEntry.internal.command);
        if (scriptEntry.internal.brokenArgs) {
            if (scriptEntry.getArguments().size() > command.maximumArguments) {
                Debug.echoError(scriptEntry.getResidingQueue(), scriptEntry.toString() + " cannot be executed! Too many arguments - did you forget to use quotes?\nUsage: " + command.getUsageHint());
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), scriptEntry.toString() + " cannot be executed! Too few arguments - did you forget a required input?\nUsage: " + command.getUsageHint());
            }
            return;
        }
        else {
            if (command != null) {
                int argCount = scriptEntry.getArguments().size();
                if (argCount < command.minimumArguments || argCount > command.maximumArguments) {
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
