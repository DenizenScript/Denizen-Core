package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class SlowWarning extends Warning {

    public long lastWarning;

    public SlowWarning(String message) {
        super(message);
    }

    @Override
    public boolean testShouldWarn() {
        long cTime = CoreUtilities.monotonicMillis();
        if (lastWarning + CoreConfiguration.deprecationWarningRate > cTime) {
            return false;
        }
        lastWarning = cTime;
        return true;
    }
}
