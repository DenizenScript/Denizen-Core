package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
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

    public static <T extends FlaggableObject> void registerFlagHandlers(ObjectTagProcessor<T> processor) {

        // <--[tag]
        // @attribute <FlaggableObject.flag[<flag_name>]>
        // @returns ObjectTag
        // @description
        // Returns the specified flag from the flaggable object.
        // If the flag is expired, will return null.
        // Consider also using <@link tag FlaggableObject.has_flag>.
        // See <@link language flag system>.
        // -->
        processor.registerTag("flag", (attribute, object) -> {
            AbstractFlagTracker tracker = object.getFlagTracker();
            if (tracker == null) {
                attribute.echoError("Cannot read flag tag for '" + object + "': flag tracker is invalid or unavailable.");
                return null;
            }
            return tracker.doFlagTag(attribute);
        });

        // <--[tag]
        // @attribute <FlaggableObject.has_flag[<flag_name>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the flaggable object has the specified flag, otherwise returns false.
        // See <@link language flag system>.
        // -->
        processor.registerTag("has_flag", (attribute, object) -> {
            AbstractFlagTracker tracker = object.getFlagTracker();
            if (tracker == null) {
                attribute.echoError("Cannot read has_flag tag for '" + object + "': flag tracker is invalid or unavailable.");
                return null;
            }
            return tracker.doHasFlagTag(attribute);
        });

        // <--[tag]
        // @attribute <FlaggableObject.flag_expiration[<flag_name>]>
        // @returns TimeTag
        // @description
        // Returns a TimeTag indicating when the specified flag will expire.
        // See <@link language flag system>.
        // -->
        processor.registerTag("flag_expiration", (attribute, object) -> {
            AbstractFlagTracker tracker = object.getFlagTracker();
            if (tracker == null) {
                attribute.echoError("Cannot read flag_expiration tag for '" + object + "': flag tracker is invalid or unavailable.");
                return null;
            }
            return tracker.doFlagExpirationTag(attribute);
        });

        // <--[tag]
        // @attribute <FlaggableObject.list_flags>
        // @returns ListTag
        // @description
        // Returns a list of the flaggable object's flags.
        // Note that this is exclusively for debug/testing reasons, and should never be used in a real script.
        // See <@link language flag system>.
        // -->
        processor.registerTag("list_flags", (attribute, object) -> {
            AbstractFlagTracker tracker = object.getFlagTracker();
            if (tracker == null) {
                attribute.echoError("Cannot read list_flags tag for '" + object + "': flag tracker is invalid or unavailable.");
                return null;
            }
            return tracker.doListFlagsTag(attribute);
        });

        // <--[tag]
        // @attribute <FlaggableObject.flag_map>
        // @returns MapTag
        // @description
        // Returns a raw map of the objects internal flag data.
        // Note that this is exclusively for debug/testing reasons, and should never be used in a real script.
        // See <@link language flag system>.
        // -->
        processor.registerTag("flag_map", (attribute, object) -> {
            AbstractFlagTracker tracker = object.getFlagTracker();
            if (tracker == null) {
                attribute.echoError("Cannot read flag_map tag for '" + object + "': flag tracker is invalid or unavailable.");
                return null;
            }
            return tracker.doFlagMapTag(attribute);
        });
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
            boolean result = !hasFlag(attribute.getContext(1));
            attribute.fulfill(1);
            return new ElementTag(result);
        }
        else if (attribute.getAttributeWithoutContext(2).equals("expiration")) {
            Deprecations.flagExpirationTag.warn(attribute.context);
            TimeTag time = getFlagExpirationTime(attribute.getContext(1));
            if (time == null) {
                return null;
            }
            attribute.fulfill(1);
            return new DurationTag((time.millis() - TimeTag.now().millis()) / 1000.0);
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

    public static Warning listFlagsTagWarning = new SlowWarning("The list_flags and flag_map tags are meant for testing/debugging only. Do not use it in scripts (ignore this warning if using for testing reasons).");

    public ListTag doListFlagsTag(Attribute attribute) {
        listFlagsTagWarning.warn(attribute.context);
        ListTag list = new ListTag();
        list.addAll(listAllFlags());
        return list;
    }

    public MapTag doFlagMapTag(Attribute attribute) {
        listFlagsTagWarning.warn(attribute.context);
        if (this instanceof MapTagFlagTracker) {
            return ((MapTagFlagTracker) this).map;
        }
        else {
            MapTag result = new MapTag();
            for (String key : listAllFlags()) {
                result.putObject(key, getFlagValue(key));
            }
            return result;
        }
    }
}
