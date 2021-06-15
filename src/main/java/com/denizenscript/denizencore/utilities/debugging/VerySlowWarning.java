package com.denizenscript.denizencore.utilities.debugging;

import java.util.ArrayList;

public class VerySlowWarning extends SlowWarning {

    public static ArrayList<VerySlowWarning> allSlowWarnings = new ArrayList<>();

    public boolean hasShown = false;

    public VerySlowWarning(String message) {
        super(message);
        allSlowWarnings.add(this);
    }

    @Override
    public boolean testShouldWarn() {
        if (hasShown && !FutureWarning.futureWarningsEnabled) {
            return false;
        }
        hasShown = true;
        return super.testShouldWarn();
    }
}
