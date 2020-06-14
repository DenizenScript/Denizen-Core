package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.FutureWarning;

import java.util.HashMap;

public class ObjectTagProcessor<T extends ObjectTag> {

    public HashMap<String, TagRunnable.ObjectInterface<T>> registeredObjectTags = new HashMap<>();

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
            attribute.seemingSuccesses.add(attrLow);
            returned = otr.run(attribute, object);
            if (returned == null) {
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
