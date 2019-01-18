package net.aufdemrand.denizencore.events.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;

public class DeltaTimeScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // delta time [hourly/minutely/secondly]
    //
    // @Regex ^on delta time (hourly|minutely|secondly)$
    //
    // @Triggers every second, minute, or hour of game calculation time.
    //
    // @Context
    // <context.second> returns the exact delta time since system start.
    //
    // -->

    public static DeltaTimeScriptEvent instance;

    public DeltaTimeScriptEvent() {
        instance = this;
    }

    public boolean couldMatch(ScriptContainer script, String event) {
        return event.startsWith("delta time");
    }

    public boolean matches(ScriptPath path) {
        String time = path.eventArgLowerAt(2);
        long seconds = DenizenCore.serverTimeMillis / 1000;
        return time.equals("secondly") || (seconds % 60 == 0 && time.equals("minutely")) || (seconds % 3600 == 0 && time.equals("hourly"));
    }

    public ScriptEntryData data = null;

    public ScriptEntryData getScriptEntryData() {
        return data;
    }

    public String getName() {
        return "DeltaTime";
    }

    public Element second;

    @Override
    public dObject getContext(String name) {
        if (name.equals("second")) {
            return second;
        }
        return super.getContext(name);
    }

    boolean enabled = false;

    public void init() {
        enabled = true;
    }

    public void destroy() {
        enabled = false;
    }

    public void checkTime() {
        if (!enabled) {
            return;
        }
        second = new Element(DenizenCore.serverTimeMillis / 1000);
        data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        fire();
    }

}
