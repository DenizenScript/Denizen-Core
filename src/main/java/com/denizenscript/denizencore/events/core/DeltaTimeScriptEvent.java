package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.DenizenCore;

public class DeltaTimeScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // delta time [hourly/minutely/secondly]
    //
    // @Switch every:<count> to only run the event every *count* times (like "on delta time secondly every:5" for every 5 seconds).
    //
    // @Regex ^on delta time (hourly|minutely|secondly)$
    //
    // @Group Core
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

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("delta time");
    }

    public boolean matches(ScriptPath path) {
        String time = path.rawEventArgAt(2);
        long seconds = DenizenCore.serverTimeMillis / 1000;
        String countString = path.switches.get("every");
        int count = countString == null ? 1 : Integer.parseInt(countString);
        if (time.equals("secondly")) {
            if (seconds % count != 0) {
                return false;
            }
        }
        else if (time.equals("minutely")) {
            if (seconds % 60 != 0) {
                return false;
            }
            long minutes = seconds / 60;
            if (minutes % count != 0) {
                return false;
            }
        }
        else if (time.equals("hourly")) {
            if (seconds % 3600 != 0) {
                return false;
            }
            long hours = seconds / 3600;
            if (hours % count != 0) {
                return false;
            }
        }
        else {
            return false;
        }
        return super.matches(path);
    }

    public ScriptEntryData data = null;

    public ScriptEntryData getScriptEntryData() {
        return data;
    }

    public String getName() {
        return "DeltaTime";
    }

    public ElementTag second;

    @Override
    public ObjectTag getContext(String name) {
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
        second = new ElementTag(DenizenCore.serverTimeMillis / 1000);
        data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        fire();
    }

}
