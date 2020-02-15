package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.scripts.commands.CommandExecutor;
import com.denizenscript.denizencore.scripts.queues.core.Delayable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.ScriptEntry;

public class ScriptEngine {

    public final CommandExecutor commandExecutor;

    public ScriptEngine() {
        commandExecutor = new CommandExecutor();
    }

    boolean shouldHold(ScriptQueue scriptQueue) {
        if (scriptQueue instanceof Delayable && ((Delayable) scriptQueue).isPaused()) {
            return true;
        }
        if (scriptQueue.getLastEntryExecuted() == null || !scriptQueue.getLastEntryExecuted().shouldWaitFor()) {
            return false;
        }
        if (!(scriptQueue instanceof Delayable)) {
            Debug.echoDebug(scriptQueue.getLastEntryExecuted(), "Forcing queue " + scriptQueue.id + " into a timed queue...");
            scriptQueue.forceToTimed(null);
        }
        return true;
    }

    public void revolveOnceForce(ScriptQueue scriptQueue) {
        ScriptEntry scriptEntry = scriptQueue.getNext();
        if (scriptEntry == null) {
            return;
        }
        scriptEntry.setSendingQueue(scriptQueue);
        scriptEntry.updateContext();
        scriptQueue.setLastEntryExecuted(scriptEntry);
        try {
            commandExecutor.execute(scriptEntry);
        }
        catch (Throwable e) {
            Debug.echoError(scriptEntry.getResidingQueue(), "An exception has been called with this command (while revolving the queue forcefully)!");
            Debug.echoError(scriptEntry.getResidingQueue(), e);
        }
    }

    public void revolve(ScriptQueue scriptQueue) {
        if (shouldHold(scriptQueue)) {
            return;
        }
        ScriptEntry scriptEntry = scriptQueue.getNext();
        while (scriptEntry != null) {
            scriptEntry.setSendingQueue(scriptQueue);
            scriptEntry.updateContext();
            scriptQueue.setLastEntryExecuted(scriptEntry);
            commandExecutor.execute(scriptEntry);
            if (scriptQueue instanceof Delayable) {
                Delayable delayedQueue = (Delayable) scriptQueue;
                if (delayedQueue.isDelayed() || delayedQueue.isPaused()) {
                    break;
                }
                if (delayedQueue.isInstantSpeed() || scriptEntry.isInstant()) {
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
}
