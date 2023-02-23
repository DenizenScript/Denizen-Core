package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.RepeatingSchedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;

import java.util.ArrayList;
import java.util.List;

public class WaitUntilCommand extends AbstractCommand implements Holdable {

    public WaitUntilCommand() {
        setName("waituntil");
        setSyntax("waituntil (rate:<duration>) (max:<duration>) [<comparisons>]");
        setRequiredArguments(1, -1);
        forceHold = true;
        isProcedural = false; // A procedure can't wait
        autoCompile();
    }

    // <--[command]
    // @Name WaitUntil
    // @Syntax waituntil (rate:<duration>) (max:<duration>) [<comparisons>]
    // @Required 1
    // @Maximum -1
    // @Short Delays a script until the If comparisons return true.
    // @Group queue
    //
    // @Description
    // Delays a script until the If comparisons return true. Refer to <@link command if> for if command syntax information.
    //
    // Optionally, specify an update rate (if unset, will update at queue speed).
    // The update rate controls how often the tag will be checked. This generally doesn't need to be set, unless you're concerned about script efficiency.
    // Never set this to faster than queue update rate.
    //
    // Optionally specify a maximum duration to wait for (delta time).
    //
    // @Tags
    // <QueueTag.speed>
    //
    // @Usage
    // Use to delay the current queue until the player respawns (useful in a death event, for example).
    // - waituntil <player.is_spawned>
    //
    // @Usage
    // Use to delay the current queue until the player is healed, only checking once per second.
    // - waituntil rate:1s <player.health> > 15
    // -->

    public static void autoExecute(ScriptEntry scriptEntry, ScriptQueue queue,
                                   @ArgUnparsed @ArgNoDebug @ArgRaw @ArgLinear @ArgName("if_comparisons") List<ScriptEntry.InternalArgument> comparisons,
                                   @ArgPrefixed @ArgName("rate") @ArgDefaultNull DurationTag rate,
                                   @ArgPrefixed @ArgName("max") @ArgDefaultNull DurationTag max) {
        boolean run = new IfCommand.ArgComparer().compare(new ArrayList<>(comparisons), scriptEntry);
        if (run) {
            Debug.echoDebug(scriptEntry, "WaitUntil first check already <A>true<W>, not waiting.");
            scriptEntry.setFinished(true);
            return;
        }
        Debug.echoDebug(scriptEntry, "WaitUntil first check <A>false<W>, will wait...");
        if (rate == null) {
            if (queue instanceof TimedQueue) {
                rate = ((TimedQueue) queue).getSpeed();
            }
            else {
                rate = new DurationTag((long) 1);
            }
        }
        long endTime = max == null ? -1 : DenizenCore.serverTimeMillis + max.getMillis();
        final RepeatingSchedulable schedulable = new RepeatingSchedulable(null, (float) rate.getSeconds());
        schedulable.run = new Runnable() {
            public int counter = 0;
            @Override
            public void run() {
                counter++;
                if (CoreConfiguration.debugVerbose) {
                    Debug.log("WaitUntil looping: " + counter);
                }
                if (queue.getEntries().isEmpty()) {
                    Debug.echoDebug(scriptEntry, "WaitUntil stopping early: queue is empty or was externally stopped.");
                    scriptEntry.setFinished(true);
                    schedulable.cancel();
                }
                if (new IfCommand.ArgComparer().compare(new ArrayList<>(comparisons), scriptEntry)) {
                    Debug.echoDebug(scriptEntry, "WaitUntil completed after <A>" + counter + "<W> re-checks.");
                    scriptEntry.setFinished(true);
                    schedulable.cancel();
                }
                else if (endTime != -1 && endTime <= DenizenCore.serverTimeMillis) {
                    Debug.echoDebug(scriptEntry, "WaitUntil stopping early due to time out.");
                    scriptEntry.setFinished(true);
                    schedulable.cancel();
                }
            }
        };
        DenizenCore.schedule(schedulable);
    }
}
