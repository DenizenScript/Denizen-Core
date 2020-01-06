package com.denizenscript.denizencore.utilities.debugging;

import java.util.HashSet;

public class StrongWarning extends Warning {

    public static HashSet<StrongWarning> recentWarnings = new HashSet<>();

    public StrongWarning(String message) {
        super(message);
    }

    @Override
    public boolean testShouldWarn() {
        recentWarnings.add(this);
        return true;
    }
}
