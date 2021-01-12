package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.NaturalOrderComparator;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import org.json.JSONObject;

import java.util.*;

public class MapTag implements ObjectTag, Adjustable {

    // <--[language]
    // @name MapTag Objects
    // @group Object System
    // @description
    // A MapTag represents a mapping of keys to values.
    // Keys are plain text, case-insensitive.
    // Values can be anything, even lists or maps themselves.
    //
    // Any given key can only appear in a map once (ie, no duplicate keys).
    // Values can be duplicated into multiple keys without issue.
    //
    // Order of keys is preserved. Casing in keys is preserved in the object but ignored for map lookups.
    //
    // These use the object notation "map@".
    // The identity format for MapTags is each key/value pair, one after the other, separated by a pipe '|' symbol.
    // The key/value pair is separated by a slash.
    // For example, a map of "taco" to "food", "chicken" to "animal", and "bob" to "person" would be "map@taco/food|chicken/animal|bob/person|"
    // A map with zero items in it is simply 'map@'.
    //
    // If the pipe symbol "|" appears in a key or value, it will be replaced by "&pipe",
    // a slash "/" will become "&fs", and an ampersand "&" will become "&amp".
    // This is a subset of Denizen standard escaping, see <@link language Escaping System>.
    //
    // -->

    public static AsciiMatcher needsEscpingMatcher = new AsciiMatcher("&|/");

    public static String escapeEntry(String value) {
        if (!needsEscpingMatcher.containsAnyMatch(value)) {
            return value;
        }
        value = CoreUtilities.replace(value, "&", "&amp");
        value = CoreUtilities.replace(value, "|", "&pipe");
        value = CoreUtilities.replace(value, "/", "&fs");
        return value;
    }

    public static String unescapeEntry(String value) {
        if (value.indexOf('&') == -1) {
            return value;
        }
        value = CoreUtilities.replace(value, "&fs", "/");
        value = CoreUtilities.replace(value, "&pipe", "|");
        value = CoreUtilities.replace(value, "&amp", "&");
        return value;
    }

    @Fetchable("map")
    public static MapTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("map@")) {
            string = string.substring("map@".length());
        }
        MapTag result = new MapTag();
        if (string.length() == 0) {
            return result;
        }
        if (!string.endsWith("|")) {
            string += "|";
        }
        int pipe = string.indexOf('|');
        int lastPipe = 0;
        while (pipe != -1) {
            int slash = string.indexOf('/', lastPipe);
            if (slash == -1 || slash > pipe) {
                return null;
            }
            String key = string.substring(lastPipe, slash);
            String value = string.substring(slash + 1, pipe);
            result.putObject(unescapeEntry(key), ObjectFetcher.pickObjectFor(unescapeEntry(value), context));
            lastPipe = pipe + 1;
            pipe = string.indexOf('|', lastPipe);
        }
        return result;
    }

    public static MapTag getMapFor(ObjectTag inp, TagContext context) {
        return inp instanceof MapTag ? (MapTag) inp : valueOf(inp.toString(), context);
    }

    public static boolean matches(String string) {
        // Starts with map@? Assume match.
        if (CoreUtilities.toLowerCase(string).startsWith("map@")) {
            return true;
        }
        return valueOf(string, CoreUtilities.noDebugContext) != null;
    }

    public LinkedHashMap<StringHolder, ObjectTag> map;

    public MapTag() {
        this.map = new LinkedHashMap<>();
    }

    public MapTag(Map<StringHolder, ObjectTag> map) {
        this.map = new LinkedHashMap<>(map);
    }

    @Override
    public MapTag duplicate() {
        MapTag newMap = new MapTag();
        for (Map.Entry<StringHolder, ObjectTag> entry : map.entrySet()) {
            newMap.map.put(entry.getKey(), entry.getValue().duplicate());
        }
        return newMap;
    }

    String prefix = "Map";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public MapTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String getObjectType() {
        return "Map";
    }

    @Override
    public String debuggable() {
        if (map.isEmpty()) {
            return "map@";
        }
        StringBuilder debugText = new StringBuilder();
        debugText.append("<G>map@<Y> ");
        for (Map.Entry<StringHolder, ObjectTag> entry : map.entrySet()) {
            debugText.append(entry.getKey().str).append(" <G>/<Y> ").append(entry.getValue().debuggable()).append(" <G>|<Y> ");
        }
        return debugText.substring(0, debugText.length() - " <G>|<Y> ".length());
    }

    @Override
    public String identify() {
        StringBuilder output = new StringBuilder();
        output.append("map@");
        for (Map.Entry<StringHolder, ObjectTag> entry : map.entrySet()) {
            output.append(escapeEntry(entry.getKey().str)).append("/").append(escapeEntry(entry.getValue().savable())).append("|");
        }
        return output.toString();
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    public ObjectTag getDeepObject(String key) {
        if (!CoreUtilities.contains(key, '.')) {
            return getObject(key);
        }
        MapTag current = this;
        List<String> subkeys = CoreUtilities.split(key, '.');
        for (int i = 0; i < subkeys.size() - 1; i++) {
            ObjectTag subValue = current.getObject(subkeys.get(i));
            if (!(subValue instanceof MapTag)) {
                return null;
            }
            current = (MapTag) subValue;
        }
        return current.getObject(subkeys.get(subkeys.size() - 1));
    }

    public ObjectTag getObject(String key) {
        return map.get(new StringHolder(key));
    }

    public void putDeepObject(String key, ObjectTag value) {
        if (!CoreUtilities.contains(key, '.')) {
            putObject(key, value);
            return;
        }
        MapTag current = this;
        List<String> subkeys = CoreUtilities.split(key, '.');
        for (int i = 0; i < subkeys.size() - 1; i++) {
            ObjectTag subValue = current.getObject(subkeys.get(i));
            if (!(subValue instanceof MapTag)) {
                subValue = new MapTag();
                current.putObject(subkeys.get(i), subValue);
            }
            current = (MapTag) subValue;
        }
        current.putObject(subkeys.get(subkeys.size() - 1), value);
    }

    public void putObject(String key, ObjectTag value) {
        if (value == null) {
            map.remove(new StringHolder(key));
        }
        else {
            map.put(new StringHolder(key), value);
        }
    }

    public static void registerTags() {

        // <--[tag]
        // @attribute <MapTag.size>
        // @returns ElementTag(Number)
        // @description
        // Returns the size of the map - that is, how many key/value pairs are within it.
        // -->
        registerTag("size", (attribute, object) -> {
            return new ElementTag(object.map.size());
        });

        // <--[tag]
        // @attribute <MapTag.is_empty>
        // @returns ElementTag(Boolean)
        // @description
        // Returns "true" if the map is empty (contains no keys), otherwise "false".
        // -->
        registerTag("is_empty", (attribute, object) -> {
            return new ElementTag(object.map.isEmpty());
        });

        // <--[tag]
        // @attribute <MapTag.sort_by_value[(<tag>)]>
        // @returns MapTag
        // @description
        // returns a copy of the map, sorted alphanumerically by the value under each key.
        // Optionally, specify a tag to apply to the value.
        // To sort by key, use <@link tag MapTag.get_subset> with list sort tags, like 'map.get_subset[map.list_keys.sort_by_value[...]]'.
        // This also lets you apply list filters or similar to the keyset.
        // To apply a '.parse' to the values, use <@link tag ListTag.map_with>, like 'map.list_keys.map_with[map.list_values.parse[...]]'
        // -->
        registerTag("sort_by_value", (attribute, object) -> {
            ArrayList<Map.Entry<StringHolder, ObjectTag>> entryList = new ArrayList<>(object.map.entrySet());
            final NaturalOrderComparator comparator = new NaturalOrderComparator();
            final String tag = attribute.hasContext(1) ? attribute.getRawContext(1) : null;
            try {
                Collections.sort(entryList, new Comparator<Map.Entry<StringHolder, ObjectTag>>() {
                    @Override
                    public int compare(Map.Entry<StringHolder, ObjectTag> e1, Map.Entry<StringHolder, ObjectTag> e2) {
                        ObjectTag o1 = e1.getValue();
                        ObjectTag o2 = e2.getValue();
                        if (tag != null) {
                            o1 = CoreUtilities.autoAttribTyped(o1, new Attribute(tag, attribute.getScriptEntry(), attribute.context));
                            o2 = CoreUtilities.autoAttribTyped(o2, new Attribute(tag, attribute.getScriptEntry(), attribute.context));
                        }
                        return comparator.compare(o1, o2);
                    }
                });
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            MapTag output = new MapTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : entryList) {
                output.map.put(entry.getKey(), entry.getValue());
            }
            return output;
        });

        // <--[tag]
        // @attribute <MapTag.filter_tag[<parseable-boolean>]>
        // @returns MapTag
        // @description
        // returns a copy of the map with all its contents parsed through the given input tag and only including ones that returned 'true'.
        // This requires a fully formed tag as input, making use of the 'filter_key' and 'filter_value' definition.
        // For example: a map of 'a/1|b/2|c/3|d/4|e/5' .filter_tag[<[filter_value].is[or_more].than[3]>] returns a list of 'c/3|d/4|e/5'.
        // -->
        registerTag("filter_tag", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("Must have input to filter_tag[...]");
                return null;
            }
            MapTag newMap = new MapTag();
            Attribute.OverridingDefinitionProvider provider = new Attribute.OverridingDefinitionProvider(attribute.context.definitionProvider);
            try {
                for (Map.Entry<StringHolder, ObjectTag> entry : object.map.entrySet()) {
                    provider.altDefs.put("filter_key", new ElementTag(entry.getKey().str));
                    provider.altDefs.put("filter_value", entry.getValue());
                    if (CoreUtilities.equalsIgnoreCase(attribute.parseDynamicContext(1, provider).toString(), "true")) {
                        newMap.map.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            return newMap;
        });

        // <--[tag]
        // @attribute <MapTag.parse_value_tag[<parseable-value>]>
        // @returns MapTag
        // @description
        // returns a copy of the map with all its values updated through the given tag.
        // This requires a fully formed tag as input, making use of the 'parse_key' and 'parse_value' definition.
        // For example: a map of 'alpha/one|bravo/two' .parse_value_tag[<[parse_value].to_uppercase>] returns a map of 'alpha/ONE|bravo/TWO'.
        // -->
        registerTag("parse_value_tag", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("Must have input to parse_value_tag[...]");
                return null;
            }
            MapTag newMap = new MapTag();
            Attribute.OverridingDefinitionProvider provider = new Attribute.OverridingDefinitionProvider(attribute.context.definitionProvider);
            try {
                for (Map.Entry<StringHolder, ObjectTag> entry : object.map.entrySet()) {
                    provider.altDefs.put("parse_key", new ElementTag(entry.getKey().str));
                    provider.altDefs.put("parse_value", entry.getValue());
                    newMap.map.put(entry.getKey(), attribute.parseDynamicContext(1, provider));
                }
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            return newMap;
        });

        // <--[tag]
        // @attribute <MapTag.contains[<key>|...]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the map contains the specified key.
        // If a list is given as input, returns whether the map contains all of the specified keys.
        // -->
        registerTag("contains", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.contains' must have an input value.");
                return null;
            }
            if (attribute.getContext(1).contains("|")) {
                ListTag keyList = attribute.getContextObject(1).asType(ListTag.class, attribute.context);
                boolean contains = true;
                for (String key : keyList) {
                    if (object.getObject(key) == null) {
                        contains = false;
                        break;
                    }
                }
                return new ElementTag(contains);
            }
            return new ElementTag(object.getObject(attribute.getContext(1)) != null);
        });

        // <--[tag]
        // @attribute <MapTag.get[<key>|...]>
        // @returns ObjectTag
        // @description
        // Returns the object value at the specified key.
        // If a list is given as input, returns a list of values.
        // For example, on a map of "a/1|b/2|c/3|", using ".get[b]" will return "2".
        // For example, on a map of "a/1|b/2|c/3|", using ".get[b|c]" will return a list of "2|3".
        // -->
        registerTag("get", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.get' must have an input value.");
                return null;
            }
            if (attribute.getContext(1).contains("|")) {
                ListTag keyList = attribute.getContextObject(1).asType(ListTag.class, attribute.context);
                ListTag valList = new ListTag();
                for (String key : keyList) {
                    valList.addObject(object.getObject(key));
                }
                return valList;
            }
            return object.getObject(attribute.getContext(1));
        });

        // <--[tag]
        // @attribute <MapTag.deep_get[<key>|...]>
        // @returns ObjectTag
        // @description
        // Returns the object value at the specified key, using deep key paths separated by the '.' symbol.
        // This means if you have a MapTag with key 'root' set to the value of a second MapTag (with key 'leaf' as "myvalue"),
        // then ".deep_get[root.leaf]" will return "myvalue".
        // If a list is given as input, returns a list of values.
        // -->
        registerTag("deep_get", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.deep_get' must have an input value.");
                return null;
            }
            if (attribute.getContext(1).contains("|")) {
                ListTag keyList = attribute.getContextObject(1).asType(ListTag.class, attribute.context);
                ListTag valList = new ListTag();
                for (String key : keyList) {
                    valList.addObject(object.getDeepObject(key));
                }
                return valList;
            }
            return object.getDeepObject(attribute.getContext(1));
        });

        // <--[tag]
        // @attribute <MapTag.get_subset[<key>|...]>
        // @returns MapTag
        // @description
        // Returns the subset of the map represented by the given keys, ordered based on the input list.
        // For example, on a map of "a/1|b/2|c/3|", using ".get_subset[b|a]" will return "b/2|a/1|".
        // Keys that aren't present in the original map will be ignored.
        // -->
        registerTag("get_subset", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.get_subset' must have an input value.");
                return null;
            }
            ListTag keys = ListTag.getListFor(attribute.getContextObject(1), attribute.context);
            MapTag output = new MapTag();
            for (String key : keys) {
                StringHolder keyHolder = new StringHolder(key);
                ObjectTag value = object.map.get(keyHolder);
                if (value != null) {
                    output.map.put(keyHolder, value);
                }
            }
            return output;
        });

        // <--[tag]
        // @attribute <MapTag.default[<key>].as[<value>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map, with the specified key defaulted to the specified value.
        // If the map does not already have the specified key, this is equivalent to the 'with[key].as[value]' tag.
        // If the map already has the specified key, this will return the original map, unmodified.
        // For example, on a map of "a/1|b/2|c/3|", using ".default[d].as[4]" will return "a/1|b/2|c/3|d/4|".
        // For example, on a map of "a/1|b/2|c/3|", using ".default[c].as[4]" will return "a/1|b/2|c/3|".
        // -->
        registerTag("default", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.default' must have an input value.");
                return null;
            }
            String key = attribute.getContext(1);
            attribute.fulfill(1);
            if (!attribute.matches("as")) {
                attribute.echoError("The tag 'MapTag.default' must be followed by '.as'.");
                return null;
            }
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.default.as' must have an input value for 'as'.");
                return null;
            }
            if (object.map.containsKey(new StringHolder(key))) {
                return object;
            }
            ObjectTag value = attribute.getContextObject(1);
            MapTag result = object.duplicate();
            result.putObject(key, value);
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.deep_with[<key>].as[<value>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map, with the specified key set to the specified value, using deep key paths separated by the '.' symbol.
        // This means for example if you use "deep_with[root.leaf].as[myvalue]", you will have the key 'root' set to the value of a second MapTag (with key 'leaf' as "myvalue").
        // -->
        registerTag("deep_with", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.deep_with' must have an input value.");
                return null;
            }
            String key = attribute.getContext(1);
            attribute.fulfill(1);
            if (!attribute.matches("as")) {
                attribute.echoError("The tag 'MapTag.deep_with' must be followed by '.as'.");
                return null;
            }
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.deep_with.as' must have an input value for 'as'.");
                return null;
            }
            ObjectTag value = attribute.getContextObject(1);
            MapTag result = object.duplicate();
            result.putDeepObject(key, value);
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.with[<key>].as[<value>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map, with the specified key set to the specified value.
        // For example, on a map of "a/1|b/2|c/3|", using ".with[d].as[4]" will return "a/1|b/2|c/3|d/4|".
        // Matching keys will be overridden. For example, on a map of "a/1|b/2|c/3|", using ".with[c].as[4]" will return "a/1|b/2|c/4|".
        // -->
        registerTag("with", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.with' must have an input value.");
                return null;
            }
            String key = attribute.getContext(1);
            attribute.fulfill(1);
            if (!attribute.matches("as")) {
                attribute.echoError("The tag 'MapTag.with' must be followed by '.as'.");
                return null;
            }
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.with.as' must have an input value for 'as'.");
                return null;
            }
            ObjectTag value = attribute.getContextObject(1);
            MapTag result = object.duplicate();
            result.putObject(key, value);
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.invert>
        // @returns MapTag
        // @description
        // Returns an inverted copy of the map. That is, keys become values and values become keys.
        // For example, on a map of "a/1|b/2|c/3|", using "invert" will return "1/a|2/b|3/c|".
        // All values in the result will be ElementTags.
        // Note that the size of the result is not guaranteed to be the same as the input (as duplicate keys are not allowed, but duplicate values are).
        // In the case of duplicate new-keys, the last instance of the new-key will be preserved.
        // For example, on a map of "a/1|b/2|c/2|", using "invert" will return "1/a|2/c|".
        // -->
        registerTag("invert", (attribute, object) -> {
            MapTag result = new MapTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : object.map.entrySet()) {
                result.map.put(new StringHolder(entry.getValue().identify()), new ElementTag(entry.getKey().str));
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.exclude[<key>|...]>
        // @returns MapTag
        // @description
        // Returns a copy of the map with the specified key(s) excluded.
        // For example, on a map of "a/1|b/2|c/3|", using ".exclude[b]" will return "a/1|c/3|".
        // -->
        registerTag("exclude", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.exclude' must have an input value.");
                return null;
            }
            MapTag result = object.duplicate();
            for (String key : ListTag.getListFor(attribute.getContextObject(1), attribute.context)) {
                result.map.remove(new StringHolder(key));
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.include[<map>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map with the specified map's contents copied in.
        // For example, on a map of "a/1|b/2|c/3|", using ".include[d/4|e/5|]" will return "a/1|b/2|c/3|d/4|e/5|".
        // Matching keys will be overridden. For example, on a map of "a/1|b/2|c/3|", using ".include[b/4|c/5|]" will return "a/1|b/4|c/5|".
        // -->
        registerTag("include", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.include' must have an input value.");
                return null;
            }
            MapTag result = object.duplicate();
            result.map.putAll(getMapFor(attribute.getContextObject(1), attribute.context).map);
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.keys>
        // @returns ListTag
        // @description
        // Returns a list of all keys in this map.
        // For example, on a map of "a/1|b/2|c/3|", using "list_keys" will return "a|b|c|".
        // -->
        registerTag("keys", (attribute, object) -> {
            ListTag result = new ListTag();
            for (StringHolder entry : object.map.keySet()) {
                result.add(entry.str);
            }
            return result;
        }, "list_keys");

        // <--[tag]
        // @attribute <MapTag.values>
        // @returns ListTag
        // @description
        // Returns a list of all values in this map.
        // For example, on a map of "a/1|b/2|c/3|", using "list_values" will return "1|2|3|".
        // -->
        registerTag("values", (attribute, object) -> {
            ListTag result = new ListTag();
            for (ObjectTag entry : object.map.values()) {
                result.addObject(entry);
            }
            return result;
        }, "list_values");

        // <--[tag]
        // @attribute <MapTag.to_pair_lists>
        // @returns ListTag
        // @description
        // Returns a list of all key/value pairs in this map, where each entry in the list is itself a list with 2 entries: the key, then the value.
        // -->
        registerTag("to_pair_lists", (attribute, object) -> {
            ListTag result = new ListTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : object.map.entrySet()) {
                ListTag pair = new ListTag();
                pair.add(entry.getKey().str);
                pair.addObject(entry.getValue());
                result.addObject(pair);
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.to_list>
        // @returns ListTag
        // @description
        // Returns a list of all key/value pairs in this map.
        // Note that slash ('/') escaping will be lost, so maps that have slashes in their keys will not be possible to convert back to a map.
        // -->
        registerTag("to_list", (attribute, object) -> {
            ListTag result = new ListTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : object.map.entrySet()) {
                result.add(entry.getKey().str + "/" + entry.getValue().identify());
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.to_json>
        // @returns ElementTag
        // @description
        // Returns a JSON encoding of this map.
        // -->
        registerTag("to_json", (attribute, object) -> {
            return new ElementTag(new JSONObject((Map) CoreUtilities.objectTagToJavaForm(object.duplicate(), false)).toString());
        });

        // <--[tag]
        // @attribute <MapTag.to_yaml>
        // @returns ElementTag
        // @description
        // Returns a YAML encoding of this map.
        // -->
        registerTag("to_yaml", (attribute, object) -> {
            YamlConfiguration output = new YamlConfiguration();
            output.contents = (Map) CoreUtilities.objectTagToJavaForm(object.duplicate(), true);
            return new ElementTag(output.saveToString(false));
        });
    }

    public static ObjectTagProcessor<MapTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectInterface<MapTag> runnable, String... variants) {
        tagProcessor.registerTag(name, runnable, variants);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("MapTags can not hold properties.");
    }

    @Override
    public void adjust(Mechanism mechanism) {
        CoreUtilities.autoPropertyMechanism(this, mechanism);
    }
}
