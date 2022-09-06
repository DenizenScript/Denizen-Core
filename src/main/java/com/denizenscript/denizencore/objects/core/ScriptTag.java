package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.flags.RedirectionFlagTracker;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.*;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import org.json.JSONObject;

import java.util.List;

public class ScriptTag implements ObjectTag, Adjustable, FlaggableObject {

    // <--[language]
    // @name Script
    // @group Denizen Scripting Language
    // @description
    // A somewhat vague term used to describe a collection of script entries and other script parts.
    //
    // For example, 'Hey, check out this script I just wrote!', probably refers to a collection of script entries that make up some kind of script container.
    // Perhaps it is a NPC Assignment Script Container that provides waypoint functionality, or a world script that implements and keeps track of a new player stat.
    // 'Script' can refer to a single container, as well as a collection of containers that share a common theme.
    //
    // Scripts that contain a collection of containers are typically kept to a single file.
    // Multiple containers are permitted inside a single file, but it should be noted that container names are stored on a global level.
    // That is, naming scripts should be done with care to avoid duplicate script names.
    //
    // -->

    // <--[language]
    // @name dScript
    // @group Denizen Scripting Language
    // @description
    // The overall 'scripting language' that Denizen implements is referred to as 'dScripting', or 'dScript'.
    // dScripts use the Denizen script syntax and the Denizen Scripting API to parse scripts that are stored as .dsc files.
    // Scripts go in the 'plugins/Denizen/scripts' folder.
    //
    // -->

    // <--[language]
    // @name Script Syntax
    // @group Denizen Scripting Language
    // @description
    // The syntax of Denizen is broken into multiple abstraction layers.
    //
    // At the highest level, Denizen scripts are stored in script files, which use the '.dsc' suffix
    //
    // Denizen script syntax is approximately based on YAML configuration files,
    // and is intended to seem generally as easy to edit as a YAML configuration.
    // However, several key differences exist between the Denizen script syntax and YAML syntax.
    // In particular, there are several changes made to support looser syntax rules
    // and avoid some of the issues that would result from writing code directly into a plain YAML file.
    //
    // Within those 'script files' are 'script containers', which are the actual unit of separating individual 'scripts' apart.
    // (Whereas putting containers across different files results in no actual difference:
    // file and folder separation is purely for your own organization, and doesn't matter to the Denizen parser).
    // Each script container has a 'type' such as 'task' or 'world' that defines how it functions.
    //
    // Within a script container are individual script paths, such as 'script:' in a 'task' script container,
    // or 'on player breaks block:' which might be found within the 'events:' section of a 'world' script container.
    // These paths are the points that might actually be executed at any given time.
    // When a path is executed, a 'script queue' is formed to process the contents of that script path.
    //
    // Within any script path is a list of 'script entries', which are the commands to be executed.
    // These can be raw commands themselves (like 'narrate') with their arguments,
    // or commands that contain additional commands within their entry (as 'if' and 'foreach' for example both do).
    //
    // -->

    // <--[ObjectType]
    // @name ScriptTag
    // @prefix s
    // @base ElementTag
    // @implements FlaggableObject
    // @ExampleTagBase script
    // @ExampleValues <script>
    // @format
    // The identity format for scripts is simply the script name.
    //
    // @description
    // An ObjectTag that represents a script container. ScriptTags contain all information inside the script,
    // and can be used in a variety of commands that require script arguments.
    // For example, run and inject will 'execute' script entries inside of a script container when given a matching ScriptTag object.
    //
    // ScriptTags also provide a way to access attributes accessed by the replaceable tag system by using the object fetcher or any other entry point to a ScriptTag object.
    //
    // This object type is flaggable.
    // Flags on this object type will be stored in the server saves file, under special sub-key "__scripts"
    //
    // -->

    ///////////////
    // Object Fetcher
    /////////////

    @Deprecated
    public static ScriptTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("s")
    public static ScriptTag valueOf(String string, TagContext context) {
        if (string.startsWith("s@")) {
            string = string.substring(2);
        }
        ScriptContainer container = ScriptRegistry.getScriptContainer(string);
        if (container == null) {
            return null;
        }
        return new ScriptTag(container);
    }

    public static boolean matches(String string) {
        if (CoreUtilities.toLowerCase(string).startsWith("s@")) {
            return true;
        }
        return ScriptRegistry.getScriptContainer(string) != null;
    }

    //////////////////
    // Constructor
    ////////////////

    public ScriptTag(ScriptContainer container) {
        this.container = container;
        name = CoreUtilities.toLowerCase(container.getName());
    }

    ///////////////////////
    // Instance fields and methods
    /////////////////////

    private ScriptContainer container;

    private String prefix = "Script";

    public ScriptTag validate() {
        container = ScriptRegistry.getScriptContainer(name);
        return container != null ? this : null;
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
        return "<LG>s@<Y>" + name + "<GR> (" + getType() + ")";
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return new RedirectionFlagTracker(DenizenCore.serverFlagMap, "__scripts." + name.replace(".", "&dot"));
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
        // Nothing to do.
    }

    public static void registerTags() {

        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        // <--[tag]
        // @attribute <ScriptTag.container_type>
        // @returns ElementTag
        // @description
        // Returns the type of script container that is associated with this ScriptTag object. For example: 'task', or
        // 'world'.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "container_type", (attribute, object) -> {
            return new ElementTag(object.container.getContainerType());
        });

        // <--[tag]
        // @attribute <ScriptTag.name>
        // @returns ElementTag
        // @description
        // Returns the name of the script container.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "name", (attribute, object) -> {
            return new ElementTag(object.name);
        });

        // <--[tag]
        // @attribute <ScriptTag.relative_filename>
        // @returns ElementTag
        // @description
        // Returns the filename that contains the script, relative to the denizen/ folder.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "relative_filename", (attribute, object) -> {
            return new ElementTag(object.container.getRelativeFileName());
        });

        // <--[tag]
        // @attribute <ScriptTag.filename>
        // @returns ElementTag
        // @description
        // Returns the absolute filename that contains the script.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "filename", (attribute, object) -> {
            return new ElementTag(object.container.getFileName().replace("\\", "/"));
        });

        // <--[tag]
        // @attribute <ScriptTag.original_name>
        // @returns ElementTag
        // @description
        // Returns the originally cased script name.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "original_name", (attribute, object) -> {
            return new ElementTag(object.container.getOriginalName());
        });

        tagProcessor.registerTag(ObjectTag.class, "constant", (attribute, object) -> {
            Deprecations.scriptConstantTag.warn(attribute.context);
            if (!attribute.hasParam()) {
                attribute.echoError("The tag ScriptTag.constant[...] must have a value.");
                return null;
            }
            YamlConfiguration section = object.getContainer().getConfigurationSection("default constants");
            if (section == null) {
                return null;
            }
            Object obj = section.get(attribute.getParam());
            if (obj == null) {
                return null;
            }
            if (obj instanceof List) {
                ListTag list = new ListTag();
                for (Object each : (List<Object>) obj) {
                    if (each == null) {
                        each = "null";
                    }
                    list.add(TagManager.tag(each.toString(), DenizenCore.implementation.getTagContext(attribute.getScriptEntry())));
                }
                return list;

            }
            else {
                return new ElementTag(TagManager.tag(obj.toString(), DenizenCore.implementation.getTagContext(attribute.getScriptEntry())));
            }
        });

        // <--[tag]
        // @attribute <ScriptTag.parsed_key[<key_name>]>
        // @returns ObjectTag
        // @description
        // Returns the value from a data key on the script as an ElementTag, ListTag, or MapTag.
        // Will automatically parse any tags contained within the value of the key, preserving key data structure
        // (meaning, a tag that returns a ListTag, inside a data list, will insert a ListTag inside the returned ListTag, as you would expect).
        // -->
        tagProcessor.registerTag(ObjectTag.class, ElementTag.class, "parsed_key", (attribute, object, key) -> {
            ScriptContainer container = object.getContainer();
            if (container == null) {
                Debug.echoError("Script '" + object.getName() + "' is missing script container?!");
                return null;
            }
            YamlConfiguration section = container.getConfigurationSection("");
            if (section == null) {
                Debug.echoError("Script '" + container.getName() + "' missing root section?!");
                return null;
            }
            Object obj = section.get(key.asString());
            if (obj == null) {
                return null;
            }
            return CoreUtilities.objectToTagForm(obj, attribute.context, true, true);
        });

        // <--[tag]
        // @attribute <ScriptTag.data_key[<key_name>]>
        // @returns ObjectTag
        // @description
        // Returns the value from a data key on the script as an ElementTag, ListTag, or MapTag.
        // For example, "script.data_key[type]" on a task script will return "task".
        // Custom keys should usually go in a 'data' script container, or under a key labeled 'data' for other script containers.
        // -->
        tagProcessor.registerStaticTag(ObjectTag.class, ElementTag.class, "data_key", (attribute, object, key) -> {
            ScriptContainer container = object.getContainer();
            if (container == null) {
                Debug.echoError("Script '" + object.getName() + "' is missing script container?!");
                return null;
            }
            YamlConfiguration section = container.getConfigurationSection("");
            if (section == null) {
                Debug.echoError("Script '" + container.getName() + "' missing root section?!");
                return null;
            }
            Object obj = section.get(key.asString());
            if (obj == null) {
                return null;
            }
            return CoreUtilities.objectToTagForm(obj, attribute.context, true);
        });
        tagProcessor.registerFutureTagDeprecation("data_key", "yaml_key");

        // <--[tag]
        // @attribute <ScriptTag.list_keys[(<start_key>)]>
        // @returns ListTag
        // @description
        // Returns a list of all data keys within a script, with an optional starting-key.
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "list_keys", (attribute, object) -> {
            YamlConfiguration conf = object.getContainer().getConfigurationSection(attribute.hasParam() ? attribute.getParam() : "");
            if (conf == null) {
                return null;
            }
            return new ListTag(conf.getKeys(false));
        });

        // <--[tag]
        // @attribute <ScriptTag.list_deep_keys[(<start_key>)]>
        // @returns ListTag
        // @description
        // Returns a list of all keys within a script, searching recursively, with an optional starting-key.
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "list_deep_keys", (attribute, object) -> {
            YamlConfiguration conf = object.getContainer().getConfigurationSection(attribute.hasParam() ? attribute.getParam() : "");
            if (conf == null) {
                return null;
            }
            return new ListTag(conf.getKeys(true));
        });

        // <--[tag]
        // @attribute <ScriptTag.to_json>
        // @returns ElementTag
        // @description
        // Converts the Script Container to a JSON array.
        // Best used with 'data' type scripts.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_json", (attribute, object) -> {
            JSONObject jsobj = new JSONObject(YamlConfiguration.reverse(object.container.getContents().getMap(), true));
            jsobj.remove("type");
            return new ElementTag(jsobj.toString());
        });

        // <--[tag]
        // @attribute <ScriptTag.to_yaml>
        // @returns ElementTag
        // @description
        // Converts the Script Container to raw YAML text.
        // Best used with 'data' type scripts.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_yaml", (attribute, object) -> {
            YamlConfiguration config = new YamlConfiguration();
            config.addAll(object.getContainer().getContents().getMap());
            config.set("type", null);
            return new ElementTag(config.saveToString(true));
        });
        tagProcessor.registerFutureTagDeprecation("to_yaml", "to_text");

        // <--[tag]
        // @attribute <ScriptTag.queues>
        // @returns ListTag(QueueTag)
        // @description
        // Returns all queues which are running for this script.
        // -->
        tagProcessor.registerTag(ListTag.class, "queues", (attribute, object) -> {
            ListTag queues = new ListTag();
            for (ScriptQueue queue : ScriptQueue.getQueues()) {
                if (queue.script != null && queue.script.getName().equals(object.getName())) {
                    queues.addObject(new QueueTag(queue));
                }
            }
            return queues;
        }, "list_queues");
    }

    public static ObjectTagProcessor<ScriptTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        mechanism.echoError("Cannot apply properties to a script!");
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // TODO: enable/disable mechs?

        tagProcessor.processMechanism(this, mechanism);
    }
}
