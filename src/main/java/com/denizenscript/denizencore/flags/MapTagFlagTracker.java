package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;
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
        ObjectTag subObj = map.getObject(key);
        if (subObj == null) {
            return null;
        }
        if (subObj instanceof MapTag) {
            return (MapTag) subObj;
        }
        MapTag toReturn = new MapTag();
        toReturn.map.put(valueString, subObj);
        return toReturn;
    }

    @Override
    public void setRootMap(String key, MapTag value) {
        if (value == null) {
            map.map.remove(new StringHolder(key));
        }
        else {
            ObjectTag subValue = value.map.get(valueString);
            if (value.map.containsKey(expirationString) || subValue instanceof MapTag) {
                map.putObject(key, value);
            }
            else {
                map.putObject(key, subValue);
            }
        }
    }
}
