package net.aufdemrand.denizencore.utilities;

import net.aufdemrand.denizencore.utilities.debugging.dB;
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
            config.contents = new HashMap<String, Object>();
            config.contents.put(null, obj);
        }
        else if (obj instanceof Map) {
            config.contents = (Map<String, Object>)obj;
        }
        else {
            dB.echoError("Invalid YAML object type: " + obj.toString() + " is " + obj.getClass().getSimpleName());
            return null;
        }
        patchNonsense(config.contents);
        return config;
    }

    Map<String, Object> contents = null;

    /**
     * Don't ask why, I can't explain this Java BS.
     */
    private static void patchNonsense(Map<String, Object> objs) {
        for (Object o: new HashSet<Object>(objs.keySet())) {
            if (!(o instanceof String)) {
                objs.put(o.toString(), objs.get(o));
                objs.remove(o);
            }
        }
        for (Map.Entry<String, Object> str: objs.entrySet()) {
            if (str.getValue() instanceof Map) {
                patchNonsense((Map<String, Object>)str.getValue());
            }
        }
    }

    private static List<String> patchListNonsense(List<Object> objs) {
        List<String> list = new ArrayList<String>();
        for (Object o: objs) {
            if (o == null)
                list.add("null");
            else
                list.add(o.toString());
        }
        return list;
    }

    public YamlConfiguration() {
        contents = new HashMap<String, Object>();
    }

    public Set<String> getKeys(boolean deep) {
        if (!deep) {
            return new HashSet<String>(contents.keySet());
        }
        else {
            return getKeysDeep(contents);
        }
    }

    public Map<String, Object> getMap() {
        return new HashMap<String, Object>(contents);
    }

    private Set<String> getKeysDeep(Map<String, Object> objs) {
        Set<String> strings = new HashSet<String>(objs.keySet());
        for (Map.Entry<String, Object> str: objs.entrySet()) {
            if (str.getValue() instanceof Map) {
                strings.addAll(getKeysDeep((Map<String, Object>)str.getValue()));
            }
        }
        return strings;
    }

    public String saveToString() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setAllowUnicode(true);
        Yaml yaml = new Yaml(options);
        return yaml.dump(contents);
    }

    public Object get(String path) {
        List<String> parts = CoreUtilities.Split(path, '.');
        Map<String, Object> portion = contents;
        for (int i = 0; i < parts.size(); i++) {
            Object oPortion = portion.get(parts.get(i));
            if (oPortion == null) {
                return null;
            }
            else if (parts.size() == i + 1) {
                return oPortion;
            }
            else if (oPortion instanceof Map) {
                portion = (Map<String, Object>) oPortion;
            }
            else {
                return null;
            }
        }
        return null;
    }

    public void set(String path, Object o) {
        if (o instanceof YamlConfiguration)
            o = new HashMap<String,Object>(((YamlConfiguration)o).contents);
        List<String> parts = CoreUtilities.Split(path, '.');
        Map<String, Object> portion = contents;
        for (int i = 0; i < parts.size(); i++) {
            Object oPortion = portion.get(parts.get(i));
            if (parts.size() == i + 1) {
                if (o == null) {
                    portion.remove(parts.get(i));
                    emptyEmptyMaps(parts);
                }
                else {
                    portion.put(parts.get(i), o);
                }
                return;
            }
            else if (oPortion == null) {
                Map<String, Object> map = new HashMap<String, Object>();
                portion.put(parts.get(i), map);
                portion = map;
            }
            else if (oPortion instanceof Map) {
                portion = (Map<String, Object>) oPortion;
            }
            else {
                Map<String, Object> map = new HashMap<String, Object>();
                portion.put(parts.get(i), map);
                portion = map;
            }
        }
        dB.echoError("Failed to set somehow?");
    }

    void emptyEmptyMaps(List<String> parts) {
        Map<String, Object> portion = contents;
        for (int i = 0; i < parts.size(); i++) {
            Object oPortion = portion.get(parts.get(i));
            if (oPortion == null) {
                return;
            }
            else if (oPortion instanceof Map) {
                if (((Map<String, Object>)oPortion).size() == 0) {
                    portion.remove(parts.get(i));
                    emptyEmptyMaps(parts);
                    return;
                }
                portion = (Map<String, Object>) oPortion;
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
            List<String> parts = CoreUtilities.Split(path, '.');
            Map<String, Object> portion = contents;
            for (int i = 0; i < parts.size(); i++) {
                Object oPortion = portion.get(parts.get(i));
                if (oPortion == null) {
                    return null;
                }
                else if (parts.size() == i + 1) {
                    YamlConfiguration configuration = new YamlConfiguration();
                    if (!(oPortion instanceof Map))
                        return null;
                    configuration.contents = (Map<String, Object>) oPortion;
                    return configuration;
                }
                else if (oPortion instanceof Map) {
                    portion = (Map<String, Object>) oPortion;
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
