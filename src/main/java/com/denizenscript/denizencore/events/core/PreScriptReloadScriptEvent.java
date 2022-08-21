package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;

public class PreScriptReloadScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // pre script reload
    //
    // @Group Core
    //
    // @Triggers immediately before Denizen scripts are reloaded.
    //
    // -->

    public static PreScriptReloadScriptEvent instance;

    public PreScriptReloadScriptEvent() {
        instance = this;
        registerCouldMatcher("pre script reload");
    }
}
