package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.*;

public class MapTagFlagTracker extends MapTagBasedFlagTracker {

    public MapTag map;

    public MapTagFlagTracker() {
        this.map = new MapTag();
    }

    public MapTagFlagTracker(MapTag map) {
        this.map = map;
        doClean(map);
    }

    public MapTagFlagTracker(String mapTagValue, TagContext context) {
        this(MapTag.valueOf(mapTagValue, context));
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public Collection<String> listAllFlags() {
        ArrayList<String> keys = new ArrayList<>(map.map.size());
        for (StringHolder string : map.map.keySet()) {
            keys.add(string.str);
        }
        return keys;
    }

    @Override
    public MapTag getRootMap(String key) {
        return (MapTag) map.getObject(key);
    }

    @Override
    public void setRootMap(String key, MapTag value) {
        if (value == null) {
            map.map.remove(new StringHolder(key));
        }
        else {
            map.putObject(key, value);
        }
    }
}
