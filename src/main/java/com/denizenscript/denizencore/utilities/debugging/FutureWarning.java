package com.denizenscript.denizencore.utilities.debugging;

public class FutureWarning extends Warning {

    public static boolean futureWarningsEnabled = false;

    public FutureWarning(String message) {
        super(message);
    }

    @Override
    public boolean testShouldWarn() {
        return futureWarningsEnabled;
    }
}
