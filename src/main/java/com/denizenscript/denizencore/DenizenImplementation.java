package com.denizenscript.denizencore;

import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.io.File;

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
    void debugEntry(Debuggable entry, Debug.DebugElement element, String message);

    /**
     * Outputs a message specific to a debuggable object.
     */
    void debugEntry(Debuggable entry, Debug.DebugElement element);

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

    boolean handleCustomArgs(ScriptEntry entry, Argument arg, boolean if_ignore);

    void refreshScriptContainers();

    String scriptQueueSpeed();

    ListTag valueOfFlagListTag(String input);

    boolean matchesFlagListTag(String input);

    TagContext getTagContext(ScriptContainer container);

    TagContext getTagContext(ScriptEntry entry);

    int getTagTimeout();

    boolean allowConsoleRedirection();

    String cleanseLogString(String str);

    boolean matchesType(String comparable, String comparedTo);

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

    boolean canReadFile(File f);

    boolean allowFileCopy();

    File getDataFolder();

    boolean allowStrangeYAMLSaves();

    String queueHeaderInfo(ScriptEntry entry);
}
