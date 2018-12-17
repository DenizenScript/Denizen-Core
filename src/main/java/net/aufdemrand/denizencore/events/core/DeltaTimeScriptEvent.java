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

    public boolean matches(ScriptContainer script, String event) {
        String time = CoreUtilities.getXthArg(2, event);
        return time.equals("secondly") || time.equals("minutely") || time.equals("hourly");
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

    static long seconds = 0;

    public void checkTime() {
        seconds++;

        if (!enabled) {
            return;
        }
        second = new Element(seconds);
        data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        fire();
    }

}
