package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.scripts.commands.CommandExecuter;
import com.denizenscript.denizencore.scripts.queues.core.Delayable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.ScriptEntry;

public class ScriptEngine {

    final private CommandExecuter commandExecuter;

    public ScriptEngine() {
        // Create Denizen CommandExecuter
        commandExecuter = new CommandExecuter();
    }

    boolean shouldHold(ScriptQueue scriptQueue) {
        if (scriptQueue instanceof Delayable && ((Delayable) scriptQueue).isPaused()) {
            return true;
        }
        if (scriptQueue.getLastEntryExecuted() != null
                && scriptQueue.getLastEntryExecuted().shouldWaitFor()) {
            if (!(scriptQueue instanceof Delayable)) {
                Debug.echoDebug(scriptQueue.getLastEntryExecuted(), "Forcing queue " + scriptQueue.id + " into a timed queue...");
                scriptQueue.forceToTimed(null);
                return true;
            }
            else {
                return true;
            }
        }
        return false;
    }

    public void revolveOnceForce(ScriptQueue scriptQueue) {
        ScriptEntry scriptEntry = scriptQueue.getNext();
        if (scriptEntry == null) {
            return;
        }
        scriptEntry.setSendingQueue(scriptQueue);
        scriptQueue.setLastEntryExecuted(scriptEntry);
        try {
            getScriptExecuter().execute(scriptEntry);
        }
        catch (Throwable e) {
            Debug.echoError(scriptEntry.getResidingQueue(), "An exception has been called with this command (while revolving the queue forcefully)!");
            Debug.echoError(scriptEntry.getResidingQueue(), e);
        }
    }

    public void revolve(ScriptQueue scriptQueue) {
        // Check last ScriptEntry to see if it should be waited for
        if (shouldHold(scriptQueue)) {
            return;
        }

        // Okay to run next scriptEntry
        ScriptEntry scriptEntry = scriptQueue.getNext();

        // Loop through the entries
        while (scriptEntry != null) {
            // Mark script entry with Queue that is sending it to the executor
            scriptEntry.setSendingQueue(scriptQueue);

            // Set as last entry executed
            scriptQueue.setLastEntryExecuted(scriptEntry);
            try {
                // Execute the scriptEntry
                getScriptExecuter().execute(scriptEntry);
            }
            // Absolutely NO errors beyond this point!
            catch (Throwable e) {
                Debug.echoError(scriptEntry.getResidingQueue(), "Woah! An exception has been called with this command (while revolving the queue)!");
                Debug.echoError(scriptEntry.getResidingQueue(), e);
            }

            // Check if the scriptQueue is delayed (EG, via wait)
            if (scriptQueue instanceof Delayable) {
                if (((Delayable) scriptQueue).isDelayed()) {
                    break;
                }
                if (((Delayable) scriptQueue).isPaused()) {
                    break;
                }
                if (((Delayable) scriptQueue).isInstantSpeed() || scriptEntry.isInstant()) {
                    if (shouldHold(scriptQueue)) {
                        return;
                    }
                    scriptEntry = scriptQueue.getNext();
                }
                else {
                    break;
                }
            }
            else if (scriptEntry.isInstant()) {
                if (shouldHold(scriptQueue)) {
                    return;
                }
                scriptEntry = scriptQueue.getNext();
            }
            else {
                break;
            }
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
