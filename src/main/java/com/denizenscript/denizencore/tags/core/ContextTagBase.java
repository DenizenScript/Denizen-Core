package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.TagManager;

public class ContextTagBase {

    public ContextTagBase() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                contextTags(event);
            }
        }, "context", "c");
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                savedEntryTags(event);
            }
        }, "entry", "e");
    }

    public void contextTags(ReplaceableTagEvent event) {
        if (!event.matches("context", "c") || event.getScriptEntry() == null) {
            return;
        }
        if (event.matches("c") && (event.getScriptEntry() == null || event.getScriptEntry().shouldDebug())) {
            Deprecations.contextShorthand.warn(event.getScriptEntry());
        }
        String object = event.getType();
        ObjectTag obj = event.getScriptEntry().getResidingQueue().getContext(object);
        if (obj != null) {
            Attribute attribute = event.getAttributes();
            event.setReplacedObject(CoreUtilities.autoAttrib(obj, attribute.fulfill(2)));
            return;
        }
        if (!event.hasAlternative()) {
            Debug.echoError(event.getScriptEntry() != null ? event.getScriptEntry().getResidingQueue() : null, "Invalid context ID '" + object + "'!");
        }
    }

    public void savedEntryTags(ReplaceableTagEvent event) {
        if (!event.matches("entry", "e")
                || event.getScriptEntry() == null
                || !event.hasNameContext()) {
            return;
        }
        if (event.matches("e")) {
            Deprecations.entryShorthand.warn(event.getScriptEntry());
        }
        if (event.getScriptEntry().getResidingQueue() != null) {
            String id = event.getNameContext();
            Attribute attribute = event.getAttributes();
            ScriptEntry held = event.getScriptEntry().getResidingQueue().getHeldScriptEntry(id);
            if (held == null) {
                if (!event.hasAlternative()) {
                    Debug.echoDebug(event.getScriptEntry(), "Bad saved entry ID '" + id + "'");
                }
            }
            else {
                String attrib = CoreUtilities.toLowerCase(attribute.getAttributeWithoutContext(2));
                ObjectTag got = held.getObjectTag(attrib);
                if (got == null) {
                    if (!event.hasAlternative()) {
                        Debug.echoDebug(event.getScriptEntry(), "Missing saved entry object '" + attrib + "'");
                        if (Debug.verbose) {
                            Debug.log("Option set is: " + held.getObjects().keySet());
                        }
                    }
                }
                else {
                    event.setReplacedObject(CoreUtilities.autoAttribTyped(got, attribute.fulfill(2)));
                }
            }
        }
    }
}
