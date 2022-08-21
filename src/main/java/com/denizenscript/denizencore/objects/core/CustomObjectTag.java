package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.CustomScriptContainer;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;

import java.util.Map;

public class CustomObjectTag implements ObjectTag, Adjustable {

    // <--[ObjectType]
    // @name CustomObjectTag
    // @prefix custom
    // @base ElementTag
    // @format
    // The identity format for custom objects is the script name, followed by property syntax listing all fields with their values.
    //
    // @description
    // Custom objects are custom object types.
    // They use a script basis to create an object similar to the base object types (ListTag, PlayerTags, etc).
    //
    // Usage of these should generally be avoided, as they can be considered 'over-engineering'...
    // That is, using a very complicated solution to solve a problem that can be solved much more simply.
    //
    // Custom objects exist for experimental reasons. Do not use these in any real scripts.
    //
    // -->

    @Fetchable("custom")
    public static CustomObjectTag valueOf(String string, TagContext context) {
        if (ObjectFetcher.isObjectWithProperties(string)) {
            return ObjectFetcher.getObjectFromWithProperties(CustomObjectTag.class, string, context);
        }
        if (string.startsWith("custom@")) {
            string = string.substring("custom@".length());
        }
        String typeData = string;
        ScriptContainer sc = ScriptRegistry.getScriptContainer(typeData);
        if (sc == null) {
            if (context == null || context.showErrors()) {
                Debug.echoError("Null script container for " + typeData);
            }
            return null;
        }
        if (!(sc instanceof CustomScriptContainer)) {
            if (context == null || context.showErrors()) {
                Debug.echoError("Wrong-typed script container for " + typeData);
            }
            return null;
        }
        return new CustomObjectTag((CustomScriptContainer) sc, ((CustomScriptContainer) sc).getVars());
    }

    public static CustomObjectTag getFor(ObjectTag obj, TagContext context) {
        return obj instanceof CustomObjectTag ? (CustomObjectTag) obj : valueOf(obj.toString(), context);
    }

    public static boolean matches(String string) {
        return string.startsWith("custom@");
    }

    public CustomScriptContainer container;
    public Map<String, ObjectTag> vars;

    public CustomObjectTag(CustomScriptContainer type, Map<String, ObjectTag> values) {
        container = type;
        vars = values;
    }

    private String prefix = "Custom";

    @Override
    public String identify() {
        StringBuilder outp = new StringBuilder();
        for (Map.Entry<String, ObjectTag> var : vars.entrySet()) {
            outp.append(PropertyParser.escapePropertyKey(var.getKey())).append("=").append(PropertyParser.escapePropertyValue(var.getValue().toString())).append(";");
        }
        return "custom@" + container.getName() + "[" + (outp.length() > 0 ? outp.substring(0, outp.length() - 1) : "") + "]";
    }

    @Override
    public String identifySimple() {
        return "custom@" + container.getName();
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
    public boolean isUnique() {
        return false;
    }

    public static ObjectTagProcessor<CustomObjectTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }


    @Override
    public ObjectTag specialTagProcessing(Attribute attribute) {
        if (attribute == null) {
            return null;
        }
        if (attribute.isComplete()) {
            return this;
        }
        ObjectTag res = vars.get(attribute.getAttribute(1));
        if (res != null) {
            return CoreUtilities.autoAttribTyped(res, attribute.fulfill(1));
        }
        String taggo = attribute.getAttributeWithoutParam(1);
        if (container.containsScriptSection("tags." + taggo)) {
            ListTag outcomes = container.runTagScript(taggo, attribute.getParamObject(), this,
                    attribute.getScriptEntry() != null ? attribute.getScriptEntry().entryData :
                            DenizenCore.implementation.getEmptyScriptEntryData());
            if (outcomes == null) {
                return null;
            }
            return CoreUtilities.autoAttribTyped(outcomes.getObject(0), attribute.fulfill(1));
        }
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        adjust(mechanism);
    }

    @Override
    public void adjust(Mechanism mechanism) {
        String name = CoreUtilities.toLowerCase(mechanism.getName());
        if (!mechanism.hasValue()) {
            vars.remove(name);
            return;
        }
        ObjectTag value = mechanism.getValue();
        if (container.containsScriptSection("mechanisms." + name)) {
            ListTag outcomes = container.runMechScript(name, this, value);
            if (outcomes == null) {
                return;
            }
            CustomObjectTag co = CustomObjectTag.getFor(outcomes.getObject(0), null);
            container = co.container;
            vars = co.vars;
        }
        else {
            vars.put(name, value);
        }
        mechanism.fulfill();
    }
}
