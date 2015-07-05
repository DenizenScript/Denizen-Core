package net.aufdemrand.denizencore;

import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;
import net.aufdemrand.denizencore.utilities.debugging.Debuggable;
import net.aufdemrand.denizencore.utilities.debugging.dB.DebugElement;

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
    public abstract void debugException(Throwable ex);

    /**
     * Output an error to console.
     */
    public abstract void debugError(String error);

    /**
     * Output an error to console, specific to a script queue.
     */
    public abstract void debugError(ScriptQueue queue, String error);

    /**
     * Output an error to console, specific to a script queue.
     */
    public abstract void debugError(ScriptQueue queue, Throwable error);

    /**
     * Output a command information report.
     */
    public abstract void debugReport(Debuggable caller, String name, String message);

    /**
     * Output an 'Okay!' message.
     */
    public abstract void debugApproval(String message);

    /**
     * Outputs a message specific to a debuggable object.
     */
    public abstract void debugEntry(Debuggable entry, String message);

    /**
     * Outputs a message specific to a debuggable object.
     */
    public abstract void debugEntry(Debuggable entry, DebugElement element, String message);

    /**
     * Outputs a message specific to a debuggable object.
     */
    public abstract void debugEntry(Debuggable entry, DebugElement element);

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
     * Return an empty ScriptEntryData object of the implementation's variety.
     * This is to avoid casting issues when ScriptEntry's use generic data objects.
     */
    public abstract ScriptEntryData getEmptyScriptEntryData();

    /**
     * Temporary.
     */
    public abstract void buildCoreContainers(YamlConfiguration yamlScripts);

    /**
     * Temporary.
     */
    public abstract List<YamlConfiguration> getOutsideScripts();

    public abstract void handleCommandSpecialCases(ScriptEntry entry);

    public abstract void debugCommandHeader(ScriptEntry entry);

    public abstract TagContext getTagContextFor(ScriptEntry entry, boolean instant);

    public abstract boolean handleCustomArgs(ScriptEntry entry, aH.Argument arg, boolean if_ignore);

    public abstract void refreshScriptContainers();

    public abstract String scriptQueueSpeed();

    public abstract dList valueOfFlagdList(String input);

    public abstract boolean matchesFlagdList(String input);

    public abstract String getLastEntryFromFlag(String flag);

    public TagContext getTagContext(ScriptEntry entry);

    public int getTagTimeout();
}
