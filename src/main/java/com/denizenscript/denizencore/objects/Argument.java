package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashSet;

public class Argument implements Cloneable {

    @Override
    public Argument clone() {
        try {
            return (Argument) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public String raw_value;
    public String prefix = null;
    public String lower_prefix = null;
    public String value;
    public String lower_value;

    public ObjectTag object = null;

    public boolean needsFill = false;
    public boolean hasSpecialPrefix = false;

    public ScriptEntry scriptEntry = null;

    public String generateRaw() {
        return prefix == null ? value : prefix + ":" + value;
    }

    public Argument(String prefix, String value) {
        this.prefix = prefix;
        this.value = TagManager.cleanOutputFully(value);
        if (prefix != null) {
            if (prefix.equals("no_prefix")) {
                this.prefix = null;
                raw_value = this.value;
            }
            else {
                raw_value = prefix + ":" + this.value;
                lower_prefix = CoreUtilities.toLowerCase(prefix);
            }
        }
        else {
            raw_value = this.value;
        }
        lower_value = CoreUtilities.toLowerCase(this.value);
        object = new ElementTag(this.value);
    }

    public Argument(String prefix, ObjectTag value) {
        this.prefix = prefix;
        this.value = TagManager.cleanOutputFully(value.toString());
        if (prefix != null) {
            if (prefix.equals("no_prefix")) {
                this.prefix = null;
                raw_value = this.value;
            }
            else {
                raw_value = prefix + ":" + this.value;
                lower_prefix = CoreUtilities.toLowerCase(prefix);
            }
        }
        else {
            raw_value = this.value;
        }
        lower_value = CoreUtilities.toLowerCase(this.value);
        if (value instanceof ElementTag) {
            object = new ElementTag(this.value);
        }
        else {
            object = value;
        }
    }

    public Argument(ObjectTag obj) {
        object = obj;
        if (obj instanceof ElementTag) {
            fillStr(obj.toString());
        }
        else {
            raw_value = TagManager.cleanOutputFully(obj.toString()); // TODO: Avoid for non-elements
            value = raw_value;
            lower_value = CoreUtilities.toLowerCase(value);
        }
    }

    void fillStr(String string) {
        string = TagManager.cleanOutputFully(string);
        raw_value = string;

        int first_colon = string.indexOf(':');
        int first_space = string.indexOf(' ');

        if ((first_space > -1 && first_space < first_colon) || first_colon == -1) {
            value = string;
            if (object == null) {
                object = new ElementTag(value);
            }
        }
        else {
            prefix = string.substring(0, first_colon);
            if (prefix.equals("no_prefix")) {
                prefix = null;
            }
            else {
                lower_prefix = CoreUtilities.toLowerCase(prefix);
            }
            value = string.substring(first_colon + 1);
            object = new ElementTag(value);
        }
        lower_value = CoreUtilities.toLowerCase(value);
    }

    // Construction
    public Argument(String string) {
        fillStr(string);
    }


    public static Argument valueOf(String string) {
        return new Argument(string);
    }


    public boolean startsWith(String string) {
        return lower_value.startsWith(CoreUtilities.toLowerCase(string));
    }


    public boolean hasPrefix() {
        return prefix != null;
    }


    public Argument getPrefix() {
        if (prefix == null) {
            return null;
        }
        return valueOf(prefix);
    }


    // TODO: REMOVE IN 1.0
    public boolean matches(String values) {
        if (!CoreUtilities.contains(values, ',')) {
            return CoreUtilities.toLowerCase(values).equals(lower_value);
        }
        for (String value : CoreUtilities.split(values, ',')) {
            if (CoreUtilities.toLowerCase(value.replace(" ", "")).equals(lower_value)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesOne(String value) {
        return CoreUtilities.toLowerCase(value).equals(lower_value);
    }

    public boolean matches(String... values) {
        for (String value : values) {
            if (CoreUtilities.toLowerCase(value).equals(lower_value)) {
                return true;
            }
        }
        return false;
    }


    public void replaceValue(String string) {
        value = string;
        lower_value = CoreUtilities.toLowerCase(value);
    }


    public String getValue() {
        return value;
    }

    public ListTag getList() {
        if (object instanceof ListTag) {
            return (ListTag) object;
        }
        return ListTag.valueOf(value);
    }

    public static HashSet<String> precalcEnum(Enum<?>[] values) {
        HashSet<String> toRet = new HashSet<>(values.length);
        for (int i = 0; i < values.length; i++) {
            toRet.add(values[i].name().toUpperCase().replace("_", ""));
        }
        return toRet;
    }

    public boolean matchesEnum(HashSet<String> values) {
        String upper = value.replace("_", "").toUpperCase();
        return values.contains(upper);
    }

    public boolean matchesEnumList(HashSet<String> values) {
        ListTag list = getList();
        for (String string : list) {
            String tval = string.replace("_", "").toUpperCase();
            if (values.contains(tval)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesEnum(Enum<?>[] values) {
        String upper = value.replace("_", "").toUpperCase();
        for (Enum<?> value : values) {
            if (value.name().replace("_", "").equals(upper)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesEnumList(Enum<?>[] values) {
        ListTag list = getList();
        for (String string : list) {
            String tval = string.replace("_", "").toUpperCase();
            for (Enum<?> value : values) {
                if (value.name().replace("_", "").equalsIgnoreCase(tval)) {
                    return true;
                }
            }
        }
        return false;
    }


    // TODO: REMOVE IN 1.0
    public boolean matchesPrefix(String values) {
        if (!hasPrefix()) {
            return false;
        }
        if (!CoreUtilities.contains(values, ',')) {
            return CoreUtilities.toLowerCase(values).equals(lower_prefix);
        }
        for (String value : CoreUtilities.split(values, ',')) {
            if (CoreUtilities.toLowerCase(value.trim()).equals(lower_prefix)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesOnePrefix(String value) {
        if (!hasPrefix()) {
            return false;
        }
        return CoreUtilities.toLowerCase(value).equals(lower_prefix);
    }

    public boolean matchesPrefix(String... values) {
        if (!hasPrefix()) {
            return false;
        }
        for (String value : values) {
            if (CoreUtilities.toLowerCase(value).equals(lower_prefix)) {
                return true;
            }
        }
        return false;
    }


    public boolean matchesPrimitive(ArgumentHelper.PrimitiveType argumentType) {
        if (value == null) {
            return false;
        }

        switch (argumentType) {
            case Word:
                return ArgumentHelper.wordPrimitive.matcher(value).matches();

            case Integer:
                return ArgumentHelper.doublePrimitive.matcher(value).matches();

            case Double:
                return ArgumentHelper.doublePrimitive.matcher(value).matches();

            case Float:
                return ArgumentHelper.floatPrimitive.matcher(value).matches();

            case Boolean:
                return ArgumentHelper.booleanPrimitive.matcher(value).matches();

            case Percentage:
                return ArgumentHelper.percentagePrimitive.matcher(value).matches();

            case String:
                return true;
        }

        return false;
    }


    // Check if this argument matches a certain ObjectTag type
    public boolean matchesArgumentType(Class<? extends ObjectTag> dClass) {
        return CoreUtilities.canPossiblyBeType(object, dClass);
    }


    // Check if this argument matches any of multiple ObjectTag types
    public boolean matchesArgumentTypes(Class<? extends ObjectTag>... dClasses) {

        for (Class<? extends ObjectTag> c : dClasses) {
            if (matchesArgumentType(c)) {
                return true;
            }
        }

        return false;
    }


    // Check if this argument matches a ListTag of a certain ObjectTag
    public boolean matchesArgumentList(Class<? extends ObjectTag> dClass) {

        ListTag list = getList();

        return list.isEmpty() || list.containsObjectsFrom(dClass);
    }


    public ElementTag asElement() {
        if (object instanceof ElementTag) {
            return (ElementTag) object;
        }
        return new ElementTag(prefix, value);
    }


    public <T extends ObjectTag> T asType(Class<T> clazz) {
        T arg = CoreUtilities.asType(object, clazz, DenizenCore.getImplementation().getTagContext(scriptEntry));
        if (arg == null) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Cannot process argument '" + object + "' as type '" + clazz.getSimpleName() + "' (conversion returned null).");
        }
        arg.setPrefix(prefix);
        return arg;
    }


    public void reportUnhandled() {
        Debug.echoError('\'' + raw_value + "' is an unknown argument!");
    }


    @Override
    public String toString() {
        return raw_value;
    }
}
