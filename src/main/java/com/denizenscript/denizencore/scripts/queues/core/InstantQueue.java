package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class InstantQueue extends ScriptQueue {

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
        DenizenCore.scriptEngine.revolve(this);
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
