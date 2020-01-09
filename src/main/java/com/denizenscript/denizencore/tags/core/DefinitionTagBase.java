package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public class DefinitionTagBase {

    public DefinitionTagBase() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                definitionTag(event);
            }
        }, "def", "definition", "d", "");
    }

    //////////
    //  ReplaceableTagEvent handler
    ////////

    public void definitionTag(ReplaceableTagEvent event) {

        if (!event.matches("definition", "def", "d", "")) {
            return;
        }

        if (event.matches("d")) {
            Deprecations.defShorthand.warn(event.getScriptEntry());
        }

        if (!event.hasNameContext()) {
            Debug.echoError("Invalid definition tag, no context specified!");
            return;
        }

        // <--[tag]
        // @attribute <definition[<name>]>
        // @returns ObjectTag
        // @description
        // Returns a definition from the current queue.
        // The object will be returned as the most-valid type based on the input.
        // -->
        String defName = event.getNameContext();

        DefinitionProvider definitionProvider = event.getContext().definitionProvider;
        if (definitionProvider == null) {
            Debug.echoError("No definitions are provided at this moment!");
            return;
        }
        ObjectTag def = definitionProvider.getDefinitionObject(defName);

        Attribute atttribute = event.getAttributes().fulfill(1);

        // <--[tag]
        // @attribute <definition[<name>].exists>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether a definition exists for the given definition name.
        // -->
        if (atttribute.startsWith("exists")) {
            if (def == null) {
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(false), atttribute.fulfill(1)));
            }
            else {
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(true), atttribute.fulfill(1)));
            }
            return;
        }

        if (def == null) {
            if (!event.hasAlternative()) {
                Debug.echoError("Invalid definition name '" + defName + "'.");
            }
            return;
        }

        event.setReplacedObject(CoreUtilities.autoAttribTyped(def, atttribute));
    }
}

