package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.DurationTag;
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

    public WaitUntilCommand() {
        setName("waituntil");
        setSyntax("waituntil (rate:<duration>) (max:<duration>) [<comparisons>]");
        setRequiredArguments(1, -1);
        setParseArgs(false);
        forceHold = true;
        isProcedural = false; // A procedure can't wait
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
    // Optionally specify a maximum duration to wait for.
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

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        List<String> arguments = new ArrayList<>(scriptEntry.getOriginalArguments());
        for (Argument arg : scriptEntry) {
            if (arg.matchesPrefix("rate")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("rate", arg.asType(DurationTag.class));
                arguments.remove(0);
            }
            else if (arg.matchesPrefix("max")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("max", arg.asType(DurationTag.class));
                arguments.remove(0);
            }
            else {
                break;
            }
        }
        scriptEntry.addObject("comparisons", arguments);
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        List<String> comparisons = (List<String>) scriptEntry.getObject("comparisons");
        DurationTag rate = scriptEntry.getObjectTag("rate");
        DurationTag max = scriptEntry.getObjectTag("max");
        boolean run = new IfCommand.ArgComparer().compare(new ArrayList<>(comparisons), scriptEntry);
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), ArgumentHelper.debugObj("run_first_check", run), rate, max);
        }
        if (run) {
            scriptEntry.setFinished(true);
            return;
        }
        if (rate == null) {
            if (scriptEntry.getResidingQueue() instanceof TimedQueue) {
                rate = ((TimedQueue) scriptEntry.getResidingQueue()).getSpeed();
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
                if (new IfCommand.ArgComparer().compare(new ArrayList<>(comparisons), scriptEntry)) {
                    Debug.echoDebug(scriptEntry, "WaitUntil completed after " + counter + " re-checks.");
                    scriptEntry.setFinished(true);
                    schedulable.cancel();
                }
                else if (endTime != -1 && endTime < DenizenCore.serverTimeMillis) {
                    Debug.echoDebug(scriptEntry, "WaitUntil completed due to time out.");
                    scriptEntry.setFinished(true);
                    schedulable.cancel();
                }
            }
        };
        DenizenCore.schedule(schedulable);
    }
}
