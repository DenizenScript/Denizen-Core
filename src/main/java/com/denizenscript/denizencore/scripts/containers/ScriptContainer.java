package com.denizenscript.denizencore.scripts.containers;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.*;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ScriptTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptContainer implements Debuggable {

    // <--[language]
    // @name Script Container
    // @group Script Container System
    // @description
    // Script Containers are the basic structure that Denizen uses inside its YAML-based scripting files found in your
    // plugins/Denizen/scripts/ folder. Regardless of type, all script containers have basic parts that can usually be
    // described as keys, list keys, parent keys, child keys, values, and list values. While specific container types
    // probably have more specific names, just remember that no matter how complicated a script, this basic structure
    // still applies.
    //
    // It's important to keep in mind that all child keys, including all the main keys of the script, must line up with
    // one another, hierarchically. If you are familiar with YAML, great, because all script containers use it at the
    // core. Every value, in one way or another, belongs to some kind of 'key'. To define a key, use a string value plus
    // a colon (:). Keys can have a single value, a list value, or own another key:
    //
    // <code>
    // script name:
    //     key: value
    //     list key:
    //         - list value
    //         - ...
    //     parent key:
    //         child key: value
    // </code>
    //
    // And here's a container, put into a more familiar context:
    //
    // <code>
    // a haiku script:
    //     type: task
    //     script:
    //       - narrate "A simple script,"
    //       - narrate "with a key value relationship."
    //       - narrate "Oh look, a list!"
    // </code>
    //
    // -->

    public ScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        if (configurationSection == null) {
            Debug.echoError("Null configuration section while generating a ScriptContainer?!");
            throw new RuntimeException("Null configuration section while generating a ScriptContainer");
        }
        contents = configurationSection;
        configurationSection.forceLoweredRootKey("type");
        configurationSection.forceLoweredRootKey("debug");
        configurationSection.forceLoweredRootKey("script");
        configurationSection.forceLoweredRootKey("speed");
        configurationSection.forceLoweredRootKey("enabled");
        this.name = CoreUtilities.toUpperCase(scriptContainerName);
    }

    public <T extends ObjectTag> T tagObject(String text, Class<T> type) {
        TagContext context = DenizenCore.implementation.getTagContext(this);
        return TagManager.tagObject(text, context).asType(type, context);
    }

    public String tag(String text) {
        return TagManager.tag(text, DenizenCore.implementation.getTagContext(this));
    }

    public Boolean enabledCache = null;

    public boolean shouldEnable() {
        if (enabledCache != null) {
            return enabledCache;
        }
        String enabledText = contents.getString("enabled");
        if (enabledText == null) {
            return enabledCache = true;
        }
        String result = tag(enabledText);
        return enabledCache = (result == null || CoreUtilities.equalsIgnoreCase(result, "true"));
    }

    public void postCheck() {
    }

    /**
     * Whether this script container type can run script logic.
     * If this is false, this is a data-only script container.
     */
    public boolean canRunScripts = true;

    /**
     * The contents of the script container
     */
    YamlConfiguration contents;

    /**
     * Gets the contents of the container.
     *
     * @return a ConfigurationSection object
     */
    public YamlConfiguration getContents() {
        return contents;
    }

    // <--[language]
    // @name Script Name
    // @group Script Container System
    // @description
    // Typically refers to the name of a script container. When using the object fetcher with dScript objects,
    // (ScriptTag_name), the script_name referred to is the name of the container.
    //
    // <code>
    // script name:         <--- script name
    //   type: script_type
    //   script:            <--- base script
    //     - script entries
    //     - ...
    //   local script:      <--- local script path
    //     - script entries
    //     - ...
    // </code>
    //
    // -->

    /**
     * The name of the script container
     */
    private String name;

    /**
     * Gets the name of the script container.
     *
     * @return the script container name.
     */
    public String getName() {
        return name;
    }

    public String getFileName() {
        return ScriptHelper.getSource(getName());
    }

    public String getRelativeFileName() {
        try {
            String fn = getFileName();
            if (fn == null) {
                return "(Error: script source is missing?)";
            }
            fn = fn.replace(DenizenCore.implementation.getScriptFolder().getParentFile().getCanonicalPath(), "");
            while (fn.startsWith("/")) {
                fn = fn.substring(1);
            }
            return fn;
        }
        catch (Exception e) {
            Debug.echoError(e);
            return getFileName();
        }
    }

    public String getOriginalName() {
        return ScriptHelper.getOriginalName(getName());
    }

    /**
     * Gets a ScriptTag object that represents this container.
     *
     * @return a ScriptTag object linking this script container.
     */
    public ScriptTag getAsScriptArg() {
        return new ScriptTag(this);
    }

    // <--[language]
    // @name Script Type
    // @group Script Container System
    // @description
    // The type of container that a script is in. For example, 'task script' is a script type that has some sort of utility.
    //
    // <code>
    // script name:
    //   type: script_type  <--- script type
    //   script:
    //     - script entries
    //     - ...
    // </code>
    //
    // -->

    /**
     * Gets the value of the type: node specified in the script container structure.
     *
     * @return the type of container
     */
    public String getContainerType() {
        String type = contents.getString("type");
        return type == null ? null : CoreUtilities.toUpperCase(type);
    }

    /**
     * Checks the ConfigurationSection for the key/path to key specified.
     *
     * @param path the path of the key
     * @return true if the key exists
     */
    public boolean contains(String path) {
        return contents.contains(path);
    }

    public boolean containsScriptSection(String path) {
        return contains(path, List.class, "script section");
    }

    public boolean contains(String path, Class type) {
        return contains(path, type, DebugInternals.getClassNameOpti(type));
    }

    public boolean contains(String path, Class type, String typeName) {
        Object o = contents.get(path);
        if (o == null) {
            return false;
        }
        if (type.isAssignableFrom(o.getClass())) {
            return true;
        }
        if (type == YamlConfiguration.class) {
            return o instanceof Map;
        }
        Debug.echoError("Script '<Y>" + getName() + "<W>' contains path '<Y>" + path + "<W>' but it is not required type '<Y>" + typeName + "<W>'");
        return false;
    }

    public String getString(String path) {
        return contents.getString(path);
    }

    public String getString(String path, String def) {
        return contents.getString(path, def);
    }

    public String getString(String path, String def, boolean parseTags) {
        String str = contents.getString(path);
        if (str == null) {
            return def;
        }
        return parseTags ? tag(str) : str;
    }

    public List<String> getStringList(String path) {
        return getStringList(path, false);
    }

    public List<String> getStringList(String path, boolean parseTags) {
        Object obj = contents.get(path);
        if (obj instanceof List list) {
            List<String> strs = YamlConfiguration.patchListNonsense(list);
            ArrayList<String> output = new ArrayList<>(strs.size());
            for (String str : strs) {
                 str = ScriptBuilder.stripLinePrefix(str);
                if (parseTags) {
                    str = tag(str);
                }
                output.add(str);
            }
            return output;
        }
        else if (parseTags) {
            return tagObject(obj.toString(), ListTag.class);
        }
        return null;
    }

    public YamlConfiguration getConfigurationSection(String path) {
        if (path.length() == 0) {
            return contents;
        }
        return contents.getConfigurationSection(path);
    }

    public void set(String path, Object object) {
        contents.set(path, object);
    }

    public ScriptEntrySet baseEntries = null;

    public List<ScriptEntry> getBaseEntries(ScriptEntryData data) {
        if (baseEntries == null) {
            baseEntries = getSetFor("script");
        }
        return cleanDup(data, baseEntries);
    }

    public static List<ScriptEntry> cleanDup(ScriptEntryData data, ScriptEntrySet set) {
        if (set == null) {
            return null;
        }
        set = set.duplicate();
        for (ScriptEntry entry : set.entries) {
            entry.entryData = data.clone();
            entry.updateContext();
            entry.entryData.scriptEntry = entry;
        }
        return set.entries;
    }

    public List<ScriptEntry> getEntries(ScriptEntryData data, String path) {
        if (path == null) {
            path = "script";
        }
        return cleanDup(data, getSetFor(path));
    }

    public ScriptEntrySet getSetFor(String path) {
        ScriptEntrySet got = scriptsMap.get(path);
        if (got != null) {
            return got;
        }
        List<Object> stringEntries = contents.getList(path);
        if (stringEntries == null || stringEntries.isEmpty()) {
            return null;
        }
        List<ScriptEntry> entries = ScriptBuilder.buildScriptEntries(stringEntries, this, null);
        if (entries == null) {
            return null;
        }
        got = new ScriptEntrySet(entries);
        scriptsMap.put(path, got);
        return got;
    }

    private Map<String, ScriptEntrySet> scriptsMap = new HashMap<>();

    /////////////
    // DEBUGGABLE
    /////////

    // Cached debug value to avoid repeated complex YAML calls
    private Boolean shouldDebug = null;

    @Override
    public boolean shouldDebug() {
        if (shouldDebug == null) {
            if (!contents.contains("debug")) {
                shouldDebug = CoreConfiguration.defaultDebugMode;
            }
            else {
                shouldDebug = !CoreUtilities.equalsIgnoreCase(contents.getString("debug"), "false");
            }
        }
        return shouldDebug;
    }

    @Override
    public String toString() {
        return "s@" + CoreUtilities.toLowerCase(getName());
    }
}
