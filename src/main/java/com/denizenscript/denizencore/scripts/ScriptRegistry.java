package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.*;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.OldEventManager;

import java.util.*;

public class ScriptRegistry {

    public static Map<String, ScriptContainer> scriptContainers = new HashMap<>();
    public static Map<String, Class<? extends ScriptContainer>> scriptContainerTypes = new HashMap<>();

    public static void _registerType(String typeName, Class<? extends ScriptContainer> scriptContainerClass) {
        scriptContainerTypes.put(typeName.toUpperCase(), scriptContainerClass);
    }

    public static void _registerCoreTypes() {
        _registerType("custom", CustomScriptContainer.class);
        _registerType("task", TaskScriptContainer.class);
        _registerType("procedure", ProcedureScriptContainer.class);
        _registerType("world", WorldScriptContainer.class);
        _registerType("data", DataScriptContainer.class);
        _registerType("yaml data", DataScriptContainer.class);
    }

    public static boolean containsScript(String id) {
        return scriptContainers.containsKey(CoreUtilities.toLowerCase(id));
    }

    public static boolean containsScript(String id, Class scriptContainerType) {
        if (!scriptContainers.containsKey(CoreUtilities.toLowerCase(id))) {
            return false;
        }
        ScriptContainer script = scriptContainers.get(CoreUtilities.toLowerCase(id));
        return scriptContainerType.isInstance(script);
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
            Class typeClass = scriptContainerTypes.get(type.toUpperCase());
            if (Debug.showLoading) {
                Debug.log("Adding script " + scriptName + " as type " + type.toUpperCase());
            }
            try {
                scriptContainers.put(CoreUtilities.toLowerCase(scriptName), (ScriptContainer) typeClass.getConstructor(YamlConfiguration.class, String.class)
                        .newInstance(ScriptHelper.getScripts().getConfigurationSection(scriptName), scriptName));
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
        Set<StringHolder> scripts = yamlScripts.getKeys(false);
        Debug.log("Loading " + scripts.size() + " scripts...");
        for (StringHolder scriptName : scripts) {
            attemptLoadSingle(scriptName.str, false);
        }
    }

    public static <T extends ScriptContainer> T getScriptContainerAs(String name, Class<T> type) {
        try {
            ScriptContainer container = scriptContainers.get(CoreUtilities.toLowerCase(name));
            if (container != null) {
                return type.cast(container);
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
        ScriptContainer container = scriptContainers.get(CoreUtilities.toLowerCase(name));
        if (container != null) {
            return (T) container;
        }
        else {
            return null;
        }
    }
}
