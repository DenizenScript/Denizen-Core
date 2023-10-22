package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.TagProcessingException;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.core.ProcedureScriptContainer;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.tags.core.EscapeTagUtil;
import com.denizenscript.denizencore.utilities.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListTag implements List<String>, ObjectTag {

    // NOTE: Explicitly no example value
    // <--[ObjectType]
    // @name ListTag
    // @prefix li
    // @base ElementTag
    // @synonyms ArrayTag
    // @ExampleTagBase list[one|two]
    // @ExampleForReturns
    // - foreach %VALUE% as:entry:
    //     - narrate "found <[entry]>"
    // @format
    // The identity format for ListTags is each item, one after the other, in order, separated by a pipe '|' symbol.
    // For example, for a list of 'taco', 'potatoes', and 'cheese', it would be 'li@taco|potatoes|cheese|'
    // A list with zero items in it is simply 'li@',
    // and a list with one item is just the one item and a pipe on the end.
    //
    // If the pipe symbol "|" appears in a list entry, it will be replaced by "&pipe",
    // similarly if an ampersand "&" appears in a list entry, it will be replaced by "&amp".
    // This is a subset of Denizen standard escaping, see <@link language Escaping System>.
    //
    // @description
    // A ListTag is a list of any data. It can hold any number of objects in any order.
    // The objects can be of any Denizen object type, including another list.
    //
    // List indices start at 1 (so, the tag 'get[1]' gets the very first entry)
    // and extend to however many entries the list has (so, if a list has 15 entries, the tag 'get[15]' gets the very last entry).
    //
    // Inputs that accept list indices will generally accept 'first' to mean '1', 'last' to mean the last entry in the list,
    // or negative numbers to automatically select an index starting at the end - so for example 'get[-1]' gets the last entry, 'get[-2]' gets the second-to-last, etc.
    //
    // -->

    public static AsciiMatcher needsEscpingMatcher = new AsciiMatcher("&|");

    public static String escapeEntry(String value) {
        if (!needsEscpingMatcher.containsAnyMatch(value)) {
            return value;
        }
        return value.replace("&", "&amp").replace("|", "&pipe");
    }

    public static String unescapeEntry(String value) {
        if (value.indexOf('&') == -1) {
            return value;
        }
        return value.replace("&pipe", "|").replace("&amp", "&");
    }

    public final ArrayList<ObjectTag> objectForms;

    @Override
    public boolean add(String addMe) {
        return objectForms.add(new ElementTag(addMe));
    }

    @Override
    public void add(int index, String addMe) {
        objectForms.add(index, new ElementTag(addMe));
    }

    @Override
    public boolean addAll(Collection<? extends String> addMe) {
        for (String str : addMe) {
            add(str);
        }
        return !addMe.isEmpty();
    }

    @Override
    public boolean addAll(int index, Collection<? extends String> c) {
        for (String str : c) {
            add(index++, str);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean allGone = true;
        for (Object obj : c) {
            if (!remove(obj)) {
                allGone = false;
            }
        }
        return allGone;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        objectForms.clear();
    }

    @Override
    public boolean contains(Object obj) {
        return indexOf(obj) != -1;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object obj : c) {
            if (!contains(obj)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Iterator<String> iterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<String> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<String> listIterator(final int index) {
        return new ListTagStringIterator(this, index);
    }

    public static class ListTagStringIterator implements ListIterator<String> {

        public ListTag list;

        int index;

        public ListTagStringIterator(ListTag list, int index) {
            this.list = list;
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return index < list.size();
        }

        @Override
        public String next() {
            return list.get(index++);
        }

        @Override
        public boolean hasPrevious() {
            return index > 0;
        }

        @Override
        public String previous() {
            return list.get(--index);
        }

        @Override
        public int nextIndex() {
            return index;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void remove() {
            list.remove(index--);
        }

        @Override
        public void set(String s) {
            list.set(index, s);
        }

        @Override
        public void add(String s) {
            list.add(index++, s);
        }
    }

    @Override
    public Object[] toArray() {
        Object[] stringArr = new Object[size()];
        for (int i = 0; i < stringArr.length; i++) {
            stringArr[i] = String.valueOf(getObject(i));
        }
        return stringArr;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        String[] stringArr = (a instanceof String[] && a.length >= size()) ? (String[]) a : new String[size()];
        for (int i = 0; i < size(); i++) {
            stringArr[i] = String.valueOf(getObject(i));
        }
        for (int i = size(); i < stringArr.length; i++) {
            stringArr[i] = null;
        }
        return (T[]) stringArr;
    }

    @Override
    public ListTag subList(int fromIndex, int toIndex) {
        return new ListTag(objectForms.subList(fromIndex, toIndex));
    }

    @Override
    public int indexOf(Object obj) {
        int size = size();
        if (obj == null) {
            for (int i = 0; i < size; i++) {
                if (getObject(i) == null) {
                    return i;
                }
            }
        }
        else if (obj instanceof String) {
            for (int i = 0; i < size; i++) {
                if (obj.equals(String.valueOf(getObject(i)))) {
                    return i;
                }
            }
        }
        else {
            for (int i = 0; i < size; i++) {
                if (obj.equals(getObject(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object obj) {
        int size = size() - 1;
        if (obj == null) {
            for (int i = size; i >= 0; i--) {
                if (getObject(i) == null) {
                    return i;
                }
            }
        }
        else if (obj instanceof String) {
            for (int i = size; i >= 0; i--) {
                if (obj.equals(String.valueOf(getObject(i)))) {
                    return i;
                }
            }
        }
        else {
            for (int i = size; i >= 0; i--) {
                if (obj.equals(getObject(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public ObjectTag removeObject(int index) {
        return objectForms.remove(index);
    }

    @Override
    public String remove(int index) {
        return String.valueOf(removeObject(index));
    }

    @Override
    public boolean remove(Object key) {
        int ind = indexOf(key);
        if (ind < 0 || ind >= size()) {
            return false;
        }
        this.remove(ind);
        return true;
    }

    @Override
    public int size() {
        return objectForms.size();
    }

    @Override
    public boolean isEmpty() {
        return objectForms.isEmpty();
    }

    @Override
    public boolean isTruthy() {
        return !isEmpty();
    }

    @Override
    public String get(int index) {
        return String.valueOf(objectForms.get(index));
    }

    @Override
    public String set(int index, String value) {
        return String.valueOf(setObject(index, new ElementTag(value)));
    }

    public boolean addAll(ListTag inp) {
        return objectForms.addAll(inp.objectForms);
    }

    public boolean addObject(ObjectTag obj) {
        return objectForms.add(obj);
    }

    public void addObject(int index, ObjectTag obj) {
        objectForms.add(index, obj);
    }

    public ObjectTag setObject(int index, ObjectTag obj) {
        return objectForms.set(index, obj);
    }

    public ObjectTag getObject(int id) {
        ObjectTag result = objectForms.get(id);
        if (result == null) {
            return null;
        }
        return result.refreshState();
    }

    @Fetchable("li")
    public static ListTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        ListTag list;
        if (string.startsWith("map@")) {
            MapTag map = MapTag.valueOf(string, context);
            list = new ListTag();
            if (map == null) {
                list.add(string);
            }
            else {
                list.addObject(map);
            }
            return list;
        }
        return new ListTag(string.startsWith("li@") ? string.substring("li@".length()) : string, context);
    }

    public static ListTag getListFor(ObjectTag inp, TagContext context) {
        if (inp instanceof ListTag) {
            return (ListTag) inp;
        }
        if (inp instanceof ElementTag) {
            return valueOf(inp.toString(), context);
        }
        ListTag output = new ListTag(1);
        output.addObject(inp);
        return output;
    }

    public static boolean matches(String arg) {
        return true;
    }

    @Override
    public ListTag duplicate() {
        ListTag outList = new ListTag(size());
        for (ObjectTag obj : objectForms) {
            outList.addObject(obj == null ? null : obj.duplicate());
        }
        return outList;
    }

    /////////////
    //   Constructors
    //////////

    public ListTag(Collection<? extends ObjectTag> objectTagList) {
        objectForms = new ArrayList<>(objectTagList);
    }

    public ListTag(ObjectTag... objects) {
        this(Arrays.asList(objects));
    }

    public ListTag(int capacity) {
        objectForms = new ArrayList<>(capacity);
    }

    public ListTag() {
        objectForms = new ArrayList<>();
    }

    public ListTag(String items) {
        this(items, null);
    }

    public boolean wasLegacy = false;

    public ListTag(String items, TagContext context) {
        objectForms = new ArrayList<>();
        if (items != null && items.length() > 0) {
            if (!items.contains("|")) {
                addObject(ObjectFetcher.pickObjectFor(items, context));
            }
            else if (items.endsWith("|")) {
                int pipe = items.indexOf('|');
                int lastPipe = 0;
                while (pipe != -1) {
                    String value = unescapeEntry(items.substring(lastPipe, pipe));
                    ObjectTag object = ObjectFetcher.pickObjectFor(value, context);
                    addObject(object);
                    lastPipe = pipe + 1;
                    pipe = items.indexOf('|', lastPipe);
                }
            }
            else {
                wasLegacy = true;
                // Count brackets
                int brackets = 0;
                // Record start position
                int start = 0;
                // Loop through characters
                for (int i = 0; i < items.length(); i++) {
                    char chr = items.charAt(i);
                    // Count brackets
                    if (chr == '[') {
                        brackets++;
                    }
                    else if (chr == ']') {
                        if (brackets > 0) {
                            brackets--;
                        }
                    }
                    // Separate if an un-bracketed pipe is found
                    else if (brackets == 0 && chr == '|') {
                        addObject(ObjectFetcher.pickObjectFor(items.substring(start, i), context));
                        start = i + 1;
                    }
                }
                // If there is an item waiting, add it too
                if (start < items.length()) {
                    addObject(ObjectFetcher.pickObjectFor(items.substring(start), context));
                }
            }
        }
    }

    public ListTag(ListTag input) {
        objectForms = new ArrayList<>(input.objectForms);
    }

    public ListTag(Collection<String> items, boolean isPlainText) {
        objectForms = new ArrayList<>(items.size());
        for (String str : items) {
            objectForms.add(new ElementTag(str, isPlainText));
        }
    }

    public ListTag(List<String> items) {
        objectForms = new ArrayList<>(items.size());
        for (String str : items) {
            objectForms.add(new ElementTag(str));
        }
    }

    // A Set<Object> of items
    public ListTag(Set<?> items) {
        objectForms = new ArrayList<>(items.size());
        for (Object o : items) {
            objectForms.add(o instanceof ObjectTag objectTag ? objectTag : new ElementTag(o.toString()));
        }
    }

    public ListTag(Stream<String> items) {
        objectForms = items.map(ElementTag::new).collect(Collectors.toCollection(ArrayList::new));
    }

    public <T> ListTag(Collection<T> objects, Function<T, ObjectTag> convertor) {
        this(objects, null, convertor);
    }

    public <T> ListTag(Collection<T> objects, Predicate<T> filter, Function<T, ObjectTag> convertor) {
        this(objects.size());
        for (T object : objects) {
            if (filter == null || filter.test(object)) {
                addObject(convertor.apply(object));
            }
        }
    }

    /////////////
    //   Instance Fields/Methods
    //////////

    public ListTag addObjects(List<ObjectTag> objectTags) {
        objectForms.addAll(objectTags);
        return this;
    }

    public boolean containsObjectsFrom(Class<? extends ObjectTag> dClass) {
        for (ObjectTag testable : objectForms) {
            if (testable.canBeType(dClass)) {
                return true;
            }
        }
        return false;
    }

    public List<String> filter(Enum[] values) {
        List<String> list = new ArrayList<>(values.length);
        for (String string : this) {
            for (Enum value : values) {
                if (CoreUtilities.equalsIgnoreCase(value.name(), string)) {
                    list.add(string);
                }
            }
        }
        if (!list.isEmpty()) {
            return list;
        }
        return null;
    }

    public <T extends ObjectTag> List<T> filter(Class<T> dClass, ScriptEntry entry) {
        return filter(dClass, entry == null ? CoreUtilities.basicContext : entry.getContext(), true);
    }

    public <T extends ObjectTag> List<T> filter(Class<T> dClass, Debuggable debugger, boolean showFailure) {
        TagContext context = DenizenCore.implementation.getTagContext((ScriptEntry) null);
        context.debug = debugger.shouldDebug();
        return filter(dClass, context, showFailure);
    }

    public <T extends ObjectTag> List<T> filter(Class<T> dClass, TagContext context) {
        return filter(dClass, context, context == null || context.debug);
    }

    public <T extends ObjectTag> List<T> filter(Class<T> dClass, TagContext context, boolean showFailure) {
        List<T> results = new ArrayList<>(objectForms.size());
        for (ObjectTag obj : objectForms) {
            try {
                if (obj.canBeType(dClass)) {
                    T object = obj.asType(dClass, context);
                    if (object != null) {
                        results.add(object);
                    }
                    else if (showFailure) {
                        Debug.echoError("Cannot process list-entry '" + obj + "' as type '" + DebugInternals.getClassNameOpti(dClass) + "' (conversion returned null).");
                    }
                }
                else if (showFailure) {
                    Debug.echoError("Cannot process list-entry '" + obj + "' as type '" + DebugInternals.getClassNameOpti(dClass) + "' (does not match expected type).");
                }
            }
            catch (Exception e) {
                Debug.echoError(e);
            }
        }
        return results;
    }

    private static HashSet<String> deduplicateHelper = new HashSet<>();

    public ListTag deduplicate() {
        deduplicateHelper.clear();
        int size = size();
        ListTag list = new ListTag(size);
        for (int i = 0; i < size; i++) {
            ObjectTag obj = objectForms.get(i);
            String entry = CoreUtilities.toLowerCase(String.valueOf(obj));
            if (!deduplicateHelper.contains(entry)) {
                list.addObject(obj);
                deduplicateHelper.add(entry);
            }
        }
        deduplicateHelper.clear();
        return list;
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public Object getJavaObject() {
        ArrayList<Object> result = new ArrayList<>();
        for (ObjectTag obj : objectForms) {
            result.add(obj.getJavaObject());
        }
        return result;
    }

    //////////////////////////////
    //    DSCRIPT ARGUMENT METHODS
    /////////////////////////

    private String prefix = "List";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ListTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debuggable() {
        if (isEmpty()) {
            return "<LG>li@ (Size <GR>0<LG>)";
        }
        StringBuilder debugText = new StringBuilder();
        debugText.append("<LG>li@ (Size <GR>").append(size()).append("<LG>): <Y>");
        for (ObjectTag item : objectForms) {
            debugText.append(item.debuggable()).append(" <LG>|<Y> ");
        }
        return debugText.substring(0, debugText.length() - " <LG>|<Y> ".length());
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public String identify() {
        return identifyList();
    }

    public String identifyList() {
        if (isEmpty()) {
            return "li@";
        }
        StringBuilder output = new StringBuilder();
        output.append("li@");
        for (ObjectTag object : objectForms) {
            output.append(escapeEntry(object.savable())).append('|');
        }
        return output.toString();
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    private static String parseString(ListTag obj, String spacer) {

        StringBuilder dScriptArg = new StringBuilder();
        for (String item : obj) {
            dScriptArg.append(item);
            dScriptArg.append(spacer);
        }
        return dScriptArg.substring(0, dScriptArg.length() - spacer.length());
    }

    public int parseIndex(String index, Attribute attribute, boolean strict) {
        int size = size();
        if (size == 0) {
            if (strict) {
                attribute.echoError("Invalid index parse, list is empty.");
                return -1;
            }
            return 0;
        }
        index = CoreUtilities.toLowerCase(index);
        if (index.equals("last")) {
            return size - 1;
        }
        if (index.equals("first")) {
            return 0;
        }
        int integerIndex;
        try {
            integerIndex = Integer.parseInt(index);
        }
        catch (NumberFormatException ex) {
            attribute.echoError("Invalid index '" + index + "': not a number.");
            return -1;
        }
        if (integerIndex < 0) {
            return Math.max(0, size + integerIndex);
        }
        if (integerIndex == 0) {
            attribute.echoError("Invalid index '0': lists start at index 1.");
            return 0;
        }
        if (integerIndex > size) {
            if (strict) {
                attribute.echoError("Invalid index '" + index + "': list only has " + size + " entries in it.");
                return -1;
            }
            else {
                return size;
            }
        }
        return integerIndex - 1;
    }

    public static void register() {

        // <--[tag]
        // @attribute <ListTag.combine>
        // @returns ListTag
        // @description
        // Returns a list containing the contents of all sublists within this list.
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "combine", (attribute, object) -> {
            ListTag output = new ListTag();
            for (ObjectTag obj : object.objectForms) {
                output.addObjects(ListTag.getListFor(obj, attribute.context).objectForms);
            }
            return output;
        });

        // <--[tag]
        // @attribute <ListTag.sub_lists[<#>]>
        // @returns ListTag(ListTag)
        // @description
        // Returns a list containing sublists of this list capped to a specific length.
        // @Example
        // # Narrates "a|b", then "c|d", then "e|f"
        // - foreach <list[a|b|c|d|e|f].sub_lists[2]> as:sublist:
        //     - narrate <[sublist]>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ElementTag.class, "sub_lists", (attribute, object, count) -> {
            int subListLength = Math.max(1, count.asInt());
            ListTag output = new ListTag();
            ListTag building = new ListTag();
            for (int i = 0; i < object.size(); i++) {
                building.addObject(object.getObject(i));
                if (building.size() == subListLength) {
                    output.addObject(building);
                    building = new ListTag();
                }
            }
            if (!building.isEmpty()) {
                output.addObject(building);
            }
            return output;
        });

        // <--[tag]
        // @attribute <ListTag.space_separated>
        // @returns ElementTag
        // @description
        // Returns the list in a cleaner format, separated by spaces.
        // @Example
        // # Narrates "one two three"
        // - narrate <list[one|two|three].space_separated>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "space_separated", (attribute, object) -> {
            if (object.isEmpty()) {
                return new ElementTag("");
            }
            return new ElementTag(parseString(object, " "));
        }, "as_string", "asstring");

        // <--[tag]
        // @attribute <ListTag.separated_by[<text>]>
        // @returns ElementTag
        // @description
        // Returns the list formatted, with each item separated by the defined text.
        // @Example
        // # Narrates "bob and joe and john"
        // - narrate "<list[bob|joe|john].separated_by[ and ]>"
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "separated_by", (attribute, object, input) -> {
            if (object.isEmpty()) {
                return new ElementTag("");
            }
            return new ElementTag(parseString(object, input.asString()));
        });

        // <--[tag]
        // @attribute <ListTag.comma_separated>
        // @returns ElementTag
        // @description
        // Returns the list in a cleaner format, separated by commas.
        // @Example
        // # Narrates "one, two, three"
        // - narrate <list[one|two|three].comma_separated>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "comma_separated", (attribute, object) -> {
            if (object.isEmpty()) {
                return new ElementTag("");
            }
            return new ElementTag(parseString(object, ", "));
        }, "ascslist", "as_cslist");

        // <--[tag]
        // @attribute <ListTag.unseparated>
        // @returns ElementTag
        // @description
        // Returns the list in a less clean format, separated by nothing.
        // @Example
        // # Narrates "onetwothree"
        // - narrate <list[one|two|three].unseparated>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "unseparated", (attribute, object) -> {
            if (object.isEmpty()) {
                return new ElementTag("");
            }
            return new ElementTag(parseString(object, ""));
        });

        // <--[tag]
        // @attribute <ListTag.get_sub_items[<#>]>
        // @returns ListTag
        // @description
        // Returns a list of the specified sub items in the list, as split by the
        // forward-slash character (/).
        // @Example
        // # Narrates a list of "one|two"
        // - narrate <list[one/alpha|two/beta].get_sub_items[1]>
        // -->
        tagProcessor.registerTag(ListTag.class, "get_sub_items", (attribute, object) -> { // non-static due to hacked sub-tag
            int index = -1;
            if (ArgumentHelper.matchesInteger(attribute.getParam())) {
                index = attribute.getIntParam() - 1;
            }

            // <--[tag]
            // @attribute <ListTag.get_sub_items[<#>].split_by[<element>]>
            // @returns ListTag
            // @description
            // Returns a list of the specified sub item in the list, allowing you to specify a
            // character in which to split the sub items by. WARNING: When setting your own split
            // character, make note that it is CASE SENSITIVE.
            // @Example
            // # Narrates a list of "one|two"
            // - narrate <list[one-alpha|two-beta].get_sub_items[1].split_by[-]>
            // -->
            String split = "/";
            if (attribute.startsWith("split_by", 2)) {
                if (attribute.hasContext(2) && attribute.getContext(2).length() > 0) {
                    split = attribute.getContext(2);
                }
                attribute.fulfill(1);
            }

            if (index < 0) {
                return null;
            }

            ListTag sub_list = new ListTag();

            for (String item : object) {
                String[] strings = item.split(Pattern.quote(split));
                if (strings.length > index) {
                    sub_list.add(strings[index]);
                }
                else {
                    sub_list.add("null");
                }
            }

            return sub_list;
        });

        tagProcessor.registerTag(ObjectTag.class, "map_get", (attribute, object) -> {
            Deprecations.listOldMapTags.warn(attribute.context);
            if (object.isEmpty()) {
                return new ElementTag("");
            }
            ListTag input = getListFor(attribute.getParamObject(), attribute.context);

            String split = "/";
            if (attribute.startsWith("split_by", 2)) {
                if (attribute.hasContext(2) && attribute.getContext(2).length() > 0) {
                    split = attribute.getContext(2);
                }
                attribute.fulfill(1);
            }

            ListTag result = new ListTag();

            for (String key : input) {
                for (String item : object) {
                    String[] strings = item.split(Pattern.quote(split), 2);
                    if (strings.length > 1 && strings[0].equalsIgnoreCase(key)) {
                        result.add(strings[1]);
                    }
                }
            }
            if (input.size() == 1 && result.size() == 1) {
                return new ElementTag(result.get(0));
            }
            else if (input.size() > 1) {
                if (result.size() != input.size()) {
                    return null;
                }
                return result;
            }
            return null;
        });

        tagProcessor.registerTag(ElementTag.class, "map_find_key", (attribute, object) -> {
            Deprecations.listOldMapTags.warn(attribute.context);
            String input = attribute.getParam();

            String split = "/";
            if (attribute.startsWith("split_by", 2)) {
                if (attribute.hasContext(2) && attribute.getContext(2).length() > 0) {
                    split = attribute.getContext(2);
                }
                attribute.fulfill(1);
            }
            for (String item : object) {
                String[] strings = item.split(Pattern.quote(split), 2);
                if (strings.length > 1 && strings[1].equalsIgnoreCase(input)) {
                    return new ElementTag(strings[0]);
                }
            }
            return null;
        });

        // <--[tag]
        // @attribute <ListTag.merge_maps>
        // @returns MapTag
        // @description
        // If this list is a list of MapTags, returns a single MapTag of all the maps combined together.
        // So a list that contains map of [a=1;b=2] and a map of [x=3;y=4] will return a single map of [a=1;b;=2;x=3;y=4]
        // Duplicate keys will have the the last value that appears in the list.
        // -->
        tagProcessor.registerStaticTag(MapTag.class, "merge_maps", (attribute, object) -> {
            MapTag map = new MapTag();
            for (ObjectTag entry : object.objectForms) {
                MapTag subMap = MapTag.getMapFor(entry, attribute.context);
                if (subMap == null) {
                    attribute.echoError("Invalid map '" + entry + "' for merge_maps tag.");
                    return null;
                }
                map.putAll(subMap);
            }
            return map;
        });

        // <--[tag]
        // @attribute <ListTag.to_map[(<separator>)]>
        // @returns MapTag
        // @description
        // Interprets a list of "key/value" pairs as a map, and returns the resulting MapTag.
        // Optionally specify the map separator symbol, by default '/'.
        // Inverted by <@link tag MapTag.to_list>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, "to_map", (attribute, object) -> {
            String symbol = "/";
            if (attribute.hasParam()) {
                symbol = attribute.getParam();
            }
            MapTag map = new MapTag();
            for (String entry : object) {
                int slash = entry.indexOf(symbol);
                if (slash == -1) {
                    return null;
                }
                String key = entry.substring(0, slash);
                String value = entry.substring(slash + symbol.length());
                map.putObject(key, new ElementTag(value));
            }
            return map;
        });

        // <--[tag]
        // @attribute <ListTag.map_with[<value>|...]>
        // @returns MapTag
        // @description
        // Interprets this list as a list of keys, and the input as a list of values,
        // and forms a mapping from keys to values based on list index.
        // Both lists must have the same size.
        // @Example
        // # Narrates a map of "[a=1;b=2;c=3]"
        // - narrate <list[a|b|c].map_with[1|2|3|]>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, ListTag.class, "map_with", (attribute, object, inputList) -> {
            if (object.size() != inputList.size()) {
                attribute.echoError("List.map_with tag failed: lists must be the same size!");
                return null;
            }
            MapTag map = new MapTag();
            for (int i = 0; i < object.size(); i++) {
                map.putObject(object.get(i), inputList.getObject(i));
            }
            return map;
        });

        // <--[tag]
        // @attribute <ListTag.size>
        // @returns ElementTag(Number)
        // @description
        // Returns the size of the list.
        // @Example
        // # Narrates '3'
        // - narrate <list[one|two|three].size>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "size", (attribute, object) -> {
            return new ElementTag(object.size());
        });

        // <--[tag]
        // @attribute <ListTag.is_empty>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the list is empty.
        // @Example
        // - if <list[something].is_empty>:
        //     - narrate "never shows"
        // - else:
        //     - narrate "it ain't empty!"
        // @Example
        // - if <list.is_empty>:
        //     - narrate "it empty!"
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_empty", (attribute, object) -> {
            return new ElementTag(object.isEmpty());
        });

        // <--[tag]
        // @attribute <ListTag.any>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the list is not empty.
        // @Example
        // - if <list.any>:
        //     - narrate "never shows"
        // - else:
        //     - narrate "ain't got any"
        // @Example
        // - if <list[something].any>:
        //     - narrate "something's here!"
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "any", (attribute, object) -> {
            return new ElementTag(!object.isEmpty());
        });

        // <--[tag]
        // @attribute <ListTag.insert[...|...].at[<#>]>
        // @returns ListTag
        // @description
        // Returns a new ListTag with the items specified inserted to the specified location.
        // Note the index input options described at <@link objecttype listtag>
        // @Example
        // # Narrates a list of "one|two|three|four"
        // - narrate <list[one|four].insert[two|three].at[2]>
        // -->
        tagProcessor.registerTag(ListTag.class, "insert", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                return null;
            }
            ListTag items = getListFor(attribute.getParamObject(), attribute.context);
            if (attribute.startsWith("at", 2) && attribute.hasContext(2)) {
                ListTag result = new ListTag(object);
                int index = object.parseIndex(attribute.getContext(2), attribute, false);
                if (index == -1) {
                    return null;
                }
                for (int i = 0; i < items.size(); i++) {
                    result.addObject(index + i, items.getObject(i));
                }
                attribute.fulfill(1);
                return result;
            }
            else {
                Debug.echoError("The tag ListTag.insert[...] must be followed by .at[#]!");
                return null;
            }
        });

        // <--[tag]
        // @attribute <ListTag.insert_single[...|...].at[<#>]>
        // @returns ListTag
        // @description
        // Returns a new ListTag with the single item specified inserted to the specified location.
        // Note the index input options described at <@link objecttype listtag>
        // @Example
        // # Narrates a list with 3 values: "one", "two|alsotwo", "three"
        // - narrate <list[one|three].insert_single[two|alsotwo].at[2]>
        // -->
        tagProcessor.registerTag(ListTag.class, "insert_single", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                return null;
            }
            ObjectTag value = attribute.getParamObject();
            if (attribute.startsWith("at", 2) && attribute.hasContext(2)) {
                ListTag result = new ListTag(object);
                int index = object.parseIndex(attribute.getContext(2), attribute, false);
                if (index == -1) {
                    return null;
                }
                result.addObject(index, value);
                attribute.fulfill(1);
                return result;
            }
            else {
                Debug.echoError("The tag ListTag.insert_single[...] must be followed by .at[#]!");
                return null;
            }
        });

        // <--[tag]
        // @attribute <ListTag.set[...|...].at[<#>]>
        // @returns ListTag
        // @description
        // Returns a new ListTag with the items specified inserted to the specified location, replacing the object already at that location.
        // Note the index input options described at <@link objecttype listtag>
        // @Example
        // # Narrates a list of "one|potato|three"
        // - narrate <list[one|two|three].set[potato].at[2]>
        // @Example
        // # Narrates a list of "one|potato|taco|hotdog|three"
        // - narrate <list[one|two|three].set[potato|taco|hotdog].at[2]>
        // -->
        tagProcessor.registerTag(ListTag.class, "set", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                return null;
            }
            ListTag items = getListFor(attribute.getParamObject(), attribute.context);
            if (attribute.startsWith("at", 2) && attribute.hasContext(2)) {
                ListTag result = new ListTag(object);
                int index = object.parseIndex(attribute.getContext(2), attribute, true);
                if (index == -1) {
                    return null;
                }
                attribute.fulfill(1);
                if (!result.isEmpty()) {
                    result.removeObject(index);
                }
                for (int i = 0; i < items.size(); i++) {
                    result.addObject(index + i, items.objectForms.get(i));
                }
                return result;
            }
            else {
                Debug.echoError("The tag ListTag.set[...] must be followed by .at[#]!");
                return null;
            }
        });

        // <--[tag]
        // @attribute <ListTag.set_single[<value>].at[<#>]>
        // @returns ListTag
        // @description
        // Returns a new ListTag with the single item specified inserted to the specified location, replacing the object already at that location.
        // Note the index input options described at <@link objecttype listtag>
        // @Example
        // # Narrates a list with 3 values: "one", "potato|taco", "three"
        // - narrate <list[one|two|three].set_single[potato|taco].at[2]>
        // -->
        tagProcessor.registerTag(ListTag.class, "set_single", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                return null;
            }
            ObjectTag value = attribute.getParamObject();
            if (attribute.startsWith("at", 2) && attribute.hasContext(2)) {
                ListTag result = new ListTag(object);
                int index = object.parseIndex(attribute.getContext(2), attribute, true);
                attribute.fulfill(1);
                if (index == -1) {
                    return null;
                }
                if (!result.isEmpty()) {
                    result.removeObject(index);
                }
                result.addObject(index, value);
                return result;
            }
            else {
                Debug.echoError("The tag ListTag.set_single[...] must be followed by .at[#]!");
                return null;
            }
        });

        // <--[tag]
        // @attribute <ListTag.overwrite[...|...].at[<#>]>
        // @returns ListTag
        // @description
        // Returns a new ListTag with the index specified and beyond replaced with the input list.
        // The result list will be the same size as the original list, unless (input_list.size + at_index) is greater than the original list size.
        // Note the index input options described at <@link objecttype listtag>
        // @Example
        // # Narrates a list of "one|potato|taco|four"
        // - narrate <list[one|two|three|four].overwrite[potato|taco].at[2]>
        // @Example
        // # Narrates a list of "one|potato|taco|hotdog|cheeseburger"
        // - narrate <list[one|two|three].overwrite[potato|taco|hotdog|cheeseburger].at[2]>
        // -->
        tagProcessor.registerTag(ListTag.class, "overwrite", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                return null;
            }
            if (object.isEmpty()) {
                return null;
            }
            ListTag items = getListFor(attribute.getParamObject(), attribute.context);
            if (attribute.startsWith("at", 2) && attribute.hasContext(2)) {
                ListTag result = new ListTag(object);
                int index = object.parseIndex(attribute.getContext(2), attribute, true);
                attribute.fulfill(1);
                if (index == -1) {
                    return null;
                }
                for (int i = 0; i < items.size(); i++) {
                    if (index + i >= result.size()) {
                        result.addObject(items.objectForms.get(i));
                    }
                    else {
                        result.setObject(index + i, items.objectForms.get(i));
                    }
                }
                return result;
            }
            else {
                Debug.echoError("The tag ListTag.overwrite[...] must be followed by .at[#]!");
                return null;
            }
        });

        // <--[tag]
        // @attribute <ListTag.include_single[<value>]>
        // @returns ListTag
        // @description
        // Returns a new ListTag including the value specified as a new entry.
        // If the value input is a list, that list becomes a list-within-a-list, still only occupying one space in the outer list.
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ObjectTag.class, "include_single", (attribute, object, val) -> {
            ListTag copy = new ListTag(object);
            copy.addObject(val);
            return copy;
        });

        // <--[tag]
        // @attribute <ListTag.include[...|...]>
        // @returns ListTag
        // @description
        // Returns a new ListTag including the items specified.
        // @Example
        // # Narrates a list of "one|two|three|four"
        // - narrate <list[one|two].include[three|four]>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ListTag.class, "include", (attribute, object, list) -> {
            ListTag copy = new ListTag(object);
            copy.addAll(list);
            return copy;
        });

        // <--[tag]
        // @attribute <ListTag.exclude_single[<value>]>
        // @returns ListTag
        // @description
        // Returns a new ListTag excluding the items specified.
        // If your list contains sub-lists, this tag can cleanly remove them.
        // Note that the "_single" refers to not taking a list of removables, it does still remove more than once instance of the value if present in the list.
        // @Example
        // # Narrates a list of "one|two|three|four", note how it does not remove anything here, as "two|three" does not appear as a *single* value in the list.
        // - narrate <list[one|two|three|four].exclude_single[two|three]>
        // @Example
        // # Narrates a list of "three|four", with the single-entry "one|two" value removed.
        // - narrate <list_single[one|two].include[three|four].exclude_single[one|two]>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ObjectTag.class, "exclude_single", (attribute, object, val) -> {
            ListTag copy = new ListTag(object);
            String toRemove = CoreUtilities.toLowerCase(val.toString());
            for (int i = 0; i < copy.size(); i++) {
                if (toRemove.contains(CoreUtilities.toLowerCase(copy.get(i)))) {
                    copy.removeObject(i--);
                }
            }
            return copy;
        });

        // <--[tag]
        // @attribute <ListTag.exclude[...|...]>
        // @returns ListTag
        // @description
        // Returns a new ListTag excluding the items specified.
        // @Example
        // # Narrates a list of "one|three"
        // - narrate <list[one|two|three|four|two].exclude[two|four]>
        // -->
        tagProcessor.registerTag(ListTag.class, ListTag.class, "exclude", (attribute, object, exclusions) -> { // non-static due to hacked sub-tag
            HashSet<String> toExclude = new HashSet<>(exclusions.size() * 2);
            for (String str : exclusions) {
                toExclude.add(CoreUtilities.toLowerCase(str));
            }
            int max = Integer.MAX_VALUE;
            // <--[tag]
            // @attribute <ListTag.exclude[...|...].max[<#>]>
            // @returns ListTag
            // @description
            // Returns a new ListTag excluding the items specified. Specify a maximum number of items to remove from the list.
            // Max must be an integer >= 1.
            // @Example
            // # Narrates a list of "taco|taco|taco|potato"
            // - narrate <list[taco|potato|taco|potato|taco|potato].exclude[potato].max[2]>
            // -->
            if (attribute.startsWith("max", 2) && attribute.hasContext(2)) {
                max = attribute.getIntContext(2);
                attribute = attribute.fulfill(1);
            }
            int removed = 0;

            // Create a new ListTag that will contain the exclusions
            ListTag copy = new ListTag(object);
            for (int i = 0; i < copy.size(); i++) {
                if (toExclude.contains(CoreUtilities.toLowerCase(copy.get(i)))) {
                    copy.removeObject(i--);
                    removed++;
                    if (removed >= max) {
                        break;
                    }
                }
            }
            return copy;
        });

        // <--[tag]
        // @attribute <ListTag.remove[<#>|...]>
        // @returns ListTag
        // @description
        // Returns a new ListTag excluding the items at the specified index.
        // Note the index input options described at <@link objecttype listtag>
        // @Example
        // # Narrates a list of "one|three|four"
        // - narrate <list[one|two|three|four].remove[2]>
        // -->
        tagProcessor.registerTag(ListTag.class, ListTag.class, "remove", (attribute, object, indices) -> { // non-static due to hacked sub-tag
            ListTag copy = new ListTag(object);

            // <--[tag]
            // @attribute <ListTag.remove[<#>].to[<#>]>
            // @returns ListTag
            // @description
            // Returns a new ListTag excluding the items in the specified index range.
            // Note the index input options described at <@link objecttype listtag>
            // @Example
            // # Narrates a list of "one|five"
            // - narrate <list[one|two|three|four|five].remove[2].to[4]>
            // -->
            if (indices.size() == 1 && attribute.startsWith("to", 2)) {
                if (!attribute.hasContext(2)) {
                    attribute.echoError("The tag ListTag.remove[#].to[#] must have a to value.");
                    return null;
                }
                int fromIndex = object.parseIndex(indices.get(0), attribute, true);
                int toIndex = object.parseIndex(attribute.getContext(2), attribute, false);
                attribute.fulfill(1);
                if (fromIndex == -1 || toIndex == -1) {
                    return null;
                }
                if (toIndex >= object.size()) {
                    toIndex = object.size() - 1;
                }
                if (toIndex < fromIndex) {
                    return copy;
                }
                copy.objectForms.subList(fromIndex, toIndex + 1).clear();
                return copy;
            }
            for (String index : indices) {
                int remove = copy.parseIndex(index, attribute, true);
                if (remove != -1) {
                    copy.set(remove, "\0");
                }
            }
            for (int i = 0; i < copy.size(); i++) {
                if (copy.get(i).equals("\0")) {
                    copy.removeObject(i--);
                }
            }
            return copy;
        });

        // <--[tag]
        // @attribute <ListTag.shared_contents[...|...]>
        // @returns ListTag
        // @description
        // Returns a list of only items that appear in both this list and the input one.
        // This will also inherently deduplicate the output as part of processing.
        // This will retain the list order of the list object the tag is on (so, for example "a|b|c" .shared_contents[c|b] returns "b|c").
        // @Example
        // # Narrates a list of "two|four"
        // - narrate <list[one|two|three|four].shared_contents[two|four|five|six]>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ListTag.class, "shared_contents", (attribute, object, secondList) -> {
            ListTag output = new ListTag();
            for (String val : object) {
                if (secondList.containsCaseInsensitive(val) && !output.containsCaseInsensitive(val)) {
                    output.add(val);
                }
            }
            return output;
        });

        // <--[tag]
        // @attribute <ListTag.replace[(regex:)<element>]>
        // @returns ListTag
        // @description
        // Returns the list with all instances of an element removed.
        // Specify regex: at the start of the replace element to replace elements that match the Regex.
        // -->

        // <--[tag]
        // @attribute <ListTag.replace[(regex:)<element>].with[<element>]>
        // @returns ListTag
        // @description
        // Returns the list with all instances of an element replaced with another.
        // Specify regex: at the start of the replace element to replace elements that match the Regex.
        // -->
        tagProcessor.registerTag(ListTag.class, "replace", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                return null;
            }
            String replace = attribute.getParam();
            ObjectTag replacement = null;
            if (attribute.startsWith("with", 2)) {
                attribute.fulfill(1);
                if (attribute.hasParam()) {
                    replacement = attribute.getParamObject();
                }
                else {
                    Debug.echoError("The tag ListTag.replace[...].with[...] must have a value.");
                    return null;
                }
            }

            ListTag list = new ListTag();

            if (replace.startsWith("regex:")) {
                String regex = replace.substring("regex:".length());
                Pattern tempPat = Pattern.compile(regex);
                for (int i = 0; i < object.size(); i++) {
                    if (tempPat.matcher(object.get(i)).matches()) {
                        if (replacement != null) {
                            list.addObject(replacement);
                        }
                    }
                    else {
                        list.addObject(object.getObject(i));
                    }
                }
            }
            else {
                for (int i = 0; i < object.size(); i++) {
                    if (CoreUtilities.equalsIgnoreCase(object.get(i), replace)) {
                        if (replacement != null) {
                            list.addObject(replacement);
                        }
                    }
                    else {
                        list.addObject(object.getObject(i));
                    }
                }
            }

            return list;
        });

        // <--[tag]
        // @attribute <ListTag.reverse>
        // @returns ListTag
        // @description
        // Returns a copy of the list, with all items placed in opposite order.
        // @Example
        // # Narrates a list of "three|two|one"
        // - narrate <list[one|two|three].reverse>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "reverse", (attribute, object) -> {
            ArrayList<ObjectTag> objs = new ArrayList<>(object.objectForms);
            Collections.reverse(objs);
            return new ListTag(objs);
        });

        // <--[tag]
        // @attribute <ListTag.deduplicate>
        // @returns ListTag
        // @description
        // Returns a copy of the list with any duplicate items removed.
        // @Example
        // # Narrates a list of "one|two|three"
        // - narrate <list[one|one|two|three].deduplicate>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "deduplicate", (attribute, object) -> {
            return object.deduplicate();
        });

        // <--[tag]
        // @attribute <ListTag.get[<#>|...]>
        // @returns ObjectTag
        // @description
        // Returns an element of the value specified by the supplied context.
        // Specify more than one index to get a list of results.
        // Note the index input options described at <@link objecttype listtag>
        // @Example
        // # Narrates "one"
        // - narrate <list[one|two|three].get[1]>
        // @Example
        // # Narrates a list of "one|three"
        // - narrate <list[one|two|three].get[1|3]>
        // -->
        TagRunnable.ObjectInterface<ListTag, ObjectTag> getRunnable = (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            if (object.isEmpty()) {
                attribute.echoError("Can't get from an empty list.");
                return null;
            }
            try {
                ListTag indices = getListFor(attribute.getParamObject(), attribute.context);
                if (indices.size() > 1) {
                    ListTag results = new ListTag();
                    for (String index : indices) {
                        int ind = object.parseIndex(index, attribute, true);
                        if (ind != -1) {
                            results.addObject(object.getObject(ind));
                        }
                    }
                    return results;
                }
                if (indices.size() > 0) {
                    int index = object.parseIndex(indices.get(0), attribute, true);
                    if (index == -1) {
                        return null;
                    }

                    // <--[tag]
                    // @attribute <ListTag.get[<#>].to[<#>]>
                    // @returns ListTag
                    // @description
                    // Returns all elements in the range from the first index to the second.
                    // Note the index input options described at <@link objecttype listtag>
                    // @Example
                    // # Narrates a list of "one|two|three"
                    // - narrate <list[one|two|three|four].get[1].to[3]>
                    // @Example
                    // # Narrates a list of "three|four"
                    // - narrate <list[one|two|three|four].get[3].to[last]>
                    // @Example
                    // # Narrates a list of "three|four"
                    // - narrate <list[one|two|three|four].get[-2].to[-1]>
                    // -->
                    if (attribute.startsWith("to", 2) && attribute.hasContext(2)) {
                        int index2 = object.parseIndex(attribute.getContext(2), attribute, false);
                        if (index2 == -1) {
                            return null;
                        }
                        if (index2 >= object.size()) {
                            index2 = object.size() - 1;
                        }
                        ListTag newList = new ListTag();
                        for (int i = index; i <= index2; i++) {
                            newList.addObject(object.getObject(i));
                        }
                        attribute.fulfill(1);
                        return newList;
                    }
                    else {
                        return object.getObject(index);
                    }
                }
            }
            catch (NumberFormatException ex) {
                attribute.echoError("ListTag.get[...] input invalid - not a valid number: " + ex.getMessage());
            }
            return null;
        };
        tagProcessor.registerTag(ObjectTag.class, "get", getRunnable); // non-static due to hacked sub-tag
        tagProcessor.registerTag(ObjectTag.class, "", getRunnable);

        // <--[tag]
        // @attribute <ListTag.find_all_partial[<element>]>
        // @returns ListTag
        // @description
        // Returns all the numbered locations of elements that contain the text within a list,
        // or an empty list if the list does not contain that item.
        // @Example
        // # Narrates a list of "2|4"
        // - narrate <list[one|two|three|two].find_all_partial[tw]>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ElementTag.class, "find_all_partial", (attribute, object, input) -> {
            String test = input.asLowerString();
            ListTag positions = new ListTag();
            for (int i = 0; i < object.size(); i++) {
                if (CoreUtilities.toLowerCase(object.get(i)).contains(test)) {
                    positions.add(String.valueOf(i + 1));
                }
            }
            return positions;
        });

        // <--[tag]
        // @attribute <ListTag.find_all_matches[<matcher>]>
        // @returns ListTag
        // @description
        // Returns all the numbered indices of elements that match within a list,
        // using the system behind <@link language Advanced Object Matching>,
        // or an empty list if the list does not contain that item.
        // @Example
        // # Narrates a list of "2|3"
        // - narrate <list[one|two|three].find_all_matches[t*]>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ElementTag.class, "find_all_matches", (attribute, list, matchText) -> {
            ListTag positions = new ListTag();
            String matcher = matchText.asString();
            for (int i = 0; i < list.size(); i++) {
                ObjectTag object = list.getObject(i);
                if (object != null && object.tryAdvancedMatcher(matcher)) {
                    positions.add(String.valueOf(i + 1));
                }
            }
            return positions;
        });

        // <--[tag]
        // @attribute <ListTag.find_all[<element>]>
        // @returns ListTag
        // @description
        // Returns all the numbered indices of all entries that match the text within a list,
        // or an empty list if the list does not contain that item.
        // @Example
        // # Narrates a list of "2|4"
        // - narrate <list[one|two|three|two].find_all[two]>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ElementTag.class, "find_all", (attribute, object, input) -> {
            ListTag positions = new ListTag();
            String test = input.asLowerString();
            for (int i = 0; i < object.size(); i++) {
                if (CoreUtilities.toLowerCase(object.get(i)).equals(test)) {
                    positions.add(String.valueOf(i + 1));
                }
            }
            return positions;
        });

        // <--[tag]
        // @attribute <ListTag.find_partial[<element>]>
        // @returns ElementTag(Number)
        // @description
        // Returns the numbered index of the first partially matching entry within a list,
        // or -1 if the list does not contain that item.
        // @Example
        // # Narrates "2"
        // - narrate <list[one|two|three].find_partial[tw]>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "find_partial", (attribute, object, input) -> {
            String test = input.asLowerString();
            for (int i = 0; i < object.size(); i++) {
                if (CoreUtilities.toLowerCase(object.get(i)).contains(test)) {
                    return new ElementTag(i + 1);
                }
            }
            return new ElementTag(-1);
        });

        // <--[tag]
        // @attribute <ListTag.find_match[<matcher>]>
        // @returns ElementTag(Number)
        // @description
        // Returns the numbered index of the first match within a list,
        // using the system behind <@link language Advanced Object Matching>,
        // or -1 if the list does not contain that item.
        // @Example
        // # Narrates "2"
        // - narrate <list[one|two|three].find_match[t*]>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "find_match", (attribute, list, matcher) -> {
            for (int i = 0; i < list.size(); i++) {
                ObjectTag object = list.getObject(i);
                if (object != null && object.tryAdvancedMatcher(matcher.asString())) {
                    return new ElementTag(i + 1);
                }
            }
            return new ElementTag(-1);
        });

        // <--[tag]
        // @attribute <ListTag.find[<element>]>
        // @returns ElementTag(Number)
        // @description
        // Returns the numbered index of an entry within a list,
        // or -1 if the list does not contain that item.
        // @Example
        // # Narrates "2"
        // - narrate <list[one|two|three].find[two]>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "find", (attribute, object, input) -> {
            String test = input.asLowerString();
            for (int i = 0; i < object.size(); i++) {
                if (CoreUtilities.toLowerCase(object.get(i)).equals(test)) {
                    return new ElementTag(i + 1);
                }
            }
            return new ElementTag(-1);
        });

        // <--[tag]
        // @attribute <ListTag.count[<element>]>
        // @returns ElementTag(Number)
        // @description
        // Returns how many times a value in the list occurs.
        // @Example
        // # Narrates "2"
        // - narrate <list[one|two|two|three].count[two]>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "count", (attribute, object, input) -> {
            String test = input.asLowerString();
            int count = 0;
            for (String s : object) {
                if (CoreUtilities.toLowerCase(s).equals(test)) {
                    count++;
                }
            }
            return new ElementTag(count);
        });

        // <--[tag]
        // @attribute <ListTag.count_matches[<matcher>]>
        // @returns ElementTag(Number)
        // @description
        // Returns how many times a value in the list matches the matcher,
        // using the system behind <@link language Advanced Object Matching>,
        // For example: a list of "one|two|three" .count[t*] returns 2.
        // @Example
        // # Narrates "2"
        // - narrate <list[one|two|two|three].count_matches[t*]>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "count_matches", (attribute, list, matcher) -> {
            int count = 0;
            for (ObjectTag object : list.objectForms) {
                if (object != null && object.tryAdvancedMatcher(matcher.asString())) {
                    count++;
                }
            }
            return new ElementTag(count);
        });

        // <--[tag]
        // @attribute <ListTag.sum>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the sum of all numbers in the list. Ignores non-numerical values.
        // @Example
        // # Narrates "6"
        // - narrate <list[1|2|3].sum>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "sum", (attribute, object) -> {
            BigDecimal sum = BigDecimal.ZERO;
            for (String entry : object) {
                if (ArgumentHelper.matchesDouble(entry)) {
                    sum = sum.add(new ElementTag(entry).asBigDecimal());
                }
            }
            return new ElementTag(sum);
        });

        // <--[tag]
        // @attribute <ListTag.average>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the mean average of all numbers in the list. Ignores non-numerical values.
        // @Example
        // # Narrates "3"
        // - narrate <list[1|2|4|5].average>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "average", (attribute, object) -> {
            if (object.isEmpty()) {
                return new ElementTag(0);
            }
            BigDecimal sum = BigDecimal.ZERO;
            for (String entry : object) {
                if (ArgumentHelper.matchesDouble(entry)) {
                    sum = sum.add(new ElementTag(entry).asBigDecimal());
                }
            }
            try {
                return new ElementTag(sum.divide(new BigDecimal(object.size()), 64, RoundingMode.HALF_UP));
            }
            catch (Throwable e) {
                return new ElementTag(sum.doubleValue() / object.size());
            }
        });

        // <--[tag]
        // @attribute <ListTag.first[(<#>)]>
        // @returns ObjectTag
        // @description
        // Returns the first element in the list.
        // If the list is empty, returns null instead.
        // Effectively equivalent to .get[1]
        // Optionally, specify a number of entries to get, which will return the # first entries, equivalent to .get[1].to[#]
        //
        // @Example
        // # Narrates "one"
        // - narrate <list[one|two|three].first>
        // @Example
        // # Narrates a list of "one" and "two"
        // - narrate <list[one|two|three].first[2]>
        // @Example
        // # Narrates a list of "one", "two", and "three"
        // - narrate <list[one|two|three].first[5]>
        // @Example
        // # Causes an error, as the list is empty.
        // - narrate <list.first>
        // -->
        tagProcessor.registerStaticTag(ObjectTag.class, "first", (attribute, object) -> {
            if (object.isEmpty()) {
                return null;
            }
            if (attribute.hasParam()) {
                int size = attribute.getIntParam();
                if (size < 0) {
                    attribute.echoError("Invalid 'first' size, cannot be negative.");
                    return null;
                }
                ListTag result = new ListTag(size);
                for (int i = 0; i < size && i < object.size(); i++) {
                    result.addObject(object.getObject(i));
                }
                return result;
            }
            return object.objectForms.get(0);
        });

        // <--[tag]
        // @attribute <ListTag.last[(<#>)]>
        // @returns ObjectTag
        // @description
        // Returns the last element in the list.
        // If the list is empty, returns null instead.
        // Effectively equivalent to .get[<[list].size>]
        // Optionally, specify a number of entries to get, which will return the # first entries, equivalent to .get[<[size].sub[#]>].to[last]
        // @Example
        // # Narrates "three"
        // - narrate <list[one|two|three].last>
        // @Example
        // # Narrates a list of "two" and "three"
        // - narrate <list[one|two|three].last[2]>
        // @Example
        // # Narrates a list of "one", "two", and "three"
        // - narrate <list[one|two|three].last[5]>
        // @Example
        // # Causes an error, as the list is empty.
        // - narrate <list.last>
        // -->
        tagProcessor.registerStaticTag(ObjectTag.class, "last", (attribute, object) -> {
            if (object.isEmpty()) {
                return null;
            }
            if (attribute.hasParam()) {
                int size = attribute.getIntParam();
                if (size < 0) {
                    attribute.echoError("Invalid 'last' size, cannot be negative.");
                    return null;
                }
                ListTag result = new ListTag(size);
                for (int i = Math.max(0, object.size() - size); i < object.size(); i++) {
                    result.addObject(object.getObject(i));
                }
                return result;
            }
            return object.objectForms.get(object.size() - 1);
        });

        // <--[tag]
        // @attribute <ListTag.lowest[(<tag>)]>
        // @returns ObjectTag
        // @description
        // Returns the smallest value in a list of decimal numbers.
        // Optionally specify a tag to run on each list entry that returns the numeric value for that entry.
        // @Example
        // # Narrates '1'
        // - narrate "<list[3|2|1|10].lowest>
        // @Example
        // # Narrates the name of the player with the least money currently online
        // - narrate <server.online_players.lowest[money].name>
        // -->
        tagProcessor.registerTag(ObjectTag.class, "lowest", (attribute, object) -> {
            String tag = null;
            if (attribute.hasParam()) {
                tag = attribute.getRawParam();
            }
            Attribute subAttribute;
            try {
                subAttribute = tag == null ? null : new Attribute(tag, attribute.getScriptEntry(), attribute.context);
            }
            catch (TagProcessingException ex) {
                attribute.echoError("Tag processing failed: " + ex.getMessage());
                return null;
            }

            // <--[tag]
            // @attribute <ListTag.lowest[(<tag>)].count[<#>]>
            // @returns ListTag
            // @description
            // Returns a list of the smallest values in a list of decimal numbers.
            // Optionally specify a tag to run on each list entry that returns the numeric value for that entry.
            // Note: if you want to sort the entire list, rather than just getting a few values, use a sort tag link <@link tag listtag.sort_by_number>
            // @Example
            // # Narrates a list of '1|2'
            // - narrate "<list[3|2|1|10].lowest.count[2]>
            // @Example
            // # Narrates the names of the 5 players with the least money currently online
            // - narrate <server.online_players.lowest[money].count[5].parse[name].formatted>
            // -->
            if (attribute.startsWith("count", 2) && attribute.hasContext(2)) {
                int count = Math.min(attribute.getIntContext(2), object.size());
                if (count < 0) {
                    attribute.echoError("Invalid 'lowest' count, cannot be negative.");
                    return null;
                }
                attribute.fulfill(1);
                int[] indices = new int[count];
                BigDecimal[] values = new BigDecimal[count];
                for (int i = 0; i < object.size(); i++) {
                    ObjectTag obj = object.getObject(i);
                    if (tag != null) {
                        obj = CoreUtilities.autoAttribTyped(obj, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                    }
                    String str = obj.toString();
                    if (ArgumentHelper.matchesDouble(str)) {
                        BigDecimal val = new ElementTag(str).asBigDecimal();
                        for (int x = 0; x < count; x++) {
                            if (values[x] == null || values[x].compareTo(val) > 0) {
                                for (int j = count - 1; j > x; j--) {
                                    values[j] = values[j - 1];
                                    indices[j] = indices[j - 1];
                                }
                                values[x] = val;
                                indices[x] = i;
                                break;
                            }
                        }
                    }
                }
                ListTag output = new ListTag(count);
                for (int i = 0; i < count; i++) {
                    if (values[i] != null) {
                        output.addObject(object.getObject(indices[i]));
                    }
                }
                return output;
            }
            ObjectTag lowestObj = null;
            BigDecimal lowest = null;
            for (ObjectTag obj : object.objectForms) {
                ObjectTag actualObj = obj;
                if (tag != null) {
                    obj = CoreUtilities.autoAttribTyped(obj, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                }
                if (obj == null) {
                    continue;
                }
                String str = obj.toString();
                if (ArgumentHelper.matchesDouble(str)) {
                    BigDecimal val = new ElementTag(str).asBigDecimal();
                    if (lowest == null || lowest.compareTo(val) > 0) {
                        lowest = val;
                        lowestObj = actualObj;
                    }
                }
            }
            return lowestObj;
        });

        // <--[tag]
        // @attribute <ListTag.highest[(<tag>)]>
        // @returns ObjectTag
        // @description
        // Returns the highest value in a list of decimal numbers.
        // Optionally specify a tag to run on each list entry that returns the numeric value for that entry.
        // @Example
        // # Narrates '10'
        // - narrate "<list[3|2|1|10].highest>
        // @Example
        // # Narrates the name of the player with the most money currently online
        // - narrate <server.online_players.highest[money].name>
        // -->
        tagProcessor.registerTag(ObjectTag.class, "highest", (attribute, object) -> {
            String tag = null;
            if (attribute.hasParam()) {
                tag = attribute.getRawParam();
            }
            Attribute subAttribute;
            try {
                subAttribute = tag == null ? null : new Attribute(tag, attribute.getScriptEntry(), attribute.context);
            }
            catch (TagProcessingException ex) {
                attribute.echoError("Tag processing failed: " + ex.getMessage());
                return null;
            }

            // <--[tag]
            // @attribute <ListTag.highest[(<tag>)].count[<#>]>
            // @returns ListTag
            // @description
            // Returns a list of the highest values in a list of decimal numbers.
            // Optionally specify a tag to run on each list entry that returns the numeric value for that entry.
            // Note: if you want to sort the entire list, rather than just getting a few values, use a sort tag like <@link tag listtag.sort_by_number>
            // @Example
            // # Narrates a list of '10|3'
            // - narrate "<list[3|2|1|10].highest.count[2]>
            // @Example
            // # Narrates the names of the 5 players with the most money currently online
            // - narrate <server.online_players.highest[money].count[5].parse[name].formatted>
            // -->
            if (attribute.startsWith("count", 2) && attribute.hasContext(2)) {
                int count = Math.min(attribute.getIntContext(2), object.size());
                if (count < 0) {
                    attribute.echoError("Invalid 'highest' count, cannot be negative.");
                    return null;
                }
                attribute.fulfill(1);
                int[] indices = new int[count];
                BigDecimal[] values = new BigDecimal[count];
                for (int i = 0; i < object.size(); i++) {
                    ObjectTag obj = object.getObject(i);
                    if (tag != null) {
                        obj = CoreUtilities.autoAttribTyped(obj, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                    }
                    String str = obj.toString();
                    if (ArgumentHelper.matchesDouble(str)) {
                        BigDecimal val = new ElementTag(str).asBigDecimal();
                        for (int x = 0; x < count; x++) {
                            if (values[x] == null || values[x].compareTo(val) < 0) {
                                for (int j = count - 1; j > x; j--) {
                                    values[j] = values[j - 1];
                                    indices[j] = indices[j - 1];
                                }
                                values[x] = val;
                                indices[x] = i;
                                break;
                            }
                        }
                    }
                }
                ListTag output = new ListTag(count);
                for (int i = 0; i < count; i++) {
                    if (values[i] != null) {
                        output.addObject(object.getObject(indices[i]));
                    }
                }
                return output;
            }
            ObjectTag highestObj = null;
            BigDecimal highest = null;
            for (ObjectTag obj : object.objectForms) {
                ObjectTag actualObj = obj;
                if (tag != null) {
                    obj = CoreUtilities.autoAttribTyped(obj, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                }
                String str = obj.toString();
                if (ArgumentHelper.matchesDouble(str)) {
                    BigDecimal val = new ElementTag(str).asBigDecimal();
                    if (highest == null || highest.compareTo(val) < 0) {
                        highest = val;
                        highestObj = actualObj;
                    }
                }
            }
            return highestObj;
        });

        // <--[tag]
        // @attribute <ListTag.numerical>
        // @returns ListTag
        // @description
        // Returns the list sorted to be in numerical order.
        // @Example
        // # Narrates a list of '1|2|3|10'
        // - narrate "<list[3|2|1|10].numerical>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "numerical", (attribute, object) -> {
            ListTag sortable = new ListTag(object);
            sortable.objectForms.sort((o1, o2) -> {
                double value = o1.asElement().asDouble() - o2.asElement().asDouble();
                if (value == 0) {
                    return 0;
                }
                else if (value > 0) {
                    return 1;
                }
                else {
                    return -1;
                }
            });
            return new ListTag(sortable);
        });

        // <--[tag]
        // @attribute <ListTag.alphanumeric>
        // @returns ListTag
        // @description
        // Returns the list sorted to be in alphabetical/numerical order.
        // @Example
        // # Narrates a list of 'a1|a10|b|c'
        // - narrate "<list[b|c|a10|a1].alphanumeric>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "alphanumeric", (attribute, object) -> {
            ListTag sortable = new ListTag(object);
            sortable.objectForms.sort(new NaturalOrderComparator());
            return new ListTag(sortable);
        });

        // <--[tag]
        // @attribute <ListTag.alphabetical>
        // @returns ListTag
        // @description
        // Returns the list sorted to be in alphabetical order.
        // @Example
        // # Narrates a list of 'a|c|d|g|q'
        // - narrate "<list[c|d|q|a|g].alphabetical>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "alphabetical", (attribute, object) -> {
            ListTag sortable = new ListTag(object);
            sortable.objectForms.sort((o1, o2) -> o1.toString().compareToIgnoreCase(o2.toString()));
            return new ListTag(sortable);
        });

        // <--[tag]
        // @attribute <ListTag.sort_by_value[<tag>]>
        // @returns ListTag
        // @description
        // Returns a copy of the list, sorted alphanumerically.
        // Rather than sorting based on the item itself, it sorts based on a tag attribute read from within the object being read.
        // For example, you might sort a list of players based on their names, via .sort_by_value[name] on the list of valid players.
        // -->
        tagProcessor.registerTag(ListTag.class, "sort_by_value", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            ListTag newlist = new ListTag(object);
            final NaturalOrderComparator comparator = new NaturalOrderComparator();
            final String tag = attribute.getRawParam();
            Attribute subAttribute;
            try {
                subAttribute = new Attribute(tag, attribute.getScriptEntry(), attribute.context);
            }
            catch (TagProcessingException ex) {
                attribute.echoError("Tag processing failed: " + ex.getMessage());
                return null;
            }
            try {
                newlist.objectForms.sort((o1, o2) -> {
                    ObjectTag or1 = CoreUtilities.autoAttribTyped(o1, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                    ObjectTag or2 = CoreUtilities.autoAttribTyped(o2, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                    return comparator.compare(or1, or2);
                });
                return new ListTag(newlist.objectForms);
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            return newlist;
        });

        // <--[tag]
        // @attribute <ListTag.sort_by_number[<tag>]>
        // @returns ListTag
        // @description
        // Returns a copy of the list, sorted such that the lower numbers appear first, and the higher numbers appear last.
        // Rather than sorting based on the item itself, it sorts based on a tag attribute read from within the object being read.
        // For example, you might sort a list of players based on the amount of money they have, via .sort_by_number[money] on the list of valid players.
        // Non-numerical input is considered an error, and the result is not guaranteed.
        // -->
        tagProcessor.registerTag(ListTag.class, "sort_by_number", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            ListTag newlist = new ListTag(object);
            final String tag = attribute.getRawParam();
            Attribute subAttribute;
            try {
                subAttribute = new Attribute(tag, attribute.getScriptEntry(), attribute.context);
            }
            catch (TagProcessingException ex) {
                attribute.echoError("Tag processing failed: " + ex.getMessage());
                return null;
            }
            try {
                newlist.objectForms.sort((o1, o2) -> {
                    ObjectTag or1 = CoreUtilities.autoAttribTyped(o1, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                    ObjectTag or2 = CoreUtilities.autoAttribTyped(o2, new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context));
                    try {
                        double r1 = Double.parseDouble(or1.toString());
                        double r2 = Double.parseDouble(or2.toString());
                        double value = r1 - r2;
                        if (value == 0) {
                            return 0;
                        }
                        else if (value > 0) {
                            return 1;
                        }
                        else {
                            return -1;
                        }
                    }
                    catch (NumberFormatException ex) {
                        attribute.echoError("Invalid non-numerical input to sort_by_number tag: " + or1 + ", " + or2.toString());
                        return 0;
                    }
                });
                return new ListTag(newlist);
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            return newlist;
        });

        // <--[tag]
        // @attribute <ListTag.sort[<procedure>]>
        // @returns ListTag
        // @description
        // Returns a list sorted according to the return values of a procedure.
        // The <procedure> should link a procedure script that takes two definitions each of which will be an item
        // in the list, and returns -1, 0, or 1 based on whether the second item should be added. EG, if a procedure
        // with definitions "one" and "two" returned -1, it would place "two" after "one". Note that this
        // uses some complex internal sorting code that could potentially throw errors if the procedure does not return
        // consistently - EG, if "one" and "two" returned 1, but "two" and "one" returned 1 as well - obviously,
        // "two" can not be both before AND after "one"!
        // Note that the script should ALWAYS return -1, 0, or 1, or glitches could happen!
        // Note that if two inputs are exactly equal, the procedure should always return 0.
        // -->
        tagProcessor.registerTag(ListTag.class, ScriptTag.class, "sort", (attribute, object, procScript) -> {
            ListTag obj = new ListTag(object);
            final ProcedureScriptContainer script = (ProcedureScriptContainer) procScript.getContainer();
            if (script == null) {
                attribute.echoError("'" + attribute.getParam() + "' is not a valid procedure script!");
                return obj;
            }
            final ScriptEntry entry = attribute.getScriptEntry();
            // <--[tag]
            // @attribute <ListTag.sort[<procedure>].context[<context>]>
            // @returns ListTag
            // @description
            // Sort a list, with context. See <@link tag ListTag.sort[<procedure>]> for general sort details.
            // -->
            ListTag context = new ListTag();
            if (attribute.startsWith("context", 2)) {
                attribute.fulfill(1);
                context = attribute.paramAsType(ListTag.class);
            }
            final ListTag context_send = context;
            try {
                obj.objectForms.sort((o1, o2) -> {
                    List<ScriptEntry> entries = script.getBaseEntries(entry == null ?
                            DenizenCore.implementation.getEmptyScriptEntryData() : entry.entryData.clone());
                    if (entries.isEmpty()) {
                        return 0;
                    }
                    InstantQueue queue = new InstantQueue("LISTTAG_SORT");
                    queue.addEntries(entries);
                    int x = 1;
                    ListTag definitions = new ListTag();
                    definitions.addObject(o1);
                    definitions.addObject(o2);
                    definitions.addAll(context_send);
                    String[] definition_names = null;
                    try {
                        definition_names = script.getString("definitions").split("\\|");
                    }
                    catch (Exception e) { /* IGNORE */ }
                    for (String definition : definitions) {
                        String name = definition_names != null && definition_names.length >= x ?
                                definition_names[x - 1].trim() : String.valueOf(x);
                        int squareBracket = name.indexOf('[');
                        if (squareBracket != -1) {
                            name = name.substring(0, squareBracket).trim();
                        }
                        queue.addDefinition(name, definition);
                        Debug.echoDebug(entries.get(0), "Adding definition '" + name + "' as " + definition);
                        x++;
                    }
                    queue.start();
                    int res = 0;
                    if (queue.determinations != null && queue.determinations.size() > 0) {
                        res = new ElementTag(queue.determinations.get(0)).asInt();
                    }
                    return Integer.compare(res, 0);
                });
            }
            catch (Exception e) {
                Debug.echoError("list.sort[...] tag failed - procedure returned unreasonable response - internal error: " + e.getMessage());
            }
            return obj;
        });

        // <--[tag]
        // @attribute <ListTag.filter[<tag>]>
        // @returns ListTag
        // @description
        // Returns a copy of the list with all its contents parsed through the given tag and only including ones that returned 'true'.
        // One should generally prefer <@link tag ListTag.filter_tag>.
        // @Example
        // # Narrates a list of '3|4|5'
        // - narrate <list[1|2|3|4|5].filter[is_more_than[3]]>
        // -->
        tagProcessor.registerTag(ListTag.class, "filter", (attribute, object) -> {
            String tag = attribute.getRawParam();
            boolean defaultValue = tag.endsWith("||true");
            if (defaultValue) {
                tag = tag.substring(0, tag.length() - "||true".length());
            }
            Attribute subAttribute;
            try {
                subAttribute = new Attribute(tag, attribute.getScriptEntry(), attribute.context);
            }
            catch (TagProcessingException ex) {
                attribute.echoError("Tag processing failed: " + ex.getMessage());
                return null;
            }
            ListTag newlist = new ListTag();
            try {
                for (ObjectTag obj : object.objectForms) {
                    Attribute tempAttrib = new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context);
                    tempAttrib.setHadAlternative(true);
                    ObjectTag objs = CoreUtilities.autoAttribTyped(obj, tempAttrib);
                    if ((objs == null) ? defaultValue : CoreUtilities.equalsIgnoreCase(objs.toString(), "true")) {
                        newlist.addObject(obj);
                    }
                }
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            return newlist;
        });

        // <--[tag]
        // @attribute <ListTag.parse[<tag>]>
        // @returns ListTag
        // @description
        // Returns a copy of the list with all its contents parsed through the given tag.
        // One should generally prefer <@link tag ListTag.parse_tag>.
        // @Example
        // # Narrates a list of 'ONE|TWO'
        // - narrate <list[one|two].parse[to_uppercase]>
        // -->
        tagProcessor.registerTag(ListTag.class, "parse", (attribute, object) -> {
            ListTag newlist = new ListTag();
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
                for (ObjectTag obj : object.objectForms) {
                    Attribute tempAttrib = new Attribute(subAttribute, attribute.getScriptEntry(), attribute.context);
                    tempAttrib.setHadAlternative(attribute.hasAlternative() || fallback);
                    ObjectTag objs = CoreUtilities.autoAttribTyped(obj, tempAttrib);
                    if (objs == null) {
                        objs = new ElementTag(defaultValue);
                    }
                    newlist.addObject(objs);
                }
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            return newlist;
        });

        // <--[tag]
        // @attribute <ListTag.filter_tag[<dynamic-boolean>]>
        // @returns ListTag
        // @description
        // Returns a copy of the list with all its contents parsed through the given input tag and only including ones that returned 'true'.
        // This requires a fully formed tag as input, making use of the 'filter_value' definition.
        // @Example
        // # Narrates a list of '3|4|5'
        // - narrate <list[1|2|3|4|5].filter_tag[<[filter_value].is_more_than[3]>]>
        // @Example
        // # Narrates a list of '4|5'
        // - narrate <list[1|2|3|4|5].filter_tag[<list[4|5].contains[<[filter_value]>]>]>
        // -->
        tagProcessor.registerTag(ListTag.class, "filter_tag", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            ListTag newlist = new ListTag();
            Attribute.OverridingDefinitionProvider provider = new Attribute.OverridingDefinitionProvider(attribute.context.definitionProvider);
            try {
                for (ObjectTag obj : object.objectForms) {
                    provider.altDefs.putObject("filter_value", obj);
                    if (CoreUtilities.equalsIgnoreCase(attribute.parseDynamicParam(provider).toString(), "true")) {
                        newlist.addObject(obj);
                    }
                }
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            return newlist;
        });

        // <--[tag]
        // @attribute <ListTag.parse_tag[<parseable-value>]>
        // @returns ListTag
        // @description
        // Returns a copy of the list with all its contents parsed through the given tag.
        // This requires a fully formed tag as input, making use of the 'parse_value' definition.
        // @Example
        // # Narrates a list of 'ONE|TWO'
        // - narrate <list[one|two].parse_tag[<[parse_value].to_uppercase>]>
        // @Example
        // # Narrates a list of 'charlie|alpha|bravo'
        // - narrate <list[3|1|2].parse_tag[<list[alpha|bravo|charlie].get[<[parse_value]>]>]>
        // -->
        tagProcessor.registerTag(ListTag.class, "parse_tag", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            ListTag newlist = new ListTag(object.size());
            Attribute.OverridingDefinitionProvider provider = new Attribute.OverridingDefinitionProvider(attribute.context.definitionProvider);
            try {
                for (ObjectTag obj : object.objectForms) {
                    provider.altDefs.putObject("parse_value", obj);
                    newlist.addObject(attribute.parseDynamicParam(provider));
                }
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            return newlist;
        });

        // <--[tag]
        // @attribute <ListTag.pad_left[<#>]>
        // @returns ListTag
        // @description
        // Returns a ListTag extended to reach a minimum specified length by adding entries to the left side.
        // -->
        tagProcessor.registerTag(ListTag.class, ElementTag.class, "pad_left", (attribute, object, lengthText) -> { // non-static due to hacked sub-tag
            ObjectTag with = new ElementTag("");
            int length = lengthText.asInt();

            // <--[tag]
            // @attribute <ListTag.pad_left[<#>].with[<element>]>
            // @returns ListTag
            // @description
            // Returns a ListTag extended to reach a minimum specified length by adding a specific entry to the left side.
            // -->
            if (attribute.startsWith("with", 2) && attribute.hasContext(2)) {
                with = attribute.getContextObject(2);
                attribute.fulfill(1);
            }
            ListTag newList = new ListTag(object);
            while (newList.size() < length) {
                newList.addObject(0, with);
            }
            return newList;
        });

        // <--[tag]
        // @attribute <ListTag.pad_right[<#>]>
        // @returns ListTag
        // @description
        // Returns a ListTag extended to reach a minimum specified length by adding entries to the right side.
        // -->
        tagProcessor.registerTag(ListTag.class, ElementTag.class, "pad_right", (attribute, object, lengthText) -> { // non-static due to hacked sub-tag
            ObjectTag with = new ElementTag("");
            int length = lengthText.asInt();

            // <--[tag]
            // @attribute <ListTag.pad_right[<#>].with[<element>]>
            // @returns ListTag
            // @description
            // Returns a ListTag extended to reach a minimum specified length by adding a specific entry to the right side.
            // -->
            if (attribute.startsWith("with", 2) && attribute.hasContext(2)) {
                with = attribute.getContextObject(2);
                attribute.fulfill(1);
            }
            ListTag newList = new ListTag(object);
            while (newList.size() < length) {
                newList.addObject(with);
            }
            return newList;
        });

        tagProcessor.registerTag(ListTag.class, "escape_contents", (attribute, object) -> {
            Deprecations.listEscapeContents.warn(attribute.context);
            ListTag escaped = new ListTag();
            for (String entry : object) {
                escaped.add(EscapeTagUtil.escape(entry));
            }
            return escaped;
        });

        tagProcessor.registerTag(ListTag.class, "unescape_contents", (attribute, object) -> {
            Deprecations.listEscapeContents.warn(attribute.context);
            ListTag escaped = new ListTag();
            for (String entry : object) {
                escaped.add(EscapeTagUtil.unEscape(entry));
            }
            return escaped;
        });

        // <--[tag]
        // @attribute <ListTag.contains_any_case_sensitive[<element>|...]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the list contains any of a list of given elements, case-sensitive.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ListTag.class, "contains_any_case_sensitive", (attribute, object, list) -> {
            boolean state = false;
            full_set:
            for (String element : object) {
                for (String sub_element : list) {
                    if (element.equals(sub_element)) {
                        state = true;
                        break full_set;
                    }
                }
            }
            return new ElementTag(state);
        });

        // <--[tag]
        // @attribute <ListTag.contains_any[<element>|...]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the list contains any of a list of given elements.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ListTag.class, "contains_any", (attribute, object, list) -> {
            boolean state = false;
            full_set:
            for (String element : object) {
                for (String sub_element : list) {
                    if (CoreUtilities.equalsIgnoreCase(element, sub_element)) {
                        state = true;
                        break full_set;
                    }
                }
            }
            return new ElementTag(state);
        });

        // <--[tag]
        // @attribute <ListTag.contains_case_sensitive[<element>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the list contains a given element, case-sensitive.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "contains_case_sensitive", (attribute, object, input) -> {
            boolean state = false;
            for (String element : object) {
                if (element.equals(input.asString())) {
                    state = true;
                    break;
                }
            }
            return new ElementTag(state);
        });

        // <--[tag]
        // @attribute <ListTag.contains_single[<element>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the list contains the given element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "contains_single", (attribute, object, needed) -> {
            String compare = needed.asLowerString();
            for (String element : object) {
                if (compare.equals(CoreUtilities.toLowerCase(element))) {
                    return new ElementTag(true);
                }
            }
            return new ElementTag(false);
        });

        // <--[tag]
        // @attribute <ListTag.contains[<element>|...]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the list contains all of the given elements.
        // See also <@link tag ListTag.contains_single> for safer single-value checks.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ListTag.class, "contains", (attribute, object, needed) -> {
            int gotten = 0;
            for (String check : needed) {
                for (String element : object) {
                    if (CoreUtilities.equalsIgnoreCase(element, check)) {
                        gotten++;
                        break;
                    }
                }
            }
            return new ElementTag(gotten == needed.size() && gotten > 0);
        });

        // <--[tag]
        // @attribute <ListTag.contains_match[<matcher>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the list contains an object that matches the specified matcher,
        // using the system behind <@link language Advanced Object Matching>.
        // -->
        tagProcessor.registerTag(ElementTag.class, ElementTag.class, "contains_match", (attribute, object, input) -> {
            String matcher = input.asString();
            for (ObjectTag objectTag : object.objectForms) {
                if (objectTag.tryAdvancedMatcher(matcher)) {
                    return new ElementTag(true);
                }
            }
            return new ElementTag(false);
        });

        // <--[tag]
        // @attribute <ListTag.random[(<#>)]>
        // @returns ObjectTag
        // @description
        // Gets a random item in the list and returns it.
        // Optionally, add [<#>] to instead get a list of multiple randomly chosen list entries.
        // @Example
        // # Narrates EITHER "one" OR "two" - different each time!
        // - narrate "<list[one|two].random>
        // @Example
        // # Could narrate "one|two", "two|three", OR "one|three" - different each time!
        // - narrate "<list[one|two|three].random[2]>
        // @Example
        // # Could narrate "one|two|three", "one|three|two", "two|one|three", "two|three|one", "three|two|one", OR "three|one|two" - different each time!
        // - narrate "<list[one|two|three].random[9999]>
        // -->
        tagProcessor.registerTag(ObjectTag.class, "random", (attribute, object) -> {
            if (attribute.hasParam()) {
                int count = Integer.parseInt(attribute.getParam());
                if (count < 0) {
                    attribute.echoError("Invalid 'random' count, cannot be negative.");
                    return null;
                }
                int times = 0;
                ArrayList<ObjectTag> available = new ArrayList<>(object.objectForms);
                ListTag toReturn = new ListTag();
                while (!available.isEmpty() && times < count) {
                    int random = CoreUtilities.getRandom().nextInt(available.size());
                    toReturn.addObject(available.get(random));
                    available.remove(random);
                    times++;
                }
                return toReturn;
            }
            else {
                if (object.isEmpty()) {
                    return null;
                }
                return object.objectForms.get(CoreUtilities.getRandom().nextInt(object.size()));
            }
        });

        // <--[tag]
        // @attribute <ListTag.closest_to[<text>]>
        // @returns ElementTag
        // @description
        // Returns the raw text of the item in the list that seems closest to the given value.
        // Particularly useful for command handlers, "<list[c1|c2|c3|...].closest_to[<argument>]>" to get the best option as  "did you mean" suggestion.
        // Be warned that this will always return /something/, excluding the case of an empty list, which will return an empty element.
        // Uses the logic of tag "ElementTag.difference"!
        // You can use that tag to add an upper limit on how different the text can be.
        // @Example
        // # Narrates "spawn"
        // - narrate "<list[dance|quit|spawn].closest_to[spwn]>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "closest_to", (attribute, object, input) -> {
            return new ElementTag(CoreUtilities.getClosestOption(object, input.asString()));
        });
    }

    public boolean containsCaseInsensitive(String val) {
        val = CoreUtilities.toLowerCase(val);
        for (String str : this) {
            if (CoreUtilities.toLowerCase(str).equals(val)) {
                return true;
            }
        }
        return false;
    }

    public static ObjectTagProcessor<ListTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public ObjectTag specialTagProcessing(Attribute attribute) {
        String attrLow = attribute.getAttributeWithoutParam(1);
        if (CoreConfiguration.debugVerbose) {
            Debug.log("ListTag alternate attribute " + attrLow);
        }
        if (ArgumentHelper.matchesInteger(attrLow)) {
            int index = Integer.parseInt(attrLow);
            if (index != 0) {
                attribute.fulfill(1);
                if (index < 1 || index > size()) {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("ListTag index " + index + " is out of range");
                    }
                    return null;
                }
                return getObject(index - 1);
            }
        }
        return null;
    }
}
