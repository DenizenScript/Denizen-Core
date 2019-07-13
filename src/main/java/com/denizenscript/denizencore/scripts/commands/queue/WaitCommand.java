package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.Duration;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.Delayable;

public class WaitCommand extends AbstractCommand {

    // <--[command]
    // @Name Wait
    // @Syntax wait (<duration>) (queue:<name>)
    // @Required 0
    // @Short Delays a script for a specified amount of time.
    // @Group queue
    //
    // @Description
    // Pauses the script queue for the duration specified. If no duration is specified it defaults to 3 seconds.
    // Accepts the 'queue:<name>' argument which allows the delay of a different queue.
    //
    // @Tags
    // <q@queue.speed>
    //
    // @Usage
    // Use to delay the current queue for 1 minute.
    // - wait 1m
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        // Initialize required fields
        ScriptQueue queue = scriptEntry.getResidingQueue();
        Duration delay = new Duration(3);

        // Iterate through arguments
        for (String arg : scriptEntry.getArguments()) {

            // Set duration
            if (ArgumentHelper.matchesDuration(arg)) {
                delay = Duration.valueOf(arg);
            }

            // Specify queue
            if (ArgumentHelper.matchesQueue(arg)) {
                queue = ScriptQueue._getExistingQueue(arg);
            }
        }

        scriptEntry.addObject("queue", queue);
        scriptEntry.addObject("delay", delay);
    }


    @Override
    public void execute(ScriptEntry scriptEntry) {

        ScriptQueue queue = (ScriptQueue) scriptEntry.getObject("queue");
        Duration delay = (Duration) scriptEntry.getObject("delay");

        if (scriptEntry.dbCallShouldDebug()) {

            Debug.report(scriptEntry, getName(),
                    ArgumentHelper.debugObj("queue", queue.id) + delay.debug());

        }

        // Tell the queue to delay
        if (queue instanceof Delayable) {
            ((Delayable) queue).delayFor(delay);
        }
        else {
            scriptEntry.setInstant(false);
            Debug.echoDebug(scriptEntry, "Forcing queue " + queue.id + " into a timed queue...");
            queue.forceToTimed(delay);
        }
    }
}
