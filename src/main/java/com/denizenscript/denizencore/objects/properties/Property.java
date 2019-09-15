package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public interface Property {

    String getPropertyString();

    String getPropertyId();

    default ObjectTag getObjectAttribute(Attribute attribute) {
        String res = getAttribute(attribute);
        return res == null ? null : new ElementTag(res);
    }

    default String getAttribute(Attribute attribute) {
        return CoreUtilities.stringifyNullPass(getObjectAttribute(attribute));
    }

    void adjust(Mechanism mechanism);
}
