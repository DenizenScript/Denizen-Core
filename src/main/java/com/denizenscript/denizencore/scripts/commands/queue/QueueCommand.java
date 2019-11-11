package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.queues.core.Delayable;

public class QueueCommand extends AbstractCommand {

    // <--[command]
    // @Name Queue
    // @Syntax queue (<queue>) [clear/stop/pause/resume/delay:<duration>]
    // @Required 1
    // @Short Modifies the current state of a script queue.
    // @Group queue
    //
    // @Description
    // Allows queues to be modified during their run. This can also be used to modify other queues currently running.
    //
    // Clearing a queue will remove any commands still queued within it, and thus end the queue.
    // When trying to clear the current queue, use <@link command stop> instead.
    //
    // Using the "stop" argument will force the queue to immediately stop running.
    // When trying to stop the current queue, use <@link command stop> instead.
    //
    // Using the "delay:<duration>" argument will cause the queue to wait for a specified duration.
    // When trying to delay the current queue, use <@link command wait> instead.
    //
    // Using the "pause" argument will freeze the queue but keep it listed, waiting for a "resume" instruction.
    // It is of course not possible to resume the current queue (as if you're running a 'queue' command, the queue can't be paused).
    //
    // Generally, the queue is considered a non-ideal way of doing things - that is, there's usually a better/cleaner way to achieve similar results.
    // It's most useful within the "/ex" command for quick problem solving
    // (eg if a script in testing gets caught in an infinite loop, you can do "/ex queue ID_HERE stop" to fix that).
    //
    // @Tags
    // <queue>
    // <QueueTag.id>
    // <QueueTag.size>
    // <queue.list>
    // <queue.stats>
    // <queue.exists[queue_id]>
    // <ScriptTag.list_queues>
    //
    // @Usage
    // Use to force-stop a given queue.
    // - queue <server.flag[OtherQueue]> clear
    //
    // @Usage
    // Use to delay the current queue (use <@link command wait> instead!)
    // - queue delay:5t
    //
    // @Usage
    // Use to pause the given queue.
    // - queue <server.flag[OtherQueue]> pause
    //
    // @Usage
    // Use to resume the given queue.
    // - queue <server.flag[OtherQueue]> resume
    // -->

    private enum Action {CLEAR, DELAY, PAUSE, RESUME, STOP}

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (!scriptEntry.hasObject("action")
                    && arg.matchesEnum(Action.values())) {
                scriptEntry.addObject("action", Action.valueOf(arg.getValue().toUpperCase()));
                if (scriptEntry.getObject("action") == Action.DELAY
                        && arg.matchesArgumentType(DurationTag.class)) {
                    scriptEntry.addObject("delay", arg.asType(DurationTag.class));
                }
            }

            // No prefix required to specify the queue
            else if ((arg.matchesArgumentType(QueueTag.class)
                    || arg.matchesPrefix("queue"))
                    && !scriptEntry.hasObject("queue")) {
                scriptEntry.addObject("queue", arg.asType(QueueTag.class));
            }

            // ...but we also need to error out this command if the queue was not found.
            else {
                throw new InvalidArgumentsException("The specified queue could not be found: " + arg.raw_value);
            }

        }

        // If no queues have been added, assume 'residing queue'
        scriptEntry.defaultObject("queue", new QueueTag(scriptEntry.getResidingQueue()));

        // Check required args
        if (!scriptEntry.hasObject("action")) {
            throw new InvalidArgumentsException("Must specify an action. Valid: CLEAR, DELAY, PAUSE, RESUME");
        }

        if (scriptEntry.getObject("action") == Action.DELAY && !scriptEntry.hasObject("delay")) {
            throw new InvalidArgumentsException("Must specify a delay.");
        }

    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        QueueTag queue = (QueueTag) scriptEntry.getObject("queue");
        Action action = (Action) scriptEntry.getObject("action");
        DurationTag delay = (DurationTag) scriptEntry.getObject("delay");

        // Debugger
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), queue.debug()
                    + ArgumentHelper.debugObj("Action", action.toString())
                    + (action == Action.DELAY ? delay.debug() : ""));
        }

        switch (action) {

            case CLEAR:
                queue.queue.clear();
                return;

            case STOP:
                queue.queue.clear();
                queue.queue.stop();
                return;

            case PAUSE:
                if (queue.queue instanceof Delayable) {
                    ((Delayable) queue.queue).setPaused(true);
                }
                else {
                    queue.queue.forceToTimed(new DurationTag(1L)).setPaused(true);
                }
                return;

            case RESUME:
                if (queue.queue instanceof Delayable) {
                    ((Delayable) queue.queue).setPaused(false);
                }
                return;

            case DELAY:
                if (queue.queue instanceof Delayable) {
                    ((Delayable) queue.queue).delayFor(delay);
                }
                else {
                    queue.queue.forceToTimed(delay);
                }
                return;
        }
    }
}
