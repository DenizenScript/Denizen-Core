package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

public class ScriptsLoadedScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // scripts loaded
    //
    // @Switch had_error:true/false to only process the event if there either was or was not an error message.
    //
    // @Group Core
    //
    // @Triggers when Denizen scripts are loaded, but on reloaded and on initial load.
    //
    // @Context
    // <context.had_error> returns an ElementTag(Boolean) whether there was an error.
    //
    // -->

    public static ScriptsLoadedScriptEvent instance;

    public ScriptsLoadedScriptEvent() {
        instance = this;
        registerCouldMatcher("scripts loaded");
        registerSwitches("had_error");
    }

    public boolean hadError = false;

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "had_error":
                return new ElementTag(hadError);
        }
        return super.getContext(name);
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!path.checkSwitch("had_error", hadError ? "true" : "false")) {
            return false;
        }
        return super.matches(path);
    }
}
