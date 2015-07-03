package net.aufdemrand.denizencore.utilities;

import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.text.StringHolder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Represents a YAML file.
 */
public class YamlConfiguration {

    public static YamlConfiguration load(String data) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        Yaml yaml = new Yaml(options);
        Object obj = yaml.load(data);
        YamlConfiguration config = new YamlConfiguration();
        if (obj == null) {
            return null;
        }
        else if (obj instanceof String) {
            config.contents = new HashMap<StringHolder, Object>();
            config.contents.put(null, obj);
        }
        else if (obj instanceof Map) {
            config.contents = (Map<StringHolder, Object>) obj;
        }
        else {
            dB.echoError("Invalid YAML object type: " + obj.toString() + " is " + obj.getClass().getSimpleName());
            return null;
        }
        switchKeys(config.contents);
        return config;
    }

    Map<StringHolder, Object> contents = null;

    /**
     * Use StringHolders instead of strings.
     */
    private static void switchKeys(Map<StringHolder, Object> objs) {
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

    private static Map<String, Object> reverse(Map<StringHolder, Object> objs) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (Map.Entry<StringHolder, Object> obj : objs.entrySet()) {
            if (obj.getValue() instanceof Map) {
                map.put(obj.getKey().str, reverse((Map<StringHolder, Object>) obj.getValue()));
            }
            else {
                map.put(obj.getKey().str, obj.getValue());
            }
        }
        return map;
    }

    private static List<String> patchListNonsense(List<Object> objs) {
        List<String> list = new ArrayList<String>();
        for (Object o : objs) {
            if (o == null)
                list.add("null");
            else
                list.add(o.toString());
        }
        return list;
    }

    public YamlConfiguration() {
        contents = new HashMap<StringHolder, Object>();
    }

    public Set<StringHolder> getKeys(boolean deep) {
        if (!deep) {
            return new HashSet<StringHolder>(contents.keySet());
        }
        else {
            return getKeysDeep(contents);
        }
    }

    public Map<StringHolder, Object> getMap() {
        return new HashMap<StringHolder, Object>(contents);
    }

    private Set<StringHolder> getKeysDeep(Map<StringHolder, Object> objs) {
        Set<StringHolder> strings = new HashSet<StringHolder>(objs.keySet());
        for (Map.Entry<StringHolder, Object> str : objs.entrySet()) {
            if (str.getValue() instanceof Map) {
                strings.addAll(getKeysDeep((Map<StringHolder, Object>) str.getValue()));
            }
        }
        return strings;
    }

    public String saveToString() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        Yaml yaml = new Yaml(options);
        String dumped = yaml.dump(reverse(contents));
        if (dB.verbose)
            dB.log("Outputting " + dumped);
        return dumped;
    }

    public Object get(String path) {
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
        if (o instanceof YamlConfiguration)
            o = new HashMap<StringHolder, Object>(((YamlConfiguration) o).contents);
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
                return;
            }
            else if (oPortion == null) {
                Map<StringHolder, Object> map = new HashMap<StringHolder, Object>();
                portion.put(new StringHolder(parts.get(i)), map);
                portion = map;
            }
            else if (oPortion instanceof Map) {
                portion = (Map<StringHolder, Object>) oPortion;
            }
            else {
                Map<StringHolder, Object> map = new HashMap<StringHolder, Object>();
                portion.put(new StringHolder(parts.get(i)), map);
                portion = map;
            }
        }
        dB.echoError("Failed to set somehow?");
    }

    void emptyEmptyMaps(List<String> parts) {
        Map<StringHolder, Object> portion = contents;
        for (int i = 0; i < parts.size(); i++) {
            Object oPortion = portion.get(new StringHolder(parts.get(i)));
            if (oPortion == null) {
                return;
            }
            else if (oPortion instanceof Map) {
                if (((Map<StringHolder, Object>) oPortion).size() == 0) {
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
        if (o == null)
            return null;
        return o.toString();
    }

    public String getString(String path, String def) {
        Object o = get(path);
        if (o == null)
            return def;
        return o.toString();
    }

    public boolean isList(String path) {
        Object o = get(path);
        if (o == null)
            return false;
        if (!(o instanceof List))
            return false;
        return true;
    }

    public List<String> getStringList(String path) {
        Object o = get(path);
        if (o == null)
            return null;
        if (!(o instanceof List))
            return null;
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
                    if (!(oPortion instanceof Map))
                        return null;
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
            dB.echoError(e);
        }
        return null;
    }
}
