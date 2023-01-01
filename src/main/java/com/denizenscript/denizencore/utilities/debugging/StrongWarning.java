package com.denizenscript.denizencore.utilities.debugging;

import java.util.concurrent.ConcurrentHashMap;

public class StrongWarning extends Warning { // Note: can be called async

    public static ConcurrentHashMap<StrongWarning, Boolean> recentWarnings = new ConcurrentHashMap<>();

    public StrongWarning(String id, String message) {
        super(id, message);
    }

    @Override
    public boolean testShouldWarn() {
        recentWarnings.put(this, true);
        return true;
    }
}
