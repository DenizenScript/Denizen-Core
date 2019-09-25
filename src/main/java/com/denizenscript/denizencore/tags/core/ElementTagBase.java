package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.tags.TagManager;

public class ElementTagBase {

    public ElementTagBase() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                elementTags(event);
            }
        }, "element");
    }

    public void elementTags(ReplaceableTagEvent event) {

        if (!event.matches("element") || event.replaced()) {
            return;
        }

        if (!event.hasNameContext()) {
            return;
        }

        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(event.getNameContext()), attribute.fulfill(1)));

    }
}
