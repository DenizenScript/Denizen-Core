package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.DenizenCore;

public class ConsoleOutputScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // console output
    //
    // @Regex ^on console output$
    //
    // @Group Core
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

    public ScriptEntryData data = DenizenCore.getImplementation().getEmptyScriptEntryData();

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
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("console output");
    }

    @Override
    public String getName() {
        return "ConsoleOutput";
    }
}
