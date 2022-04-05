package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.exceptions.TagProcessingException;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.commands.Comparable;
import com.denizenscript.denizencore.scripts.containers.core.ProcedureScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.utilities.codegen.TagNamer;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashMap;

public class ObjectTagProcessor<T extends ObjectTag> {

    public static class TagData<T extends ObjectTag, R extends ObjectTag> {

        public String name;

        public TagRunnable.ObjectInterface<T, R> runner;

        public Class<R> returnType;

        public ObjectTagProcessor<R> processor;

        public ObjectTagProcessor<T> source;

        public boolean isStatic;

        public TagData(ObjectTagProcessor<T> source, String name, TagRunnable.ObjectInterface<T, R> runner, Class<R> returnType, boolean isStatic) {
            this.source = source;
            this.name = name;
            this.runner = runner;
            this.returnType = returnType;
            this.isStatic = isStatic;
            ObjectFetcher.ObjectType<R> type = (ObjectFetcher.ObjectType<R>) ObjectFetcher.objectsByClass.get(returnType);
            this.processor = type == null ? null : type.tagProcessor;
        }
    }

    public HashMap<String, TagData<? extends ObjectTag, ? extends ObjectTag>> registeredObjectTags = new HashMap<>();

    public Class<T> type;

    public void generateCoreTags() {

        // <--[tag]
        // @attribute <ObjectTag.prefix>
        // @returns ElementTag
        // @description
        // Returns the prefix of the tag type that is processing this tag, like 'List'.
        // Prefixes are generally only used for debugging (for example, command execution reports show them).
        // -->
        registerTag(ElementTag.class, "prefix", (attribute, object) -> {
            return new ElementTag(object.getPrefix());
        });

        // <--[tag]
        // @attribute <ObjectTag.object_type>
        // @returns ElementTag
        // @description
        // Returns the name of the tag type that is processing this tag, like 'List'.
        // This tag is made available to help you debug script issues, for example if you think an object isn't processing its own type correctly.
        // -->
        registerTag(ElementTag.class, "object_type", (attribute, object) -> {
            return new ElementTag(object.getObjectType());
        }, "type");

        // <--[tag]
        // @attribute <ObjectTag.proc[<procedure_script_name>]>
        // @returns ObjectTag
        // @description
        // Returns the 'determine' result of a procedure script, passing this object in as the context value.
        // -->
        registerTag(ObjectTag.class, "proc", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            ScriptTag script;
            String path = null;
            if (attribute.getParam().indexOf('.') > 0) {
                String[] split = attribute.getParam().split("\\.", 2);
                path = split[1];
                script = ScriptTag.valueOf(split[0], attribute.context);
            }
            else {
                script = attribute.paramAsType(ScriptTag.class);
            }
            if (script == null) {
                attribute.echoError("Missing script for procedure script tag '" + attribute.getParam() + "'!");
                return null;
            }
            if (!(script.getContainer() instanceof ProcedureScriptContainer)) {
                attribute.echoError("Chosen script is not a procedure script!");
                return null;
            }
            ListTag definitions = new ListTag();
            definitions.addObject(object);

            // <--[tag]
            // @attribute <ObjectTag.proc[<procedure_script_name>].context[<object>|...]>
            // @returns ObjectTag
            // @description
            // Returns the 'determine' result of a procedure script, passing this object in as the first context value, with a list of additional context values.
            // -->
            if (attribute.startsWith("context", 2) && attribute.hasContext(2)) {
                definitions.objectForms.addAll(attribute.contextAsType(2, ListTag.class).objectForms);
                attribute.fulfill(1);
            }
            ScriptQueue queue = ScriptUtilities.createAndStartQueue(script.getContainer(), path, attribute.context.getScriptEntryData(), null, (q) -> {
                q.procedural = true;
            }, null, null, definitions, script.getContainer());
            if (queue == null) {
                attribute.echoError("Procedure queue start failed.");
                return null;
            }
            if (queue.determinations != null && queue.determinations.size() > 0) {
                return queue.determinations.getObject(0);
            }
            return null;
        });

        // <--[tag]
        // @attribute <ObjectTag.if_null[<object>]>
        // @returns ObjectTag
        // @description
        // If the object is null (or the tag errors), this will return the input object.
        // If the object isn't null, the input won't be parsed, and the original object will be returned.
        // For example, "<player.if_null[<npc>]>" will return the player if there is a player, and otherwise will return the NPC.
        // This functions as a fallback - meaning, if the tag up to this point errors, that error will be hidden.
        // -->
        registerTag(ObjectTag.class, "if_null", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            return object;
        });

        // <--[tag]
        // @attribute <ObjectTag.exists>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the object exists (is non-null). Returns false if the object doesn't exist, is null, or the tag errored.
        // This functions as a fallback - meaning, if the tag up to this point errors, that error will be hidden.
        // -->
        registerTag(ElementTag.class, "exists", (attribute, object) -> {
            return new ElementTag(true);
        });

        // <--[tag]
        // @attribute <ObjectTag.is_truthy>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the object is 'truthy'. An object is 'truthy' if it exists and is valid, and does not represent a concept like emptiness.
        // An empty list or an air item will return 'false'. Plaintext "null" or "false", an empty element, or a numeric zero will return 'false' as well.
        // Some object types may have their own logical implementations, for examples an EntityTag value is 'truthy' only if the entity it represents is spawned.
        // Errored/broken/invalid tags are also considered 'false' by this logic.
        // This functions as a fallback - meaning, if the tag up to this point errors, that error will be hidden.
        // -->
        registerTag(ElementTag.class, "is_truthy", (attribute, object) -> {
            return new ElementTag(object.isTruthy());
        });

        // <--[tag]
        // @attribute <ObjectTag.null_if[<tag>]>
        // @returns ObjectTag
        // @description
        // Parses the given tag on the object, expecting a boolean return.
        // If the return is 'true', the 'null_if' tag returns null.
        // If the return is 'false', the 'null_if' tag returns the original object.
        // Consider also <@link tag ObjectTag.null_if_tag>.
        // -->
        registerTag(ObjectTag.class, "null_if", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            String tag = attribute.getParam();
            boolean defaultValue = tag.endsWith("||true");
            if (defaultValue) {
                tag = tag.substring(0, tag.length() - "||true".length());
            }
            Attribute subAttribute;
            try {
                subAttribute = new Attribute(tag, attribute.getScriptEntry(), attribute.context);
            }
            catch (TagProcessingException ex) {
                attribute.echoError("Tag processing failed: " + ex.getMessage());
                return null;
            }
            Attribute tempAttrib = new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context);
            tempAttrib.setHadAlternative(true);
            ObjectTag objs = CoreUtilities.autoAttribTyped(object, tempAttrib);
            if ((objs == null) ? defaultValue : CoreUtilities.equalsIgnoreCase(objs.toString(), "true")) {
                return null;
            }
            return object;
        });

        // <--[tag]
        // @attribute <ObjectTag.null_if_tag[<dynamic-boolean>]>
        // @returns ObjectTag
        // @description
        // Parses the given tag on the object, expecting a boolean return.
        // This requires a fully formed tag as input, making use of the 'null_if_value' definition.
        // If the return is 'true', the 'null_if' tag returns null.
        // If the return is 'false', the 'null_if' tag returns the original object.
        // Consider also <@link tag ObjectTag.null_if_tag>.
        // -->
        registerTag(ObjectTag.class, "null_if_tag", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            Attribute.OverridingDefinitionProvider provider = new Attribute.OverridingDefinitionProvider(attribute.context.definitionProvider);
            provider.altDefs.putObject("null_if_value", object);
            if (CoreUtilities.equalsIgnoreCase(attribute.parseDynamicParam(provider).toString(), "true")) {
                return null;
            }
            return object;
        });

        registerTag(ElementTag.class, "is", (attribute, object) -> {

            // <--[tag]
            // @attribute <ObjectTag.is[<operator>].to[<object>]>
            // @returns ElementTag(Boolean)
            // @group comparison
            // @description
            // Takes an operator, and compares the first object to the given second object.
            // Returns the outcome of the comparable, either true or false. For information on operators, see <@link language operator>.
            // Equivalent to <@link tag ObjectTag.is[<operator>].than[<element>]>
            // -->

            // <--[tag]
            // @attribute <ObjectTag.is[<operator>].than[<object>]>
            // @returns ElementTag(Boolean)
            // @group comparison
            // @description
            // Takes an operator, and compares the first object to the given second object.
            // Returns the outcome of the comparable, either true or false. For information on operators, see <@link language operator>.
            // Equivalent to <@link tag ObjectTag.is[<operator>].to[<element>]>
            // -->
            if (attribute.hasParam() && (attribute.startsWith("to", 2) || attribute.startsWith("than", 2)) && attribute.hasContext(2)) {
                boolean negative = false;
                String operator;
                if (attribute.getParam().startsWith("!")) {
                    operator = attribute.getParam().substring(1);
                    negative = true;
                }
                else {
                    operator = attribute.getParam();
                }
                attribute = attribute.fulfill(1);
                Comparable.Operator comparableOperator = Comparable.getOperatorFor(operator);
                if (comparableOperator == null) {
                    attribute.echoError("Unknown operator '" + operator + "'.");
                    return null;
                }
                return new ElementTag(Comparable.compare(object, attribute.getParamObject(), comparableOperator, negative, attribute.context));
            }
            return null;
        });

        // <--[tag]
        // @attribute <ObjectTag.repeat_as_list[<#>]>
        // @returns ListTag
        // @group element manipulation
        // @description
        // Returns a list contained the input number of entries, each of which is an exact copy of the object.
        // For example, element[hello].repeat_as_list[3] returns a ListTag of "hello|hello|hello|"
        // An input value or zero or a negative number will result in an empty list.
        // -->
        registerStaticTag(ListTag.class, "repeat_as_list", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag ObjectTag.repeat_as_list[...] must have a value.");
                return null;
            }
            int repeatTimes = attribute.getIntParam();
            ListTag result = new ListTag();
            for (int i = 0; i < repeatTimes; i++) {
                result.addObject(object.duplicate());
            }
            return result;
        });

    }

    public void registerFutureTagDeprecation(String name, String... deprecatedVariants) {
        TagData properTag = registeredObjectTags.get(name);
        for (String variant : deprecatedVariants) {
            TagRunnable.ObjectInterface<T, ?> newRunnable = (attribute, object) -> {
                if (CoreConfiguration.futureWarningsEnabled) {
                    Debug.echoError(attribute.context,  "Using deprecated form of tag '" + name + "': '" + variant + "'.");
                }
                return properTag.runner.run(attribute, object);
            };
            registeredObjectTags.put(variant, new TagData(this, variant, newRunnable, properTag.returnType, false));
        }
    }

    public <R extends ObjectTag> void registerStaticTag(Class<R> returnType, String name, TagRunnable.ObjectInterface<T, R> runnable, String... deprecatedVariants) {
        registerTagInternal(returnType, name, runnable, true, deprecatedVariants);
    }


    public <R extends ObjectTag> void registerTag(Class<R> returnType, String name, TagRunnable.ObjectInterface<T, R> runnable, String... deprecatedVariants) {
        registerTagInternal(returnType, name, runnable, false, deprecatedVariants);
    }

    public <R extends ObjectTag> void registerTagInternal(Class<R> returnType, String name, TagRunnable.ObjectInterface<T, R> runnable, boolean isStatic, String[] deprecatedVariants) {
        final TagRunnable.ObjectInterface<T, R> namedRunnable = TagNamer.nameTagInterface(type, name, runnable);
        for (String variant : deprecatedVariants) {
            TagRunnable.ObjectInterface<T, R> newRunnable = TagNamer.nameTagInterface(type, variant, (attribute, object) -> {
                Debug.echoError(attribute.context, "Using deprecated form of tag '" + name + "': '" + variant + "'.");
                return namedRunnable.run(attribute, object);
            });
            registeredObjectTags.put(variant, new TagData<>(this, variant, newRunnable, returnType, false));
        }
        registeredObjectTags.put(name, new TagData<>(this, name, namedRunnable, returnType, isStatic));
    }

    public final ObjectTag getObjectAttribute(T object, Attribute attribute) {
        if (attribute == null) {
            if (CoreConfiguration.debugVerbose) {
                Debug.log("TagProcessor - Attribute null!");
            }
            return null;
        }
        attribute.lastValid = object;
        if (attribute.isComplete()) {
            if (CoreConfiguration.debugVerbose) {
                Debug.log("TagProcessor - Attribute complete! Self return!");
            }
            return object;
        }
        Attribute.AttributeComponent nextComponent = attribute.attributes[attribute.fulfilled];
        ObjectTag returned;
        TagData data = nextComponent.data;
        if (data == null) {
            data = registeredObjectTags.get(nextComponent.key);
        }
        if (data != null) {
            if (CoreConfiguration.debugVerbose) {
                Debug.log("TagProcessor - Sub-tag found for " + nextComponent.key);
            }
            returned = data.runner.run(attribute, object);
            if (returned == null) {
                if (CoreConfiguration.debugVerbose) {
                    Debug.log("TagProcessor - result was null");
                }
                attribute.trackLastTagFailure();
                return null;
            }
            if (data.processor != null) {
                return data.processor.getObjectAttribute(returned, attribute.fulfill(1));
            }
            return returned.getObjectAttribute(attribute.fulfill(1));
        }
        returned = CoreUtilities.autoPropertyTagObject(object, attribute);
        if (returned == null) {
            returned = object.specialTagProcessing(attribute);
        }
        if (returned != null) {
            return returned;
        }
        return object.getNextObjectTypeDown().getObjectAttribute(attribute);
    }
}
