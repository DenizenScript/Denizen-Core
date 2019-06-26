package net.aufdemrand.denizencore.scripts.commands.queue;

import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class StopCommand extends AbstractCommand {

    // <--[command]
    // @Name Stop
    // @Syntax stop
    // @Required 0
    // @Short Stops the current queue.
    // @Group queue
    //
    // @Description
    // This will immediately stop the current queue, preventing it from processing any further.
    //
    // @Tags
    // <queue>
    //
    // @Usage
    // Use to stop the current queue.
    // - stop
    // -->

    private enum Action {CLEAR, DELAY, PAUSE, RESUME, STOP}

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpretArguments(scriptEntry.aHArgs)) {
            arg.reportUnhandled();
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), scriptEntry.getResidingQueue().debug());
        }

        scriptEntry.getResidingQueue().clear();
        scriptEntry.getResidingQueue().stop();
    }
}
