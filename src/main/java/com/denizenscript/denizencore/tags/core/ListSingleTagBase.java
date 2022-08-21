package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.TagManager;

public class ListSingleTagBase {

    public ListSingleTagBase() {

        // <--[tag]
        // @attribute <list_single[<object>]>
        // @returns ListTag
        // @description
        // Returns a ListTag object with exactly 1 entry: whatever the input value is (even if that input is a list).
        // This is primarily useful for creating lists-within-lists.
        // -->
        TagManager.registerStaticTagBaseHandler(ListTag.class, ObjectTag.class, "list_single", (attribute, param) -> {
            ListTag list = new ListTag();
            list.addObject(param);
            return list;
        });
    }
}
