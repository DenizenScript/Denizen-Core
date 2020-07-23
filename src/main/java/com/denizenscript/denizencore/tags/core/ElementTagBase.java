package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagManager;

public class ElementTagBase {

    public ElementTagBase() {

        // <--[tag]
        // @attribute <element[<element>]>
        // @returns ElementTag
        // @description
        // Returns an element constructed from the input value.
        // Refer to <@link language ElementTag objects>.
        // -->
        TagManager.registerTagHandler("element", (attribute) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("Element tag base must have input.");
                return null;
            }
            return new ElementTag(attribute.getContext(1));
        });
    }
}
