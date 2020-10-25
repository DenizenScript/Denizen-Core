package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
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

    private String raw_value;
    public String prefix = null;
    public String lower_prefix = null;
    private String value;
    public String lower_value;

    public ObjectTag object = null;

    public boolean needsFill = false;
    public boolean hasSpecialPrefix = false;

    public ScriptEntry scriptEntry = null;

    public boolean canBeElement = true;

    public void unsetValue() {
        raw_value = null;
    }

    public String getRawValue() {
        requireValue();
        return raw_value;
    }

    public void requireValue() {
        if (raw_value == null && object != null) {
            value = object.toString();
            lower_value = CoreUtilities.toLowerCase(value);
            raw_value = prefix == null ? value : prefix + ":" + value;
        }
    }

    public Argument(String prefix, String value) {
        this.prefix = prefix;
        this.value = value;
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

    public static AsciiMatcher prefixCharsAllowed = new AsciiMatcher("ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "_");

    public void fillStr(String string) {
        raw_value = string;
        int first_colon = string.indexOf(':');
        int first_not_prefix = prefixCharsAllowed.indexOfFirstNonMatch(string);
        if ((first_not_prefix > -1 && first_not_prefix < first_colon) || first_colon == -1) {
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
        if (!canBeElement && !CoreUtilities.contains(string, '@')) {
            return false;
        }
        requireValue();
        return lower_value.startsWith(string);
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

    public boolean matches(String value) {
        if (!canBeElement) {
            return false;
        }
        requireValue();
        return value.equals(lower_value);
    }

    public boolean matches(String... values) {
        if (!canBeElement) {
            return false;
        }
        requireValue();
        for (String value : values) {
            if (value.equals(lower_value)) {
                return true;
            }
        }
        return false;
    }

    public String getValue() {
        requireValue();
        return value;
    }

    public ListTag getList(TagContext context) {
        if (object instanceof ListTag) {
            return (ListTag) object;
        }
        if (object instanceof ElementTag) {
            if (context == null && scriptEntry != null) {
                context = scriptEntry.getContext();
            }
            requireValue();
            return ListTag.valueOf(value, context);
        }
        ListTag result = new ListTag();
        result.addObject(object);
        return result;
    }

    public static HashSet<String> precalcEnum(Enum<?>[] values) {
        HashSet<String> toRet = new HashSet<>(values.length);
        for (int i = 0; i < values.length; i++) {
            toRet.add(values[i].name().toUpperCase().replace("_", ""));
        }
        return toRet;
    }

    public boolean matchesEnum(HashSet<String> values) {
        if (!canBeElement) {
            return false;
        }
        requireValue();
        String upper = value.replace("_", "").toUpperCase();
        return values.contains(upper);
    }

    public boolean matchesEnum(Enum<?>[] values) {
        if (!canBeElement) {
            return false;
        }
        requireValue();
        String upper = value.replace("_", "").toUpperCase();
        for (Enum<?> value : values) {
            if (value.name().replace("_", "").equals(upper)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesEnumList(Enum<?>[] values) {
        ListTag list = getList(CoreUtilities.noDebugContext);
        for (String string : list) {
            String tval = string.replace("_", "");
            for (Enum<?> value : values) {
                if (CoreUtilities.equalsIgnoreCase(value.name().replace("_", ""), tval)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean matchesPrefix(String value) {
        if (!hasPrefix()) {
            return false;
        }
        return value.equals(lower_prefix);
    }

    public boolean matchesPrefix(String... values) {
        if (!hasPrefix()) {
            return false;
        }
        for (String value : values) {
            if (value.equals(lower_prefix)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesBoolean() {
        if (!canBeElement) {
            return false;
        }
        requireValue();
        return lower_value.equals("true") || lower_value.equals("false");
    }

    public boolean matchesInteger() {
        return matchesFloat();
    }

    public boolean matchesFloat() {
        if (!canBeElement) {
            return false;
        }
        requireValue();
        return ArgumentHelper.matchesDouble(lower_value);
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
        ListTag list = getList(CoreUtilities.noDebugContext);
        return list.isEmpty() || list.containsObjectsFrom(dClass);
    }

    public ElementTag asElement() {
        if (object instanceof ElementTag) {
            return (ElementTag) object;
        }
        requireValue();
        return new ElementTag(prefix, value);
    }

    public <T extends ObjectTag> T asType(Class<T> clazz) {
        T arg = CoreUtilities.asType(object, clazz, DenizenCore.getImplementation().getTagContext(scriptEntry));
        if (arg == null) {
            Debug.echoError("Cannot process argument '" + object + "' as type '" + clazz.getSimpleName() + "' (conversion returned null).");
            return null;
        }
        arg.setPrefix(prefix);
        return arg;
    }

    public void reportUnhandled() {
        if (TagManager.recentTagError) {
            Debug.echoError('\'' + getRawValue() + "' is an unknown argument! This was probably caused by a tag not parsing properly.");
            return;
        }
        if (prefix != null) {
            Debug.echoError('\'' + getRawValue() + "' is an unknown argument! Did you mess up the command syntax?");
        }
        else {
            Debug.echoError('\'' + getRawValue() + "' is an unknown argument! Did you forget quotes, or did you mess up the command syntax?");
        }
        if (scriptEntry != null && scriptEntry.getCommand() != null) {
            Debug.log("Command usage: " + scriptEntry.getCommand().getUsageHint());
        }
    }

    @Override
    public String toString() {
        return getRawValue();
    }
}
