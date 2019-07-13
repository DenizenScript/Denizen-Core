package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class SlowWarning {

    public static long WARNING_RATE = 10000;

    public long lastWarning;

    public String message;

    public SlowWarning(String message) {
        this.message = message;
    }

    public void warn(ScriptEntry entry) {
        warn(entry == null ? null : entry.getResidingQueue());
    }

    public void warn() {
        warn((ScriptQueue) null);
    }

    public void warn(ScriptQueue queue) {
        long cTime = System.currentTimeMillis();
        if (lastWarning + WARNING_RATE > cTime) {
            return;
        }
        lastWarning = cTime;
        Debug.echoError(queue, message);
    }
}
