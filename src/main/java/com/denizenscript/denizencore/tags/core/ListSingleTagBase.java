package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class ListSingleTagBase {

    public ListSingleTagBase() {

        // <--[tag]
        // @attribute <list_single[<object>]>
        // @returns ListTag
        // @description
        // Returns a ListTag object with exactly 1 entry: whatever the input value is (even if that input is a list).
        // This is primarily useful for creating lists-within-lists.
        // -->
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                listTags(event);
            }
        }, "list_single");
    }

    public void listTags(ReplaceableTagEvent event) {

        if (!event.matches("list_single") || event.replaced()) {
            return;
        }

        if (!event.hasNameContext()) {
            return;
        }

        ListTag list = new ListTag();
        list.addObject(event.getAttributes().getContextObject(1));

        // Build and fill attributes
        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(list, attribute.fulfill(1)));
    }
}
