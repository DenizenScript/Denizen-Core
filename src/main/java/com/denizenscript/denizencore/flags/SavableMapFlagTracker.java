package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptHelper;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
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

    public static SavableMapFlagTracker loadFlagFile(String filePath) {
        try {
            File realPath;
            File flagFile = new File(filePath + ".dat");
            if (flagFile.exists()) {
                realPath = flagFile;
            }
            else {
                File bakFile = new File(filePath + ".dat~2");
                if (bakFile.exists()) {
                    realPath = bakFile;
                }
                // Note: ~1 are likely corrupted, so ignore them.
                else {
                    return new SavableMapFlagTracker();
                }
            }
            FileInputStream fis = new FileInputStream(realPath);
            String str = ScriptHelper.convertStreamToString(fis);
            fis.close();
            return new SavableMapFlagTracker(str);
        }
        catch (Throwable ex) {
            Debug.echoError("Failed to load flag data for path '" + filePath + "'");
            Debug.echoError(ex);
            return new SavableMapFlagTracker();
        }
    }

    public void saveToFile(String filePath) {
        saveToFile(filePath, toString());
    }

    public static void saveToFile(String filePath, String flagData) {
        File saveToFile = new File(filePath + ".dat~1");
        try {
            Charset charset = ScriptHelper.encoding == null ? null : ScriptHelper.encoding.charset();
            FileOutputStream fiout = new FileOutputStream(saveToFile);
            OutputStreamWriter writer;
            if (charset == null) {
                writer = new OutputStreamWriter(fiout);
            }
            else {
                writer = new OutputStreamWriter(fiout, charset);
            }
            writer.write(flagData);
            writer.close();
            File bakFile = new File(filePath + ".dat~2");
            File realFile = new File(filePath + ".dat");
            if (realFile.exists()) {
                realFile.renameTo(bakFile);
            }
            saveToFile.renameTo(realFile);
            if (bakFile.exists()) {
                bakFile.delete();
            }
        }
        catch (Throwable ex) {
            Debug.echoError("Failed to save flag data to path '" + filePath + "'");
            Debug.echoError(ex);
        }
    }
}
