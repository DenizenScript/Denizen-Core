package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashMap;

public class ObjectTagProcessor {

    public HashMap<String, TagRunnable.ObjectForm> registeredObjectTags = new HashMap<>();

    public void registerTag(String name, TagRunnable.ObjectForm runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        else {
            registeredObjectTags.put(name, new TagRunnable.ObjectForm() {
                @Override
                public ObjectTag run(Attribute attribute, ObjectTag object) {
                    Debug.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                            "Using deprecated form of tag '" + runnable.name + "': '" + name + "'.");
                    return runnable.run(attribute, object);
                }
            });
        }
        registeredObjectTags.put(name, runnable);
    }

    public ObjectTag getObjectAttribute(ObjectTag object, Attribute attribute) {
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
        TagRunnable.ObjectForm otr = registeredObjectTags.get(attrLow);
        if (otr != null) {
            attribute.seemingSuccesses.add(otr.name);
            return otr.run(attribute, object);
        }
        ObjectTag returned = CoreUtilities.autoPropertyTagObject(object, attribute);
        if (returned != null) {
            return returned;
        }
        returned = object.specialTagProcessing(attribute);
        if (returned != null) {
            return returned;
        }
        return object.getNextObjectTypeDown().getObjectAttribute(attribute);
    }
}
