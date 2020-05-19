package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

public class DurationTagBase {

    public DurationTagBase() {

        // <--[tag]
        // @attribute <duration[<duration>]>
        // @returns DurationTag
        // @description
        // Returns a duration object constructed from the input value.
        // -->
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                durationTags(event);
            }
        }, "duration");
    }

    public void durationTags(ReplaceableTagEvent event) {

        if (!event.matches("duration") || event.replaced()) {
            return;
        }

        DurationTag duration = null;

        if (event.hasNameContext()) {
            duration = DurationTag.valueOf(event.getNameContext(), event.getAttributes().context);
        }

        if (duration == null) {
            return;
        }

        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(duration, attribute.fulfill(1)));
    }
}
