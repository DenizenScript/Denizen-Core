package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.LinkedHashMap;
import java.util.Map;

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
    // This is a subset of Denizen standard escaping, see <@link language property escaping>.
    //
    // -->

    public static String escapeEntry(String value) {
        return value.replace("&", "&amp").replace("|", "&pipe").replace("/", "&fs");
    }

    public static String unescapeEntry(String value) {
        return value.replace("&fs", "/").replace("&pipe", "|").replace("&amp", "&");
    }

    public static MapTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("map")
    public static MapTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }

        if (string.startsWith("map@") && string.length() > "map@".length()) {
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
            result.map.put(new StringHolder(unescapeEntry(key)), ObjectFetcher.pickObjectFor(unescapeEntry(value), context));
            lastPipe = pipe + 1;
            pipe = string.indexOf('|', lastPipe);
        }

        return result;
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
        return "map";
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
            output.append(escapeEntry(entry.getKey().str)).append("/").append(escapeEntry(entry.getValue().identify())).append("|");
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
        // @attribute <MapTag.get[<key>]>
        // @returns ObjectTag
        // @description
        // Returns the object value at the specified key.
        // For example, on a map of "a/1|b/2|c/3|", using ".get[b]" will return "2".
        // -->
        registerTag("get", (attribute, object) -> {
            if (!attribute.hasContext(1)) {
                attribute.echoError("The tag 'MapTag.get' must have an input value.");
                return null;
            }
            return object.map.get(new StringHolder(attribute.getContext(1)));
        });

        // <--[tag]
        // @attribute <MapTag.with[<key>].as[<value>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map, with the specified key set to the specified value.
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
            result.map.put(new StringHolder(key), value);
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.invert>
        // @returns MapTag
        // @description
        // Returns an inverted copy of the map. That is, keys become values and values become keys.
        // Note that the size of the result is not guaranteed to be the same as the input (as duplicate keys are not allowed, but duplicate values are).
        // For example, on a map of "a/1|b/2|c/3|", using "invert" will return "1/a|2/b|3/c|".
        // All values in the result will be ElementTags.
        // -->
        registerTag("invert", (attribute, object) -> {
            MapTag result = new MapTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : object.map.entrySet()) {
                result.map.put(new StringHolder(entry.getValue().identify()), new ElementTag(entry.getKey().str));
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.list_keys>
        // @returns ListTag
        // @description
        // Returns an list of all keys in this map.
        // For example, on a map of "a/1|b/2|c/3|", using "list_keys" will return "a|b|c|".
        // -->
        registerTag("list_keys", (attribute, object) -> {
            ListTag result = new ListTag();
            for (StringHolder entry : object.map.keySet()) {
                result.add(entry.str);
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.list_values>
        // @returns ListTag
        // @description
        // Returns an list of all values this map.
        // For example, on a map of "a/1|b/2|c/3|", using "list_values" will return "1|2|3|".
        // -->
        registerTag("list_values", (attribute, object) -> {
            ListTag result = new ListTag();
            for (ObjectTag entry : object.map.values()) {
                result.addObject(entry);
            }
            return result;
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
