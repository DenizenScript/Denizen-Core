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
    File getScriptFolder();

    /**
     * Return the current version of the implementation.
     * EG, "Gamey Game 1.0 Denizen 0.9"
     */
    String getImplementationVersion();

    /**
     * Output a debug message to console.
     */
    void debugMessage(String message);

    /**
     * Output an exception to console.
     */
    void debugException(Throwable ex);

    /**
     * Output an error to console.
     */
    void debugError(String error);

    /**
     * Output an error to console, specific to a script queue.
     */
    void debugError(ScriptQueue queue, String error);

    /**
     * Output an error to console, specific to a script queue.
     */
    void debugError(ScriptQueue queue, Throwable error);

    /**
     * Output a command information report.
     */
    void debugReport(Debuggable caller, String name, String message);

    /**
     * Output an 'Okay!' message.
     */
    void debugApproval(String message);

    /**
     * Outputs a message specific to a debuggable object.
     */
    void debugEntry(Debuggable entry, String message);

    /**
     * Outputs a message specific to a debuggable object.
     */
    void debugEntry(Debuggable entry, DebugElement element, String message);

    /**
     * Outputs a message specific to a debuggable object.
     */
    void debugEntry(Debuggable entry, DebugElement element);

    /**
     * Return the name of the implementation.
     * EG, "Gamey Game".
     */
    String getImplementationName();

    /**
     * Run any code that fires before a script reload goes through,
     * EG, clearing custom data.
     */
    void preScriptReload();

    /**
     * Run any code that fires after a script reload goes through,
     * EG, running a public Reload event.
     */
    void onScriptReload();

    /**
     * Return an empty ScriptEntryData object of the implementation's variety.
     * This is to avoid casting issues when ScriptEntry's use generic data objects.
     */
    ScriptEntryData getEmptyScriptEntryData();

    /**
     * Temporary.
     */
    void buildCoreContainers(YamlConfiguration yamlScripts);

    /**
     * Temporary.
     */
    List<YamlConfiguration> getOutsideScripts();

    void debugCommandHeader(ScriptEntry entry);

    TagContext getTagContextFor(ScriptEntry entry, boolean instant);

    boolean handleCustomArgs(ScriptEntry entry, aH.Argument arg, boolean if_ignore);

    void refreshScriptContainers();

    String scriptQueueSpeed();

    dList valueOfFlagdList(String input);

    boolean matchesFlagdList(String input);

    String getLastEntryFromFlag(String flag);

    TagContext getTagContext(ScriptEntry entry);

    int getTagTimeout();

    boolean allowConsoleRedirection();

    String cleanseLogString(String str);

    boolean matchesType(String comparable, String comparedTo);

    Thread getMainThread();

    boolean allowedToWebget();

    void preTagExecute();

    void postTagExecute();

    boolean needsHandleArgPrefix(String prefix);

    boolean shouldDebug(Debuggable debug);

    void debugQueueExecute(ScriptEntry entry, String queue, String execute);

    void debugTagFill(Debuggable entry, String tag, String result);

    boolean tagTimeoutWhenSilent();

    boolean getDefaultDebugMode();

    boolean canWriteToFile(File f);

    String getRandomColor();

    int whileMaxLoops();

    boolean allowLogging();
}
