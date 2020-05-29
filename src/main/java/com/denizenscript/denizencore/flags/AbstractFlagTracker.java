package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.TimeTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.SlowWarning;
import com.denizenscript.denizencore.utilities.debugging.Warning;

import java.util.Collection;

public abstract class AbstractFlagTracker {

    public abstract ObjectTag getFlagValue(String key);

    public abstract TimeTag getFlagExpirationTime(String key);

    public abstract Collection<String> listAllFlags();

    public abstract void setFlag(String key, ObjectTag value, TimeTag expiration);

    public boolean hasFlag(String key) {
        return getFlagValue(key) != null;
    }

    public ElementTag doHasFlagTag(Attribute attribute) {
        if (!attribute.hasContext(1)) {
            attribute.echoError("The has_flag[...] tag must have an input!");
            return null;
        }
        return new ElementTag(hasFlag(attribute.getContext(1)));
    }

    public ObjectTag doFlagTag(Attribute attribute) {
        if (!attribute.hasContext(1)) {
            attribute.echoError("The flag[...] tag must have an input!");
            return null;
        }
        if (attribute.getAttributeWithoutContext(2).equals("is_expired")) {
            Deprecations.flagIsExpiredTag.warn(attribute.context);
            return new ElementTag(!hasFlag(attribute.getContext(1)));
        }
        else if (attribute.getAttributeWithoutContext(2).equals("expiration")) {
            Deprecations.flagExpirationTag.warn(attribute.context);
            return getFlagExpirationTime(attribute.getContext(1));
        }
        return getFlagValue(attribute.getContext(1));
    }

    public TimeTag doFlagExpirationTag(Attribute attribute) {
        if (!attribute.hasContext(1)) {
            attribute.echoError("The flag_expiration[...] tag must have an input!");
            return null;
        }
        return getFlagExpirationTime(attribute.getContext(1));
    }

    public static Warning listFlagsTagWarning = new SlowWarning("The list_flags tag is meant for testing/debugging only. Do not use it in scripts (ignore this warning if using for testing reasons).");

    public ListTag doListFlagsTag(Attribute attribute) {
        listFlagsTagWarning.warn(attribute.context);
        ListTag list = new ListTag();
        list.addAll(listAllFlags());
        return list;
    }
}
