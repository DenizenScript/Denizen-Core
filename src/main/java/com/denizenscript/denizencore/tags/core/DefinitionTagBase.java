package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
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
        // You can use "." in a definition name to read a submapped key if the root definition is a MapTag.
        // @example
        // - define x 3
        // # Narrates '3'
        // - narrate <[x]>
        // @example
        // - definemap mymap:
        //     mykey: example
        // # Narrates 'example'
        // - narrate <[mymap.mykey]>
        // -->
        TagRunnable.BaseInterface<ObjectTag> defTag = (attribute) -> {
            if (!attribute.hasParam()) {
                Debug.echoError("Invalid definition tag, no context specified!");
                return null;
            }
            String defName = attribute.getParam();
            DefinitionProvider definitionProvider = attribute.context.definitionProvider;
            if (definitionProvider == null) {
                Debug.echoError("No definitions are provided in this tag's context!");
                return null;
            }
            ObjectTag def = definitionProvider.getDefinitionObject(defName);
            if (def == null) {
                attribute.echoError("Invalid definition name '" + defName + "'.");
                return null;
            }
            if (attribute.attributes.length == 1) {
                return def;
            }
            return CoreUtilities.fixType(def, attribute.context);
        };
        TagManager.registerTagHandler(ObjectTag.class, "def", defTag);
        TagManager.registerTagHandler(ObjectTag.class, "definition", defTag);
        TagManager.registerTagHandler(ObjectTag.class, "", defTag);
    }
}
