package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.TagRunnable;
import com.denizenscript.denizencore.objects.dList;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

public class ListTags {

    public ListTags() {
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

        dList list = null;

        if (event.hasNameContext()) {
            list = dList.valueOf(event.getNameContext(), event.getAttributes().context);
        }

        // Check if list is null, return null if it is
        if (list == null) {
            return;
        }

        // Build and fill attributes
        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(list, attribute.fulfill(1)));

    }
}
