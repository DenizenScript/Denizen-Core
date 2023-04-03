package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;

public interface Property {

    default ObjectTag getPropertyValue() {
        String str = getPropertyString();
        return str == null ? null : new ElementTag(str);
    }

    @Deprecated
    String getPropertyString();

    String getPropertyId();

    @Deprecated
    default ObjectTag getObjectAttribute(Attribute attribute) {
        return null;
    }

    @Deprecated
    default void adjust(Mechanism mechanism) {
    }
}
