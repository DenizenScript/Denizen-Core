package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.tags.TagManager;

public class ContextTagBase {

    public ContextTagBase() {
        // Intentionally no docs
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                contextTags(event);
            }
        }, "context", "c");
        // Intentionally no docs
        TagManager.registerTagHandler(ObjectTag.class, ElementTag.class, "entry", (attribute, heldId) -> {
            attribute.fulfill(1);
            String saveEntryKey = attribute.getAttributeWithoutParam(1);
            DefinitionProvider definitionProvider = attribute.context.definitionProvider;
            if (definitionProvider == null) {
                attribute.echoError("No definitions are provided in this tag's context!");
                return null;
            }
            ObjectTag def = definitionProvider.getDefinitionObject("__save_entries." + heldId.toString() + "." + saveEntryKey);
            if (def == null) {
                attribute.echoError("Invalid saved entry ID '" + heldId + "." + saveEntryKey + "'");
                return null;
            }
            return def;
        });
    }

    public void contextTags(ReplaceableTagEvent event) {
        Attribute attribute = event.getAttributes();
        if (!event.matches("context", "c") || attribute.context.contextSource == null) {
            return;
        }
        if (event.matches("c")) {
            Deprecations.contextShorthand.warn(event.getScriptEntry());
        }
        String contextName = attribute.getAttributeWithoutParam(2);
        ObjectTag obj = event.getAttributes().context.contextSource.getContext(contextName);
        if (obj != null) {
            event.setReplacedObject(CoreUtilities.autoAttrib(obj, attribute.fulfill(2)));
            return;
        }
        if (!event.hasAlternative()) {
            attribute.echoError("Invalid context ID '" + contextName + "'!");
        }
    }
}
