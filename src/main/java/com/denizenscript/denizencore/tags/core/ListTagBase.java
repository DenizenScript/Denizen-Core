package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

public class ListTagBase {

    public ListTagBase() {

        // <--[tag]
        // @attribute <list[(<list>)]>
        // @returns ListTag
        // @description
        // Returns a list object constructed from the input value.
        // Give no input to create an empty list.
        // -->
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                listTags(event);
            }
        }, "list");
    }

    public void listTags(ReplaceableTagEvent event) {
        if (!event.matches("list") || event.replaced()) {
            return;
        }
        ListTag list;
        if (event.hasNameContext()) {
            list = ListTag.valueOf(event.getNameContext(), event.getAttributes().context);
        }
        else {
            list = new ListTag();
        }
        if (list == null) {
            return;
        }
        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(list, attribute.fulfill(1)));
    }
}
