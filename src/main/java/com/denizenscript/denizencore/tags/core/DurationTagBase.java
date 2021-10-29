package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.tags.TagManager;

public class DurationTagBase {

    public DurationTagBase() {

        // <--[tag]
        // @attribute <duration[<duration>]>
        // @returns DurationTag
        // @description
        // Returns a duration object constructed from the input value.
        // Refer to <@link ObjectType DurationTag>.
        // -->
        TagManager.registerTagHandler(DurationTag.class, "duration", (attribute) -> { // non-static because there is a randomized constructor option
            if (!attribute.hasParam()) {
                attribute.echoError("Duration tag base must have input.");
                return null;
            }
            return DurationTag.valueOf(attribute.getParam(), attribute.context);
        });
    }
}
