package net.aufdemrand.denizencore;

import net.aufdemrand.denizencore.utilities.YamlConfiguration;

import java.io.File;
import java.util.List;

/**
 * Interface representing all the information that an implementation must provide to the engine.
 */
public interface DenizenImplementation {

    /**
     * Return a list of all folders that the implementation has scripts within.
     */
    public abstract File getScriptFolder();

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
     * Output an 'Okay!' message.
     */
    public abstract void debugApproval(String message);

    /**
     * Return the name of the implementation.
     * EG, "Gamey Game".
     */
    public abstract String getImplementationName();

    /**
     * Run any code that fires before a script reload goes through,
     * EG, clearing custom data.
     */
    public abstract void preScriptReload();

    /**
     * Run any code that fires after a script reload goes through,
     * EG, running a public Reload event.
     */
    public abstract void onScriptReload();

    /**
     * Temporary.
     */
    public abstract void buildCoreContainers(YamlConfiguration yamlScripts);

    public abstract List<YamlConfiguration> getOutsideScripts();
}
