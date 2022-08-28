package com.denizenscript.denizencore;

import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.io.File;
import java.util.function.Consumer;

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
     * Output a debug message to console.
     */
    void debugMessage(String caller, String message);

    /**
     * Output a debug message to console.
     */
    void debugMessage(Debug.DebugElement element, String message);

    /**
     * Output an exception to console.
     */
    void debugException(Throwable ex);

    /**
     * Output an error to console.
     */
    void debugError(String addedContext, String error);

    /**
     * Output an error to console, specific to a script queue.
     */
    void debugError(ScriptEntry queue, String addedContext, String error);

    /**
     * Output an error to console, specific to a script queue.
     */
    void debugError(ScriptEntry queue, Throwable error);

    /**
     * Output a command information report.
     */
    void debugReport(Debuggable caller, String name, String message);

    /**
     * Output a command information report.
     */
    void debugReport(Debuggable caller, String name, Object... values);

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

    TagContext getTagContext(ScriptContainer container);

    TagContext getTagContext(ScriptEntry entry);

    String cleanseLogString(String str);

    void preTagExecute();

    void postTagExecute();

    boolean needsHandleArgPrefix(String prefix);

    boolean shouldDebug(Debuggable debug);

    void debugQueueExecute(ScriptEntry entry, String queue, String execute);

    boolean canWriteToFile(File f);

    String getRandomColor();

    boolean canReadFile(File f);

    File getDataFolder();

    String queueHeaderInfo(ScriptEntry entry);

    void submitRecording(Consumer<String> processResult);

    FlaggableObject simpleWordToFlaggable(String word, ScriptEntry entry);

    ObjectTag getSpecialDef(String def, ScriptQueue queue);

    boolean setSpecialDef(String def, ScriptQueue queue, ObjectTag value);

    String getTextColor();

    String getEmphasisColor();

    void saveClassToLoader(Class<?> clazz);

    boolean isSafeThread();
}
