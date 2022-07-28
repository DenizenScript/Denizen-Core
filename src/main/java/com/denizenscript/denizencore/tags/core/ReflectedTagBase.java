package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.JavaReflectedObjectTag;
import com.denizenscript.denizencore.tags.TagManager;

public class ReflectedTagBase {

    public ReflectedTagBase() {

        // <--[tag]
        // @attribute <reflected[<reflected-tag>]>
        // @returns JavaReflectedObjectTag
        // @description
        // Returns a JavaReflectedObjectTag constructed from the input reference ID lookup.
        // Refer to <@link objecttype JavaReflectedObjectTag>.
        // -->
        TagManager.registerStaticTagBaseHandler(JavaReflectedObjectTag.class, "reflected", (attribute) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("Reflected tag base must have input.");
                return null;
            }
            return JavaReflectedObjectTag.valueOf(attribute.getParam(), attribute.context);
        });
    }
}
