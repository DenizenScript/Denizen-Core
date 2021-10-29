package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.TimeTag;
import com.denizenscript.denizencore.tags.TagManager;

public class TimeTagBase {

    public TimeTagBase() {

        // <--[tag]
        // @attribute <time[<time>]>
        // @returns TimeTag
        // @description
        // Returns a time object constructed from the input value.
        // Refer to <@link ObjectType TimeTag>.
        // -->
        TagManager.registerStaticTagBaseHandler(TimeTag.class, "time", (attribute) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("Time tag base must have input.");
                return null;
            }
            return TimeTag.valueOf(attribute.getParam(), attribute.context);
        });
    }
}
