package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.Element;
import com.denizenscript.denizencore.objects.TagRunnable;
import com.denizenscript.denizencore.objects.dObject;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.debugging.SlowWarning;
import com.denizenscript.denizencore.utilities.debugging.dB;

public class DefinitionTags {

    public DefinitionTags() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                definitionTag(event);
            }
        }, "definition", "def", "d", "");
    }


    //////////
    //  ReplaceableTagEvent handler
    ////////

    public SlowWarning defShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'def' instead of 'd' as a root tag.");

    public void definitionTag(ReplaceableTagEvent event) {

        if (!event.matches("definition", "def", "d", "")) {
            return;
        }

        if (event.matches("d")) {
            defShorthand.warn(event.getScriptEntry());
        }

        if (!event.hasNameContext()) {
            dB.echoError("Invalid definition tag, no context specified!");
            return;
        }

        // <--[tag]
        // @attribute <definition[<name>]>
        // @returns dObject
        // @description
        // Returns a definition from the current queue.
        // The object will be returned as the most-valid type based on the input.
        // -->
        // Get the definition from the name input
        String defName = event.getNameContext();

        DefinitionProvider definitionProvider = event.getContext().definitionProvider;
        if (definitionProvider == null) {
            dB.echoError("No definitions are provided at this moment!");
            return;
        }
        dObject def = definitionProvider.getDefinitionObject(defName);

        Attribute atttribute = event.getAttributes().fulfill(1);

        // <--[tag]
        // @attribute <definition[<name>].exists>
        // @returns Element(Boolean)
        // @description
        // Returns whether a definition exists for the given definition name.
        // -->
        if (atttribute.startsWith("exists")) {
            if (def == null) {
                event.setReplacedObject(CoreUtilities.autoAttrib(new Element(false), atttribute.fulfill(1)));
            }
            else {
                event.setReplacedObject(CoreUtilities.autoAttrib(new Element(true), atttribute.fulfill(1)));
            }
            return;
        }

        // No invalid definitions!
        if (def == null) {
            if (!event.hasAlternative()) {
                dB.echoError("Invalid definition name '" + defName + "'.");
            }
            return;
        }

        event.setReplacedObject(CoreUtilities.autoAttribTyped(def, atttribute));
    }
}


