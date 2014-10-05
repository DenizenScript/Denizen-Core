package net.aufdemrand.denizencore;

import net.aufdemrand.denizencore.scripts.ScriptHelper;
import net.aufdemrand.denizencore.utilities.debugging.dB;

/**
 * The entry point of the core Denizen engine.
 */
public class DenizenCore {

    public final static String VERSION = "1.0";

    static DenizenImplementation implementation;

    public static DenizenImplementation getImplementation() {
        return implementation;
    }

    /**
     * Must be called first: prepares the engine!
     * @param implementation your Denizen implementation.
     */
    public static void init(DenizenImplementation implementation) {
        DenizenCore.implementation = implementation;
        dB.log("Initializing Denizen Core v" + VERSION +
                ", implementation for " + implementation.getImplementationName()
                + " version " + implementation.getImplementationVersion());
    }

    /**
     * Called after all objects are added and registered, to calculate
     * everything that needs calculating.
     */
    public static void initSecondary() {

    }

    /**
     * Called last in the init sequence, loads all scripts and starts the Denizen engine.
     */
    public static void loadScripts() {
        try {
            implementation.preScriptReload();
            ScriptHelper.resetError();
            ScriptHelper.reloadScripts();
            implementation.onScriptReload();
        }
        catch (Exception ex) {
            implementation.debugMessage("Error loading scripts:");
            implementation.debugException(ex);
        }
    }

    /**
     * Call when a script reload is required (EG, requested by user command.)
     */
    public static void reloadScripts() {
        loadScripts();
    }
}
