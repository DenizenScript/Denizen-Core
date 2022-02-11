package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.TimeTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RedirectionFlagTracker extends AbstractFlagTracker {

    public RedirectionFlagTracker(AbstractFlagTracker original, String prefix) {
        this.original = original;
        this.prefix = prefix;
    }

    public AbstractFlagTracker original;

    public String prefix;

    @Override
    public MapTag getRootMap(String key) {
        List<String> parts = CoreUtilities.split(prefix, '.');
        parts.add(key);
        MapTag target = original.getRootMap(parts.get(0));
        if (target == null) {
            return null;
        }
        for (int i = 1; i < parts.size(); i++) {
            target = (MapTag) target.map.get(MapTagBasedFlagTracker.valueString);
            if (target == null) {
                return null;
            }
            target = (MapTag) target.getObject(parts.get(i));
            if (target == null) {
                return null;
            }
        }
        return target;
    }

    @Override
    public void setRootMap(String key, MapTag map) {
        original.setFlag(prefix + "." + key, map, null, false);
    }

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
    public void setFlag(String key, ObjectTag value, TimeTag expiration, boolean doFlaggify) {
        original.setFlag(prefix + "." + key, value, expiration, doFlaggify);
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
