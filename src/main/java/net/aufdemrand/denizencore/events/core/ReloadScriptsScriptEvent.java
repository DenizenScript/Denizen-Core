package net.aufdemrand.denizencore.events.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;

public class ReloadScriptsScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // reload scripts
    // script reload
    //
    // @Switch haderror true|false
    // @Switch sender player|server
    // @Switch all true|false
    //
    // @Regex ^on ((reload scripts)|(script reload))$
    //
    // @Triggers when Denizen scripts are reloaded.
    //
    // @Context
    // <context.sender> returns an Element of the name of the sender who triggered the reload.
    // <context.all> returns an Element(Boolean) of whether 'reload -a' was used.
    // <context.haderror> returns an Element(Boolean) whether there was an error.
    //
    // -->

    public static ReloadScriptsScriptEvent instance;

    public boolean hadError = false;

    public boolean all = false;

    public String sender = null;

    public ScriptEntryData data = null;

    @Override
    public void reset() {
        hadError = false;
        sender = null;
        all = false;
        data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        super.reset();
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return data;
    }

    @Override
    public dObject getContext(String name) {
        if (name.equals("haderror")) {
            return new Element(hadError);
        }
        else if (name.equals("all")) {
            return new Element(all);
        }
        else if (name.equals("sender")) {
            return new Element(sender);
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
                && path.checkSwitch("sender", sender.equalsIgnoreCase("console") ? "server" : "player")
                && path.checkSwitch("all", all ? "true" : "false");
    }

    @Override
    public String getName() {
        return "ReloadScripts";
    }
}
