package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

public abstract class ObjectProperty<TObj extends ObjectTag, TData extends ObjectTag> implements Property {

    public TObj object;

    public abstract TData getPropertyValue();

    public TData getTagValue() {
        return getPropertyValue();
    }

    public boolean isDefaultValue(TData data) {
        return false;
    }

    @Override
    public TData getPropertyValueNoDefault() {
        TData res = getPropertyValue();
        return res == null || isDefaultValue(res) ? null : getPropertyValue();
    }

    @Override
    public String getPropertySavableValue() {
        TData res = getPropertyValue();
        return res == null || isDefaultValue(res) ? null : getPropertyValue().savable();
    }

    @Deprecated @Override
    public String getPropertyString() {
        TData res = getPropertyValue();
        return res == null || isDefaultValue(res) ? null : getPropertyValue().identify();
    }

    public abstract void setPropertyValue(TData data, Mechanism mechanism);

    public static <TObj extends ObjectTag, TData extends ObjectTag, TProp extends ObjectProperty<TObj, TData>>
        void autoRegister(String name, Class<TProp> propClass, Class<TData> dataClass, boolean isStatic, String... deprecatedVariants) {
        PropertyParser.registerTagInternal(propClass, dataClass, name, (attribute, prop) -> prop.getTagValue(), deprecatedVariants, isStatic);
        PropertyParser.registerMechanism(propClass, dataClass, name, (prop, mechanism, param) -> prop.setPropertyValue(param, mechanism), deprecatedVariants);
    }

    public static <TObj extends ObjectTag, TData extends ObjectTag, TProp extends ObjectProperty<TObj, TData>>
        void autoRegisterNullable(String name, Class<TProp> propClass, Class<TData> dataClass, boolean isStatic, String... deprecatedVariants) {
        PropertyParser.registerTagInternal(propClass, dataClass, name, (attribute, prop) -> prop.getTagValue(), deprecatedVariants, isStatic);
        PropertyParser.registerMechanism(propClass, name, (prop, mechanism) -> {
            if (!mechanism.hasValue()) {
                prop.setPropertyValue(null, mechanism);
                return;
            }
            TData param = mechanism.value.asType(dataClass, mechanism.context);
            if (param == null) {
                mechanism.echoError("Invalid " + DebugInternals.getClassNameOpti(dataClass) + " specified.");
                return;
            }
            prop.setPropertyValue(param, mechanism);
        }, deprecatedVariants);
    }
}
