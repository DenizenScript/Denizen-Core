package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class ElseCommand extends AbstractCommand {

    // <--[command]
    // @Name Else
    // @Syntax else (if <comparison logic>)
    // @Required 0
    // @Short Helper command for usage with the if command.
    // @Group core
    // @Video /denizen/vids/Alternate/Dynamic%20Actions:%20The%20If%20Command
    // @Description
    // A helper command for ':' syntax if commands.
    // See IF command documentation.
    // @Tags
    // See IF command documentation.
    // @Usage
    // See IF command documentation.
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
    }


    @Override
    public void execute(ScriptEntry scriptEntry) {

        // If this command executes normally, it's misplaced. It should always be skipped past under normal execution.
        dB.echoError(scriptEntry.getResidingQueue(), "Misplaced ELSE command.");
    }
}
