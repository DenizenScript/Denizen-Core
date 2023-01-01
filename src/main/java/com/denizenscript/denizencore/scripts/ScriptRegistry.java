package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.*;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.denizenscript.denizencore.DenizenCore;

import java.lang.invoke.MethodHandle;
import java.util.*;

public class ScriptRegistry {

    public static Map<String, ScriptContainer> scriptContainers = new HashMap<>();
    public static Map<String, MethodHandle> typeConstructors = new HashMap<>();

    public static void _registerType(String typeName, Class<? extends ScriptContainer> scriptContainerClass) {
        typeConstructors.put(CoreUtilities.toLowerCase(typeName), ReflectionHelper.getConstructor(scriptContainerClass, YamlConfiguration.class, String.class));
    }

    public static void _registerCoreTypes() {
        _registerType("custom", CustomScriptContainer.class);
        _registerType("task", TaskScriptContainer.class);
        _registerType("procedure", ProcedureScriptContainer.class);
        _registerType("world", WorldScriptContainer.class);
        _registerType("data", DataScriptContainer.class);
        _registerType("yaml data", DataScriptContainer.class);
    }

    public static boolean containsScript(String id, Class scriptContainerType) {
        if (!scriptContainers.containsKey(CoreUtilities.toLowerCase(id))) {
            return false;
        }
        ScriptContainer script = scriptContainers.get(CoreUtilities.toLowerCase(id));
        return scriptContainerType.isInstance(script);
    }

    public static ArrayList<Map.Entry<String, YamlConfiguration>> toPostLoadAttempt = new ArrayList<>();

    public static void postLoadScripts() {
        try {
            for (Map.Entry<String, YamlConfiguration> script : toPostLoadAttempt) {
                attemptLoadSingle(script.getValue(), script.getKey(), true);
            }
        }
        finally {
            toPostLoadAttempt.clear();
        }
    }

    public static void attemptLoadSingle(YamlConfiguration script, String scriptName, boolean shouldErrorOnType) {
        // Make sure the script has a type
        String type = script.getString("type");
        if (type == null) {
            Debug.echoError("Found type-less container: '<Y>" + scriptName + "<W>'.");
            ScriptHelper.setHadError();
            return;
        }
        type = CoreUtilities.toLowerCase(type);
        // Check that types is a registered type
        if (!typeConstructors.containsKey(type)) {
            if (shouldErrorOnType) {
                Debug.echoError("Trying to load an invalid script. '<A>" + scriptName + "<Y>(" + type + ")<W>' is an unknown type.");
                ScriptHelper.setHadError();
            }
            else {
                toPostLoadAttempt.add(new AbstractMap.SimpleEntry<>(scriptName, script));
            }
            return;
        }
        MethodHandle constructor = typeConstructors.get(type);
        if (CoreConfiguration.debugLoadingInfo) {
            Debug.log("Adding script " + scriptName + " as type " + type);
        }
        try {
            String nameLow = CoreUtilities.toLowerCase(scriptName);
            if (scriptContainers.containsKey(nameLow)) {
                Debug.echoError("Duplicate script name '<Y>" + scriptName + "<W>'");
            }
            ScriptContainer instance = (ScriptContainer) constructor.invoke(script, scriptName);
            scriptContainers.put(nameLow, instance);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            ScriptHelper.setHadError();
        }
    }

    public static void buildCoreYamlScriptContainers(List<YamlConfiguration> yamlScripts) {
        scriptContainers.clear();
        DenizenCore.implementation.refreshScriptContainers();
        if (yamlScripts == null) {
            return;
        }
        Debug.log("Loading <A>" + yamlScripts.size() + "<W> script files...");
        for (YamlConfiguration script : yamlScripts) {
            for (StringHolder key : script.contents.keySet()) {
                YamlConfiguration container = script.getConfigurationSection(key.str);
                if (container == null) {
                    Debug.echoError("Invalid container '" + key.str + "' in file '" + ScriptHelper.getSource(key.low) + "' - missing contents?");
                }
                else {
                    attemptLoadSingle(container, key.str, false);
                }
            }
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
