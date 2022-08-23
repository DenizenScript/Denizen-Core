package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class StopCommand extends AbstractCommand {

    public StopCommand() {
        setName("stop");
        setSyntax("stop");
        setRequiredArguments(0, 0);
        isProcedural = true;
        autoCompile();
    }

    // <--[command]
    // @Name Stop
    // @Syntax stop
    // @Required 0
    // @Maximum 0
    // @Short Stops the current queue.
    // @Group queue
    //
    // @Description
    // This will immediately stop the current queue, preventing it from processing any further.
    //
    // @Tags
    // <queue> to get the current queue.
    //
    // @Usage
    // Use to stop the current queue.
    // - stop
    // -->

    public static void autoExecute(ScriptEntry scriptEntry) {
        scriptEntry.getResidingQueue().clear();
        scriptEntry.getResidingQueue().stop();
    }
}
