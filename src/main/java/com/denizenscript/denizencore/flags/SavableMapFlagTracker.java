package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SavableMapFlagTracker extends MapTagBasedFlagTracker {

    public static class SaveOptimizedFlag {

        public MapTag map;

        public String string;

        public boolean canExpire;

        public MapTag getMap() {
            if (map == null) {
                if (string.startsWith("map@")) {
                    map = MapTag.valueOf(string, CoreUtilities.errorButNoDebugContext);
                }
                else {
                    map = new MapTag();
                    map.map.put(valueString, ObjectFetcher.pickObjectFor(string, CoreUtilities.errorButNoDebugContext));
                }
            }
            return map;
        }

        public String getString() {
            if (string == null) {
                if (map.map.containsKey(expirationString) || map.map.get(valueString) instanceof MapTag) {
                    string = map.savable();
                }
                else {
                    string = map.map.get(valueString).savable();
                }
            }
            return string;
        }
    }

    public HashMap<StringHolder, SaveOptimizedFlag> map;

    public boolean modified;

    public SavableMapFlagTracker() {
        map = new HashMap<>();
    }

    public SavableMapFlagTracker(String input) {
        map = new HashMap<>(input.length() / 50);
        int eol = input.indexOf('\n');
        int startOfLine = 0;
        while (eol != -1) {
            int colon = input.indexOf(':', startOfLine);
            if (colon != -1) {
                String key = input.substring(startOfLine, colon);
                boolean expirable = key.startsWith("\\ex");
                if (expirable) {
                    key = key.substring("\\ex".length());
                }
                key = unescapeKey(key);
                String value = unescapeValue(input.substring(colon + 1, eol));
                SaveOptimizedFlag flag = new SaveOptimizedFlag();
                flag.canExpire = expirable;
                flag.string = value;
                map.put(new StringHolder(key), flag);
            }
            startOfLine = eol + 1;
            eol = input.indexOf('\n', eol + 1);
        }
        doTotalClean();
    }

    public void doTotalClean() {
        if (MapTagBasedFlagTracker.skipAllCleanings) {
            return;
        }
        ArrayList<StringHolder> toRemove = new ArrayList<>();
        for (Map.Entry<StringHolder, SaveOptimizedFlag> entry : map.entrySet()) {
            if (!entry.getValue().canExpire) {
                continue;
            }
            if (isExpired(entry.getValue().getMap().map.get(expirationString))) {
                toRemove.add(entry.getKey());
                modified = true;
            }
            else {
                ObjectTag subValue = entry.getValue().getMap().map.get(valueString);
                if (subValue instanceof MapTag) {
                    if (doClean((MapTag) subValue)) {
                        entry.getValue().string = null;
                        modified = true;
                    }
                }
            }
        }
        for (StringHolder str : toRemove) {
            map.remove(str);
        }
    }

    @Override
    public MapTag getRootMap(String key) {
        SaveOptimizedFlag flag = map.get(new StringHolder(key));
        if (flag == null) {
            return null;
        }
        return flag.getMap();
    }

    @Override
    public void setRootMap(String key, MapTag value) {
        modified = true;
        if (value == null) {
            map.remove(new StringHolder(key));
            return;
        }
        SaveOptimizedFlag flag = new SaveOptimizedFlag();
        flag.map = value;
        flag.string = null;
        if (value.map.containsKey(expirationString) || value.map.get(valueString) instanceof MapTag) {
            flag.canExpire = true;
        }
        map.put(new StringHolder(key), flag);
    }

    @Override
    public Collection<String> listAllFlags() {
        ArrayList<String> keys = new ArrayList<>(map.size());
        for (StringHolder string : map.keySet()) {
            keys.add(string.str);
        }
        return keys;
    }

    public static AsciiMatcher valueEscapeNeededMatcher = new AsciiMatcher("\0\n\\");

    public static String unescapeValue(String key) {
        if (!CoreUtilities.contains(key, '\\')) {
            return key;
        }
        key = CoreUtilities.replace(key, "\\nl", "\n");
        key = CoreUtilities.replace(key, "\\bs", "\\");
        return key;
    }

    public static String escapeValue(String key) {
        if (!valueEscapeNeededMatcher.containsAnyMatch(key)) {
            return key;
        }
        key = CoreUtilities.replace(key, "\\", "\\bs");
        key = CoreUtilities.replace(key, "\n", "\\nl");
        key = CoreUtilities.replace(key, "\0", "");
        return key;
    }

    public static AsciiMatcher keyEscapeNeededMatcher = new AsciiMatcher("\0:\n\\");

    public static String unescapeKey(String key) {
        if (!CoreUtilities.contains(key, '\\')) {
            return key;
        }
        key = CoreUtilities.replace(key, "\\co", ":");
        key = CoreUtilities.replace(key, "\\nl", "\n");
        key = CoreUtilities.replace(key, "\\bs", "\\");
        return key;
    }

    public static String escapeKey(String key) {
        if (!keyEscapeNeededMatcher.containsAnyMatch(key)) {
            return key;
        }
        key = CoreUtilities.replace(key, "\\", "\\bs");
        key = CoreUtilities.replace(key, ":", "\\co");
        key = CoreUtilities.replace(key, "\n", "\\nl");
        key = CoreUtilities.replace(key, "\0", "");
        return key;
    }

    @Override
    public String toString() {
        StringBuilder toOutput = new StringBuilder(map.size() * 100);
        for (Map.Entry<StringHolder, SavableMapFlagTracker.SaveOptimizedFlag> flag : map.entrySet()) {
            if (flag.getValue().canExpire) {
                toOutput.append("\\ex");
            }
            toOutput.append(escapeKey(flag.getKey().str)).append(":").append(escapeValue(flag.getValue().getString())).append('\n');
        }
        return toOutput.toString();
    }
}
