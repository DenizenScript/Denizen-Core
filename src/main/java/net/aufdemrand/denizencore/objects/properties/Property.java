package net.aufdemrand.denizencore.objects.properties;

import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.Mechanism;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.tags.Attribute;

public interface Property {

    String getPropertyString();

    String getPropertyId();

    default dObject getObjectAttribute(Attribute attribute) {
        String res = getAttribute(attribute);
        return res == null ? null : new Element(res);
    }

    String getAttribute(Attribute attribute);

    void adjust(Mechanism mechanism);
}
