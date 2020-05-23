package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.TimeTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class TimeTagBase {

    public TimeTagBase() {

        // <--[tag]
        // @attribute <time[<time>]>
        // @returns TimeTag
        // @description
        // Returns a time object constructed from the input value.
        // -->
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                timeTags(event);
            }
        }, "time");
    }

    public void timeTags(ReplaceableTagEvent event) {

        if (!event.matches("time") || event.replaced()) {
            return;
        }

        TimeTag time = null;

        if (event.hasNameContext()) {
            time = TimeTag.valueOf(event.getNameContext(), event.getAttributes().context);
        }

        if (time == null) {
            return;
        }

        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(time, attribute.fulfill(1)));
    }
}
