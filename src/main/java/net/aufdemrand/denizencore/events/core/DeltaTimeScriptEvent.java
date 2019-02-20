package net.aufdemrand.denizencore.events.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;

public class DeltaTimeScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // delta time [hourly/minutely/secondly]
    //
    // @Switch every <count>
    //
    // @Regex ^on delta time (hourly|minutely|secondly)$
    //
    // @Triggers every <count> seconds, minutes, or hours of game calculation time. Default repetitions count of 1.
    // This is specifically based on the rate of time advancement in the game server,
    // which is not necessarily equivalent to the real passage of time
    // (for example, this event may fire slower if the server is lagging).
    // For real time, see <@link event system time>.
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
        String countString = path.switches.get("every");
        int count = countString == null ? 1 : aH.getIntegerFrom(countString);
        if (time.equals("secondly")) {
            return seconds % count == 0;
        }
        if (time.equals("minutely")) {
            if (seconds % 60 != 0) {
                return false;
            }
            long minutes = seconds / 60;
            return minutes % count == 0;
        }
        if (time.equals("hourly")) {
            if (seconds % 3600 != 0) {
                return false;
            }
            long hours = seconds / 3600;
            return hours % count == 0;
        }
        return false;
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
