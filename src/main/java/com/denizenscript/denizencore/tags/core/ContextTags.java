package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.TagRunnable;
import com.denizenscript.denizencore.objects.dObject;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.SlowWarning;
import com.denizenscript.denizencore.utilities.debugging.dB;
import com.denizenscript.denizencore.tags.TagManager;

public class ContextTags {

    public ContextTags() {
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

    public SlowWarning contextShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'context' instead of 'c' as a root tag.");
    public SlowWarning entryShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'entry' instead of 'e' as a root tag.");

    public void contextTags(ReplaceableTagEvent event) {
        if (!event.matches("context", "c") || event.getScriptEntry() == null) {
            return;
        }
        if (event.matches("c") && (event.getScriptEntry() == null || event.getScriptEntry().shouldDebug())) {
            contextShorthand.warn(event.getScriptEntry());
        }
        String object = event.getType();
        dObject obj = event.getScriptEntry().getResidingQueue().getContext(object);
        if (obj != null) {
            Attribute attribute = event.getAttributes();
            event.setReplacedObject(CoreUtilities.autoAttrib(obj, attribute.fulfill(2)));
            return;
        }
        if (!event.hasAlternative()) {
            dB.echoError(event.getScriptEntry() != null ? event.getScriptEntry().getResidingQueue() : null, "Invalid context ID '" + object + "'!");
        }
    }

    public void savedEntryTags(ReplaceableTagEvent event) {
        if (!event.matches("entry", "e")
                || event.getScriptEntry() == null
                || !event.hasNameContext()) {
            return;
        }
        if (event.matches("e")) {
            entryShorthand.warn(event.getScriptEntry());
        }
        if (event.getScriptEntry().getResidingQueue() != null) {
            String id = event.getNameContext();
            Attribute attribute = event.getAttributes();
            ScriptEntry held = event.getScriptEntry().getResidingQueue().getHeldScriptEntry(id);
            if (held == null) {
                if (!event.hasAlternative()) {
                    dB.echoDebug(event.getScriptEntry(), "Bad saved entry ID '" + id + "'");
                }
            }
            else {
                String attrib = CoreUtilities.toLowerCase(attribute.getAttributeWithoutContext(2));
                dObject got = held.getdObject(attrib);
                if (got == null) {
                    if (!event.hasAlternative()) {
                        dB.echoDebug(event.getScriptEntry(), "Missing saved entry object '" + attrib + "'");
                        if (dB.verbose) {
                            dB.log("Option set is: " + held.getObjects().keySet());
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
