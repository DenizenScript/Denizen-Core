package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;

public class ReloadScriptsScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // reload scripts
    // script reload
    //
    // @Switch had_error:true/false to only process the event if there either was or was not an error message.
    //
    // @Group Core
    //
    // @Triggers when Denizen scripts are reloaded.
    //
    // @Context
    // <context.had_error> returns an ElementTag(Boolean) whether there was an error.
    //
    // -->

    public static ReloadScriptsScriptEvent instance;

    public ReloadScriptsScriptEvent() {
        instance = this;
        registerCouldMatcher("reload scripts");
        registerCouldMatcher("script reload");
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

    @Override
    public String getName() {
        return "ReloadScripts";
    }
}
