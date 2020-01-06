package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.scripts.ScriptBuilder;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.*;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import org.json.JSONObject;

import java.util.List;

public class ScriptTag implements ObjectTag, Adjustable {

    // <--[language]
    // @name Script
    // @group Denizen Scripting Language
    // @description
    // A somewhat vague term used to describe a collection of script entries and other script parts.
    //
    // For example, 'Hey, check out this script I just wrote!', probably refers to a collection of script entries
    // that make up some kind of script container. Perhaps it is a NPC Assignment Script Container that provides
    // waypoint functionality, or a world script that implements and keeps track of a new player stat. 'Script' can
    // refer to a single container, as well as a collection of containers that share a common theme.
    //
    // Scripts that contain a collection of containers are typically kept to a single file. Multiple containers are
    // permitted inside a single file, but it should be noted that container names are stored on a global level. That
    // is, naming scripts should be done with care to avoid duplicate script names.
    //
    // -->

    // <--[language]
    // @name dScript
    // @group Denizen Scripting Language
    // @description
    // The overall 'scripting language' that Denizen implements is referred to as 'dScripting', or 'dScript'.
    // dScripts use a YAML-styled layout + Denizen's Scripting API to parse scripts that are stored as .yml files. Scripts
    // go in the .../plugins/Denizen/scripts folder.
    //
    // -->

    // <--[language]
    // @name Script Syntax
    // @group Denizen Scripting Language
    // @description
    // The syntax of Denizen is broken into multiple abstraction layers.
    //
    // At the highest level, Denizen scripts are stored in YAML-like files, which use the '.yml' suffix but do not
    // fully conform to the YAML specification. In particular, there are several changes made to support looser syntax rules
    // and avoid some of the issues that would result from writing code directly into a plain YAML file.
    //
    // Within those YAML files are 'script containers', which is the actual unit of separating individual scripts apart.
    // (Whereas putting containers across different files results in no actual difference - file and folder separation
    // is purely for your own organization, and doesn't matter to the Denizen parser).
    // Each script container has a 'type' such as 'task' or 'world' that defines how it functions.
    //
    // Within a script container are individual script paths, such as 'script:' in a 'task' script container,
    // or 'on player breaks block:' which might be found within the 'events:' section of a 'world' script container.
    // These paths are the points that might actually be executed at any given time.
    // When a path is executed, a 'script queue' is formed to process the contents of that script path.
    //
    // Within any script path is a list of 'script entries', which are the commands to be executed.
    // These can be raw commands themselves (like 'narrate') with their arguments, or commands that contain
    // additional commands within their entry (as 'if' and 'foreach' for example both do).
    //
    // -->

    // <--[language]
    // @name ScriptTag Objects
    // @group Object System
    // @description
    // A ObjectTag that represents a script container. ScriptTags contain all information inside the script, and can be
    // used in a variety of commands that require script arguments. For example, run and inject will 'execute'
    // script entries inside of a script container when given a matching ScriptTag object.
    //
    // ScriptTags also provide a way to access attributes accessed by the replaceable tag system by using the object
    // fetcher or any other entry point to a ScriptTag object. ScriptTag objects have the object identifier of 's@'.
    // For example: ScriptTag_name
    //
    // For format info, see <@link language s@>
    //
    // -->

    ///////////////
    // Object Fetcher
    /////////////

    // <--[language]
    // @name s@
    // @group Object Fetcher System
    // @description
    // s@ refers to the 'object identifier' of a ScriptTag. The 's@' is notation for Denizen's Object
    // Fetcher. The only valid constructor for a ScriptTag is the name of the script container that it should be
    // associated with. For example, if my script container is called 'cool_script', the ScriptTag object for that script
    // would be able to be referenced (fetched) with s@cool_script.
    //
    // For general info, see <@link language ScriptTag Objects>
    // -->

    public static ScriptTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("s")
    public static ScriptTag valueOf(String string, TagContext context) {

        if (string.startsWith("s@")) {
            string = string.substring(2);
        }

        ScriptTag script = new ScriptTag(string);
        // Make sure it's valid.
        if (script.isValid()) {
            return script;
        }
        else {
            return null;
        }
    }


    public static boolean matches(String string) {

        if (CoreUtilities.toLowerCase(string).startsWith("s@")) {
            return true;
        }

        ScriptTag script = new ScriptTag(string);
        // Make sure it's valid.
        return script.isValid();
    }

    //////////////////
    // Constructor
    ////////////////


    /**
     * Creates a script object from a script name. If the script is valid, {@link #isValid()} will return true.
     *
     * @param scriptName the name of the script
     */
    public ScriptTag(String scriptName) {
        if (ScriptRegistry.getScriptContainer(scriptName) != null) {
            container = ScriptRegistry.getScriptContainer(scriptName);
            name = scriptName.toUpperCase();
            valid = true;
        }
    }

    public ScriptTag(ScriptContainer container) {
        this.container = container;
        name = container.getName().toUpperCase();
        valid = true;
    }

    ///////////////////////
    // Instance fields and methods
    /////////////////////

    // Keep track of the corresponding ScriptContainer
    private ScriptContainer container;

    // Make the default prefix "Container"
    private String prefix = "Container";


    private boolean valid = false;

    /**
     * Confirms that the script references a valid name and type in current loaded ScriptsContainers.
     *
     * @return true if the script is valid, false if the script was not found, or the type is missing
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Gets the type of the ScriptContainer, as defined by the TYPE: key.
     *
     * @return the type of the Script Container
     */
    public String getType() {
        return (container != null ? container.getContainerType() : "invalid");
    }


    private String name = null;

    /**
     * Gets the name of the ScriptContainer.
     *
     * @return script name
     */
    public String getName() {
        return name;
    }


    /**
     * Gets the contents of the scriptContainer.
     *
     * @return ConfigurationSection of the script contents
     */
    public ScriptContainer getContainer() {
        return container;
    }

    ///////////////
    // ObjectTag Methods
    ////////////

    @Override
    public String getObjectType() {
        return "Container";
    }

    @Override
    public String identify() {
        return "s@" + name;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debuggable() {
        return "s@" + name + "<GR> (" + getType() + ")";
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    public static void registerTags() {

        // <--[tag]
        // @attribute <ScriptTag.container_type>
        // @returns ElementTag
        // @description
        // Returns the type of script container that is associated with this ScriptTag object. For example: 'task', or
        // 'world'.
        // -->

        registerTag("container_type", (attribute, object) -> {
            return new ElementTag(object.container.getContainerType());
        });

        // <--[tag]
        // @attribute <ScriptTag.name>
        // @returns ElementTag
        // @description
        // Returns the name of the script container.
        // -->

        registerTag("name", (attribute, object) -> {
            return new ElementTag(object.name);
        });

        // <--[tag]
        // @attribute <ScriptTag.relative_filename>
        // @returns ElementTag
        // @description
        // Returns the filename that contains the script, relative to the denizen/ folder.
        // -->

        registerTag("relative_filename", (attribute, object) -> {
            return new ElementTag(object.container.getRelativeFileName());
        });

        // <--[tag]
        // @attribute <ScriptTag.filename>
        // @returns ElementTag
        // @description
        // Returns the absolute filename that contains the script.
        // -->

        registerTag("filename", (attribute, object) -> {
            return new ElementTag(object.container.getFileName().replace("\\", "/"));
        });

        // <--[tag]
        // @attribute <ScriptTag.original_name>
        // @returns ElementTag
        // @description
        // Returns the originally cased script name.
        // -->

        registerTag("original_name", (attribute, object) -> {
            return new ElementTag(object.container.getOriginalName());
        });

        // <--[tag]
        // @attribute <ScriptTag.constant[<constant_name>]>
        // @returns ElementTag or ListTag
        // @description
        // Returns the value of the constant as either an ElementTag or ListTag.
        // A constant is a script key under the 'default constants' node.
        // For example:
        // myscript:
        //   type: task
        //   default constants:
        //     myconstant: myvalue
        // -->

        registerTag("constant", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                Debug.echoError("The tag ScriptTag.constant[...] must have a value.");
                return null;
            }
            YamlConfiguration section = object.getContainer().getConfigurationSection("default constants");
            if (section == null) {
                return null;
            }
            Object obj = section.get(attribute.getContext(1).toUpperCase());
            if (obj == null) {
                return null;
            }

            if (obj instanceof List) {
                ListTag list = new ListTag();
                for (Object each : (List<Object>) obj) {
                    if (each == null) {
                        each = "null";
                    }
                    // TODO
                    list.add(TagManager.tag(each.toString(), DenizenCore.getImplementation().getTagContext(attribute.getScriptEntry())));
                }
                return list;

            }
            // TODO
            else {
                return new ElementTag(TagManager.tag(obj.toString(), DenizenCore.getImplementation().getTagContext(attribute.getScriptEntry())));
            }
        });

        // <--[tag]
        // @attribute <ScriptTag.yaml_key[<constant_name>]>
        // @returns ObjectTag
        // @description
        // Returns the value of the script's YAML as either an ElementTag or ListTag.
        // -->

        registerTag("yaml_key", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                Debug.echoError("The tag ScriptTag.constant[...] must have a value.");
                return null;
            }
            ScriptContainer container = object.getContainer();
            if (container == null) {
                Debug.echoError("Missing script container?!");
                return null;
            }
            YamlConfiguration section = container.getConfigurationSection("");
            if (section == null) {
                Debug.echoError("Missing YAML section?!");
                return null;
            }
            Object obj = section.get(attribute.getContext(1).toUpperCase());
            if (obj == null) {
                return null;
            }

            if (obj instanceof List) {
                ListTag list = new ListTag();
                for (Object each : (List<Object>) obj) {
                    if (each == null) {
                        each = "null";
                    }
                    list.add(ScriptBuilder.stripLinePrefix(each.toString()));
                }
                return list;

            }
            else {
                return new ElementTag(obj.toString());
            }
        });

        // <--[tag]
        // @attribute <ScriptTag.list_keys[<constant_name>]>
        // @returns ListTag
        // @description
        // Returns a list of all keys within a script.
        // -->

        registerTag("list_keys", (attribute, object) -> {
            YamlConfiguration conf = object.getContainer().getConfigurationSection(attribute.hasContext(1) ?
                    attribute.getContext(1) : "");
            if (conf == null) {
                return null;
            }
            return new ListTag(conf.getKeys(false));
        });

        // <--[tag]
        // @attribute <ScriptTag.list_deep_keys[<constant_name>]>
        // @returns ListTag
        // @description
        // Returns a list of all keys within a script, searching recursively.
        // -->

        registerTag("list_deep_keys", (attribute, object) -> {
            YamlConfiguration conf = object.getContainer().getConfigurationSection(attribute.hasContext(1) ?
                    attribute.getContext(1) : "");
            if (conf == null) {
                return null;
            }
            return new ListTag(conf.getKeys(true));
        });

        // <--[tag]
        // @attribute <ScriptTag.to_json>
        // @returns ElementTag
        // @description
        // Converts the YAML Script Container to a JSON array.
        // Best used with 'yaml data' type scripts.
        // -->

        registerTag("to_json", (attribute, object) -> {
            JSONObject jsobj = new JSONObject(YamlConfiguration.reverse(object.container.getContents().getMap(), true));
            jsobj.remove("type");
            return new ElementTag(jsobj.toString());
        });

        // <--[tag]
        // @attribute <ScriptTag.to_text>
        // @returns ElementTag
        // @description
        // Converts the YAML Script Container to raw YAML text.
        // Best used with 'yaml data' type scripts.
        // -->

        registerTag("to_text", (attribute, object) -> {
            YamlConfiguration config = new YamlConfiguration();
            config.addAll(object.getContainer().getContents().getMap());
            config.set("type", null);
            return new ElementTag(config.saveToString(true));
        });

        // <--[tag]
        // @attribute <ScriptTag.type>
        // @returns ElementTag
        // @description
        // Always returns 'Script' for ScriptTag objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->

        registerTag("type", (attribute, object) -> {
            return new ElementTag("Script");
        });

        // <--[tag]
        // @attribute <ScriptTag.list_queues>
        // @returns ListTag(Queue)
        // @description
        // Returns all queues which are running for this script.
        // -->

        registerTag("list_queues", (attribute, object) -> {
            ListTag queues = new ListTag();
            for (ScriptQueue queue : ScriptQueue.getQueues()) {
                if (queue.script != null && queue.script.getName().equals(object.getName())) {
                    queues.addObject(new QueueTag(queue));
                }
            }
            return queues;
        });
    }

    public static ObjectTagProcessor<ScriptTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectInterface<ScriptTag> runnable, String... variants) {
        tagProcessor.registerTag(name, runnable, variants);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("Cannot apply properties to a script!");
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // TODO: enable/disable

        if (!mechanism.fulfilled()) {
            mechanism.reportInvalid();
        }
    }

}
