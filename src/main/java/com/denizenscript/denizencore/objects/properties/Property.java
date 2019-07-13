package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;

public interface Property {

    String getPropertyString();

    String getPropertyId();

    default ObjectTag getObjectAttribute(Attribute attribute) {
        String res = getAttribute(attribute);
        return res == null ? null : new ElementTag(res);
    }

    String getAttribute(Attribute attribute);

    void adjust(Mechanism mechanism);
}
