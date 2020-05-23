package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.DurationTag;

import java.util.Collection;

public abstract class AbstractFlagTracker {

    public abstract ObjectTag getFlagValue(String key);

    public abstract DurationTag getFlagExpirationTime(String key);

    public abstract Collection<String> listAllFlags();

    public abstract void setFlag(String key, ObjectTag value, DurationTag expiration);

    public boolean hasFlag(String key) {
        return getFlagValue(key) != null;
    }
}
