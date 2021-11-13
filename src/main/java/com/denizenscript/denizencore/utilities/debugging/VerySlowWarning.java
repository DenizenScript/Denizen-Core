package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;

public class VerySlowWarning extends SlowWarning {

    /**
     * Last reload ID this warning was shown for, or -1 if never shown.
     */
    public int lastShown = -1;

    public VerySlowWarning(String message) {
        super(message);
    }

    @Override
    public boolean testShouldWarn() {
        if (lastShown == DenizenCore.reloads && !FutureWarning.futureWarningsEnabled) {
            return false;
        }
        lastShown = DenizenCore.reloads;
        return super.testShouldWarn();
    }
}
