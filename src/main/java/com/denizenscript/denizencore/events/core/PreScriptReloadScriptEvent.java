package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.scripts.ScriptEntryData;

public class PreScriptReloadScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // pre script reload
    //
    // @Regex ^on pre script reload$
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

    @Override
    public ScriptEntryData getScriptEntryData() {
        return DenizenCore.implementation.getEmptyScriptEntryData();
    }

    @Override
    public String getName() {
        return "PreScriptReload";
    }
}
