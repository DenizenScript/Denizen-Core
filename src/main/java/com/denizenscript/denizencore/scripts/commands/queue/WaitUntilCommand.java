package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.RepeatingSchedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;

import java.util.ArrayList;
import java.util.List;

public class WaitUntilCommand extends AbstractCommand implements Holdable {

    // <--[command]
    // @Name WaitUntil
    // @Syntax waituntil [<comparisons>]
    // @Required 0
    // @Short Delays a script until the If comparisons return true.
    // @Group queue
    //
    // @Description
    // Delays a script until the If comparisons return true.
    // Will be checked as often as the queue updates (based on queue speed).
    //
    // @Tags
    // <q@queue.speed>
    //
    // @Usage
    // Use to delay the current queue until the player respawns (useful in a death event, for example).
    // - waituntil <player.is_spawned>
    // -->

    @Override
    public void onEnable() {
        setBraced();
        setParseArgs(false);
        forceHold = true;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        List<String> comparisons = new ArrayList<>();
        for (String arg : scriptEntry.getArguments()) {
            if (arg.equals("{")) {
                break;
            }
            comparisons.add(arg);
        }
        scriptEntry.addObject("comparisons", comparisons);
    }


    @Override
    public void execute(ScriptEntry scriptEntry) {

        List<String> comparisons = (List<String>) scriptEntry.getObject("comparisons");

        boolean run = new IfCommand.ArgComparer().compare(new ArrayList<>(comparisons), scriptEntry);

        // Report to dB
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), ArgumentHelper.debugObj("run_first_check", run));
        }

        if (run) {
            scriptEntry.setFinished(true);
            return;
        }

        final RepeatingSchedulable schedulable = new RepeatingSchedulable(null, scriptEntry.getResidingQueue() instanceof TimedQueue ?
                (float)((TimedQueue) scriptEntry.getResidingQueue()).getSpeed().getSeconds(): 0.05f);
        schedulable.run = new Runnable() {
            public int counter = 0;
            @Override
            public void run() {
                counter++;
                if (new IfCommand.ArgComparer().compare(new ArrayList<>(comparisons), scriptEntry)) {
                    Debug.echoDebug(scriptEntry, "WaitUntil completed after " + counter + " re-checks.");
                    scriptEntry.setFinished(true);
                    schedulable.cancel();
                }
            }
        };
        DenizenCore.schedule(schedulable);
    }
}
