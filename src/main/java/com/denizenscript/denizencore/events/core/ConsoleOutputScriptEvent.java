package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.DenizenCore;

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
    //
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
    public ObjectTag getContext(String name) {
        if (name.equals("message")) {
            return new ElementTag(message);
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
