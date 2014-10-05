package net.aufdemrand.denizencore;

import java.io.File;
import java.util.List;

/**
 * The entry point of the core Denizen engine.
 */
public class DenizenCore {

    static DenizenImplementation implementation;

    public static DenizenImplementation getImplementation() {
        return implementation;
    }

    /**
     * Must be called first: prepares the engine!
     * @param implementation your Denizen implementation.
     */
    public static void Init(DenizenImplementation implementation) {
        DenizenCore.implementation = implementation;
    }

    public static void LoadScripts() {
        try {
            List<File> folders = implementation.getScriptFolders();
        }
        catch (Exception ex) {
            implementation.debugMessage("Error loading scripts:");
            implementation.debugException(ex);
        }
    }
}
