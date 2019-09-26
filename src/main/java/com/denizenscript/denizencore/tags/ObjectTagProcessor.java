package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashMap;

public class ObjectTagProcessor<T extends ObjectTag> {

    public HashMap<String, TagRunnable.ObjectForm<T>> registeredObjectTags = new HashMap<>();

    public void registerTag(String name, TagRunnable.ObjectForm<T> runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        else {
            TagRunnable.ObjectForm<T> newRunnable = new TagRunnable.ObjectForm<T>() {
                @Override
                public ObjectTag run(Attribute attribute, T object) {
                    Debug.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                            "Using deprecated form of tag '" + runnable.name + "': '" + name + "'.");
                    return runnable.run(attribute, object);
                }
            };
            newRunnable.name = runnable.name;
            registeredObjectTags.put(name, newRunnable);
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
        TagRunnable.ObjectForm<T> otr = registeredObjectTags.get(attrLow);
        if (otr != null) {
            attribute.seemingSuccesses.add(otr.name);
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
