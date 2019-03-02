package net.aufdemrand.denizencore.events.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;

public class ConsoleOutputScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // console output
    //
    // @Regex ^on console output$
    //
    // @Cancellable true
    //
    // @Triggers when any message is printed to console. (Requires <@link mechanism system.redirect_logging> be set true.)
    // @Context
    // <context.message> returns the message that is being printed to console.
    //
    // -->

    public static ConsoleOutputScriptEvent instance;

    public String message = null;

    public ScriptEntryData data = null;

    @Override
    public void reset() {
        message = null;
        data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        super.reset();
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return data;
    }

    @Override
    public dObject getContext(String name) {
        if (name.equals("message")) {
            return new Element(message);
        }
        return super.getContext(name);
    }

    public ConsoleOutputScriptEvent() {
        instance = this;
    }

    @Override
    public boolean couldMatch(ScriptContainer script, String event) {
        String lower = CoreUtilities.toLowerCase(event);
        return lower.startsWith("console output");
    }

    @Override
    public boolean matches(ScriptPath path) {
        return true;
    }

    @Override
    public String getName() {
        return "ConsoleOutput";
    }
}
