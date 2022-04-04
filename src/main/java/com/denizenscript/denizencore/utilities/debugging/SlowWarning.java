package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.utilities.CoreConfiguration;

public class SlowWarning extends Warning {

    public long lastWarning;

    public SlowWarning(String message) {
        super(message);
    }

    @Override
    public boolean testShouldWarn() {
        long cTime = System.currentTimeMillis();
        if (lastWarning + CoreConfiguration.deprecationWarningRate > cTime) {
            return false;
        }
        lastWarning = cTime;
        return true;
    }
}
