package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class SlowWarning extends Warning {

    public static long WARNING_RATE = 10000;

    public long lastWarning;

    public SlowWarning(String message) {
        super(message);
    }

    @Override
    public void warn(ScriptQueue queue) {
        long cTime = System.currentTimeMillis();
        if (lastWarning + WARNING_RATE > cTime) {
            return;
        }
        lastWarning = cTime;
        Debug.echoError(queue, message);
    }
}
