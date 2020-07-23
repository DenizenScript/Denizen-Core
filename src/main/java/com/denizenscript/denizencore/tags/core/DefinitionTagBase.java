package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public class DefinitionTagBase {

    public DefinitionTagBase() {

        // <--[tag]
        // @attribute <definition[<name>]>
        // @returns ObjectTag
        // @description
        // Returns a definition from the current queue.
        // The object will be returned as the most-valid type based on the input.
        // In most usages, the tag name is left blank, like "<[defhere]>".
        // -->
        TagRunnable.BaseInterface defTag = (attribute) -> {
            if (!attribute.hasContext(1)) {
                Debug.echoError("Invalid definition tag, no context specified!");
                return null;
            }
            String defName = attribute.getContext(1);
            DefinitionProvider definitionProvider = attribute.context.definitionProvider;
            if (definitionProvider == null) {
                Debug.echoError("No definitions are provided in this tag's context!");
                return null;
            }
            ObjectTag def = definitionProvider.getDefinitionObject(defName);
            if (attribute.startsWith("exists", 2)) {
                Deprecations.defExistsTag.warn(attribute.context);
                attribute.fulfill(1);
                return new ElementTag(def != null);
            }
            if (def == null) {
                attribute.echoError("Invalid definition name '" + defName + "'.");
                return null;
            }
            return CoreUtilities.fixType(def, attribute.context);
        };
        TagManager.registerTagHandler("def", defTag);
        TagManager.registerTagHandler("definition", defTag);
        TagManager.registerTagHandler("", defTag);
    }
}

