package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.TimeTag;

import java.util.ArrayList;
import java.util.Collection;

public class RedirectionFlagTracker extends AbstractFlagTracker {

    public RedirectionFlagTracker(AbstractFlagTracker original, String prefix) {
        this.original = original;
        this.prefix = prefix;
    }

    public AbstractFlagTracker original;

    public String prefix;

    @Override
    public ObjectTag getFlagValue(String key) {
        return original.getFlagValue(prefix + "." + key);
    }

    @Override
    public TimeTag getFlagExpirationTime(String key) {
        return original.getFlagExpirationTime(prefix + "." + key);
    }

    @Override
    public Collection<String> listAllFlags() {
        MapTag map = getFlagMap();
        if (map == null) {
            return new ArrayList<>();
        }
        return map.keys();
    }

    @Override
    public void setFlag(String key, ObjectTag value, TimeTag expiration) {
        original.setFlag(prefix + "." + key, value, expiration);
    }

    @Override
    public MapTag getFlagMap() {
        ObjectTag obj = original.getFlagValue(prefix);
        if (!(obj instanceof MapTag)) {
            return null;
        }
        return (MapTag) obj;
    }
}
