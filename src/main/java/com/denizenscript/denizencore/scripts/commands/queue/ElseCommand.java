package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class ElseCommand extends AbstractCommand {

    // <--[command]
    // @Name Else
    // @Syntax else (if <comparison logic>)
    // @Required 0
    // @Short Helper command for usage with the if command.
    // @Group queue
    // @Guide https://guide.denizenscript.com/guides/basics/if-command.html
    //
    // @Description
    // A helper command for ':' syntax if commands.
    // See IF command documentation.
    //
    // @Tags
    // See IF command documentation.
    //
    // @Usage
    // See IF command documentation.
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        // If this command executes normally, it's misplaced. It should always be skipped past under normal execution.
        Debug.echoError(scriptEntry.getResidingQueue(), "Misplaced ELSE command.");
    }
}
