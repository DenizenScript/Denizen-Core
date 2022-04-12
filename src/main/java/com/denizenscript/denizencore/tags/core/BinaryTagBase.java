package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.BinaryTag;
import com.denizenscript.denizencore.tags.TagManager;

public class BinaryTagBase {

    public BinaryTagBase() {

        // <--[tag]
        // @attribute <binary[<binary>]>
        // @returns ElementTag
        // @description
        // Returns a BinaryTag constructed from the input binary data in hexadecimal format.
        // Refer to <@link objecttype BinaryTag>.
        // -->
        TagManager.registerStaticTagBaseHandler(BinaryTag.class, "binary", (attribute) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("Binary tag base must have input.");
                return null;
            }
            return BinaryTag.valueOf(attribute.getParam(), attribute.context);
        });
    }
}
