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

    @Override
    public ScriptEntryData getScriptEntryData() {
        return DenizenCore.getImplementation().getEmptyScriptEntryData();
    }

    public PreScriptReloadScriptEvent() {
        instance = this;
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("pre script reload");
    }

    @Override
    public String getName() {
        return "PreScriptReload";
    }
}
