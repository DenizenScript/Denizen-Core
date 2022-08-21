package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;

public class ConsoleOutputScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // console output
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

    public ConsoleOutputScriptEvent() {
        instance = this;
        registerCouldMatcher("console output");
    }

    public String message = null;

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "message": return new ElementTag(message);
        }
        return super.getContext(name);
    }
}
