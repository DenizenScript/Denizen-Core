package net.aufdemrand.denizencore.scripts.queues;

import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.CommandExecuter;
import net.aufdemrand.denizencore.scripts.queues.core.Delayable;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class ScriptEngine {


    final private CommandExecuter commandExecuter;


    public ScriptEngine() {
        // Create Denizen CommandExecuter
        commandExecuter = new CommandExecuter();
    }


    boolean shouldHold(ScriptQueue scriptQueue) {
        if (scriptQueue instanceof Delayable && ((Delayable) scriptQueue).isPaused())
            return true;
        if (scriptQueue.getLastEntryExecuted() != null
                && scriptQueue.getLastEntryExecuted().shouldWaitFor()) {
            if (!(scriptQueue instanceof Delayable)) {
                dB.echoDebug(scriptQueue.getLastEntryExecuted(), "Forcing queue " + scriptQueue.id + " into a timed queue...");
                scriptQueue.forceToTimed(null);
                return true;
            }
            else {
                return true;
            }
        }
        return false;
    }

    public void revolve(ScriptQueue scriptQueue) {
        // Check last ScriptEntry to see if it should be waited for
        if (shouldHold(scriptQueue))
            return;

        // Okay to run next scriptEntry
        ScriptEntry scriptEntry = scriptQueue.getNext();

        // Loop through the entries
        while (scriptEntry != null) {
            // Mark script entry with Queue that is sending it to the executor
            scriptEntry.setSendingQueue(scriptQueue);

            try {
                // Execute the scriptEntry
                getScriptExecuter().execute(scriptEntry);
            }
            // Absolutely NO errors beyond this point!
            catch (Throwable e) {
                dB.echoError(scriptEntry.getResidingQueue(), "Woah! An exception has been called with this command!");
                dB.echoError(scriptEntry.getResidingQueue(), e);
            }
            // Set as last entry executed
            scriptQueue.setLastEntryExecuted(scriptEntry);

            // Check if the scriptQueue is delayed (EG, via wait)
            if (scriptQueue instanceof Delayable) {
                if (((Delayable) scriptQueue).isDelayed())
                    break;
                if (((Delayable) scriptQueue).isPaused())
                    break;
            }

            // If the entry is instant, and not injected, get the next Entry
            if (scriptEntry.isInstant()) {
                // If it's holding, even if it's instant, just stop and wait
                if (shouldHold(scriptQueue))
                    return;
                // Remove from execution list
                scriptEntry = scriptQueue.getNext();
            }

            // If entry isn't instant, end the revolutions and wait
            else
                break;
        }
    }

    /**
     * Gets the currently loaded instance of the Command Executer
     *
     * @return CommandExecuter
     */
    public CommandExecuter getScriptExecuter() {
        return commandExecuter;
    }
}
