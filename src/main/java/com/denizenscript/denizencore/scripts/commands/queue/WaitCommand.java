package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.queues.core.Delayable;

public class WaitCommand extends AbstractCommand {

    public WaitCommand() {
        setName("wait");
        setSyntax("wait (<duration>) (queue:<name>)");
        setRequiredArguments(0, 2);
        isProcedural = false; // A procedure can't wait
    }

    // <--[command]
    // @Name Wait
    // @Syntax wait (<duration>) (queue:<name>)
    // @Required 0
    // @Maximum 2
    // @Short Delays a script for a specified amount of time.
    // @Group queue
    //
    // @Description
    // Pauses the script queue for the duration specified. If no duration is specified it defaults to 3 seconds.
    // Accepts the 'queue:<name>' argument which allows the delay of a different queue.
    //
    // @Tags
    // <QueueTag.speed>
    //
    // @Usage
    // Use to delay the current queue for 1 minute.
    // - wait 1m
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (arg.matchesArgumentType(DurationTag.class)
                    && !scriptEntry.hasObject("delay")) {
                scriptEntry.addObject("delay", arg.asType(DurationTag.class));
            }
            else if (arg.matchesArgumentType(QueueTag.class)
                    && !scriptEntry.hasObject("queue")) {
                scriptEntry.addObject("delay", arg.asType(QueueTag.class));
            }
            else {
                arg.reportUnhandled();
            }
        }

        scriptEntry.defaultObject("queue", new QueueTag(scriptEntry.getResidingQueue()));
        scriptEntry.defaultObject("delay", new DurationTag(3));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        QueueTag queue = scriptEntry.getObjectTag("queue");
        DurationTag delay = scriptEntry.getObjectTag("delay");

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(),
                    queue.debug() + delay.debug());
        }

        // Tell the queue to delay
        if (queue.queue instanceof Delayable) {
            ((Delayable) queue.queue).delayFor(delay);
        }
        else {
            scriptEntry.setInstant(false);
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.echoDebug(scriptEntry, "Forcing queue " + queue.queue.id + " into a timed queue...");
            }
            queue.queue.forceToTimed(delay);
        }
    }
}
