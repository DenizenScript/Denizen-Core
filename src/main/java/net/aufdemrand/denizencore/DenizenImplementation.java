package net.aufdemrand.denizencore;

import java.io.File;
import java.util.List;

/**
 * Abstract class representing all the information that an implementation must provide to the engine.
 */
public abstract class DenizenImplementation {

    /**
     * Return a list of all folders that the implementation has scripts within.
     */
    public abstract List<File> getScriptFolders();

    /**
     * Return the current version of the implementation.
     * EG, "Gamey Game 1.0 Denizen 0.9"
     */
    public abstract String getImplementationVersion();
}
