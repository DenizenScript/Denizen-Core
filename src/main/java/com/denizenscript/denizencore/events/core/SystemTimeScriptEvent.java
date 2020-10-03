package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.DenizenCore;

import java.util.Calendar;

public class SystemTimeScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // system time [<HH:MM>/hourly/minutely/secondly]
    //
    // @Switch every:<count> to only run the event every *count* times (like "on system time secondly every:5" for every 5 seconds).
    //
    // @Regex ^on system time( (\d\d\:\d\d|hourly|minutely|secondly))?$
    //
    // @Group Core
    //
    // @Triggers when the system time changes to the specified value.
    // The system time is the real world time set in the server's operating system.
    // It is not necessarily in sync with the game server time, which may vary (for example, when the server is lagging).
    // For events based on in-game time passage, use <@link event delta time> or <@link command wait>.
    //
    // @Context
    // <context.hour> returns the exact hour of the system time.
    // <context.minute> returns the exact minute of the system time.
    //
    // -->

    public static SystemTimeScriptEvent instance;

    public SystemTimeScriptEvent() {
        instance = this;
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("system time");
    }

    public ElementTag hour;

    public ScriptEntryData data = null;

    public ElementTag minute;

    public long seconds;

    @Override
    public ScriptEntryData getScriptEntryData() {
        return data;
    }

    @Override
    public boolean matches(ScriptPath path) {
        String time = path.rawEventArgAt(2);
        String countString = path.switches.get("every");
        int count = countString == null ? 1 : Integer.parseInt(countString);
        if (time.equals("secondly")) {
            if (seconds % count != 0) {
                return false;
            }
        }
        else if (time.equals("minutely")) {
            if (!minuteChanged) {
                return false;
            }
            long minutes = seconds / 60;
            if (minutes % count != 0) {
                return false;
            }
        }
        else if (time.equals("hourly")) {
            if (!minuteChanged || lM != 0) {
                return false;
            }
            long hours = seconds / 3600;
            if (hours % count != 0) {
                return false;
            }
        }
        else if (!minuteChanged || !time.equals(hour.asString() + ":" + minute.asString())) {
            return false;
        }
        return super.matches(path);
    }

    @Override
    public String getName() {
        return "SystemTime";
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("hour")) {
            return hour;
        }
        else if (name.equals("minute")) {
            return minute;
        }
        return super.getContext(name);
    }

    boolean enab = false;

    @Override
    public void init() {
        enab = true;
    }

    @Override
    public void destroy() {
        enab = false;
    }

    int lH = 0;
    int lM = 0;
    long lS = 0;
    boolean minuteChanged = true;

    public void checkTime() {
        if (!enab) {
            return;
        }
        seconds = System.currentTimeMillis() / 1000;
        if (lS == seconds) {
            return;
        }
        lS = seconds;
        Calendar calendar = Calendar.getInstance();
        int h = calendar.get(Calendar.HOUR_OF_DAY);
        int m = calendar.get(Calendar.MINUTE);
        minuteChanged = lH != h || lM != m;
        lH = h;
        lM = m;
        if (h < 10) {
            hour = new ElementTag("0" + h);
        }
        else {
            hour = new ElementTag(h);
        }
        if (m < 10) {
            minute = new ElementTag("0" + m);
        }
        else {
            minute = new ElementTag(m);
        }
        data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        fire();
    }
}
