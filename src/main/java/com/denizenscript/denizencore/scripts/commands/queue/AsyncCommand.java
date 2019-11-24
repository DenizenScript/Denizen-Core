package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.List;

public class AsyncCommand extends BracedCommand implements Holdable {

    // <--[command]
    // @Name Async
    // @Syntax async [<commands>]
    // @Required 0
    // @Short Runs commands asynchronously. Inverse of <@link command sync>. (WARNING: DO NOT USE. See description for safety warning!)
    // @Warning Do NOT use this. This is VERY dangerous. It is added purely as a tech experiment, not something to use in serious code!
    // @Group queue
    //
    // @Description
    // Runs commands asynchronously. This means that anything executed within will run off the main
    // thread of the server, allowing server-intensive scripts to have virtually no impact on the amount
    // of time between ticks (AKA majorly reduces lag).
    //
    // Generally, this command is only recommended for those who know what they're doing with it, as there
    // is always a slight possibility of corruption.
    //
    // The safety of things such as editing worlds is NOT guaranteed.
    //
    // Do NOT use this. This is VERY dangerous. It is added purely as a tech experiment, not something to use in serious code!
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to perform intensive commands without major lag.
    // - async:
    //   - repeat 100:
    //     - some intensive command
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

        Debug.echoError("WARNING: THE 'ASYNC' COMMAND SHOULD **NEVER** BE USED.");

        InstantQueue queue = new InstantQueue("ASYNC_COMMAND");
        queue.run_async = true;
        queue.addEntries(((List<BracedData>) scriptEntry.getObject("braces")).get(0).value);
        queue.getAllDefinitions().putAll(scriptEntry.getResidingQueue().getAllDefinitions());
        queue.contextSource = scriptEntry.getResidingQueue().contextSource;
        queue.cachedContext = scriptEntry.getResidingQueue().cachedContext;

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

        queue.start();
    }
}
