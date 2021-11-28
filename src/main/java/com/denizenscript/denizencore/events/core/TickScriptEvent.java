package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;

public class TickScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // tick
    //
    // @Switch every:<count> to only run the event every *count* times (like "every:5" for every 5 ticks).
    //
    // @Group Core
    //
    // @Warning This event fires very rapidly and is usually not the most ideal way to handle things.
    //
    // @Triggers every single tick.
    //
    // @Context
    // <context.tick> how many ticks have passed since the server started.
    //
    // -->

    public static TickScriptEvent instance;

    public TickScriptEvent() {
        instance = this;
        registerCouldMatcher("tick");
        registerSwitches("every");
    }

    public long ticks = 0;

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("tick")) {
            return new ElementTag(ticks);
        }
        return super.getContext(name);
    }

    @Override
    public boolean matches(ScriptPath path) {
        String countString = path.switches.get("every");
        int count = countString == null ? 1 : Integer.parseInt(countString);
        if (ticks % count != 0) {
             return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "Tick";
    }

    public boolean enabled = false;

    @Override
    public void init() {
        enabled = true;
    }

    @Override
    public void destroy() {
        enabled = false;
    }
}
