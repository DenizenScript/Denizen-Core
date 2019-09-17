package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.*;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.OldEventManager;

import java.util.*;

public class ScriptRegistry {

    // Currently loaded 'script-containers'
    private static Map<String, Object> scriptContainers = new HashMap<>();
    private static Map<String, Class<? extends ScriptContainer>> scriptContainerTypes = new HashMap<>();

    public static void _registerType(String typeName, Class<? extends ScriptContainer> scriptContainerClass) {
        scriptContainerTypes.put(typeName.toUpperCase(), scriptContainerClass);
    }

    public static Set<String> _getScriptNames() {
        return scriptContainers.keySet();
    }

    public static void _registerCoreTypes() {
        _registerType("custom", CustomScriptContainer.class);
        _registerType("task", TaskScriptContainer.class);
        _registerType("procedure", ProcedureScriptContainer.class);
        _registerType("world", WorldScriptContainer.class);
        _registerType("yaml data", YamlDataScriptContainer.class);
    }

    public static boolean containsScript(String id) {
        return scriptContainers.containsKey(id.toUpperCase());
    }

    public static boolean containsScript(String id, Class scriptContainerType) {
        if (!scriptContainers.containsKey(id.toUpperCase())) {
            return false;
        }
        ScriptContainer script = (ScriptContainer) scriptContainers.get(id.toUpperCase());
        String type = null;
        for (Map.Entry<String, Class<? extends ScriptContainer>> entry : scriptContainerTypes.entrySet()) {
            if (entry.getValue() == scriptContainerType) {
                type = entry.getKey();
            }
        }
        return type != null && (script.getContainerType().equalsIgnoreCase(type));
    }

    public static YamlConfiguration fullYaml;

    public static ArrayList<String> toPostLoadAttempt = new ArrayList<>();

    public static void postLoadScripts() {
        for (String scriptName : toPostLoadAttempt) {
            attemptLoadSingle(scriptName, true);
        }
        toPostLoadAttempt.clear();
        fullYaml = null;
    }

    public static void attemptLoadSingle(String scriptName, boolean shouldErrorOnType) {
        // Make sure the script has a type
        if (fullYaml.contains(scriptName + ".TYPE")) {
            String type = fullYaml.getString(scriptName + ".TYPE");
            // Check that types is a registered type
            if (!scriptContainerTypes.containsKey(type.toUpperCase())) {
                if (shouldErrorOnType) {
                    Debug.log("<G>Trying to load an invalid script. '<A>" + scriptName + "<Y>(" + type + ")'<G> is an unknown type.");
                    ScriptHelper.setHadError();
                }
                else {
                    toPostLoadAttempt.add(scriptName);
                }
                return;
            }
            // Instantiate a new scriptContainer of specified type.
            Class typeClass = scriptContainerTypes.get(type.toUpperCase());
            Debug.log("Adding script " + scriptName + " as type " + type.toUpperCase());
            try {
                scriptContainers.put(scriptName, typeClass.getConstructor(YamlConfiguration.class, String.class)
                        .newInstance(ScriptHelper._gs().getConfigurationSection(scriptName), scriptName));
            }
            catch (Exception e) {
                Debug.echoError(e);
                ScriptHelper.setHadError();
            }
        }
        else {
            Debug.echoError("Found type-less container: '" + scriptName + "'.");
            ScriptHelper.setHadError();
        }
    }

    public static void buildCoreYamlScriptContainers(YamlConfiguration yamlScripts) {
        fullYaml = yamlScripts;
        scriptContainers.clear();
        OldEventManager.world_scripts.clear();
        OldEventManager.events.clear();
        DenizenCore.getImplementation().refreshScriptContainers();
        if (yamlScripts == null) {
            return;
        }
        // Get a set of key names in concatenated Denizen Scripts
        Set<StringHolder> scripts = yamlScripts.getKeys(false);
        // Iterate through set
        for (StringHolder scriptName : scripts) {
            attemptLoadSingle(scriptName.str, false);
        }
    }

    public static List<YamlConfiguration> outside_scripts = new ArrayList<>();

    /**
     * Adds a YAML FileConfiguration to the list of scripts to be loaded. Adding a new
     * FileConfiguration will reload the scripts automatically.
     *
     * @param yaml_script the FileConfiguration containing the script
     */
    public static void addYamlScriptContainer(YamlConfiguration yaml_script) {
        outside_scripts.add(yaml_script);
    }

    /**
     * Removes a YAML FileConfiguration to the list of scripts to be loaded. Removing a
     * FileConfiguration will reload the scripts automatically.
     *
     * @param yaml_script the FileConfiguration containing the script
     */
    public static void removeYamlScriptContainer(YamlConfiguration yaml_script) {
        outside_scripts.remove(yaml_script);
        DenizenCore.reloadScripts();
    }

    public static <T extends ScriptContainer> T getScriptContainerAs(String name, Class<T> type) {
        try {
            if (scriptContainers.containsKey(name.toUpperCase())) {
                return type.cast(scriptContainers.get(name.toUpperCase()));
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
        }

        return null;
    }

    public static <T extends ScriptContainer> T getScriptContainer(String name) {
        if (scriptContainers.containsKey(name.toUpperCase())) {
            return (T) scriptContainers.get(name.toUpperCase());
        }

        else {
            return null;
        }
    }
}
