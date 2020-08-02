package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.scripts.ScriptBuilder;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.scanner.ScannerImpl;

import java.util.*;

/**
 * Represents a YAML file.
 */
public class YamlConfiguration {

    static {
        ScannerImpl.ESCAPE_REPLACEMENTS.put('/', "/");
    }

    public static class CustomResolver extends Resolver {
        @Override
        protected void addImplicitResolvers() {
        }
    }

    public static YamlConfiguration load(String data) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        Yaml yaml = new Yaml(new Constructor(), new Representer(), options, new CustomResolver());
        Object obj = yaml.load(data);
        YamlConfiguration config = new YamlConfiguration();
        if (obj == null) {
            return null;
        }
        else if (obj instanceof String) {
            config.contents = new HashMap<>();
            config.contents.put(null, obj);
        }
        else if (obj instanceof Map) {
            config.contents = (Map<StringHolder, Object>) obj;
        }
        else {
            Debug.echoError("Invalid YAML object type: " + obj.toString() + " is " + obj.getClass().getSimpleName());
            return null;
        }
        switchKeys(config.contents);
        return config;
    }

    public Map<StringHolder, Object> contents;
    boolean dirty;

    /**
     * Use StringHolders instead of strings.
     */
    public static void switchKeys(Map<StringHolder, Object> objs) {
        for (Object o : new HashSet<Object>(objs.keySet())) {
            Object got = objs.get(o);
            objs.remove(o);
            objs.put(new StringHolder(o.toString()), got);
        }
        for (Map.Entry<StringHolder, Object> str : objs.entrySet()) {
            if (str.getValue() instanceof Map) {
                Map map = (Map<StringHolder, Object>) str.getValue();
                switchKeys(map);
                objs.remove(map);
                objs.put(str.getKey(), map);
            }
        }
    }

    public static Map<String, Object> reverse(Map<StringHolder, Object> objs, boolean patchLines) {
        HashMap<String, Object> map = new HashMap<>();
        for (Map.Entry<StringHolder, Object> obj : objs.entrySet()) {
            if (obj.getValue() instanceof Map) {
                map.put(obj.getKey().str, reverse((Map<StringHolder, Object>) obj.getValue(), patchLines));
            }
            else if (patchLines && obj.getValue() instanceof List) {
                List vals = (List) obj.getValue();
                List<String> output = new ArrayList<>(vals.size());
                for (Object val : vals) {
                    if (val == null) {
                        continue;
                    }
                    output.add(ScriptBuilder.stripLinePrefix(val.toString()));
                }
                map.put(obj.getKey().str, output);
            }
            else {
                map.put(obj.getKey().str, obj.getValue());
            }
        }
        return map;
    }

    public void forceLoweredRootKey(String keyName) {
        StringHolder key = new StringHolder(CoreUtilities.toLowerCase(keyName));
        Object obj = contents.get(key);
        if (obj != null) {
            contents.remove(key);
            contents.put(key, obj);
        }
    }

    public static List<String> patchListNonsense(List<Object> objs) {
        List<String> list = new ArrayList<>();
        for (Object o : objs) {
            if (o == null) {
                list.add("null");
            }
            else {
                list.add(o.toString());
            }
        }
        return list;
    }

    public YamlConfiguration() {
        contents = new HashMap<>();
        dirty = false;
    }

    public Set<StringHolder> getKeys(boolean deep) {
        if (!deep) {
            return new HashSet<>(contents.keySet());
        }
        else {
            return getKeysDeep(contents, "");
        }
    }

    public Map<StringHolder, Object> getMap() {
        return new HashMap<>(contents);
    }

    public void addAll(Map<StringHolder, Object> newContents) {
        contents.putAll(newContents);
    }

    private Set<StringHolder> getKeysDeep(Map<StringHolder, Object> objs, String base) {
        Set<StringHolder> strings = new HashSet<>();
        for (Map.Entry<StringHolder, Object> obj : objs.entrySet()) {
            strings.add(new StringHolder(base + obj.getKey()));
            if (obj.getValue() instanceof Map) {
                strings.addAll(getKeysDeep((Map<StringHolder, Object>) obj.getValue(), base + obj.getKey() + "."));
            }
        }
        return strings;
    }

    public String saveToString(boolean patchLines) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        Yaml yaml = new Yaml(options);
        String dumped = yaml.dump(reverse(contents, patchLines));
        if (Debug.verbose) {
            Debug.log("Outputting " + dumped);
        }
        return dumped;
    }

    public Object get(String path) {
        if (path.isEmpty()) {
            return contents;
        }
        List<String> parts = CoreUtilities.split(path, '.');
        Map<StringHolder, Object> portion = contents;
        for (int i = 0; i < parts.size(); i++) {
            Object oPortion = portion.get(new StringHolder(parts.get(i)));
            if (oPortion == null) {
                return null;
            }
            else if (parts.size() == i + 1) {
                return oPortion;
            }
            else if (oPortion instanceof Map) {
                portion = (Map<StringHolder, Object>) oPortion;
            }
            else {
                return null;
            }
        }
        return null;
    }

    public void set(String path, Object o) {
        if (o instanceof YamlConfiguration) {
            o = new HashMap<>(((YamlConfiguration) o).contents);
        }
        List<String> parts = CoreUtilities.split(path, '.');
        Map<StringHolder, Object> portion = contents;
        for (int i = 0; i < parts.size(); i++) {
            Object oPortion = portion.get(new StringHolder(parts.get(i)));
            if (parts.size() == i + 1) {
                if (o == null) {
                    portion.remove(new StringHolder(parts.get(i)));
                    emptyEmptyMaps(parts);
                }
                else {
                    portion.put(new StringHolder(parts.get(i)), o);
                }
                dirty = true;
                return;
            }
            else if (oPortion == null) {
                Map<StringHolder, Object> map = new HashMap<>();
                portion.put(new StringHolder(parts.get(i)), map);
                portion = map;
            }
            else if (oPortion instanceof Map) {
                portion = (Map<StringHolder, Object>) oPortion;
            }
            else {
                Map<StringHolder, Object> map = new HashMap<>();
                portion.put(new StringHolder(parts.get(i)), map);
                portion = map;
            }
        }
        Debug.echoError("Failed to set somehow?");
    }

    void emptyEmptyMaps(List<String> parts) {
        Map<StringHolder, Object> portion = contents;
        for (int i = 0; i < parts.size(); i++) {
            Object oPortion = portion.get(new StringHolder(parts.get(i)));
            if (oPortion == null) {
                return;
            }
            else if (oPortion instanceof Map) {
                if (((Map<StringHolder, Object>) oPortion).isEmpty()) {
                    portion.remove(new StringHolder(parts.get(i)));
                    emptyEmptyMaps(parts);
                    return;
                }
                portion = (Map<StringHolder, Object>) oPortion;
            }
            else {
                return;
            }
        }
    }

    public boolean contains(String path) {
        return get(path) != null;
    }

    public String getString(String path) {
        Object o = get(path);
        if (o == null) {
            return null;
        }
        return o.toString();
    }

    public String getString(String path, String def) {
        Object o = get(path);
        if (o == null) {
            return def;
        }
        return o.toString();
    }

    public boolean isList(String path) {
        Object o = get(path);
        if (o == null) {
            return false;
        }
        if (!(o instanceof List)) {
            return false;
        }
        return true;
    }

    public List<Object> getList(String path) {
        Object o = get(path);
        if (o == null) {
            return null;
        }
        if (!(o instanceof List)) {
            return null;
        }
        return (List<Object>) o;
    }

    public List<String> getStringList(String path) {
        Object o = get(path);
        if (o == null) {
            return null;
        }
        if (!(o instanceof List)) {
            return null;
        }
        return patchListNonsense((List<Object>) o);
    }

    public YamlConfiguration getConfigurationSection(String path) {
        try {
            List<String> parts = CoreUtilities.split(path, '.');
            Map<StringHolder, Object> portion = contents;
            for (int i = 0; i < parts.size(); i++) {
                Object oPortion = portion.get(new StringHolder(parts.get(i)));
                if (oPortion == null) {
                    return null;
                }
                else if (parts.size() == i + 1) {
                    YamlConfiguration configuration = new YamlConfiguration();
                    if (!(oPortion instanceof Map)) {
                        return null;
                    }
                    configuration.contents = (Map<StringHolder, Object>) oPortion;
                    return configuration;
                }
                else if (oPortion instanceof Map) {
                    portion = (Map<StringHolder, Object>) oPortion;
                }
                else {
                    return null;
                }
            }
        }
        catch (Exception e) {
            Debug.echoError(e);
        }
        return null;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
