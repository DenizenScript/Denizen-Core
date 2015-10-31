package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.ObjectFetcher;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class DefinitionTags {

    public DefinitionTags() {
        TagManager.registerTagEvents(this);
    }


    //////////
    //  ReplaceableTagEvent handler
    ////////

    @TagManager.TagEvents
    public void definitionTag(ReplaceableTagEvent event) {

        if (!event.matches("definition", "def", "d")) return;

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
        if (event.getScriptEntry() == null) {
            dB.echoError("No definitions available outside of a queue.");
            return;
        }
        String def = event.getContext().definitionProvider.getDefinition(defName);

        Attribute atttribute = event.getAttributes().fulfill(1);

        // <--[tag]
        // @attribute <definition[<name>].exists>
        // @returns Element(Boolean)
        // @description
        // Returns whether a definition exists for the given definition name.
        // -->
        if (atttribute.startsWith("exists")) {
            if (def == null)
                event.setReplaced(Element.FALSE.getAttribute(atttribute.fulfill(1)));
            else
                event.setReplaced(Element.TRUE.getAttribute(atttribute.fulfill(1)));
            return;
        }

        // No invalid definitions!
        if (def == null) {
            if (!event.hasAlternative())
                dB.echoError("Invalid definition name '" + defName + "'.");
            return;
        }


        event.setReplaced(ObjectFetcher.pickObjectFor(def)
                .getAttribute(atttribute));
    }
}


