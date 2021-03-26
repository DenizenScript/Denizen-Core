package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.containers.core.ProcedureScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.FutureWarning;

import java.util.HashMap;

public class ObjectTagProcessor<T extends ObjectTag> {

    public HashMap<String, TagRunnable.ObjectInterface<T>> registeredObjectTags = new HashMap<>();

    public ObjectTagProcessor() {

        // <--[tag]
        // @attribute <ObjectTag.prefix>
        // @returns ElementTag
        // @description
        // Returns the prefix of the tag type that is processing this tag, like 'List'.
        // Prefixes are generally only used for debugging (for example, command execution reports show them).
        // -->
        registerTag("prefix", (attribute, object) -> {
            return new ElementTag(object.getPrefix());
        });

        // <--[tag]
        // @attribute <ObjectTag.object_type>
        // @returns ElementTag
        // @description
        // Returns the name of the tag type that is processing this tag, like 'List'.
        // This tag is made available to help you debug script issues, for example if you think an object isn't processing its own type correctly.
        // -->
        registerTag("object_type", (attribute, object) -> {
            return new ElementTag(object.getObjectType());
        }, "type");

        // <--[tag]
        // @attribute <ObjectTag.proc[<procedure_script_name>]>
        // @returns ObjectTag
        // @description
        // Returns the 'determine' result of a procedure script, passing this object in as the context value.
        // -->
        registerTag("proc", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                return null;
            }
            ScriptTag script;
            String path = null;
            if (attribute.getContext(1).indexOf('.') > 0) {
                String[] split = attribute.getContext(1).split("\\.", 2);
                path = split[1];
                script = ScriptTag.valueOf(split[0], attribute.context);
            }
            else {
                script = attribute.contextAsType(1, ScriptTag.class);
            }
            if (script == null) {
                attribute.echoError("Missing script for procedure script tag '" + attribute.getContext(1) + "'!");
                return null;
            }
            if (!(script.getContainer() instanceof ProcedureScriptContainer)) {
                attribute.echoError("Chosen script is not a procedure script!");
                return null;
            }
            ListTag definitions = new ListTag();
            definitions.addObject(object);
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
    }

    public void registerFutureTagDeprecation(String name, String... deprecatedVariants) {
        TagRunnable.ObjectInterface<T> properTag = registeredObjectTags.get(name);
        for (String variant : deprecatedVariants) {
            TagRunnable.ObjectInterface<T> newRunnable = (attribute, object) -> {
                if (FutureWarning.futureWarningsEnabled) {
                    Debug.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                            "Using deprecated form of tag '" + name + "': '" + variant + "'.");
                }
                return properTag.run(attribute, object);
            };
            registeredObjectTags.put(variant, newRunnable);
        }
    }

    public void registerTag(String name, TagRunnable.ObjectInterface<T> runnable, String... deprecatedVariants) {
        for (String variant : deprecatedVariants) {
            TagRunnable.ObjectInterface<T> newRunnable = (attribute, object) -> {
                Debug.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + name + "': '" + variant + "'.");
                return runnable.run(attribute, object);
            };
            registeredObjectTags.put(variant, newRunnable);
        }
        registeredObjectTags.put(name, runnable);
    }

    public ObjectTag getObjectAttribute(T object, Attribute attribute) {
        if (attribute == null) {
            if (Debug.verbose) {
                Debug.log("TagProcessor - Attribute null!");
            }
            return null;
        }
        if (attribute.isComplete()) {
            if (Debug.verbose) {
                Debug.log("TagProcessor - Attribute complete! Self return!");
            }
            return object;
        }
        String attrLow = attribute.getAttributeWithoutContext(1);
        ObjectTag returned;
        TagRunnable.ObjectInterface<T> otr = registeredObjectTags.get(attrLow);
        if (otr != null) {
            if (Debug.verbose) {
                Debug.log("TagProcessor - Sub-tag found for " + attrLow);
            }
            attribute.seemingSuccesses.add(attrLow);
            returned = otr.run(attribute, object);
            if (returned == null) {
                if (Debug.verbose) {
                    Debug.log("TagProcessor - result was null");
                }
                return null;
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
