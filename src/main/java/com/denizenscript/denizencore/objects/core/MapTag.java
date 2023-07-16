package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.exceptions.TagProcessingException;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.NaturalOrderComparator;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import org.json.JSONObject;

import java.util.*;
import java.util.function.Supplier;

public class MapTag implements ObjectTag {

    // NOTE: Explicitly no example value
    // <--[ObjectType]
    // @name MapTag
    // @prefix map
    // @base ElementTag
    // @ExampleTagBase map[key=value]
    // @ExampleForReturns
    // - foreach %VALUE% key:key as:val:
    //     - narrate "<[key]> is set as <[val]>"
    // @format
    // The identity format for MapTags is a replica of property syntax - square brackets surrounded a semi-colon separated list of key=value pairs.
    // For example, a map of "taco" to "food", "chicken" to "animal", and "bob" to "person" would be "map@[taco=food;chicken=animal;bob=person]"
    // A map with zero items in it is simply 'map@[]'.
    //
    // If the semicolon symbol ";" appears in a key or value, it will be replaced by "&sc", an equal sign "=" will become "&eq",
    // a left bracket "[" will become "&lb", a right bracket "]" will become "&rb", and an ampersand "&" will become "&amp".
    // This is a subset of Denizen standard escaping, see <@link language Escaping System>.
    //
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
    // -->

    public static String unescapeLegacyEntry(String value) {
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
        return valueOf(string, context, true);
    }

    public static MapTag valueOf(String string, TagContext context, boolean processValues) {
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
        if (string.endsWith("|")) {
            int pipe = string.indexOf('|');
            int lastPipe = 0;
            while (pipe != -1) {
                int slash = string.indexOf('/', lastPipe);
                if (slash == -1 || slash > pipe) {
                    return null;
                }
                String key = string.substring(lastPipe, slash);
                String value = string.substring(slash + 1, pipe);
                result.putObject(unescapeLegacyEntry(key), ObjectFetcher.pickObjectFor(unescapeLegacyEntry(value), context));
                lastPipe = pipe + 1;
                pipe = string.indexOf('|', lastPipe);
            }
            return result;
        }
        boolean hasBrackets = string.startsWith("[") && string.endsWith("]");
        if (!hasBrackets && string.contains("=")) {
            string = "[" + string + "]";
            hasBrackets = true;
        }
        if (hasBrackets) {
            if (string.equals("[]")) {
                return result;
            }
            List<String> properties = ObjectFetcher.separateProperties(string);
            for (int i = 1; i < properties.size(); i++) {
                List<String> data = CoreUtilities.split(properties.get(i), '=', 2);
                if (data.size() != 2) {
                    if (context == null || context.showErrors()) {
                        Debug.echoError("Invalid map key=value pair string '" + properties.get(i) + "' for map input '" + string + "'!");
                    }
                    return null;
                }
                String rawVal = ObjectFetcher.unescapeProperty(data.get(1));
                ObjectTag val = processValues ? ObjectFetcher.pickObjectFor(rawVal, context) : new ElementTag(rawVal);
                result.putObject(ObjectFetcher.unescapeProperty(data.get(0)), val);
            }
            return result;
        }
        return null;
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
        for (Map.Entry<StringHolder, ObjectTag> entry : entrySet()) {
            newMap.putObject(entry.getKey(), entry.getValue().duplicate());
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
    public boolean isTruthy() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public int size() {
        return map.size();
    }

    public Set<StringHolder> keySet() {
        return map.keySet();
    }

    public Set<Map.Entry<StringHolder, ObjectTag>> entrySet() {
        return map.entrySet();
    }

    public Collection<ObjectTag> values() {
        return map.values();
    }

    @Override
    public String debuggable() {
        if (isEmpty()) {
            return "map@";
        }
        StringBuilder debugText = new StringBuilder();
        debugText.append("<LG>map@[<Y>");
        for (Map.Entry<StringHolder, ObjectTag> entry : entrySet()) {
            debugText.append(entry.getKey().str).append(" <LG>=<Y> ").append(entry.getValue().debuggable()).append("<LG>;<Y> ");
        }
        debugText.setLength(debugText.length() - "<LG>;<Y> ".length());
        debugText.append("<LG>]");
        return debugText.toString();
    }

    @Override
    public String identify() {
        if (isEmpty()) {
            return "map@[]";
        }
        StringBuilder output = new StringBuilder();
        output.append("map@[");
        for (Map.Entry<StringHolder, ObjectTag> entry : entrySet()) {
            output.append(PropertyParser.escapePropertyKey(entry.getKey().str)).append("=").append(PropertyParser.escapePropertyValue(entry.getValue().savable())).append(";");
        }
        output.setLength(output.length() - 1);
        output.append(']');
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

    @Override
    public Object getJavaObject() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<StringHolder, ObjectTag> pair : entrySet()) {
            result.put(pair.getKey().str, pair.getValue().getJavaObject());
        }
        return result;
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

    public ObjectTag getObject(String key, Supplier<ObjectTag> defaultGetter) {
        ObjectTag object = getDeepObject(key);
        if (object == null) {
            return defaultGetter == null ? null : defaultGetter.get();
        }
        return object.refreshState();
    }

    public <T extends ObjectTag> T getObjectAs(String key, Class<T> type, TagContext context) {
        return getObjectAs(key, type, context, null);
    }

    public <T extends ObjectTag> T getRequiredObjectAs(String key, Class<T> type, Attribute attribute) {
        T result = getObjectAs(key, type, attribute.context, null);
        if (result == null) {
            attribute.echoError("Invalid tag input - missing required key '" + key + "'");
        }
        return result;
    }

    public <T extends ObjectTag> T getObjectAs(String key, Class<T> type, TagContext context, Supplier<T> defaultGetter) {
        ObjectTag object = getDeepObject(key);
        if (object == null) {
            return defaultGetter == null ? null : defaultGetter.get();
        }
        return object.asType(type, context);
    }

    public ElementTag getElement(String key) {
        return getElement(key, null);
    }

    public ElementTag getElement(String key, String defaultValue) {
        ObjectTag object = getDeepObject(key);
        if (object == null) {
            return defaultValue == null ? null : new ElementTag(defaultValue);
        }
        return object.asElement();
    }

    public ObjectTag getObject(String key) {
        return map.get(new StringHolder(key));
    }

    public ObjectTag getObject(StringHolder key) {
        return map.get(key);
    }

    public boolean containsKey(String key) {
        return map.containsKey(new StringHolder(key));
    }

    public boolean containsKey(StringHolder key) {
        return map.containsKey(key);
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
                if (value == null) {
                    return;
                }
                subValue = new MapTag();
                current.putObject(subkeys.get(i), subValue);
            }
            current = (MapTag) subValue;
        }
        current.putObject(subkeys.get(subkeys.size() - 1), value);
    }

    public void putObject(String key, ObjectTag value) {
        if (value == null) {
            remove(key);
        }
        else {
            map.put(new StringHolder(key), value);
        }
    }

    public void putObject(StringHolder key, ObjectTag value) {
        if (value == null) {
            remove(key);
        }
        else {
            map.put(key, value);
        }
    }

    public void remove(String key) {
        map.remove(new StringHolder(key));
    }

    public void remove(StringHolder key) {
        map.remove(key);
    }

    public ListTag keys() {
        return new ListTag(map.keySet(), stringHolder -> new ElementTag(stringHolder.str, true));
    }

    public void putAll(MapTag otherMap) {
        map.putAll(otherMap.map);
    }

    public static void register() {

        // <--[tag]
        // @attribute <MapTag.size>
        // @returns ElementTag(Number)
        // @description
        // Returns the size of the map - that is, how many key/value pairs are within it.
        // @example
        // # Narrates '2'
        // - narrate <map[a=1;b=2].size>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "size", (attribute, object) -> {
            return new ElementTag(object.size());
        });

        // <--[tag]
        // @attribute <MapTag.is_empty>
        // @returns ElementTag(Boolean)
        // @description
        // Returns "true" if the map is empty (contains no keys), otherwise "false".
        // @example
        // - if <map[a=1;b=2].is_empty>:
        //     - narrate "This won't show"
        // - else:
        //     - narrate "This will show! The map has stuff in it!"
        // @example
        // - if <map.is_empty>:
        //     - narrate "This will show! That map is empty!"
        // - else:
        //     - narrate "This won't show"
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_empty", (attribute, object) -> {
            return new ElementTag(object.isEmpty());
        });

        // <--[tag]
        // @attribute <MapTag.any>
        // @returns ElementTag(Boolean)
        // @description
        // Returns "true" if the map contains any keys, or "false" if it is empty.
        // @example
        // - if <map[a=1;b=2].any>:
        //     - narrate "This will show! The map has stuff in it!"
        // - else:
        //     - narrate "This won't show"
        // @example
        // - if <map.any>:
        //     - narrate "This won't show"
        // - else:
        //     - narrate "This will show! That map is empty!"
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "any", (attribute, object) -> {
            return new ElementTag(!object.isEmpty());
        });

        // <--[tag]
        // @attribute <MapTag.sort_by_value[(<tag>)]>
        // @returns MapTag
        // @description
        // Returns a copy of the map, sorted alphanumerically by the value under each key.
        // Optionally, specify a tag to apply to the value.
        // To sort by key, use <@link tag MapTag.get_subset> with list sort tags, like 'map.get_subset[map.keys.sort_by_value[...]]'.
        // This also lets you apply list filters or similar to the keyset.
        // To apply a '.parse' to the values, use <@link tag ListTag.map_with>, like 'map.keys.map_with[map.values.parse[...]]'
        // @example
        // # Narrates a map of [a=1;b=2;c=3]
        // - narrate <map[c=3;a=1;b=2].sort_by_value>
        // @example
        // # Narrates a map of [c=3;b=2;a=1]
        // - narrate <map[c=3;a=1;b=2].sort_by_value[mul[-1]]>
        // -->
        tagProcessor.registerTag(MapTag.class, "sort_by_value", (attribute, object) -> {
            ArrayList<Map.Entry<StringHolder, ObjectTag>> entryList = new ArrayList<>(object.entrySet());
            final NaturalOrderComparator comparator = new NaturalOrderComparator();
            final String tag = attribute.hasParam() ? attribute.getRawParam() : null;
            Attribute subAttribute;
            try {
                subAttribute = tag == null ? null : new Attribute(tag, attribute.getScriptEntry(), attribute.context);
            }
            catch (TagProcessingException ex) {
                attribute.echoError("Tag processing failed: " + ex.getMessage());
                return null;
            }
            try {
                entryList.sort((e1, e2) -> {
                    ObjectTag o1 = e1.getValue();
                    ObjectTag o2 = e2.getValue();
                    if (tag != null) {
                        o1 = CoreUtilities.autoAttribTyped(o1, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                        o2 = CoreUtilities.autoAttribTyped(o2, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                    }
                    return comparator.compare(o1, o2);
                });
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            MapTag output = new MapTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : entryList) {
                output.putObject(entry.getKey(), entry.getValue());
            }
            return output;
        });

        // <--[tag]
        // @attribute <MapTag.filter_tag[<parseable-boolean>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map with all its contents parsed through the given input tag and only including ones that returned 'true'.
        // This requires a fully formed tag as input, making use of the 'filter_key' and 'filter_value' definition.
        // @example
        // # Narrates a map of '[c=3;d=4;e=5]'
        // - narrate <map[a=1;b=2;c=3;d=4;e=5].filter_tag[<[filter_value].is[or_more].than[3]>]>
        // -->
        tagProcessor.registerTag(MapTag.class, "filter_tag", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("Must have input to filter_tag[...]");
                return null;
            }
            MapTag newMap = new MapTag();
            Attribute.OverridingDefinitionProvider provider = new Attribute.OverridingDefinitionProvider(attribute.context.definitionProvider);
            try {
                for (Map.Entry<StringHolder, ObjectTag> entry : object.entrySet()) {
                    provider.altDefs.putObject("filter_key", new ElementTag(entry.getKey().str));
                    provider.altDefs.putObject("filter_value", entry.getValue());
                    if (CoreUtilities.equalsIgnoreCase(attribute.parseDynamicParam(provider).toString(), "true")) {
                        newMap.putObject(entry.getKey(), entry.getValue());
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
        // Returns a copy of the map with all its values updated through the given tag.
        // This requires a fully formed tag as input, making use of the 'parse_key' and 'parse_value' definition.
        // @example
        // # Narrates a map of '[alpha=ONE;bravo=TWO]'
        // - narrate <map[alpha=one;bravo=two].parse_value_tag[<[parse_value].to_uppercase>]>
        // -->
        tagProcessor.registerTag(MapTag.class, "parse_value_tag", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("Must have input to parse_value_tag[...]");
                return null;
            }
            MapTag newMap = new MapTag();
            Attribute.OverridingDefinitionProvider provider = new Attribute.OverridingDefinitionProvider(attribute.context.definitionProvider);
            try {
                for (Map.Entry<StringHolder, ObjectTag> entry : object.entrySet()) {
                    provider.altDefs.putObject("parse_key", new ElementTag(entry.getKey().str));
                    provider.altDefs.putObject("parse_value", entry.getValue());
                    newMap.putObject(entry.getKey(), attribute.parseDynamicParam(provider));
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
        // @example
        // - if <map[a=1;b=2].contains[a]>:
        //     - narrate "Yep it sure does have 'a' as a key!"
        // @example
        // - if <map[a=1;b=2].contains[c]>:
        //     - narrate "This won't show"
        // - else:
        //     - narrate "No it doesn't have 'c'"
        // @example
        // # Narrates 'true'
        // - narrate <map[a=1;b=2].contains[a|b]>
        // @example
        // # Narrates 'false'
        // - narrate <map[a=1;b=2].contains[a|b|c]>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "contains", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.contains' must have an input value.");
                return null;
            }
            if (attribute.getParam().contains("|")) {
                ListTag keyList = attribute.getParamObject().asType(ListTag.class, attribute.context);
                boolean contains = true;
                for (String key : keyList) {
                    if (object.getObject(key) == null) {
                        contains = false;
                        break;
                    }
                }
                return new ElementTag(contains);
            }
            return new ElementTag(object.getObject(attribute.getParam()) != null);
        });

        // <--[tag]
        // @attribute <MapTag.get[<key>|...]>
        // @returns ObjectTag
        // @description
        // Returns the object value at the specified key.
        // If a list is given as input, returns a list of values.
        // @example
        // # Narrates '2'
        // - narrate <map[a=1;b=2;c=3].get[b]>
        // @example
        // # Demonstrates that list input gives list output - narrates '2' then '3'
        // - foreach <map[a=1;b=2;c=3].get[b|c]> as:value:
        //     - narrate "One of the values is <[value]>"
        // -->
        tagProcessor.registerStaticTag(ObjectTag.class, "get", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.get' must have an input value.");
                return null;
            }
            if (attribute.getParam().contains("|")) {
                return new ListTag(attribute.paramAsType(ListTag.class), object::getObject);
            }
            return object.getObject(attribute.getParam());
        });

        // <--[tag]
        // @attribute <MapTag.deep_get[<key>|...]>
        // @returns ObjectTag
        // @description
        // Returns the object value at the specified key, using deep key paths separated by the '.' symbol.
        // If a list is given as input, returns a list of values.
        // @example
        // # Narrates 'myvalue'
        // - narrate <map.with[root].as[<map[leaf=myvalue]>].deep_get[root.leaf]>
        // @example
        // # Narrates 'myvalue'
        // - definemap mymap:
        //     root:
        //         leaf: myvalue
        // - narrate <[mymap].deep_get[root.leaf]>
        // # The below will also get the same result ('myvalue') using the definition tag's special automatic deep get syntax:
        // - narrate <[mymap.root.leaf]>
        // -->
        TagRunnable.ObjectInterface<MapTag, ObjectTag> deepGetRunnable = (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.deep_get' must have an input value.");
                return null;
            }
            if (attribute.getParam().contains("|")) {
                return new ListTag(attribute.paramAsType(ListTag.class), object::getDeepObject);
            }
            return object.getDeepObject(attribute.getParam());
        };
        tagProcessor.registerStaticTag(ObjectTag.class, "deep_get", deepGetRunnable);
        tagProcessor.registerStaticTag(ObjectTag.class, "", deepGetRunnable);

        // <--[tag]
        // @attribute <MapTag.get_subset[<key>|...]>
        // @returns MapTag
        // @description
        // Returns the subset of the map represented by the given keys, ordered based on the input list.
        // Keys that aren't present in the original map will be ignored.
        // @example
        // # Narrates a map of '[b=2;a=1]'
        // - narrate <map[a=1;b=2;c=3].get_subset[b|a]>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, ListTag.class, "get_subset", (attribute, object, keys) -> {
            MapTag output = new MapTag();
            for (String key : keys) {
                StringHolder keyHolder = new StringHolder(key);
                ObjectTag value = object.getObject(keyHolder);
                if (value != null) {
                    output.putObject(keyHolder, value);
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
        // @example
        // # Narrates a map of '[a=1;b=2;c=3;d=4]'
        // - narrate <map[a=1;b=2;c=3].default[d].as[4]>
        // @example
        // # Demonstrates matching keys not being replaced - narrates a map of '[a=1;b=2;c=3]'
        // - narrate <map[a=1;b=2;c=3].default[c].as[4]>
        // -->
        tagProcessor.registerTag(MapTag.class, "default", (attribute, object) -> { // Non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.default' must have an input value.");
                return null;
            }
            String key = attribute.getParam();
            attribute.fulfill(1);
            if (!attribute.matches("as")) {
                attribute.echoError("The tag 'MapTag.default' must be followed by '.as'.");
                return null;
            }
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.default.as' must have an input value for 'as'.");
                return null;
            }
            if (object.containsKey(key)) {
                return object;
            }
            ObjectTag value = attribute.getParamObject();
            MapTag result = object.duplicate();
            result.putObject(key, value);
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.deep_with[<key>].as[<value>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map, with the specified key set to the specified value, using deep key paths separated by the '.' symbol.
        // @example
        // # Narrates a map of '[root=[leaf=myvalue]]', such that <[that].get[root]> itself returns a map of '[leaf=myvalue]'
        // - narrate <map.deep_with[root.leaf].as[myvalue]>
        // -->
        tagProcessor.registerTag(MapTag.class, "deep_with", (attribute, object) -> { // Non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.deep_with' must have an input value.");
                return null;
            }
            String key = attribute.getParam();
            attribute.fulfill(1);
            if (!attribute.matches("as")) {
                attribute.echoError("The tag 'MapTag.deep_with' must be followed by '.as'.");
                return null;
            }
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.deep_with.as' must have an input value for 'as'.");
                return null;
            }
            ObjectTag value = attribute.getParamObject();
            MapTag result = object.duplicate();
            result.putDeepObject(key, value);
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.with[<key>].as[<value>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map, with the specified key set to the specified value.
        // Matching keys will be overridden.
        // @example
        // # Narrates a map of '[a=1;b=2;c=3;d=4]'
        // - narrate <map[a=1;b=2;c=3].with[d].as[4]>
        // @example
        // # Demonstrates matching key overriding - narrates a map of '[a=1;b=2;c=4]'
        // - narrate <map[a=1;b=2;c=3].with[c].as[4]>
        // -->
        tagProcessor.registerTag(MapTag.class, "with", (attribute, object) -> { // Non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.with' must have an input value.");
                return null;
            }
            String key = attribute.getParam();
            attribute.fulfill(1);
            if (!attribute.matches("as")) {
                attribute.echoError("The tag 'MapTag.with' must be followed by '.as'.");
                return null;
            }
            if (!attribute.hasParam()) {
                attribute.echoError("The tag 'MapTag.with.as' must have an input value for 'as'.");
                return null;
            }
            ObjectTag value = attribute.getParamObject();
            MapTag result = object.duplicate();
            result.putObject(key, value);
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.invert>
        // @returns MapTag
        // @description
        // Returns an inverted copy of the map. That is, keys become values and values become keys.
        // All values in the result will be ElementTags.
        // Note that the size of the result is not guaranteed to be the same as the input (as duplicate keys are not allowed, but duplicate values are).
        // In the case of duplicate new-keys, the last instance of the new-key will be preserved.
        // @example
        // # Narrates a map of '[1=a;2=b;3=c]'
        // - narrate <map[a=1;b=2;c=3].invert>
        // @example
        // # Demonstrates how duplicate values in the input become a single key in the output - narrates a map of '[1=a;2=c]'
        // - narrate <map[a=1;b=2;c=2].invert>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, "invert", (attribute, object) -> {
            MapTag result = new MapTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : object.entrySet()) {
                result.putObject(new StringHolder(entry.getValue().identify()), new ElementTag(entry.getKey().str));
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.reverse>
        // @returns MapTag
        // @description
        // Returns a reversed copy of the map. That is, the last key becomes the first key and vice-versa, akin to <@link tag ListTag.reverse>
        // Not to be confused with <@link tag MapTag.invert>
        // @example
        // # Narrates a map of '[c=3;b=2;a=1]'
        // - narrate <map[a=1;b=2;c=3].reverse>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, "reverse", (attribute, object) -> {
            ArrayList<Map.Entry<StringHolder, ObjectTag>> entries = new ArrayList<>(object.entrySet());
            Collections.reverse(entries);
            MapTag result = new MapTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : entries) {
                result.putObject(entry.getKey(), entry.getValue());
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.deep_exclude[<key>|...]>
        // @returns MapTag
        // @description
        // Returns a copy of the map with the specified deep key(s) excluded.
        // @example
        // - definemap mymap:
        //     root:
        //         first: kept
        //         second: lost
        // # Will narrate the initial map of '[root=[first=kept;second=lost]]'
        // - narrate <[mymap]>
        // # Demonstrates deep_exclude of 'second', narrating a new map of '[root=[first=kept]]' without 'second' in it
        // - narrate <[mymap].deep_exclude[root.second]>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, ListTag.class, "deep_exclude", (attribute, object, list) -> {
            MapTag result = object.duplicate();
            for (String key : list) {
                result.putDeepObject(key, null);
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.exclude[<key>|...]>
        // @returns MapTag
        // @description
        // Returns a copy of the map with the specified key(s) excluded.
        // @example
        // # Narrates a map of '[a=1;c=3]'
        // - narrate <map[a=1;b=2;c=3].exclude[b]>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, ListTag.class, "exclude", (attribute, object, list) -> {
            MapTag result = object.duplicate();
            for (String key : list) {
                result.remove(key);
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.include[<map>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map with the specified map's contents copied in.
        // Matching keys will be overridden.
        // @example
        // # Narrates a map of '[a=1;b=2;c=3;d=4;e=5]'
        // - narrate <map[a=1;b=2;c=3].include[d=4;e=5]>
        // @example
        // # Demonstrates matching keys overriding - Narrates a map of '[a=1;b=4;c=5]'
        // - narrate <map[a=1;b=2;c=3].include[b=4;c=5]>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, MapTag.class, "include", (attribute, object, second) -> {
            MapTag result = object.duplicate();
            result.putAll(second.duplicate());
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.deep_keys>
        // @returns ListTag
        // @description
        // Returns a list of all keys in this map, including keys in any sub-maps (map values that are in turn MapTags), using deep key paths separated by the '.' symbol.
        // No returned key value will refer to a MapTag instance.
        // @example
        // - definemap mymap:
        //     root:
        //         first: 1
        //         second: 2
        // # Will narrate a list of 'root.first' and 'root.second'
        // # Note that 'root' itself is not in the output list, as it is a map, not a leaf value.
        // - narrate <[mymap].deep_keys>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "deep_keys", (attribute, object) -> {
            ListTag result = new ListTag();
            for (Map.Entry<StringHolder, ObjectTag> entry : object.entrySet()) {
                if (entry.getValue() instanceof MapTag) {
                    ((MapTag) entry.getValue()).appendDeepKeys(entry.getKey().str, result);
                }
                else {
                    result.add(entry.getKey().str);
                }
            }
            return result;
        });

        // <--[tag]
        // @attribute <MapTag.keys>
        // @returns ListTag
        // @description
        // Returns a list of all keys in this map.
        // @example
        // # Narrates a list of 'a|b|c|'
        // - narrate <map[a=1;b=2;c=3].key>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "keys", (attribute, object) -> {
            return object.keys();
        }, "list_keys");

        // <--[tag]
        // @attribute <MapTag.values>
        // @returns ListTag
        // @description
        // Returns a list of all values in this map.
        // @example
        // # Narrates a list of '1|2|3|'
        // - narrate <map[a=1;b=2;c=3].values>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "values", (attribute, object) -> {
            return new ListTag(object.values());
        }, "list_values");

        // <--[tag]
        // @attribute <MapTag.to_pair_lists>
        // @returns ListTag
        // @description
        // Returns a list of all key/value pairs in this map, where each entry in the list is itself a list with 2 entries: the key, then the value.
        // @example
        // # Narrates "a is set to 1", then "b is set to 2", then "c is set to 3"
        // - foreach <map[a=1;b=2;c=3].to_pair_lists> as:pair:
        //     - narrate "<[pair].get[1]> is set to <[pair].get[2]>"
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "to_pair_lists", (attribute, object) -> {
            return new ListTag(object.entrySet(), entry -> {
                ListTag pair = new ListTag(2);
                pair.addObject(new ElementTag(entry.getKey().str, true));
                pair.addObject(entry.getValue());
                return pair;
            });
        });

        // <--[tag]
        // @attribute <MapTag.to_list[(<separator>)]>
        // @returns ListTag
        // @description
        // Returns a list of all key/value pairs in this map, separated by the specified separator symbol. If none is given, uses the slash '/' symbol.
        // Note that there is no escaping of the separator, so maps that have the separator in their keys will not be possible to convert back to a map.
        // Inverted by <@link tag ListTag.to_map>
        // @example
        // # Narrates "a/1", then "b/2", then "c/3"
        // - foreach <map[a=1;b=2;c=3].to_list> as:slashed:
        //     - narrate "<[slashed]>"
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "to_list", (attribute, object) -> {
            String separator = attribute.hasParam() ? attribute.getParam() : "/";
            return new ListTag(object.entrySet(), entry -> new ElementTag(entry.getKey().str + separator + entry.getValue().identify(), true));
        });

        // <--[tag]
        // @attribute <MapTag.to_json[(native_types=<true/false>);(indent=<#>)]>
        // @returns ElementTag
        // @description
        // Returns a JSON encoding of this map. Primarily useful with interop with other software, such as when use <@link command webget> or <@link command webserver>.
        // Optionally specify configuration input with:
        // 'native_types' (defaults to false) if 'true' will attempt to convert 'true' or 'false' to booleans, and numbers to raw numbers.
        // 'indent' (defaults to 0) to specify the indentation level of the JSON text output. 0 means no spacing or newlines at all.
        // @example
        // # Narrates {"a":"1","b":"2"}
        // - narrate <map[a=1;b=2].to_json>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_json", (attribute, object) -> {
            MapTag input = attribute.paramAsType(MapTag.class);
            boolean nativeTypes = input != null && input.getElement("native_types", "false").asBoolean();
            int indent = input == null ? 0 : input.getElement("indent", "0").asInt();
            return new ElementTag(new JSONObject((Map) CoreUtilities.objectTagToJavaForm(object.duplicate(), false, nativeTypes)).toString(indent));
        });

        // <--[tag]
        // @attribute <MapTag.to_yaml>
        // @returns ElementTag
        // @description
        // Returns a YAML encoding of this map. Sometimes useful for debugging or for interop with other software.
        // @example
        // # Narrates multiple lines, as follows:
        // # a: '1'
        // # b: '2'
        // - narrate <map[a=1;b=2].to_yaml>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_yaml", (attribute, object) -> {
            YamlConfiguration output = new YamlConfiguration();
            output.contents = (Map) CoreUtilities.objectTagToJavaForm(object.duplicate(), true, false);
            return new ElementTag(output.saveToString(false));
        });

        // <--[tag]
        // @attribute <MapTag.parse_value[<tag>]>
        // @returns MapTag
        // @description
        // Returns a copy of the map with all its values updated through the given tag.
        // One should generally prefer <@link tag MapTag.parse_value_tag>.
        // @Example
        // # Narrates a map of '[alpha=ONE;beta=TWO]'
        // - narrate <map[alpha=one;bravo=two].parse_value[to_uppercase]>
        // -->
        tagProcessor.registerTag(MapTag.class, "parse_value", (attribute, object) -> {
           MapTag newMap = new MapTag();
           String tag = attribute.getRawParam();
           String defaultValue = "null";
           boolean fallback = false;
           if (tag.contains("||")) {
               int marks = 0;
               int lengthLimit = tag.length() - 1;
               for (int i = 0; i < lengthLimit; i++) {
                   char c = tag.charAt(i);
                   if (c == '<') {
                       marks++;
                   }
                   else if (c == '>') {
                       marks--;
                   }
                   else if (marks == 0 && c == '|' && tag.charAt(i + 1) == '|') {
                       fallback = true;
                       defaultValue = tag.substring(i + 2);
                       tag = tag.substring(0, i);
                       break;
                   }
               }
           }
           Attribute subAttribute;
           try {
               subAttribute = new Attribute(tag, attribute.getScriptEntry(), attribute.context);
           }
           catch (TagProcessingException ex) {
               attribute.echoError("Tag processing failed: " + ex.getMessage());
               return null;
           }
           try {
               for (Map.Entry<StringHolder, ObjectTag> entry : object.entrySet()) {
                   Attribute tempAttrib = new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context);
                   tempAttrib.setHadAlternative(attribute.hasAlternative() || fallback);
                   ObjectTag objs = CoreUtilities.autoAttribTyped(entry.getValue(), tempAttrib);
                   if (objs == null) {
                       objs = new ElementTag(defaultValue);
                   }
                   newMap.putObject(entry.getKey().toString(), objs);
               }
           }
           catch (Exception ex) {
               Debug.echoError(ex);
           }
           return newMap;
        });
    }

    public void appendDeepKeys(String path, ListTag result) {
        for (Map.Entry<StringHolder, ObjectTag> entry : entrySet()) {
            if (entry.getValue() instanceof MapTag) {
                ((MapTag) entry.getValue()).appendDeepKeys(path + "." + entry.getKey().str, result);
            }
            else {
                result.add(path + "." + entry.getKey().str);
            }
        }
    }

    public static ObjectTagProcessor<MapTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }
}
