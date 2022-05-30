package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.SecretTag;
import com.denizenscript.denizencore.tags.TagManager;

public class SecretTagBase {

    public SecretTagBase() {

        // <--[tag]
        // @attribute <secret[<secret>]>
        // @returns SecretTag
        // @description
        // Returns a SecretTag object constructed from the input value.
        // Refer to <@link ObjectType SecretTag>.
        // @Example
        // - webget <secret[my_secret_url]> "post:Message to secret address!"
        // -->
        TagManager.registerStaticTagBaseHandler(SecretTag.class, "secret", (attribute) -> {
            return SecretTag.valueOf(attribute.getParam(), attribute.context);
        });
    }
}
