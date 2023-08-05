package com.denizenscript.denizencore.scripts.containers.core;

import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.CustomObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomScriptContainer extends ScriptContainer {

    // <--[language]
    // @name Custom Script Containers
    // @group Script Container System
    // @description
    // Custom script containers are used to define a template type for a custom object.
    //
    // Usage of these should generally be avoided, as they can be considered 'over-engineering'...
    // That is, using a very complicated solution to solve a problem that can be solved much more simply.
    //
    // Custom objects exist for experimental reasons. Use at your own risk.
    //
    // Custom script containers have no required keys but several optional ones.
    // Use 'tags' key to define scripted tags,
    // 'mechanisms' to define scripted mechanisms,
    // 'inherit' to define what other custom script to inherit from,
    // and any other key name to define a default object field.
    //
    // <code>
    // Custom_Script_Name:
    //
    //     type: custom
    //
    //     # Use 'inherit' to make this custom script container inherit from another custom object.
    //     inherit: some_object
    //
    //     # This adds default field 'some_field' with initial value of 'some_value'.
    //     some_field: some_value
    //
    //     # List additional fields here...
    //
    //     # Use 'tags' to define scripted tags on the object.
    //     # Tags are subject to the same rules as procedure scripts:
    //     # NEVER change any external state. Just determine a value. Nothing else should change from the script.
    //     tags:
    //
    //         # This would be read like <[my_object].my_tag>
    //         my_tag:
    //         # Perform any logic here, and 'determine' the result.
    //         - determine 3
    //
    //         # list additional tags here...
    //
    //     # Use 'mechanisms' to define scripted mechanisms on the object.
    //     # Note that these should only ever determine a new object,
    //     # with NO CHANGES AT ALL outside the replacement determined object.
    //     # (Same rules as tags and procedure scripts).
    //     mechanisms:
    //
    //         # This would be used like custom@Custom_Script_Name[my_mech=3]
    //         my_mech:
    //         - adjust <context.this> true_value:<context.value.mul[2]> save:new_val
    //         - determine <entry[new_val].result>
    //
    //         # list additional mechanisms here...
    //
    // </code>
    //
    // -->

    public HashMap<String, String> defaultVars = new HashMap<>();

    public HashMap<String, ObjectTag> getVars() {
        HashMap<String, ObjectTag> vars;
        if (inherit != null) {
            ScriptContainer sc = ScriptRegistry.getScriptContainer(inherit);
            if (sc instanceof CustomScriptContainer) {
                vars = ((CustomScriptContainer) sc).getVars();
            }
            else {
                vars = new HashMap<>();
            }
        }
        else {
            vars = new HashMap<>();
        }
        for (Map.Entry<String, String> str : defaultVars.entrySet()) {
            vars.put(str.getKey(), new ElementTag(str.getValue()));
        }
        return vars;
    }

    public String inherit = null;

    public CustomScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);

        for (StringHolder str : getConfigurationSection("").getKeys(false)) {
            if (str.low.equals("inherit")) {
                inherit = getString(str.str);
            }
            else if (!(str.low.equals("type") || str.low.equals("tags") || str.low.equals("mechanisms")
                    || str.low.equals("speed") || str.low.equals("debug")
                    || configurationSection.getConfigurationSection(str.str) != null)) {
                defaultVars.put(str.low, getString(str.str));
            }
        }
    }

    public ListTag runTagScript(String path, ObjectTag val, CustomObjectTag obj, ScriptEntryData data) {
        CustomScriptContainer csc = this;
        while (csc != null) {
            if (csc.containsScriptSection("tags." + path)) {
                Debug.echoDebug(this, "[CustomObject] Calculating tag: " + path + " for " + csc.getName());
                ScriptQueue queue = new InstantQueue("TAG_" + csc.getName() + "_" + path + "__");
                List<ScriptEntry> listOfEntries = csc.getEntries(data, "tags." + path);
                CustomScriptContextSource cscs = new CustomScriptContextSource();
                cscs.obj = obj;
                cscs.value = val;
                queue.setContextSource(cscs);
                queue.addEntries(listOfEntries);
                queue.start();
                return queue.determinations;
            }
            Debug.echoDebug(this, "[CustomObject] Grabbing parent of " + csc.getName());
            csc = ScriptRegistry.getScriptContainer(csc.inherit);
        }
        Debug.echoDebug(this, "Unable to find tag handler for " + path + " for " + this.getName());
        return null;
    }

    public ListTag runMechScript(String path, CustomObjectTag obj, ObjectTag value) {
        CustomScriptContainer csc = this;
        while (csc != null) {
            if (csc.containsScriptSection("mechanisms." + path)) {
                ScriptQueue queue = new InstantQueue("MECH_" + csc.getName() + "_" + path + "__");
                List<ScriptEntry> listOfEntries = csc.getEntries(DenizenCore.implementation.getEmptyScriptEntryData(), "mechanisms." + path);
                CustomScriptContextSource cscs = new CustomScriptContextSource();
                cscs.obj = obj;
                cscs.value = value;
                queue.setContextSource(cscs);
                queue.addEntries(listOfEntries);
                queue.start();
                return queue.determinations;
            }
            csc = ScriptRegistry.getScriptContainer(csc.inherit);
        }
        return null;
    }

    public static class CustomScriptContextSource implements ContextSource {

        public CustomObjectTag obj;

        public ObjectTag value;

        @Override
        public ObjectTag getContext(String name) {
            if (name.equals("this")) {
                return obj;
            }
            else if (name.equals("value")) {
                return value;
            }
            else {
                return null;
            }
        }
    }
}
