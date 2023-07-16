package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.TimeTag;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class MapTagBasedFlagTracker extends AbstractFlagTracker {

    public static StringHolder valueString = new StringHolder("__value");

    public static StringHolder expirationString = new StringHolder("__expiration");

    public static boolean isExpired(ObjectTag expirationObj) {
        if (expirationObj == null) {
            return false;
        }
        if (DenizenCore.currentTimeMillis > ((TimeTag) expirationObj).millis()) {
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
        if (isExpired(map.getObject(expirationString))) {
            return null;
        }
        if (splitKey.size() == 1) {
            ObjectTag returnValue = map.getObject(type);
            if (returnValue instanceof MapTag) {
                return deflaggedSubMap((MapTag) returnValue);
            }
            return returnValue;
        }
        ObjectTag rootValue = map.getObject(valueString);
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
            if (isExpired(subMap.getObject(expirationString))) {
                return null;
            }
            ObjectTag subValue = subMap.getObject(valueString);
            if (!(subValue instanceof MapTag)) {
                return null;
            }
            map = (MapTag) subValue;
        }
        MapTag obj = (MapTag) map.getObject(endKey);
        if (obj == null) {
            return null;
        }
        ObjectTag value = obj.getObject(type);
        if (value == null) {
            return null;
        }
        if (isExpired(obj.getObject(expirationString))) {
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
        for (Map.Entry<StringHolder, ObjectTag> pair : map.entrySet()) {
            MapTag subMap = (MapTag) pair.getValue();
            if (isExpired(subMap.getObject(expirationString))) {
                continue;
            }
            ObjectTag subValue = subMap.getObject(valueString);
            if (subValue instanceof MapTag) {
                subValue = deflaggedSubMap((MapTag) subValue);
            }
            toReturn.putObject(pair.getKey(), subValue);
        }
        return toReturn;
    }

    @Override
    public TimeTag getFlagExpirationTime(String key) {
        return (TimeTag) getFlagValueOfType(key, expirationString);
    }

    public boolean doClean(MapTag map) {
        if (CoreConfiguration.skipAllFlagCleanings) {
            return false;
        }
        boolean anyCleaned = false;
        ArrayList<StringHolder> toRemove = new ArrayList<>();
        for (Map.Entry<StringHolder, ObjectTag> entry : map.entrySet()) {
            if (!(entry.getValue() instanceof MapTag)) {
                continue;
            }
            if (isExpired(((MapTag) entry.getValue()).getObject(expirationString))) {
                toRemove.add(entry.getKey());
                anyCleaned = true;
            }
            else {
                ObjectTag subValue = ((MapTag) entry.getValue()).getObject(valueString);
                if (subValue instanceof MapTag) {
                    boolean didClean = doClean((MapTag) subValue);
                    anyCleaned = anyCleaned || didClean;
                }
            }
        }
        for (StringHolder str : toRemove) {
            map.remove(str);
        }
        return anyCleaned;
    }

    public MapTag flaggifyMapTag(MapTag map) {
        MapTag toReturn = new MapTag();
        for (Map.Entry<StringHolder, ObjectTag> pair : map.entrySet()) {
            MapTag flagMap = new MapTag();
            if (pair.getValue() instanceof MapTag) {
                flagMap.putObject(valueString, flaggifyMapTag((MapTag) pair.getValue()));
            }
            else {
                flagMap.putObject(valueString, pair.getValue());
            }
            toReturn.putObject(pair.getKey(), flagMap);
        }
        return toReturn;
    }

    @Override
    public void setFlag(String key, ObjectTag value, TimeTag expiration, boolean doFlaggify) {
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
            ObjectTag innerMapTag = flagMap.getObject(valueString);
            flagMap.remove(expirationString);
            if (!(innerMapTag instanceof MapTag)) {
                innerMapTag = new MapTag();
                flagMap.putObject(valueString, innerMapTag);
            }
            map = (MapTag) innerMapTag;
        }
        if (value == null) {
            map.remove(endKey);
            setRootMap(splitKey.get(0), rootMap);
        }
        else {
            MapTag resultMap;
            if (value instanceof MapTag && !doFlaggify) {
                resultMap = (MapTag) value;
            }
            else {
                resultMap = new MapTag();
                if (value.shouldBeType(MapTag.class)) {
                    MapTag mappified = value.asType(MapTag.class, CoreUtilities.noDebugContext);
                    if (mappified != null) {
                        value = flaggifyMapTag(mappified);
                    }
                }
                resultMap.putObject(valueString, value);
                if (expiration != null) {
                    resultMap.putObject(expirationString, expiration);
                }
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
