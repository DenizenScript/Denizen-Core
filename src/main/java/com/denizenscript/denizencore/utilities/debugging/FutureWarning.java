package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.utilities.CoreConfiguration;

public class FutureWarning extends Warning {

    public FutureWarning(String message) {
        super(message);
    }

    @Override
    public boolean testShouldWarn() {
        return CoreConfiguration.futureWarningsEnabled;
    }
}
