package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;

public class ShutdownScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // shutdown
    //
    // @Group Server
    //
    // @Warning not all plugins will be loaded and delayed scripts will be dropped.
    // Also note that this event is not guaranteed to always run (eg if the server crashes).
    //
    // @Triggers when the server is shutting down.
    //
    // @Example
    // # This *might* show a message in logs during shutdown. No guarantee.
    // on shutdown:
    // - announce to_console "Last message in the logs from Denizen probably!"
    //
    // -->

    public static ShutdownScriptEvent instance;

    public ShutdownScriptEvent() {
        instance = this;
        registerCouldMatcher("shutdown");
    }

    @Override
    public String getName() {
        return "ServerShutdown";
    }
}
