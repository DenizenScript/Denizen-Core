package net.aufdemrand.denizencore.objects;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.scripts.ScriptRegistry;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class dScript implements dObject, Adjustable {

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
    // @group Object System
    // @description
    // 1) A dObject that represents a script container. dScripts contain all information inside the script, and can be
    // used in a variety of commands that require script arguments. For example, run and inject will 'execute'
    // script entries inside of a script container when given a matching dScript object.
    //
    // dScripts also provide a way to access attributes accessed by the replaceable tag system by using the object
    // fetcher or any other entry point to a dScript object. dScript objects have the object identifier of 's@'.
    // For example: s@script_name
    //
    // 2) The overall 'scripting language' that Denizen implements is referred to as 'dScripting', or 'dScript'.
    // dScripts use YAML + Denizen's Scripting API to parse scripts that are stored as .yml or .dscript files. Scripts
    // go in the .../plugins/Denizen/scripts folder.
    //
    // -->

    ///////////////
    // Object Fetcher
    /////////////

    // <--[language]
    // @name s@
    // @group Object Fetcher System
    // @description
    // s@ refers to the 'object identifier' of a dScript. The 's@' is notation for Denizen's Object
    // Fetcher. The only valid constructor for a dScript is the name of the script container that it should be
    // associated with. For example, if my script container is called 'cool_script', the dScript object for that script
    // would be able to be referenced (fetched) with s@cool_script.
    // -->


    public static dScript valueOf(String string) {
        return valueOf(string, null);
    }

    /**
     * Gets a dContainer Object from a dScript argument.
     *
     * @param string the dScript argument String
     * @return a Script, or null if incorrectly formatted
     */
    @Fetchable("s")
    public static dScript valueOf(String string, TagContext context) {

        if (string.startsWith("s@")) {
            string = string.substring(2);
        }

        dScript script = new dScript(string);
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

        dScript script = new dScript(string);
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
    public dScript(String scriptName) {
        if (ScriptRegistry.getScriptContainer(scriptName) != null) {
            container = ScriptRegistry.getScriptContainer(scriptName);
            name = scriptName.toUpperCase();
            valid = true;
        }
    }

    public dScript(ScriptContainer container) {
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
    // dObject Methods
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
    public dObject setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debug() {
        return String.format("<G>%s='<A>%s<Y>(%s)<G>'  ", prefix, name, getType());
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    public static void registerTags() {

        // <--[tag]
        // @attribute <s@script.container_type>
        // @returns Element
        // @description
        // Returns the type of script container that is associated with this dScript object. For example: 'task', or
        // 'world'.
        // -->

        registerTag("container_type", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((dScript) object).container.getContainerType()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.name>
        // @returns Element
        // @description
        // Returns the name of the script container.
        // -->

        registerTag("name", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((dScript) object).name).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.relative_filename>
        // @returns Element
        // @description
        // Returns the filename that contains the script, relative to the denizen/ folder.
        // -->

        registerTag("relative_filename", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                try {
                    String fn = ((dScript) object).container.getFileName().replace(DenizenCore.getImplementation()
                            .getScriptFolder().getParentFile().getCanonicalPath(), "").replace("\\", "/");
                    while (fn.startsWith("/")) {
                        fn = fn.substring(1);
                    }
                    return new Element(fn)
                            .getAttribute(attribute.fulfill(1));
                }
                catch (Exception e) {
                    dB.echoError(e);
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <s@script.filename>
        // @returns Element
        // @description
        // Returns the absolute filename that contains the script.
        // -->

        registerTag("filename", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((dScript) object).container.getFileName().replace("\\", "/")).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.original_name>
        // @returns Element
        // @description
        // Returns the originally cased script name.
        // -->

        registerTag("original_name", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((dScript) object).container.getOriginalName()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.constant[<constant_name>]>
        // @returns Element or dList
        // @description
        // Returns the value of the constant as either an Element or dList.
        // A constant is a script key under the 'default constants' node.
        // For example:
        // myscript:
        //   type: task
        //   default constants:
        //     myconstant: myvalue
        // -->

        registerTag("constant", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag s@script.constant[...] must have a value.");
                    return null;
                }
                YamlConfiguration section = ((dScript) object).getContainer().getConfigurationSection("default constants");
                if (section == null) {
                    return null;
                }
                Object obj = section.get(attribute.getContext(1).toUpperCase());
                if (obj == null) {
                    return null;
                }

                if (obj instanceof List) {
                    dList list = new dList();
                    for (Object each : (List<Object>) obj) {
                        if (each == null) {
                            each = "null";
                        }
                        // TODO
                        list.add(TagManager.tag(each.toString(), DenizenCore.getImplementation().getTagContext(attribute.getScriptEntry())));
                    }
                    return list.getAttribute(attribute.fulfill(1));

                }
                // TODO
                else {
                    return new Element(TagManager.tag(obj.toString(), DenizenCore.getImplementation().getTagContext(attribute.getScriptEntry())))
                            .getAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <s@script.yaml_key[<constant_name>]>
        // @returns dObject
        // @description
        // Returns the value of the script's YAML as either an Element or dList.
        // -->

        registerTag("yaml_key", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag s@script.constant[...] must have a value.");
                    return null;
                }
                dScript scr = (dScript) object;
                ScriptContainer container = scr.getContainer();
                if (container == null) {
                    dB.echoError("Missing script container?!");
                    return new Element(scr.identify()).getAttribute(attribute);
                }
                YamlConfiguration section = container.getConfigurationSection("");
                if (section == null) {
                    dB.echoError("Missing YAML section?!");
                    return new Element(scr.identify()).getAttribute(attribute);
                }
                Object obj = section.get(attribute.getContext(1).toUpperCase());
                if (obj == null) {
                    return null;
                }

                if (obj instanceof List) {
                    dList list = new dList();
                    for (Object each : (List<Object>) obj) {
                        if (each == null) {
                            each = "null";
                        }
                        list.add(each.toString());
                    }
                    return list.getAttribute(attribute.fulfill(1));

                }
                else {
                    return new Element(obj.toString())
                            .getAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <s@script.list_keys[<constant_name>]>
        // @returns dList
        // @description
        // Returns a list of all keys within a script.
        // -->

        registerTag("list_keys", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                YamlConfiguration conf = ((dScript) object).getContainer().getConfigurationSection(attribute.hasContext(1) ?
                        attribute.getContext(1) : "");
                if (conf == null) {
                    return null;
                }
                return new dList(conf.getKeys(false))
                        .getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.list_deep_keys[<constant_name>]>
        // @returns dList
        // @description
        // Returns a list of all keys within a script, searching recursively.
        // -->

        registerTag("list_deep_keys", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                YamlConfiguration conf = ((dScript) object).getContainer().getConfigurationSection(attribute.hasContext(1) ?
                        attribute.getContext(1) : "");
                if (conf == null) {
                    return null;
                }
                return new dList(conf.getKeys(true))
                        .getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.to_json>
        // @returns Element
        // @description
        // Converts the YAML Script Container to a JSON array.
        // Best used with 'yaml data' type scripts.
        // -->

        registerTag("to_json", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                JSONObject jsobj = new JSONObject(((dScript) object).container.getConfigurationSection("").getMap());
                jsobj.remove("TYPE");
                return new Element(jsobj.toString()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.to_text>
        // @returns Element
        // @description
        // Converts the YAML Script Container to raw YAML text.
        // Best used with 'yaml data' type scripts.
        // -->

        registerTag("to_text", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                YamlConfiguration config = new YamlConfiguration();
                config.addAll(((dScript) object).getContainer().getContents().getMap());
                config.set("type", null);
                return new Element(config.saveToString()).getAttribute(attribute.fulfill(1));
            }
        });

        /////////////////
        // dObject attributes
        ///////////////

        // <--[tag]
        // @attribute <s@script.debug>
        // @returns Element
        // @description
        // Returns the debug entry for this object. This contains the prefix, the name of the dScript object, and the
        // type of ScriptContainer is held within. All objects fetchable by the Object Fetcher will return a valid
        // debug entry for the object that is fulfilling this attribute.
        // -->

        registerTag("debug", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(object.debug()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.prefix>
        // @returns Element
        // @description
        // Returns the prefix for this object. By default this will return 'Script', however certain situations will
        // return a finer scope. All objects fetchable by the Object Fetcher will return a valid prefix for the object
        // that is fulfilling this attribute.
        // -->

        registerTag("prefix", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((dScript) object).prefix).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.type>
        // @returns Element
        // @description
        // Always returns 'Script' for dScript objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->

        registerTag("type", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element("Script").getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <s@script.list_queues>
        // @returns dList(Queue)
        // @description
        // Returns all queues which are running for this script.
        // -->

        registerTag("list_queues", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dScript script = (dScript) object;
                dList queues = new dList();
                for (ScriptQueue queue : ScriptQueue._getQueues()) {
                    if (queue.script != null && queue.script.getName().equals(script.getName())) {
                        queues.add(queue.identify());
                    }
                }
                return queues.getAttribute(attribute.fulfill(1));
            }
        });
    }

    public static HashMap<String, TagRunnable> registeredTags = new HashMap<>();

    public static void registerTag(String name, TagRunnable runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registeredTags.put(name, runnable);
    }

    @Override
    public String getAttribute(Attribute attribute) {
        if (attribute == null) {
            return "null";
        }
        // TODO: Scrap getAttribute, make this functionality a core system
        String attrLow = CoreUtilities.toLowerCase(attribute.getAttributeWithoutContext(1));
        TagRunnable tr = registeredTags.get(attrLow);
        if (tr != null) {
            if (!tr.name.equals(attrLow)) {
                dB.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + tr.name + "': '" + attrLow + "'.");
            }
            return tr.run(attribute, this);
        }

        String returned = CoreUtilities.autoPropertyTag(this, attribute);
        if (returned != null) {
            return returned;
        }

        return new Element(identify()).getAttribute(attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        dB.echoError("Cannot apply properties to a script!");
    }

    @Override
    public void adjust(Mechanism mechanism) {

        // TODO: enable/disable

        if (!mechanism.fulfilled()) {
            mechanism.reportInvalid();
        }
    }

}
