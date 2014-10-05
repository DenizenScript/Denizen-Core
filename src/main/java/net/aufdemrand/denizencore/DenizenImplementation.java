package net.aufdemrand.denizencore;

import java.io.File;
import java.util.List;

/**
 * Interface representing all the information that an implementation must provide to the engine.
 */
public interface DenizenImplementation {

    /**
     * Return a list of all folders that the implementation has scripts within.
     */
    public abstract List<File> getScriptFolders();

    /**
     * Return the current version of the implementation.
     * EG, "Gamey Game 1.0 Denizen 0.9"
     */
    public abstract String getImplementationVersion();

    /**
     * Output a debug message to console.
     */
    public abstract void debugMessage(String message);

    /**
     * Output an exception to console.
     */
    public abstract void debugException(Exception ex);

    /**
     * Output an error to console.
     */
    public abstract void debugError(String error);

    /**
     * Return the name of the implementation.
     * EG, "Gamey Game".
     */
    public abstract String getImplementationName();
}
