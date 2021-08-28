package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.TagContext;

public class Mechanism {

    private boolean fulfilled;
    private String raw_mechanism;
    public ObjectTag value;
    public ObjectTag adjusting;

    public TagContext context;

    public boolean isProperty = false;

    public Mechanism(String mechanism, ObjectTag value, TagContext context) {
        fulfilled = false;
        raw_mechanism = CoreUtilities.toLowerCase(mechanism);
        this.value = value;
        this.context = context;
    }

    public void fulfill() {
        fulfilled = true;
    }

    public boolean fulfilled() {
        return fulfilled;
    }

    public String getName() {
        return raw_mechanism;
    }

    public ElementTag getValue() {
        if (value == null) {
            return new ElementTag("");
        }
        return new ElementTag(value.toString());
    }

    public <T extends ObjectTag> T valueAsType(Class<T> dClass) {
        return getValue().asType(dClass, context);
    }

    public boolean hasValue() {
        if (value == null) {
            return false;
        }
        if (value instanceof ElementTag && ((ElementTag) value).asString().isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean matches(String string) {
        if (string.equals(raw_mechanism)) {
            fulfill();
            return true;
        }
        return false;
    }

    public String forMechanismText() {
        String text = " <LR>For mechanism '<A>" + raw_mechanism + "<LR>'" + (value == null ? "" : " with value '<A>" + value.toString() + "<LR>'");
        if (adjusting == null) {
            return text;
        }
        return text + ", while " + (isProperty ? "applying properties to" : "adjusting") + " object '<A>" + adjusting.debuggable() + "<LR>'";
    }

    public boolean requireBoolean() {
        return requireBoolean("Invalid boolean. Must specify TRUE or FALSE.");
    }

    public boolean requireDouble() {
        return requireDouble("Invalid decimal number specified.");
    }

    public boolean requireEnum(boolean allowInt, Enum<?>... values) {
        return requireEnum(null, allowInt, values);
    }

    public boolean requireFloat() {
        return requireFloat("Invalid decimal number specified.");
    }

    public boolean requireInteger() {
        return requireInteger("Invalid integer number specified.");
    }

    public <T extends ObjectTag> boolean requireObject(Class<T> type) {
        return requireObject(null, type);
    }

    public boolean requireBoolean(String error) {
        if (hasValue() && getValue().isBoolean()) {
            return true;
        }
        echoError(error);
        return false;
    }

    public boolean requireDouble(String error) {
        if (hasValue() && getValue().isDouble()) {
            return true;
        }
        echoError(error);
        return false;
    }

    public boolean requireEnum(String error, boolean allowInt, Enum<?>... values) {
        if (!hasValue()) {
            return false;
        }
        ElementTag value = getValue();
        if (hasValue() && allowInt && value.isInt() && value.asInt() < values.length) {
            return true;
        }
        if (hasValue() && value.isString()) {
            String raw_value = value.asString().toUpperCase();
            for (Enum<?> check_value : values) {
                if (raw_value.equals(check_value.name())) {
                    return true;
                }
            }
        }
        if (error == null) {
            echoError("Invalid " + values[0].getDeclaringClass().getSimpleName() + "."
                    + " Must specify a valid name" + (allowInt ? " or number" : "") + ".");
        }
        else {
            echoError(error);
        }
        return false;
    }

    public boolean requireFloat(String error) {
        if (hasValue() && getValue().isFloat()) {
            return true;
        }
        echoError(error);
        return false;
    }

    public boolean requireInteger(String error) {
        if (hasValue() && getValue().isInt()) {
            return true;
        }
        echoError(error);
        return false;
    }

    public <T extends ObjectTag> boolean requireObject(String error, Class<T> type) {
        if (hasValue() && CoreUtilities.canPossiblyBeType(value, type)) {
            return true;
        }
        if (error == null) {
            // TODO: Remove getSimpleName(), or simplify somehow.
            echoError("Invalid " + type.getSimpleName() + " specified.");
        }
        else {
            echoError(error);
        }
        return false;
    }

    public boolean shouldDebug() {
        return context == null || context.debug;
    }

    public void echoError(String error) {
        // TODO: Consider special cases of whether object properties with debug off should even show errors
        Debug.echoError(context, forMechanismText(), error);
    }

    public void reportInvalid() {
        echoError("Invalid mechanism specified: " + raw_mechanism);
    }

    public void autoReport() {
        if (!fulfilled()) {
            reportInvalid();
        }
    }
}
