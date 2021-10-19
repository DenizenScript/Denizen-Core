package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;

public class ShutdownScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // shutdown
    //
    // @Regex ^on shutdown$
    //
    // @Group Server
    //
    // @Warning not all plugins will be loaded and delayed scripts will be dropped.
    // Also note that this event is not guaranteed to always run (eg if the server crashes).
    //
    // @Triggers when the server is shutting down.
    //
    // -->

    public ShutdownScriptEvent() {
        instance = this;
    }

    public static ShutdownScriptEvent instance;

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("shutdown")) {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "ServerShutdown";
    }
}
