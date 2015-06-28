package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;


/**
 * Location tag is a starting point for getting attributes for a
 */

public class ListTags {

    public ListTags() {
        TagManager.registerTagEvents(this);
    }

    @TagManager.TagEvents
    public void listTags(ReplaceableTagEvent event) {

        if (!event.matches("list") || event.replaced()) return;

        dList list = null;

        if (event.hasNameContext())
            list = dList.valueOf(event.getNameContext());

        // Check if list is null, return null if it is
        if (list == null) {
            return;
        }

        // Build and fill attributes
        Attribute attribute = event.getAttributes();
        event.setReplaced(list.getAttribute(attribute.fulfill(1)));

    }
}
