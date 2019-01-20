package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.BracedCommand;
import net.aufdemrand.denizencore.scripts.commands.Holdable;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.utilities.scheduling.OneTimeSchedulable;

import java.util.HashMap;
import java.util.List;

public class SyncCommand extends BracedCommand implements Holdable {

    @Override
    public void onEnable() {
        setBraced();
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        ScriptQueue residingQueue = scriptEntry.getResidingQueue();

        final InstantQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId("SYNC_COMMAND"));
        queue.addEntries(((List<BracedData>) scriptEntry.getObject("braces")).get(0).value);
        queue.getAllDefinitions().putAll(residingQueue.getAllDefinitions());
        if (residingQueue.cachedContext != null) {
            queue.cachedContext = new HashMap<>();
            queue.cachedContext.putAll(residingQueue.cachedContext);
        }

        // Setup a callback if the queue is being waited on
        if (scriptEntry.shouldWaitFor()) {
            // Record the ScriptEntry
            final ScriptEntry se = scriptEntry;
            queue.callBack(new Runnable() {
                @Override
                public void run() {
                    se.setFinished(true);
                }
            });
        }

        // If the current queue is asynchronous, delay this queue until the next tick
        if (residingQueue.run_async) {
            DenizenCore.schedule(new OneTimeSchedulable(new Runnable() {
                @Override
                public void run() {
                    queue.start();
                }
            }, 0));
        }
        else {
            // TODO: warn user about running SyncCommand when already sync?
            queue.start();
        }
    }
}
