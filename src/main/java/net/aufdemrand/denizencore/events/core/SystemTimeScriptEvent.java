package net.aufdemrand.denizencore.events.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;

import java.util.Date;

public class SystemTimeScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // system time [<HH:MM>/hourly/minutely]
    //
    // @Regex ^on system time (\d\d\:\d\d|hourly|minutely)$
    //
    // @Triggers when the system time changes to the specified value.
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
    public boolean couldMatch(ScriptContainer script, String event) {
        return event.startsWith("system time");
    }

    public Element hour;

    public ScriptEntryData data = null;

    public Element minute;

    @Override
    public ScriptEntryData getScriptEntryData() {
        return data;
    }

    @Override
    public boolean matches(ScriptContainer script, String event) {
        String time = CoreUtilities.getXthArg(2, event);
        return time.equals("minutely") || time.equals(hour.asString() + ":" + minute.asString()) || (minute.asString().equals("00") && time.equals("hourly"));
    }

    @Override
    public String getName() {
        return "SystemTime";
    }

    @Override
    public dObject getContext(String name) {
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

    public void checkTime() {
        if (!enab) {
            return;
        }
        Date date = new Date();
        // BY WHICH BLACK MAGIC DOES JAVA EXPECT US TO HANDLE DATETIMES IF NOT THIS?!
        int h = date.getHours();
        int m = date.getMinutes();
        if (lH == h && lM == m) {
            return;
        }
        lH = h;
        lM = m;
        if (h < 10) {
            hour = new Element("0" + h);
        }
        else {
            hour = new Element(h);
        }
        if (m < 10) {
            minute = new Element("0" + m);
        }
        else {
            minute = new Element(m);
        }
        data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        fire();
    }
}
