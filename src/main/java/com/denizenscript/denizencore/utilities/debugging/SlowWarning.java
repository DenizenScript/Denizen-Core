package com.denizenscript.denizencore.utilities.debugging;

public class SlowWarning extends Warning {

    public static long WARNING_RATE = 10000;

    public long lastWarning;

    public SlowWarning(String message) {
        super(message);
    }

    @Override
    public boolean testShouldWarn() {
        long cTime = System.currentTimeMillis();
        if (lastWarning + WARNING_RATE > cTime) {
            return false;
        }
        lastWarning = cTime;
        return true;
    }
}
