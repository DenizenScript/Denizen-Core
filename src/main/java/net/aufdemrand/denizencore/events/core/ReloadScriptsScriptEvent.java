package net.aufdemrand.denizencore.events.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;

import java.util.HashMap;

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
    public HashMap<String, dObject> getContext() {
        HashMap<String, dObject> context = super.getContext();
        context.put("haderror", new Element(hadError));
        context.put("all", new Element(all));
        context.put("sender", new Element(sender));
        return context;
    }

    public ReloadScriptsScriptEvent() {
        instance = this;
    }

    @Override
    public boolean couldMatch(ScriptContainer script, String event) {
        String lower = event.toLowerCase();
        return lower.startsWith("reload scripts") || lower.startsWith("script reload");
    }

    @Override
    public boolean matches(ScriptContainer script, String event) {
        return checkSwitch(event, "haderror", hadError ? "true" : "false")
                && checkSwitch(event, "sender", sender.equalsIgnoreCase("console") ? "server" : "player")
                && checkSwitch(event, "all", all ? "true" : "false");
    }

    @Override
    public String getName() {
        return "ReloadScripts";
    }
}
