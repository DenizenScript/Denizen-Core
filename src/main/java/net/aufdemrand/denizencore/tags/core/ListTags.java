package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.TagRunnable;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;


/**
 * Location tag is a starting point for getting attributes for a
 */

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
            list = dList.valueOf(event.getNameContext());
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
