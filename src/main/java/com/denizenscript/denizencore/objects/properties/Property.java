package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.Element;
import com.denizenscript.denizencore.objects.dObject;
import com.denizenscript.denizencore.tags.Attribute;

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
