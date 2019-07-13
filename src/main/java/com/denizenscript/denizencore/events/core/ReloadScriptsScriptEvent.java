package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.DenizenCore;

public class ReloadScriptsScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // reload scripts
    // script reload
    //
    // @Switch haderror true|false
    // @Switch all true|false
    //
    // @Regex ^on ((reload scripts)|(script reload))$
    //
    // @Triggers when Denizen scripts are reloaded.
    //
    // @Context
    // <context.all> returns an Element(Boolean) of whether 'reload -a' was used.
    // <context.haderror> returns an Element(Boolean) whether there was an error.
    //
    // -->

    public static ReloadScriptsScriptEvent instance;

    public boolean hadError = false;

    public boolean all = false;

    public ScriptEntryData data = null;

    @Override
    public void reset() {
        hadError = false;
        all = false;
        data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        super.reset();
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return data;
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("haderror")) {
            return new ElementTag(hadError);
        }
        else if (name.equals("all")) {
            return new ElementTag(all);
        }
        return super.getContext(name);
    }

    public ReloadScriptsScriptEvent() {
        instance = this;
    }

    @Override
    public boolean couldMatch(ScriptContainer script, String event) {
        String lower = CoreUtilities.toLowerCase(event);
        return lower.startsWith("reload scripts") || lower.startsWith("script reload");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return path.checkSwitch("haderror", hadError ? "true" : "false")
                && path.checkSwitch("all", all ? "true" : "false");
    }

    @Override
    public String getName() {
        return "ReloadScripts";
    }
}
