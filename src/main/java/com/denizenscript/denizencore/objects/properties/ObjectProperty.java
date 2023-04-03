package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;

public abstract class ObjectProperty<TObj extends ObjectTag, TData extends ObjectTag> implements Property {

    public TObj object;

    public abstract TData getPropertyValue();

    public boolean isDefaultValue(TData data) {
        return false;
    }

    @Deprecated @Override
    public String getPropertyString() {
        TData res = getPropertyValue();
        return res == null || isDefaultValue(res) ? null : getPropertyValue().identify();
    }

    public abstract void setPropertyValue(TData data, Mechanism mechanism);

    public static <TObj extends ObjectTag, TData extends ObjectTag, TProp extends ObjectProperty<TObj, TData>>
        void autoRegister(String name, Class<TProp> propClass, Class<TData> dataClass, boolean isStatic, String... deprecatedVariants) {
        PropertyParser.registerTagInternal(propClass, dataClass, name, (attribute, prop) -> prop.getPropertyValue(), deprecatedVariants, isStatic);
        PropertyParser.registerMechanism(propClass, dataClass, name, (prop, mechanism, param) -> prop.setPropertyValue(param, mechanism), deprecatedVariants);
    }
}
