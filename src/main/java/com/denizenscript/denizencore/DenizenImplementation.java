package com.denizenscript.denizencore;

import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.VectorObject;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.DefinitionProvider;

import java.io.File;

/**
 * Interface representing all the information that an implementation must provide to the engine.
 */
public interface DenizenImplementation {

    /**
     * Return a list of all folders that the implementation has scripts within.
     * Note: can be called async
     */
    File getScriptFolder();

    /**
     * Return the current version of the implementation.
     * EG, "Gamey Game 1.0 Denizen 0.9"
     */
    String getImplementationVersion();

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

    boolean handleCustomArgs(ScriptEntry entry, Argument arg);

    void refreshScriptContainers();

    TagContext getTagContext(ScriptContainer container);

    TagContext getTagContext(ScriptEntry entry);

    String cleanseLogString(String str);

    void preTagExecute();

    void postTagExecute();

    boolean needsHandleArgPrefix(String prefix);

    boolean canWriteToFile(File f);

    String getRandomColor();

    boolean canReadFile(File f);

    File getDataFolder();

    String queueHeaderInfo(ScriptEntry entry);

    FlaggableObject simpleWordToFlaggable(String word, ScriptEntry entry);

    ObjectTag getSpecialDef(String def, ScriptQueue queue);

    boolean setSpecialDef(String def, ScriptQueue queue, ObjectTag value);

    void addExtraErrorHeaders(StringBuilder headerBuilder, ScriptEntry source);

    String applyDebugColors(String uncolored);

    void doFinalDebugOutput(String rawText);

    void addFormatScriptDefinitions(DefinitionProvider provider, TagContext context);

    String stripColor(String message);

    void reloadConfig();

    void reloadSaves();

    VectorObject getVector(double x, double y, double z);

    VectorObject vectorize(ObjectTag input, TagContext context);
}
