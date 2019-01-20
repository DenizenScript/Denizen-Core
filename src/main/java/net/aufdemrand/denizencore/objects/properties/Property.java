package net.aufdemrand.denizencore.objects.properties;

import net.aufdemrand.denizencore.objects.Mechanism;
import net.aufdemrand.denizencore.objects.ObjectFetcher;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.tags.Attribute;

public interface Property {

    String getPropertyString();

    String getPropertyId();

    default dObject getObjectAttribute(Attribute attribute) {
        return ObjectFetcher.pickObjectFor(getAttribute(attribute), attribute.context);
    }

    String getAttribute(Attribute attribute);

    void adjust(Mechanism mechanism);
}
