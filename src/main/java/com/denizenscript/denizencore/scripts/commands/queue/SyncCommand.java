package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;

import java.util.HashMap;
import java.util.List;

public class SyncCommand extends BracedCommand implements Holdable {

    // <--[command]
    // @Name Sync
    // @Syntax sync [<commands>]
    // @Required 0
    // @Short Runs commands synchronously. Inverse of <@link command async>.
    // @Group queue
    //
    // @Description
    // Runs commands synchronously. This means that anything executed within will run on the
    // main server thread, without the possibility of corrupting anything that an asynchronous
    // queue could theoretically do. This is only needed for use alongside <@link command async>.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to perform possibly not thread-safe commands.
    // - sync:
    //   - edit the world, etc
    //
    // -->

    @Override
    public void onEnable() {
        setBraced();
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        Debug.echoError("WARNING: THE 'SYNC' COMMAND SHOULD **NEVER** BE USED.");

        ScriptQueue residingQueue = scriptEntry.getResidingQueue();

        final InstantQueue queue = new InstantQueue("SYNC_COMMAND");
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
