package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class ElseCommand extends AbstractCommand {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
    }


    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        // If this command executes normally, it's misplaced. It should always be skipped past under normal execution.
        dB.echoError(scriptEntry.getResidingQueue(), "Misplaced ELSE command.");
    }
}
