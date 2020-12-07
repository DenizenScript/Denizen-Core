package com.denizenscript.denizencore.flags;

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

        public MapTag getMap() {
            if (map == null) {
                map = MapTag.valueOf(string, CoreUtilities.errorButNoDebugContext);
            }
            return map;
        }

        public String getString() {
            if (string == null) {
                string = map.toString();
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
                String key = unescapeKey(input.substring(startOfLine, colon));
                String value = unescapeValue(input.substring(colon + 1, eol));
                SaveOptimizedFlag flag = new SaveOptimizedFlag();
                flag.string = value;
                map.put(new StringHolder(key), flag);
            }
            startOfLine = eol + 1;
            eol = input.indexOf('\n', eol + 1);
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

    public static AsciiMatcher valueEscapeNeededMatcher = new AsciiMatcher("\n\\");

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
        return key;
    }

    public static AsciiMatcher keyEscapeNeededMatcher = new AsciiMatcher(":\n\\");

    public static String unescapeKey(String key) {
        if (!CoreUtilities.contains(key, '\\')) {
            return key;
        }
        key = CoreUtilities.replace(key, "\\amp", "&");
        key = CoreUtilities.replace(key, "\\co", ":");
        key = CoreUtilities.replace(key, "\\nl", "\n");
        return key;
    }

    public static String escapeKey(String key) {
        if (!keyEscapeNeededMatcher.containsAnyMatch(key)) {
            return key;
        }
        key = CoreUtilities.replace(key, "\\", "\\bs");
        key = CoreUtilities.replace(key, ":", "\\co");
        key = CoreUtilities.replace(key, "\n", "\\nl");
        return key;
    }

    @Override
    public String toString() {
        StringBuilder toOutput = new StringBuilder(map.size() * 100);
        for (Map.Entry<StringHolder, SavableMapFlagTracker.SaveOptimizedFlag> flag : map.entrySet()) {
            toOutput.append(escapeKey(flag.getKey().str)).append(":").append(escapeValue(flag.getValue().getString())).append('\n');
        }
        return toOutput.toString();
    }
}
