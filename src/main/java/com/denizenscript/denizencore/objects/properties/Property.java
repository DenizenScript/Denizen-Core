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

    default ObjectTag getPropertyValueNoDefault() {
        return getPropertyValue();
    }

    @Deprecated // TODO: This is for legacy compatibility, can be removed when getPropertyString is
    default String getPropertySavableValue() {
        return getPropertyString();
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
