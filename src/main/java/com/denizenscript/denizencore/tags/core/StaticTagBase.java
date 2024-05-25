package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagManager;

public class StaticTagBase {

    public StaticTagBase() {

        // <--[tag]
        // @attribute <static[<tagged-value>]>
        // @returns ObjectTag
        // @description
        // Forces the tag inside to static-parse.
        // That is, any value, no matter how dynamic, will parse only exactly once at reload time.
        // This is a special internal behavior of Denizen that usually should not be used unless you have a very specific reason.
        // Whatever tag is inside should be globally-unique to the use case, as the raw text of the tag itself is used for a cache lookup.
        // @example
        // # This example will narrate the same number every time it's ran, until "/ex reload" is used.
        // - narrate <static[<util.random_decimal>]>
        // -->
        TagManager.registerStaticTagBaseHandler(ObjectTag.class, "static", (attribute) -> {
            boolean isStatic = TagManager.isStaticParsing;
            TagManager.isStaticParsing = false;
            try {
                return attribute.getParamObject();
            }
            finally {
                TagManager.isStaticParsing = isStatic;
            }
        });
        TagManager.baseTags.get("static").doesStaticOverride = true;
    }
}
