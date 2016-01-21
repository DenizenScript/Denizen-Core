package net.aufdemrand.denizencore.objects;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.objects.properties.Property;
import net.aufdemrand.denizencore.objects.properties.PropertyParser;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptRegistry;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.containers.core.ProcedureScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.tags.core.EscapeTags;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.NaturalOrderComparator;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.*;
import java.util.regex.Pattern;

public class dList extends ArrayList<String> implements dObject {

    public final static char internal_escape_char = (char) 0x05;
    public final static String internal_escape = String.valueOf(internal_escape_char);

    public static dList valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("li, fl")
    public static dList valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }

        ///////
        // Match @object format

        dList list = DenizenCore.getImplementation().valueOfFlagdList(string);

        if (list != null) {
            return list;
        }

        // Use value of string, which will separate values by the use of a pipe '|'
        return new dList(string.startsWith("li@") ? string.substring(3) : string);
    }


    public static boolean matches(String arg) {

        boolean flag = DenizenCore.getImplementation().matchesFlagdList(arg);

        return flag || arg.contains("|") || arg.contains(internal_escape) || arg.startsWith("li@");
    }


    /////////////
    //   Constructors
    //////////

    // A list of dObjects
    public dList(Collection<? extends dObject> dObjectList) {
        for (dObject obj : dObjectList) {
            add(obj.identify());
        }
    }

    // Empty dList
    public dList() {
    }

    // A string of items, split by '|'
    public dList(String items) {
        if (items != null && items.length() > 0) {
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
                else if ((brackets == 0) && (chr == '|' || chr == internal_escape_char)) {
                    add(items.substring(start, i));
                    start = i + 1;
                }
            }
            // If there is an item waiting, add it too
            if (start < items.length()) {
                add(items.substring(start, items.length()));
            }
        }
    }

    public dList(String flag, boolean is_flag, List<String> flag_contents) {
        if (is_flag) {
            this.flag = flag;
        }
        addAll(flag_contents);
    }

    // A List<String> of items
    public dList(List<String> items) {
        if (items != null) {
            addAll(items);
        }
    }

    // A Set<Object> of items
    public dList(Set<? extends Object> items) {
        if (items != null) {
            for (Object o : items) {
                add(o.toString());
            }
        }
    }

    // A List<String> of items, with a prefix
    public dList(List<String> items, String prefix) {
        for (String element : items) {
            add(prefix + element);
        }
    }

    /////////////
    //   Instance Fields/Methods
    //////////

    /**
     * Adds a list of dObjects to a dList by forcing each to 'identify()'.
     *
     * @param dObjects the List of dObjects
     * @return a dList
     */
    public dList addObjects(List<dObject> dObjects) {
        for (dObject obj : dObjects) {
            add(obj.identify());
        }

        return this;
    }

    /**
     * Fetches a String Array copy of the dList,
     * with the same size as the dList.
     *
     * @return the array copy
     */
    public String[] toArray() {
        return toArray(size());
    }

    /**
     * Fetches a String Array copy of the dList.
     *
     * @param arraySize the size of the new array
     * @return the array copy
     */
    public String[] toArray(int arraySize) { // TODO: Why does this exist?
        List<String> list = new ArrayList<String>();

        for (String string : this) {
            list.add(string); // TODO: Why is this a manual copy?
        }

        return list.toArray(new String[arraySize]);
    }


    // Returns if the list contains objects from the specified dClass
    // by using the matches() method.
    public boolean containsObjectsFrom(Class<? extends dObject> dClass) {

        // Iterate through elements until one matches() the dClass
        for (String element : this) {
            if (ObjectFetcher.checkMatch(dClass, element)) {
                return true;
            }
        }

        return false;
    }


    /**
     * Return a new list that includes only strings that match the values of an Enum array
     *
     * @param values the Enum's value
     * @return a filtered list
     */
    public List<String> filter(Enum[] values) {
        List<String> list = new ArrayList<String>();

        for (String string : this) {
            for (Enum value : values) {
                if (value.name().equalsIgnoreCase(string)) {
                    list.add(string);
                }
            }
        }

        if (!list.isEmpty()) {
            return list;
        }
        else {
            return null;
        }
    }


    // Return a list that includes only elements belonging to a certain class
    public <T extends dObject> List<T> filter(Class<T> dClass) {
        return filter(dClass, null);
    }


    public <T extends dObject> List<T> filter(Class<T> dClass, ScriptEntry entry) {
        List<T> results = new ArrayList<T>();

        for (String element : this) {

            try {
                if (ObjectFetcher.checkMatch(dClass, element)) {

                    T object = ObjectFetcher.getObjectFrom(dClass, element,
                            (entry == null ? DenizenCore.getImplementation().getTagContext(null) :
                                    entry.entryData.getTagContext()));

                    // Only add the object if it is not null, thus filtering useless
                    // list items

                    if (object != null) {
                        results.add(object);
                    }
                }
            }
            catch (Exception e) {
                dB.echoError(e);
            }
        }

        return results;
    }


    @Override
    public String toString() {
        return identify();
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
    public dList setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debug() {
        return "<G>" + prefix + "='<Y>" + identify() + "<G>'  ";
    }

    public String flag = null;

    @Override
    public boolean isUnique() {
        return flag != null;
    }

    @Override
    public String getObjectType() {
        return "List";
    }

    @Override
    public String identify() {
        if (flag != null) {
            return flag;
        }

        if (isEmpty()) {
            return "li@";
        }

        StringBuilder dScriptArg = new StringBuilder();
        dScriptArg.append("li@");
        for (String item : this) {
            dScriptArg.append(item);
            // Items are separated by the | character in dLists
            dScriptArg.append('|');
        }

        return dScriptArg.toString().substring(0, dScriptArg.length() - 1);
    }


    @Override
    public String identifySimple() {
        return identify();
    }

    public static void registerTags() {
        // <--[tag]
        // @attribute <li@list.space_separated>
        // @returns Element
        // @description
        // returns the list in a cleaner format, separated by spaces.
        // For example: a list of "one|two|three" will return "one two three".
        // -->

        registerTag("space_separated", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (((dList) object).isEmpty()) {
                    return new Element("").getAttribute(attribute.fulfill(1));
                }
                String input = " ";
                return new Element(parseString((dList) object, input)).getAttribute(attribute.fulfill(1));
            }
        });
        registerTag("as_string", registeredTags.get("space_separated"));
        registerTag("asstring", registeredTags.get("space_separated"));

        // <--[tag]
        // @attribute <li@list.separated_by[<text>]>>
        // @returns Element
        // @description
        // returns the list formatted, with each item separated by the defined text.
        // For example: <li@bob|jacob|mcmonkey.separated_by[ and ]> will return "bob and jacob and mcmonkey".
        // -->

        registerTag("separated_by", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList list = (dList) object;
                if (list.isEmpty()) {
                    return new Element("").getAttribute(attribute.fulfill(1));
                }
                String input = attribute.getContext(1);
                return new Element(parseString(list, input)).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.comma_separated>
        // @returns Element
        // @description
        // returns the list in a cleaner format, separated by commas.
        // For example: a list of "one|two|three" will return "one, two, three".
        // -->

        registerTag("comma_separated", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (((dList) object).isEmpty()) {
                    return new Element("").getAttribute(attribute.fulfill(1));
                }
                String input = ", ";
                return new Element(parseString((dList) object, input)).getAttribute(attribute.fulfill(1));
            }
        });
        registerTag("ascslist", registeredTags.get("comma_separated"));
        registerTag("as_cslist", registeredTags.get("comma_separated"));

        // @attribute <li@list.unseparated>
        // @returns Element
        // @description
        // returns the list in a less clean format, separated by nothing.
        // For example: a list of "one|two|three" will return "onetwothree".
        // -->

        registerTag("unseparated", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (((dList) object).isEmpty()) {
                    return new Element("").getAttribute(attribute.fulfill(1));
                }
                String input = "";
                return new Element(parseString((dList) object, input)).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.get_sub_items[<#>]>
        // @returns dList
        // @description
        // returns a list of the specified sub items in the list, as split by the
        // forward-slash character (/).
        // For example: .get_sub_items[1] on a list of "one/alpha|two/beta" will return "one|two".
        // -->

        registerTag("get_sub_items", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                int index = -1;
                if (aH.matchesInteger(attribute.getContext(1))) {
                    index = attribute.getIntContext(1) - 1;
                }
                attribute.fulfill(1);

                // <--[tag]
                // @attribute <li@list.get_sub_items[<#>].split_by[<element>]>
                // @returns dList
                // @description
                // returns a list of the specified sub item in the list, allowing you to specify a
                // character in which to split the sub items by. WARNING: When setting your own split
                // character, make note that it is CASE SENSITIVE.
                // For example: .get_sub_items[1].split_by[-] on a list of "one-alpha|two-beta" will return "one|two".
                // -->

                String split = "/";
                if (attribute.startsWith("split_by")) {
                    if (attribute.hasContext(1) && attribute.getContext(1).length() > 0) {
                        split = attribute.getContext(1);
                    }
                    attribute.fulfill(1);
                }

                if (index < 0) {
                    return null;
                }

                dList sub_list = new dList();

                for (String item : (dList) object) {
                    String[] strings = item.split(Pattern.quote(split));
                    if (strings.length > index) {
                        sub_list.add(strings[index]);
                    }
                    else {
                        sub_list.add("null");
                    }
                }

                return sub_list.getAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <li@list.map_get[<element>]>
        // @returns Element
        // @description
        // Returns the element split by the / symbol's value for the matching input element.
        // TODO: Clarify
        // For example: li@one/a|two/b.map_get[one] returns a.
        // -->

        registerTag("map_get", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (((dList) object).isEmpty()) {
                    return new Element("").getAttribute(attribute.fulfill(1));
                }
                String input = attribute.getContext(1);
                attribute.fulfill(1);

                // <--[tag]
                // @attribute <li@list.map_get[<element>].split_by[<element>]>
                // @returns Element
                // @description
                // Returns the element split by the given symbol's value for the matching input element.
                // TODO: Clarify
                // For example: li@one/a|two/b.map_get[one].split_by[/] returns a.
                // -->
                String split = "/";
                if (attribute.startsWith("split_by")) {
                    if (attribute.hasContext(1) && attribute.getContext(1).length() > 0) {
                        split = attribute.getContext(1);
                    }
                    attribute.fulfill(1);
                }

                for (String item : (dList) object) {
                    String[] strings = item.split(Pattern.quote(split), 2);
                    if (strings.length > 1 && strings[0].equalsIgnoreCase(input)) {
                        return new Element(strings[1]).getAttribute(attribute);
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <li@list.map_find_key[<element>]>
        // @returns Element
        // @description
        // Returns the element split by the / symbol's value for the matching input element.
        // TODO: Clarify
        // For example: li@one/a|two/b.map_find_key[a] returns one.
        // -->

        registerTag("map_find_key", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                String input = attribute.getContext(1);
                attribute.fulfill(1);

                // <--[tag]
                // @attribute <li@list.map_find_key[<element>].split_by[<element>]>
                // @returns Element
                // @description
                // Returns the element split by the given symbol's value for the matching input element.
                // TODO: Clarify
                // For example: li@one/a|two/b.map_find_key[a].split_by[/] returns one.
                // -->

                String split = "/";
                if (attribute.startsWith("split_by")) {
                    if (attribute.hasContext(1) && attribute.getContext(1).length() > 0) {
                        split = attribute.getContext(1);
                    }
                    attribute.fulfill(1);
                }
                for (String item : (dList) object) {
                    String[] strings = item.split(Pattern.quote(split), 2);
                    if (strings.length > 1 && strings[1].equalsIgnoreCase(input)) {
                        return new Element(strings[0]).getAttribute(attribute);
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <li@list.size>
        // @returns Element(Number)
        // @description
        // returns the size of the list.
        // For example: a list of "one|two|three" will return "3".
        // -->
        registerTag("size", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((dList) object).size()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.is_empty>
        // @returns Element(Boolean)
        // @description
        // returns whether the list is empty.
        // For example: a list of "" returns true, while "one" returns false.
        // -->
        registerTag("is_empty", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((dList) object).isEmpty()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.insert[...|...].at[<#>]>
        // @returns dList
        // @description
        // returns a new dList with the items specified inserted to the specified location.
        // For example: .insert[two|three].at[2] on a list of "one|four" will return "one|two|three|four".
        // -->

        registerTag("insert", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.insert[...] must have a value.");
                    return null;
                }
                dList items = dList.valueOf(attribute.getContext(1));
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("at") && attribute.hasContext(1)) {
                    dList result = new dList((dList) object);
                    int index = new Element(attribute.getContext(1)).asInt() - 1;
                    if (index < 0) {
                        index = 0;
                    }
                    if (index > result.size()) {
                        index = result.size();
                    }
                    for (int i = 0; i < items.size(); i++) {
                        result.add(index + i, items.get(i));
                    }
                    return result.getAttribute(attribute.fulfill(1));
                }
                else {
                    dB.echoError("The tag li@list.insert[...] must be followed by .at[#]!");
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <li@list.set[...|...].at[<#>]>
        // @returns dList
        // @description
        // returns a new dList with the items specified inserted to the specified location, replacing the element
        // already at that location.
        // For example: .set[potato].at[2] on a list of "one|two|three" will return "one|potato|three".
        // -->

        registerTag("set", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.set[...] must have a value.");
                    return null;
                }
                if (((dList) object).isEmpty()) {
                    return null;
                }
                dList items = dList.valueOf(attribute.getContext(1));
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("at")
                        && attribute.hasContext(1)) {
                    dList result = new dList((dList) object);
                    int index = new Element(attribute.getContext(1)).asInt() - 1;
                    if (index < 0) {
                        index = 0;
                    }
                    if (index > result.size() - 1) {
                        index = result.size() - 1;
                    }
                    result.remove(index);
                    for (int i = 0; i < items.size(); i++) {
                        result.add(index + i, items.get(i));
                    }
                    return result.getAttribute(attribute.fulfill(1));
                }
                else {
                    dB.echoError("The tag li@list.set[...] must be followed by .at[#]!");
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <li@list.include[...|...]>
        // @returns dList
        // @description
        // returns a new dList including the items specified.
        // For example: .include[three|four] on a list of "one|two" will return "one|two|three|four".
        // -->

        registerTag("include", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.include[...] must have a value.");
                    return null;
                }
                dList list = new dList((dList) object);
                list.addAll(dList.valueOf(attribute.getContext(1)));
                return list.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.exclude[...|...]>
        // @returns dList
        // @description
        // returns a new dList excluding the items specified.
        // For example: .exclude[two|four] on a list of "one|two|three|four" will return "one|three".
        // -->

        registerTag("exclude", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.exclude[...] must have a value.");
                    return null;
                }
                dList exclusions = dList.valueOf(attribute.getContext(1));
                // Create a new dList that will contain the exclusions
                dList list = new dList((dList) object);
                // Iterate through
                for (String exclusion : exclusions) {
                    for (int i = 0; i < list.size(); i++) {
                        if (list.get(i).equalsIgnoreCase(exclusion)) {
                            list.remove(i--);
                        }
                    }
                }
                // Return the modified list
                return list.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.remove[<#>|...]>
        // @returns dList
        // @description
        // returns a new dList excluding the items at the specified index.
        // For example: .remove[2] on a list of "one|two|three|four" will return "one|three|four".
        // Also supports [first] and [last] values.
        // -->

        registerTag("remove", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.remove[#] must have a value.");
                    return null;
                }
                dList indices = dList.valueOf(attribute.getContext(1));
                dList list = new dList((dList) object);
                for (String index : indices) {
                    int remove;
                    if (index.equalsIgnoreCase("last")) {
                        remove = list.size() - 1;
                    }
                    else if (index.equalsIgnoreCase("first")) {
                        remove = 0;
                    }
                    else {
                        remove = new Element(index).asInt() - 1;
                    }
                    if (remove >= 0 && remove < list.size()) {
                        list.set(remove, "\0");
                    }
                }
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).equals("\0")) {
                        list.remove(i--);
                    }
                }
                return list.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.reverse>
        // @returns dList
        // @description
        // returns a copy of the list, with all items placed in opposite order.
        // For example: a list of "one|two|three" will become "three|two|one".
        // -->

        registerTag("reverse", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList list = new dList((dList) object);
                Collections.reverse(list);
                return list.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.deduplicate>
        // @returns dList
        // @description
        // returns a copy of the list with any duplicate items removed.
        // For example: a list of "one|one|two|three" will become "one|two|three".
        // -->

        registerTag("deduplicate", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList obj = (dList) object;
                dList list = new dList();
                int size = obj.size();
                for (int i = 0; i < size; i++) {
                    String entry = obj.get(i);
                    boolean duplicate = false;
                    for (int x = 0; x < i; x++) {
                        if (obj.get(x).equalsIgnoreCase(entry)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        list.add(entry);
                    }
                }
                return list.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.get[<#>|...]>
        // @returns dObject
        // @description
        // returns an element of the value specified by the supplied context.
        // For example: .get[1] on a list of "one|two" will return "one", and .get[2] will return "two"
        // Specify more than one index to get a list of results.
        // -->

        registerTag("get", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.get[...] must have a value.");
                    return null;
                }
                dList list = new dList((dList) object);
                if (list.isEmpty()) {
                    return null;
                }
                dList indices = dList.valueOf(attribute.getContext(1));
                if (indices.size() > 1) {
                    dList results = new dList();
                    for (String index : indices) {
                        int ind = aH.getIntegerFrom(index);
                        if (ind > 0 && ind <= list.size()) {
                            results.add(list.get(ind - 1));
                        }
                    }
                    return results.getAttribute(attribute.fulfill(1));
                }
                if (indices.size() > 0) {
                    int index = aH.getIntegerFrom(indices.get(0));
                    if (index > list.size()) {
                        return null;
                    }
                    if (index < 1) {
                        index = 1;
                    }
                    attribute = attribute.fulfill(1);

                    // <--[tag]
                    // @attribute <li@list.get[<#>].to[<#>]>
                    // @returns dList
                    // @description
                    // returns all elements in the range from the first index to the second.
                    // For example: .get[1].to[3] on a list of "one|two|three|four" will return "one|two|three"
                    // -->
                    if (attribute.startsWith("to") && attribute.hasContext(1)) {
                        int index2 = attribute.getIntContext(1);
                        if (index2 > list.size()) {
                            index2 = list.size();
                        }
                        if (index2 < 1) {
                            index2 = 1;
                        }
                        String item = "";
                        for (int i = index; i <= index2; i++) {
                            item += list.get(i - 1) + (i < index2 ? "|" : "");
                        }
                        return new dList(item).getAttribute(attribute.fulfill(1));
                    }
                    else {
                        String item;
                        item = list.get(index - 1);
                        return ObjectFetcher.pickObjectFor(item).getAttribute(attribute);
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <li@list.find_all_partial[<element>]>
        // @returns dList(Element(Number))
        // @description
        // returns all the numbered locations of elements that contain the text within a list,
        // or an empty list if the list does not contain that item.
        // For example: .find_all_partial[tw] on a list of "one|two|three|two" will return "2|4".
        // TODO: Take multiple inputs? Or a regex?
        // -->

        registerTag("find_all_partial", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.find_all_partial[...] must have a value.");
                    return null;
                }
                dList list = new dList((dList) object);
                String test = attribute.getContext(1).toUpperCase();
                dList positions = new dList();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).toUpperCase().contains(test)) {// TODO: Efficiency
                        positions.add(String.valueOf(i + 1));
                    }
                }
                return positions.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.find_all[<element>]>
        // @returns dList(Element(Number))
        // @description
        // returns all the numbered locations of elements that match the text within a list,
        // or an empty list if the list does not contain that item.
        // For example: .find[two] on a list of "one|two|three|two" will return "2|4".
        // TODO: Take multiple inputs? Or a regex?
        // -->

        registerTag("find_all", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.find_all[...] must have a value.");
                    return null;
                }
                dList list = new dList((dList) object);
                dList positions = new dList();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).equalsIgnoreCase(attribute.getContext(1))) {
                        positions.add(String.valueOf(i + 1));
                    }
                }
                return positions.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.find_partial[<element>]>
        // @returns Element(Number)
        // @description
        // returns the numbered location of the first partially matching element within a list,
        // or -1 if the list does not contain that item.
        // For example: .find[two] on a list of "one|two|three" will return "2".
        // TODO: Take multiple inputs? Or a regex?
        // -->

        registerTag("find_partial", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.find_partial[...] must have a value.");
                    return null;
                }
                dList list = new dList((dList) object);
                String test = attribute.getContext(1).toUpperCase();
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).toUpperCase().contains(test)) { // TODO: Efficiency
                        return new Element(i + 1).getAttribute(attribute.fulfill(1));
                    }
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <li@list.find[<element>]>
        // @returns Element(Number)
        // @description
        // returns the numbered location of an element within a list,
        // or -1 if the list does not contain that item.
        // For example: .find[two] on a list of "one|two|three" will return "2".
        // TODO: Take multiple inputs? Or a regex?
        // -->

        registerTag("find", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.find[...] must have a value.");
                    return null;
                }
                dList list = new dList((dList) object);
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).equalsIgnoreCase(attribute.getContext(1))) {
                        return new Element(i + 1).getAttribute(attribute.fulfill(1));
                    }
                }
                // TODO: This should be find_partial or something
            /*
            for (int i = 0; i < size(); i++) {
                if (get(i).toUpperCase().contains(attribute.getContext(1).toUpperCase()))
                    return new Element(i + 1).getAttribute(attribute.fulfill(1));
            }
            */
                return new Element(-1).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.count[<element>]>
        // @returns Element(Number)
        // @description
        // returns how many times in the sub-list occurs.
        // For example: a list of "one|two|two|three" .count[two] returns 2.
        // -->

        registerTag("count", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.count[...] must have a value.");
                    return null;
                }
                dList list = new dList((dList) object);
                String element = attribute.getContext(1);
                int count = 0;
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).equalsIgnoreCase(element)) {
                        count++;
                    }
                }
                return new Element(count).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.first>
        // @returns Element
        // @description
        // returns the first element in the list.
        // If the list is empty, returns null instead.
        // For example: a list of "one|two|three" will return "one".
        // Effectively equivalent to .get[1]
        // -->

        registerTag("first", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList list = new dList((dList) object);
                if (list.isEmpty()) {
                    return null;
                }
                else {
                    return new Element(list.get(0)).getAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <li@list.last>
        // @returns Element
        // @description
        // returns the last element in the list.
        // If the list is empty, returns null instead.
        // For example: a list of "one|two|three" will return "three".
        // Effectively equivalent to .get[<list.size>]
        // -->

        registerTag("last", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList list = new dList((dList) object);
                if (list.isEmpty()) {
                    return null;
                }
                else {
                    return new Element(list.get(list.size() - 1)).getAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <li@list.numerical>
        // @returns Element
        // @description
        // returns the list sorted to be in numerical order.
        // For example: a list of "3|2|1|10" will return "1|2|3|10".
        // -->

        registerTag("numerical", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList list = new dList((dList) object);
                Collections.sort(list, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        double value = new Element(o1).asDouble() - new Element(o2).asDouble();
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
                });
                return list.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.alphanumeric>
        // @returns Element
        // @description
        // returns the list sorted to be in alphabetical/numerical order.
        // For example: a list of "b|c|a10|a1" will return "a1|a10|b|c".
        // -->

        registerTag("alphanumeric", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList list = new dList((dList) object);
                Collections.sort(list, new NaturalOrderComparator());
                return list.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.alphabetical>
        // @returns Element
        // @description
        // returns the list sorted to be in alphabetical order.
        // For example: a list of "c|d|q|a|g" will return "a|c|d|g|q".
        // -->

        registerTag("alphabetical", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList list = new dList((dList) object);
                Collections.sort(list, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.compareToIgnoreCase(o2);
                    }
                });
                return list.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.sort[<procedure>]>
        // @returns Element
        // @description
        // returns a list sorted according to the return values of a procedure.
        // The <procedure> should link a procedure script that takes two definitions each of which will be an item
        // in the list, and returns -1, 0, or 1 based on whether the second item should be added. EG, if a procedure
        // with definitions "one" and "two" returned -1, it would place "two" after "one". Note that this
        // uses some complex internal sorting code that could potentially throw errors if the procedure does not return
        // consistently - EG, if "one" and "two" returned 1, but "two" and "one" returned 1 as well - obviously,
        // "two" can not be both before AND after "one"!
        // Note that the script should ALWAYS return -1, 0, or 1, or glitches could happen!
        // Note that if two inputs are exactly equal, the procedure should always return 0.
        // -->

        registerTag("sort", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList obj = new dList((dList) object);
                final ProcedureScriptContainer script = (ProcedureScriptContainer) dScript.valueOf(attribute.getContext(1)).getContainer();
                if (script == null) {
                    dB.echoError("'" + attribute.getContext(1) + "' is not a valid procedure script!");
                    return obj.getAttribute(attribute.fulfill(1));
                }
                final ScriptEntry entry = attribute.getScriptEntry();
                attribute = attribute.fulfill(1);
                // <--[tag]
                // @attribute <li@list.sort[<procedure>].context[<context>]>
                // @returns Element
                // @description
                // Sort a list, with context. See <@link tag li@list.sort[<procedure>]> for general sort details.
                // -->
                dList context = new dList();
                if (attribute.startsWith("context")) {
                    context = dList.valueOf(attribute.getContext(1));
                    attribute = attribute.fulfill(1);
                }
                final dList context_send = context;
                List<String> list = new ArrayList<String>(obj);
                try {
                    Collections.sort(list, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            List<ScriptEntry> entries = script.getBaseEntries(entry == null ?
                                    DenizenCore.getImplementation().getEmptyScriptEntryData() : entry.entryData.clone());
                            if (entries.isEmpty()) {
                                return 0;
                            }
                            long id = DetermineCommand.getNewId();
                            ScriptBuilder.addObjectToEntries(entries, "ReqId", id);
                            InstantQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId("DLIST_SORT"));
                            queue.addEntries(entries);
                            queue.setReqId(id);
                            int x = 1;
                            dList definitions = new dList();
                            definitions.add(o1);
                            definitions.add(o2);
                            definitions.addAll(context_send);
                            String[] definition_names = null;
                            try {
                                definition_names = script.getString("definitions").split("\\|");
                            }
                            catch (Exception e) { /* IGNORE */ }
                            for (String definition : definitions) {
                                String name = definition_names != null && definition_names.length >= x ?
                                        definition_names[x - 1].trim() : String.valueOf(x);
                                queue.addDefinition(name, definition);
                                dB.echoDebug(entries.get(0), "Adding definition %" + name + "% as " + definition);
                                x++;
                            }
                            queue.start();
                            int res = 0;
                            if (DetermineCommand.hasOutcome(id)) {
                                res = new Element(DetermineCommand.getOutcome(id).get(0)).asInt();
                            }
                            if (res < 0) {
                                return -1;
                            }
                            else if (res > 0) {
                                return 1;
                            }
                            else {
                                return 0;
                            }
                        }
                    });
                }
                catch (Exception e) {
                    dB.echoError("list.sort[...] tag failed - procedure returned unreasonable response - internal error: " + e.getMessage());
                }
                return new dList(list).getAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <li@list.filter[<tag>]>
        // @returns dList
        // @description
        // returns a copy of the list with all its contents parsed through the given tag and only including ones that returned 'true'.
        // For example: a list of '1|2|3|4|5' .filter[is[or_more].than[3]] returns a list of '3|4|5'.
        // -->

        registerTag("filter", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList newlist = new dList();
                try {
                    for (String str : (dList) object) {
                        if (str == null) {
                            dB.echoError("Null string in dList! (From .filter tag)");
                        }
                        String result = ObjectFetcher.pickObjectFor(str).getAttribute(new Attribute(attribute.getContext(1),
                                attribute.getScriptEntry(), attribute.context));
                        if (result != null && result.equalsIgnoreCase("true")) {
                            newlist.add(str);
                        }
                    }
                }
                catch (Exception ex) {
                    dB.echoError(ex);
                }
                return newlist.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.parse[<tag>]>
        // @returns dList
        // @description
        // returns a copy of the list with all its contents parsed through the given tag.
        // For example: a list of 'one|two' .parse[to_uppercase] returns a list of 'ONE|TWO'.
        // -->

        registerTag("parse", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList newlist = new dList();
                try {
                    for (String str : (dList) object) {
                        newlist.add(ObjectFetcher.pickObjectFor(str).getAttribute(new Attribute(attribute.getContext(1),
                                attribute.getScriptEntry(), attribute.context)));
                    }
                }
                catch (Exception ex) {
                    dB.echoError(ex);
                }
                return newlist.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.escape_contents>
        // @returns dList
        // @description
        // returns a copy of the list with all its contents escaped.
        // Inverts <@link tag li@list.unescape_contents>.
        // See <@link language property escaping>.
        // -->

        registerTag("escape_contents", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList escaped = new dList();
                for (String entry : (dList) object) {
                    escaped.add(EscapeTags.Escape(entry));
                }
                return escaped.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.unescape_contents>
        // @returns dList
        // @description
        // returns a copy of the list with all its contents unescaped.
        // Inverts <@link tag li@list.escape_contents>.
        // See <@link language property escaping>.
        // -->

        registerTag("unescape_contents", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList escaped = new dList();
                for (String entry : (dList) object) {
                    escaped.add(EscapeTags.unEscape(entry));
                }
                return escaped.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.contains_any_case_sensitive[<element>|...]>
        // @returns Element(Boolean)
        // @description
        // returns whether the list contains any of a list of given elements, case-sensitive.
        // -->

        registerTag("contains_any_case_sensitive", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.contains_any_case_sensitive[...] must have a value.");
                    return null;
                }
                dList list = dList.valueOf(attribute.getContext(1));
                boolean state = false;

                full_set:
                for (String element : (dList) object) {
                    for (String sub_element : list) {
                        if (element.equals(sub_element)) {
                            state = true;
                            break full_set;
                        }
                    }
                }

                return new Element(state).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.contains_any[<element>|...]>
        // @returns Element(Boolean)
        // @description
        // returns whether the list contains any of a list of given elements.
        // -->

        registerTag("contains_any", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.contains_any[...] must have a value.");
                    return null;
                }
                dList list = dList.valueOf(attribute.getContext(1));
                boolean state = false;

                full_set:
                for (String element : (dList) object) {
                    for (String sub_element : list) {
                        if (element.equalsIgnoreCase(sub_element)) {
                            state = true;
                            break full_set;
                        }
                    }
                }

                return new Element(state).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.contains_case_sensitive[<element>]>
        // @returns Element(Boolean)
        // @description
        // returns whether the list contains a given element, case-sensitive.
        // -->

        registerTag("contains_case_sensitive", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.contains_case_sensitive[...] must have a value.");
                    return null;
                }
                boolean state = false;

                for (String element : (dList) object) {
                    if (element.equals(attribute.getContext(1))) {
                        state = true;
                        break;
                    }
                }

                return new Element(state).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.contains[<element>|...]>
        // @returns Element(Boolean)
        // @description
        // returns whether the list contains all of the given elements.
        // -->

        registerTag("contains", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag li@list.contains[...] must have a value.");
                    return null;
                }
                dList needed = dList.valueOf(attribute.getContext(1));
                int gotten = 0;

                for (String check : needed) {
                    for (String element : (dList) object) {
                        if (element.equalsIgnoreCase(check)) {
                            gotten++;
                            break;
                        }
                    }
                }

                return new Element(gotten == needed.size() && gotten > 0).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.type>
        // @returns Element
        // @description
        // Always returns 'List' for dList objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->

        registerTag("type", new TagRunnable() { // TODO: ???
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element("List").getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.random[<#>]>
        // @returns Element
        // @description
        // Gets a random item in the list and returns it as an Element.
        // Optionally, add [<#>] to get a list of multiple randomly chosen elements.
        // For example: .random on a list of "one|two" could return EITHER "one" or "two" - different each time!
        // For example: .random[2] on a list of "one|two|three" could return "one|two", "two|three", OR "one|three" - different each time!
        // For example: .random[9999] on a list of "one|two|three" could return "one|two|three", "one|three|two", "two|one|three",
        // "two|three|one", "three|two|one", OR "three|one|two" - different each time!
        // -->

        registerTag("random", new TagRunnable() { // TODO: ???
            @Override
            public String run(Attribute attribute, dObject object) {
                dList obj = (dList) object;
                if (obj.isEmpty()) {
                    return null;
                }
                if (attribute.hasContext(1)) {
                    int count = Integer.valueOf(attribute.getContext(1));
                    int times = 0;
                    ArrayList<String> available = new ArrayList<String>();
                    available.addAll(obj);
                    dList toReturn = new dList();
                    while (!available.isEmpty() && times < count) {
                        int random = CoreUtilities.getRandom().nextInt(available.size());
                        toReturn.add(available.get(random));
                        available.remove(random);
                        times++;
                    }
                    return toReturn.getAttribute(attribute.fulfill(1));
                }
                else {
                    return ObjectFetcher.pickObjectFor(obj.get(CoreUtilities.getRandom().nextInt(obj.size())))
                            .getAttribute(attribute.fulfill(1));
                }
            }
        });


        /////////////////
        // dObject attributes
        ///////////////

        // <--[tag]
        // @attribute <li@list.prefix>
        // @returns Element
        // @description
        // Returns the prefix for this object. By default this will return 'List', however certain situations will
        // return a finer scope. All objects fetchable by the Object Fetcher will return a valid prefix for the object
        // that is fulfilling this attribute.
        // -->

        registerTag("prefix", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((dList) object).prefix).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.debug>
        // @returns Element
        // @description
        // Returns the debug entry for this object. This contains the prefix, the name of the dList object, and the
        // data that is held within. All objects fetchable by the Object Fetcher will return a valid
        // debug entry for the object that is fulfilling this attribute.
        // -->

        registerTag("debug", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(object.debug()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <li@list.type>
        // @returns Element
        // @description
        // Always returns 'List' for dScript objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->

        registerTag("identify", new TagRunnable() { // TODO: ???
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(object.identify()).getAttribute(attribute.fulfill(1));
            }
        });

    }

    public static HashMap<String, TagRunnable> registeredTags = new HashMap<String, TagRunnable>();

    public static void registerTag(String name, TagRunnable runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registeredTags.put(name, runnable);
    }

    //
    // parseString(dList object, String str)
    // Returns a string value of the elements of dList separated by str.
    //

    private static String parseString(dList obj, String spacer) {

        StringBuilder dScriptArg = new StringBuilder();
        for (String item : obj) {
            dScriptArg.append(item);
            dScriptArg.append(spacer);
        }
        return dScriptArg.toString().substring(0,
                dScriptArg.length() - spacer.length());
    }

    @Override
    public String getAttribute(Attribute attribute) {

        if (attribute == null) {
            return null;
        }

        // TODO: Scrap getAttribute, make this functionality a core system
        String attrLow = CoreUtilities.toLowerCase(attribute.getAttributeWithoutContext(1));
        TagRunnable tr = registeredTags.get(attrLow);
        if (tr != null) {
            if (!tr.name.equals(attrLow)) {
                dB.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + tr.name + "': '" + attrLow + "'.");
            }
            return tr.run(attribute, this);
        }

        //
        // TODO: Everything below is deprecated and will be moved to registerTag() format
        //

        // FLAG Specific Attributes

        // Note: is_expired attribute is handled in player/npc/server
        // since expired flags return 'null'

        // <--[tag]
        // @attribute <fl@flag_name.is_expired>
        // @returns Element(Boolean)
        // @description
        // returns true of the flag is expired or does not exist, false if it
        // is not yet expired, or has no expiration.
        // -->
        // NOTE: Defined in UtilTags.java

        // Need this attribute (for flags) since they return the last
        // element of the list, unless '.as_list' is specified.

        // <--[tag]
        // @attribute <fl@flag_name.as_list>
        // @returns dList
        // @description
        // returns a dList containing the items in the flag.
        // -->
        if (flag != null && (attribute.startsWith("as_list")
                || attribute.startsWith("aslist"))) {
            return new dList(this).getAttribute(attribute.fulfill(1));
        }


        // If this is a flag, return the last element (this is how it has always worked...)
        // Use as_list to return a list representation of the flag.
        // If this is NOT a flag, but instead a normal dList, return an element
        // with dList's identify() value.

        // Iterate through this object's properties' attributes
        for (Property property : PropertyParser.getProperties(this)) {
            String returned = property.getAttribute(attribute);
            if (returned != null) {
                return returned;
            }
        }

        return (flag != null
                ? new Element(DenizenCore.getImplementation().getLastEntryFromFlag(flag)).getAttribute(attribute)
                : new Element(identify()).getAttribute(attribute));
    }
}
