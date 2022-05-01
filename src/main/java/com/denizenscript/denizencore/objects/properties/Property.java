package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;

public interface Property {

    String getPropertyString();

    String getPropertyId();

    default ObjectTag getObjectAttribute(Attribute attribute) {
        return null;
    }

    void adjust(Mechanism mechanism);
}
