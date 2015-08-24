package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.BracedCommand;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;

import java.util.List;

public class AsyncCommand extends BracedCommand {

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
        InstantQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId("ASYNC_COMMAND"));
        queue.run_async = true;
        queue.addEntries(((List<BracedData>) scriptEntry.getObject("braces")).get(0).value);
        queue.getAllDefinitions().putAll(scriptEntry.getResidingQueue().getAllDefinitions());
        queue.start();
    }
}
