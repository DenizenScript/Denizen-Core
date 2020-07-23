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
        // Refer to <@link language DurationTag objects>.
        // -->
        TagManager.registerTagHandler("duration", (attribute) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("Duration tag base must have input.");
                return null;
            }
            return DurationTag.valueOf(attribute.getContext(1), attribute.context);
        });
    }
}
