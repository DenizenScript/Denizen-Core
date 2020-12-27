package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.TimeTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class MapTagBasedFlagTracker extends AbstractFlagTracker {

    public static StringHolder valueString = new StringHolder("__value");

    public static StringHolder expirationString = new StringHolder("__expiration");

    public static boolean skipAllCleanings = false;

    public abstract MapTag getRootMap(String key);

    public abstract void setRootMap(String key, MapTag map);

    public static boolean isExpired(ObjectTag expirationObj) {
        if (expirationObj == null) {
            return false;
        }
        if (TimeTag.now().millis() > ((TimeTag) expirationObj).millis()) {
            return true;
        }
        return false;
    }

    public ObjectTag getFlagValueOfType(String key, StringHolder type) {
        List<String> splitKey = CoreUtilities.split(key, '.');
        MapTag map = getRootMap(splitKey.get(0));
        if (map == null) {
            return null;
        }
        if (isExpired(map.map.get(expirationString))) {
            return null;
        }
        if (splitKey.size() == 1) {
            ObjectTag returnValue = map.map.get(type);
            if (returnValue instanceof MapTag) {
                return deflaggedSubMap((MapTag) returnValue);
            }
            return returnValue;
        }
        ObjectTag rootValue = map.map.get(valueString);
        if (!(rootValue instanceof MapTag)) {
            return null;
        }
        map = (MapTag) rootValue;
        String endKey = splitKey.get(splitKey.size() - 1);
        for (int i = 1; i < splitKey.size() - 1; i++) {
            MapTag subMap = (MapTag) map.getObject(splitKey.get(i));
            if (subMap == null) {
                return null;
            }
            if (isExpired(subMap.map.get(expirationString))) {
                return null;
            }
            ObjectTag subValue = subMap.map.get(valueString);
            if (!(subValue instanceof MapTag)) {
                return null;
            }
            map = (MapTag) subValue;
        }
        MapTag obj = (MapTag) map.getObject(endKey);
        if (obj == null) {
            return null;
        }
        ObjectTag value = obj.map.get(type);
        if (value == null) {
            return null;
        }
        if (isExpired(obj.map.get(expirationString))) {
            return null;
        }
        if (value instanceof MapTag) {
            return deflaggedSubMap((MapTag) value);
        }
        return value;
    }

    @Override
    public ObjectTag getFlagValue(String key) {
        return getFlagValueOfType(key, valueString);
    }

    public MapTag deflaggedSubMap(MapTag map) {
        MapTag toReturn = new MapTag();
        for (Map.Entry<StringHolder, ObjectTag> pair : map.map.entrySet()) {
            MapTag subMap = (MapTag) pair.getValue();
            if (isExpired(subMap.map.get(expirationString))) {
                continue;
            }
            ObjectTag subValue = subMap.map.get(valueString);
            if (subValue instanceof MapTag) {
                subValue = deflaggedSubMap((MapTag) subValue);
            }
            toReturn.map.put(pair.getKey(), subValue);
        }
        return toReturn;
    }

    @Override
    public TimeTag getFlagExpirationTime(String key) {
        return (TimeTag) getFlagValueOfType(key, expirationString);
    }

    public boolean doClean(MapTag map) {
        if (skipAllCleanings) {
            return false;
        }
        boolean anyCleaned = false;
        ArrayList<StringHolder> toRemove = new ArrayList<>();
        for (Map.Entry<StringHolder, ObjectTag> entry : map.map.entrySet()) {
            if (!(entry.getValue() instanceof MapTag)) {
                continue;
            }
            if (isExpired(((MapTag) entry.getValue()).map.get(expirationString))) {
                toRemove.add(entry.getKey());
                anyCleaned = true;
            }
            else {
                ObjectTag subValue = ((MapTag) entry.getValue()).map.get(valueString);
                if (subValue instanceof MapTag) {
                    boolean didClean = doClean((MapTag) subValue);
                    anyCleaned = anyCleaned || didClean;
                }
            }
        }
        for (StringHolder str : toRemove) {
            map.map.remove(str);
        }
        return anyCleaned;
    }

    public MapTag flaggifyMapTag(MapTag map) {
        MapTag toReturn = new MapTag();
        for (Map.Entry<StringHolder, ObjectTag> pair : map.map.entrySet()) {
            MapTag flagMap = new MapTag();
            if (pair.getValue() instanceof MapTag) {
                flagMap.map.put(valueString, flaggifyMapTag((MapTag) pair.getValue()));
            }
            else {
                flagMap.map.put(valueString, pair.getValue());
            }
            toReturn.map.put(pair.getKey(), flagMap);
        }
        return toReturn;
    }

    @Override
    public void setFlag(String key, ObjectTag value, TimeTag expiration) {
        List<String> splitKey = CoreUtilities.split(key, '.');
        if (value == null && splitKey.size() == 1) {
            setRootMap(key, null);
            return;
        }
        MapTag rootMap = getRootMap(splitKey.get(0));
        MapTag map = rootMap;
        String endKey = splitKey.get(splitKey.size() - 1);
        for (int i = 0; i < splitKey.size() - 1; i++) {
            MapTag flagMap = i == 0 ? rootMap : (MapTag) map.getObject(splitKey.get(i));
            if (flagMap == null) {
                flagMap = new MapTag();
                if (i == 0) {
                    rootMap = flagMap;
                    setRootMap(splitKey.get(0), flagMap);
                }
                else {
                    map.putObject(splitKey.get(i), flagMap);
                }
            }
            ObjectTag innerMapTag = flagMap.map.get(valueString);
            flagMap.map.remove(expirationString);
            if (!(innerMapTag instanceof MapTag)) {
                innerMapTag = new MapTag();
                flagMap.map.put(valueString, innerMapTag);
            }
            map = (MapTag) innerMapTag;
        }
        if (value == null) {
            map.map.remove(new StringHolder(endKey));
            setRootMap(splitKey.get(0), rootMap);
        }
        else {
            MapTag resultMap = new MapTag();
            if (value instanceof MapTag) {
                value = flaggifyMapTag((MapTag) value);
            }
            else if (value instanceof ElementTag && value.toString().startsWith("map@")) {
                MapTag mappified = MapTag.valueOf(value.toString(), CoreUtilities.noDebugContext);
                if (mappified != null) {
                    value = flaggifyMapTag(mappified);
                }
            }
            resultMap.map.put(valueString, value);
            if (expiration != null) {
                resultMap.map.put(expirationString, expiration);
            }
            if (splitKey.size() != 1) {
                map.putObject(endKey, resultMap);
                setRootMap(splitKey.get(0), rootMap);
            }
            else {
                setRootMap(key, resultMap);
            }
        }
    }
}
