package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.TagRunnable;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;

public class ElementTags {

    public ElementTags() {
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
        event.setReplacedObject(CoreUtilities.autoAttrib(new Element(event.getNameContext()), attribute.fulfill(1)));

    }
}
