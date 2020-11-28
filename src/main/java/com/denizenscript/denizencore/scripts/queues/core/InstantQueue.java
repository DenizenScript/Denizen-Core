package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class InstantQueue extends ScriptQueue {

    /**
     * Gets an InstantQueue instance.
     * <p/>
     * If a queue already exists with the given id, it will return that instance,
     * which may be currently running, unless the type of Queue is not an InstantQueue.
     * If a queue does not exist, a new stopped queue is created instead.
     * <p/>
     * IDs are case insensitive.  If having an easy-to-recall ID is not necessary, just
     * pass along null as the id, and it will use ScriptQueue's static method _getNextId()
     * which will return a random UUID.
     * <p/>
     * The default speed node will be automatically read from the configuration,
     * and new ScriptQueues may need further information before they
     * can start(), including entries, delays, loops, and possibly context.
     *
     * @param id unique id of the queue
     * @return a ScriptQueue
     */
    public static InstantQueue getQueue(String id) {
        // Get id if not specified.
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null!");
        }
        InstantQueue scriptQueue;
        // Does the queue already exist?
        if (queueExists(id)) {
            scriptQueue = (InstantQueue) allQueues.get(id);
        }
        // If not, create a new one.
        else {
            scriptQueue = new InstantQueue(id);
        }
        return scriptQueue;
    }

    /////////////////////
    // Private instance fields and constructors
    /////////////////////

    public InstantQueue(String id) {
        super(id);
    }

    @Override
    public void onStart() {
        while (is_started) {
            revolve();
        }
    }

    @Override
    public void revolve() {
        if (script_entries.isEmpty()) {
            stop();
            return;
        }
        DenizenCore.getScriptEngine().revolve(this);
    }

    @Override
    public String getName() {
        return "InstantQueue";
    }

    public void onStop() {
        // Nothing to do here!
    }

    @Override
    public boolean shouldRevolve() {
        // Instant queues aren't picky!
        return true;
    }
}
