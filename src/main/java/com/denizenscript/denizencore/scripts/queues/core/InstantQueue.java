package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.scripts.queues.ScriptEngine;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class InstantQueue extends ScriptQueue {

    public InstantQueue(String id) {
        super(id);
    }

    @Override
    public void onStart() {
        while (is_started) {
            if (script_entries.isEmpty() && holdingOn == null) {
                stop();
                return;
            }
            ScriptEngine.revolve(this);
        }
    }

    @Override
    public String getName() {
        return "InstantQueue";
    }
}
