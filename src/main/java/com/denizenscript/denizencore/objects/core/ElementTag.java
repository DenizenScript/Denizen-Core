package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.scripts.commands.queue.Comparable;
import com.denizenscript.denizencore.tags.*;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.SQLEscaper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.core.EscapeTagBase;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElementTag implements ObjectTag {

    // <--[language]
    // @name ElementTags
    // @group Object System
    // @description
    // ElementTags are simple objects that contain either a boolean (true/false),
    // string, or number value. Their main usage is within the replaceable tag
    // system, often times returned from the use of another tag that isn't returning
    // a specific object type, such as a location or entity. For example,
    // <player.name> or <list[item1|item2|item3].as_cslist> will both return ElementTags.
    //
    // Pluses to the ElementTag system is the ability to utilize its attributes that
    // can provide a range of functionality that should be familiar from any other
    // programming language, such as 'to_uppercase', 'split', 'replace', 'contains',
    // and many more. See 'elementtag.*' tags for more information.
    //
    // While information fetched from other tags resulting in an ElementTag is often
    // times automatically handled, it may be desirable to utilize element
    // attributes from text/numbers/etc. that aren't already an element object.
    // To accomplish this, the standard 'element' tag base can be used for the
    // creation of a new element. For example: <element[This_is_a_test].to_uppercase>
    // will result in the value 'THIS_IS_A_TEST' Note that while other objects often
    // return their object identifier (el@, li@, e@, etc.), elements do not.
    //
    // For format info, see <@link language el@>
    // -->

    @Deprecated
    public final static ElementTag TRUE = new ElementTag(Boolean.TRUE);
    @Deprecated
    public final static ElementTag FALSE = new ElementTag(Boolean.FALSE);
    @Deprecated
    public final static ElementTag SERVER = new ElementTag("server");
    @Deprecated
    public final static ElementTag NULL = new ElementTag("null");

    final static Pattern VALUE_PATTERN =
            Pattern.compile("el@val(?:ue)?\\[([^\\[\\]]+)\\].*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);


    public static ElementTag valueOf(String string) {
        return valueOf(string, null);
    }

    // <--[language]
    // @name el@
    // @group Object Fetcher System
    // @description
    // el@ refers to the 'object identifier' of an Element. The 'el@' is notation for Denizen's Object
    // Fetcher. The constructor for an ElementTag is just any text.
    //
    // For example 'el@hello' forms an element with text 'hello'.
    //
    // Elements do not output with 'el@' visible. The 'el@' is only for use as a shorthanded constructor.
    // If you need an element constructor, consider using the '<element[text here]>' tag base instead.
    //
    // For general info, see <@link language Element>
    // -->

    @Fetchable("el")
    public static ElementTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }

        Matcher m = VALUE_PATTERN.matcher(string);

        // Allow construction of elements with el@val[<value>]
        if (m.matches()) {
            String value = m.group(1);
            return new ElementTag(value);
        }

        return new ElementTag(CoreUtilities.toLowerCase(string).startsWith("el@") ? string.substring(3) : string);
    }

    public static boolean matches(String string) {
        return string != null;
    }

    /**
     * Handle null ObjectTags appropriately for potentionally null tags.
     * Will show a dB error message and return Element.NULL for null objects.
     *
     * @param tag    The input string that produced a potentially null object, for debugging.
     * @param object The potentially null object.
     * @param type   The type of object expected, for debugging. (EG: 'dNPC')
     * @return The object or Element.NULL if the object is null.
     */
    public static <T extends ObjectTag> T handleNull(String tag, T object, String type, boolean has_fallback) {
        if (object == null) {
            if (!has_fallback) {
                Debug.echoError("'" + tag + "' is an invalid " + type + "!");
            }
            return null;
        }
        return object;
    }

    private final String element;

    public ElementTag(String string) {
        this.prefix = "element";
        if (string == null) {
            if (Debug.verbose) {
                try {
                    throw new RuntimeException("Trace");
                }
                catch (Exception ex) {
                    Debug.echoError(ex);
                }
                Debug.log("Element - Null construction!");
            }
            this.element = "null";
        }
        else {
            this.element = TagManager.cleanOutput(string);
        }
    }

    public ElementTag(boolean bool) {
        this.prefix = "boolean";
        this.element = String.valueOf(bool);
    }

    public ElementTag(int integer) {
        this.prefix = "number";
        this.element = String.valueOf(integer);
    }

    public ElementTag(byte byt) {
        this.prefix = "number";
        this.element = String.valueOf(byt);
    }

    public ElementTag(short shrt) {
        this.prefix = "number";
        this.element = String.valueOf(shrt);
    }

    public ElementTag(long lng) {
        this.prefix = "number";
        this.element = String.valueOf(lng);
    }

    public ElementTag(BigDecimal bdl) {
        this.prefix = "decimal";
        this.element = CoreUtilities.bigDecToString(bdl);
    }

    public ElementTag(double dbl) {
        this.prefix = "decimal";
        this.element = CoreUtilities.doubleToString(dbl);
    }

    public ElementTag(float flt) {
        this.prefix = "decimal";
        this.element = CoreUtilities.doubleToString(flt);
    }

    public ElementTag(String prefix, String string) {
        if (prefix == null) {
            this.prefix = "element";
        }
        else {
            this.prefix = prefix;
        }
        this.element = TagManager.cleanOutput(string);
    }

    static final BigDecimal max = new BigDecimal("10E1000");

    private BigDecimal getBD(String text) {
        BigDecimal bd = new BigDecimal(text);
        if (bd.compareTo(max) >= 1) {
            Debug.echoError("Unreasonably large number detected!");
            return max;
        }
        return bd;
    }

    public BigDecimal asBigDecimal() {
        return getBD(element.replaceAll("%", ""));
    }

    public double asDouble() {
        return Double.valueOf(element.replaceAll("%", ""));
    }

    public float asFloat() {
        return Float.valueOf(element.replaceAll("%", ""));
    }

    public int asInt() {
        try {
            return Integer.valueOf(element.replaceAll("(%)|(\\.\\d+)", ""));
        }
        catch (NumberFormatException ex) {
            Debug.echoError("'" + element + "' is not a valid integer!");
            return 0;
        }
    }

    public long asLong() {
        try {
            return Long.valueOf(element.replaceAll("(%)|(\\.\\d+)", ""));
        }
        catch (NumberFormatException ex) {
            Debug.echoError("'" + element + "' is not a valid integer!");
            return 0;
        }
    }

    public boolean asBoolean() {
        return Boolean.valueOf(element.replaceAll("el@", ""));
    }

    public String asString() {
        return element;
    }

    public boolean isBoolean() {
        return (element != null && (element.equalsIgnoreCase("true") || element.equalsIgnoreCase("false")));
    }

    public boolean isDouble() {
        try {
            if (!Double.valueOf(element).isNaN()) {
                return true;
            }
        }
        catch (Exception e) {
        }
        return false;
    }

    public boolean isFloat() {
        try {
            if (!Float.valueOf(element).isNaN()) {
                return true;
            }
        }
        catch (Exception e) {
        }
        return false;
    }

    public boolean isInt() {
        try {
            Integer val = Integer.valueOf(element.replaceAll("(%)|(\\.\\d+)", ""));
            if (val.hashCode() != 0.5) { // if intentionally always passes
                return true;
            }
        }
        catch (Exception e) {
        }
        return false;
    }

    public boolean isString() {
        return (element != null && !element.isEmpty());
    }

    public boolean matchesType(Class<? extends ObjectTag> dClass) {
        return ObjectFetcher.checkMatch(dClass, element);
    }

    public <T extends ObjectTag> T asType(Class<T> dClass) {
        return ObjectFetcher.getObjectFrom(dClass, element);
    }

    public <T extends ObjectTag> T asType(Class<T> dClass, TagContext context) {
        return ObjectFetcher.getObjectFrom(dClass, element, context);
    }

    public boolean matchesEnum(Enum[] values) {
        for (Enum value : values) {
            if (value.name().equalsIgnoreCase(element)) {
                return true;
            }
        }

        return false;
    }

    private String prefix;

    @Override
    public String getObjectType() {
        return "Element";
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String identify() {
        return element;
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
    public boolean isUnique() {
        return false;
    }

    public static void registerTags() {

        /////////////////////
        //   CONVERSION ATTRIBUTES
        /////////////////

        registerTag("is", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {

                // <--[tag]
                // @attribute <ElementTag.is[<operator>].to[<element>]>
                // @returns ElementTag(Boolean)
                // @group comparison
                // @description
                // Takes an operator, and compares the value of the element to the supplied
                // element. Returns the outcome of the comparable, either true or false. For
                // information on operators, see <@link language operator>.
                // Equivalent to <@link tag ElementTag.is[<operator>].than[<element>]>
                // -->

                // <--[tag]
                // @attribute <ElementTag.is[<operator>].than[<element>]>
                // @returns ElementTag(Boolean)
                // @group comparison
                // @description
                // Takes an operator, and compares the value of the element to the supplied
                // element. Returns the outcome of the comparable, either true or false. For
                // information on operators, see <@link language operator>.
                // Equivalent to <@link tag ElementTag.is[<operator>].to[<element>]>
                // -->
                if (attribute.hasContext(1) && (attribute.startsWith("to", 2) || attribute.startsWith("than", 2)) && attribute.hasContext(2)) {

                    // Use the Comparable object as implemented for the IF command. First, a new Comparable!
                    Comparable com = new Comparable();

                    // Check for negative logic
                    String operator;
                    if (attribute.getContext(1).startsWith("!")) {
                        operator = attribute.getContext(1).substring(1);
                        com.setNegativeLogic();
                    }
                    else {
                        operator = attribute.getContext(1);
                    }

                    // Operator is the value of the .is[] context. Valid are Comparable.Operators, same
                    // as used by the IF command.
                    Comparable.Operator comparableOperator = null;
                    try {
                        comparableOperator = Comparable.Operator.valueOf(operator.replace("==", "EQUALS")
                                .replace(">=", "OR_MORE").replace("<=", "OR_LESS").replace("<", "LESS")
                                .replace(">", "MORE").replace("=", "EQUALS").toUpperCase());
                    }
                    catch (IllegalArgumentException e) {
                    }

                    if (comparableOperator != null) {
                        com.setOperator(comparableOperator);

                        // Comparable is the value of this element
                        com.setComparable(object.toString());
                        // Compared_to is the value of the .to[] context.
                        com.setComparedto(attribute.getContext(2));

                        attribute.fulfill(1);

                        return new ElementTag(com.determineOutcome());
                    }
                    else {
                        Debug.echoError("Unknown operator '" + operator + "'.");
                    }
                }

                return null;
            }
        });

        // <--[tag]
        // @attribute <ElementTag.is_boolean>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is a boolean ('true' or 'false').
        // -->
        registerTag("is_boolean", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(element.equalsIgnoreCase("true")
                        || element.equalsIgnoreCase("false"));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.is_integer>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is an integer number (a number without a decimal point).
        // -->
        registerTag("is_integer", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(ArgumentHelper.integerPrimitive.matcher(element).matches());
            }
        });

        // <--[tag]
        // @attribute <ElementTag.is_decimal>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is a valid decimal number (the decimal point is optional).
        // -->
        registerTag("is_decimal", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(ArgumentHelper.doublePrimitive.matcher(element).matches());
            }
        });

        // <--[tag]
        // @attribute <ElementTag.is_odd>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is an odd-valued decimal number. Returns 'false' for non-numbers.
        // -->
        registerTag("is_odd", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(ArgumentHelper.doublePrimitive.matcher(element).matches()
                            && (object.asBigDecimal().longValue() % 2) == 1);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.is_even>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is an even-valued decimal number. Returns 'false' for non-numbers.
        // -->
        registerTag("is_even", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(ArgumentHelper.doublePrimitive.matcher(element).matches()
                        && (object.asBigDecimal().longValue() % 2) == 0);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.as_element>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element as itself.
        // For use in special cases, generally not very useful.
        // -->
        registerTag("as_element", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return object;
            }
        });
        registerTag("aselement", tagProcessor.registeredObjectTags.get("as_element"));

        // <--[tag]
        // @attribute <ElementTag.as_boolean>
        // @returns ElementTag(Boolean)
        // @group conversion
        // @description
        // Returns the element as true/false.
        // -->
        registerTag("as_boolean", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(element.equalsIgnoreCase("true")
                        || element.equalsIgnoreCase("t")
                        || element.equalsIgnoreCase("1"));
            }
        });
        registerTag("asboolean", tagProcessor.registeredObjectTags.get("as_boolean"));

        // <--[tag]
        // @attribute <ElementTag.as_decimal>
        // @returns ElementTag(Decimal)
        // @group conversion
        // @description
        // Returns the element as a decimal number, or shows an error.
        // -->
        registerTag("as_decimal", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                try {
                    return new ElementTag(Double.valueOf(element));
                }
                catch (NumberFormatException e) {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("'" + element + "' is not a valid decimal number.");
                    }
                    return null;
                }
            }
        });
        registerTag("as_double", tagProcessor.registeredObjectTags.get("as_decimal"));
        registerTag("asdouble", tagProcessor.registeredObjectTags.get("as_decimal"));

        registerTag("as_int", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                Deprecations.elementAsInTag.warn(attribute.context);
                String element = object.element;
                try {
                    return new ElementTag(Double.valueOf(element).longValue());
                }
                catch (NumberFormatException e) {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("'" + element + "' is not a valid decimal number.");
                    }
                    return null;
                }
            }
        });
        registerTag("asint", tagProcessor.registeredObjectTags.get("as_int"));

        // <--[tag]
        // @attribute <ElementTag.truncate>
        // @returns ElementTag(Number)
        // @group conversion
        // @description
        // Returns the element as a number without a decimal by way of stripping the decimal value off the end.
        // That is, rounds towards zero.
        // -->
        registerTag("truncate", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                try {
                    return new ElementTag(object.asBigDecimal().longValue());
                }
                catch (NumberFormatException e) {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("'" + object.element + "' is not a valid decimal number.");
                    }
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <ElementTag.as_money>
        // @returns ElementTag(Decimal)
        // @group conversion
        // @description
        // Returns the element as a number with two decimal places.
        // -->
        registerTag("as_money", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                try {
                    DecimalFormat d = new DecimalFormat("0.00");
                    return new ElementTag(d.format(Double.valueOf(element)));
                }
                catch (NumberFormatException e) {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("'" + element + "' is not a valid decimal number.");
                    }
                    return null;
                }
            }
        });
        registerTag("asmoney", tagProcessor.registeredObjectTags.get("as_money"));

        // <--[tag]
        // @attribute <ElementTag.as_list>
        // @returns ListTag
        // @group conversion
        // @description
        // Returns the element as a ListTag.
        // -->
        registerTag("as_list", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                ListTag obj = handleNull(element, ListTag.valueOf(element), "dList", attribute.hasAlternative());
                if (obj != null) {
                    return obj;
                }
                return null;
            }
        });
        registerTag("aslist", tagProcessor.registeredObjectTags.get("as_list"));

        // <--[tag]
        // @attribute <ElementTag.as_custom>
        // @returns CustomObject
        // @group conversion
        // @description
        // Returns the element as a custom object.
        // -->
        registerTag("as_custom", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                CustomObjectTag obj = handleNull(element, CustomObjectTag.valueOf(element, null), "Custom", attribute.hasAlternative());
                if (obj != null) {
                    return obj;
                }
                return null;
            }
        });
        registerTag("ascustom", tagProcessor.registeredObjectTags.get("as_custom"));

        // <--[tag]
        // @attribute <ElementTag.as_script>
        // @returns ScriptTag
        // @group conversion
        // @description
        // Returns the element as a ScriptTag.
        // Note: the value must be a valid script.
        // -->
        registerTag("as_script", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                ScriptTag obj = handleNull(element, ScriptTag.valueOf(element), "dScript", attribute.hasAlternative());
                return obj;
            }
        });
        registerTag("asscript", tagProcessor.registeredObjectTags.get("as_script"));

        // <--[tag]
        // @attribute <ElementTag.as_queue>
        // @returns QueueTag
        // @group conversion
        // @description
        // Returns the element as a ScriptQueue.
        // Note: the value must be a valid ScriptQueue.
        // -->
        registerTag("as_queue", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                QueueTag obj = handleNull(element, QueueTag.valueOf(element), "ScriptQueue", attribute.hasAlternative());
                return obj;
            }
        });
        registerTag("asqueue", tagProcessor.registeredObjectTags.get("as_queue"));

        // <--[tag]
        // @attribute <ElementTag.as_duration>
        // @returns DurationTag
        // @group conversion
        // @description
        // Returns the element as a Duration.
        // Note: the value must be a valid Duration.
        // -->
        registerTag("as_duration", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                DurationTag obj = handleNull(element, DurationTag.valueOf(element), "Duration", attribute.hasAlternative());
                return obj;
            }
        });
        registerTag("asduration", tagProcessor.registeredObjectTags.get("as_duration"));

        // <--[tag]
        // @attribute <ElementTag.escaped>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element, escaped for safe reuse.
        // Inverts <@link tag ElementTag.unescaped>
        // See <@link language property escaping>
        // -->
        registerTag("escaped", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(EscapeTagBase.escape(element));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.sql_escaped>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element, escaped for safe use in SQL.
        // -->
        registerTag("sql_escaped", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(SQLEscaper.escapeSQL(element));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.unescaped>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element, unescaped.
        // Inverts <@link tag ElementTag.escaped>
        // See <@link language property escaping>
        // -->
        registerTag("unescaped", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                return new ElementTag(EscapeTagBase.unEscape(element));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.parsed>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element, with any contained tags parsed.
        // WARNING: THIS TAG IS DANGEROUS TO USE, DO NOT USE IT UNLESS
        // YOU KNOW WHAT YOU ARE DOING. USE AT YOUR OWN RISK.
        // -->
        registerTag("parsed", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ObjectTag read = TagManager.tagObject(TagManager.cleanOutputFully(object.element), attribute.context);
                return read;
            }
        });

        /////////////////////
        //   ELEMENT CHECKING ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <ElementTag.difference[<element>]>
        // @returns ElementTag(Number)
        // @group element checking
        // @description
        // Returns a number representing the difference between the two elements. (Uses Levenshtein logic).
        // -->
        registerTag("difference", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                String two = attribute.getContext(1);
                return new ElementTag(CoreUtilities.getLevenshteinDistance(element, two));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.contains_any_case_sensitive_text[<element>|...]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains any of a list of specified elements, case sensitive.
        // -->
        registerTag("contains_any_case_sensitive", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                ListTag list = ListTag.valueOf(attribute.getContext(1));
                for (String list_element : list) {
                    if (element.contains(list_element)) {
                        return new ElementTag(true);
                    }
                }
                return new ElementTag(false);
            }
        });
        TagRunnable.ObjectForm<ElementTag> r = tagProcessor.registeredObjectTags.get("contains_any_case_sensitive").clone();
        r.name = null;
        registerTag("contains_any_case_sensitive_text", r);

        // <--[tag]
        // @attribute <ElementTag.contains_any_text[<element>|...]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains any of a list of specified elements, case insensitive.
        // -->
        registerTag("contains_any", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                ListTag list = ListTag.valueOf(CoreUtilities.toLowerCase(attribute.getContext(1)));
                String ellow = CoreUtilities.toLowerCase(element);
                for (String list_element : list) {
                    if (ellow.contains(list_element)) {
                        return new ElementTag(true);
                    }
                }
                return new ElementTag(false);
            }
        });
        r = tagProcessor.registeredObjectTags.get("contains_any").clone();
        r.name = null;
        registerTag("contains_any_text", r);

        // <--[tag]
        // @attribute <ElementTag.contains_case_sensitive_text[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains a specified element, case sensitive.
        // -->
        registerTag("contains_case_sensitive", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                String contains = attribute.getContext(1);
                if (element.contains(contains)) {
                    return new ElementTag("true");
                }
                else {
                    return new ElementTag("false");
                }
            }
        });
        r = tagProcessor.registeredObjectTags.get("contains_case_sensitive").clone();
        r.name = null;
        registerTag("contains_case_sensitive_text", r);

        // <--[tag]
        // @attribute <ElementTag.contains_text[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains a specified element, case insensitive. Can use
        // regular expression by prefixing the element with regex:
        // -->
        registerTag("contains", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                String contains = attribute.getContext(1);

                if (CoreUtilities.toLowerCase(contains).startsWith("regex:")) {

                    if (Pattern.compile(contains.substring(("regex:").length()), Pattern.CASE_INSENSITIVE).matcher(element).matches()) {
                        return new ElementTag("true");
                    }
                    else {
                        return new ElementTag("false");
                    }
                }
                else if (CoreUtilities.toLowerCase(element).contains(CoreUtilities.toLowerCase(contains))) {
                    return new ElementTag("true");
                }
                else {
                    return new ElementTag("false");
                }
            }
        });
        r = tagProcessor.registeredObjectTags.get("contains").clone();
        r.name = null;
        registerTag("contains_text", r);

        // <--[tag]
        // @attribute <ElementTag.contains_all_text[<element>|...]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains all of the specified strings, case insensitive.
        // -->
        registerTag("contains_all", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                ListTag list = ListTag.valueOf(CoreUtilities.toLowerCase(attribute.getContext(1)));
                String ellow = CoreUtilities.toLowerCase(element);
                for (String list_element : list) {
                    if (!ellow.contains(list_element)) {
                        return new ElementTag("false");
                    }
                }
                return new ElementTag("true");
            }
        });
        r = tagProcessor.registeredObjectTags.get("contains_all").clone();
        r.name = null;
        registerTag("contains_all_text", r);

        // <--[tag]
        // @attribute <ElementTag.contains_all_case_sensitive_text[<element>|...]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains all of the specified strings, case sensitive.
        // -->
        registerTag("contains_all_case_sensitive", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String element = object.element;
                ListTag list = ListTag.valueOf(attribute.getContext(1));
                for (String list_element : list) {
                    if (!element.contains(list_element)) {
                        return new ElementTag("false");
                    }
                }
                return new ElementTag("true");
            }
        });
        r = tagProcessor.registeredObjectTags.get("contains_all_case_sensitive").clone();
        r.name = null;
        registerTag("contains_all_case_sensitive_text", r);

        // <--[tag]
        // @attribute <ElementTag.ends_with[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element ends with a specified element.
        // -->
        registerTag("ends_with", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(CoreUtilities.toLowerCase(object.element).
                        endsWith(CoreUtilities.toLowerCase(attribute.getContext(1))));
            }
        });
        registerTag("endswith", tagProcessor.registeredObjectTags.get("ends_with"));

        // <--[tag]
        // @attribute <ElementTag.equals_case_sensitive[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element matches another element, case-sensitive.
        // -->
        registerTag("equals_case_sensitive", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.equals_case_sensitive[...] must have a value.");
                    return null;
                }
                return new ElementTag(object.element.equals(attribute.getContext(1)));
            }
        });
        registerTag("equals_with_case", tagProcessor.registeredObjectTags.get("equals_case_sensitive"));

        // <--[tag]
        // @attribute <ElementTag.matches[<regex>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element matches a regex input.
        // -->
        registerTag("matches", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.matches[...] must have a value.");
                    return null;
                }
                return new ElementTag(object.element.matches(attribute.getContext(1)));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.regex[<regex>].group[<group>]>
        // @returns ElementTag
        // @group element checking
        // @description
        // Returns the specific group from a regex match.
        // Specify group 0 for the whole match.
        // For example, <element[hello5world].regex[.*(\d).*].group[1]> returns '5'.
        // -->
        registerTag("regex", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1) || !attribute.hasContext(2)) {
                    Debug.echoError("The tag ElementTag.regex[...] must have a value.");
                    return null;
                }
                String regex = attribute.getContext(1);
                Matcher m = Pattern.compile(regex).matcher(object.element);
                if (!m.matches()) {
                    return null;
                }
                int group = new ElementTag(attribute.getContext(2)).asInt();
                if (group < 0) {
                    group = 0;
                }
                if (group > m.groupCount()) {
                    group = m.groupCount();
                }
                attribute.fulfill(1);
                return new ElementTag(m.group(group));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.length>
        // @returns ElementTag(Number)
        // @group element checking
        // @description
        // Returns the length of the element.
        // -->
        registerTag("length", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(object.element.length());
            }
        });

        // <--[tag]
        // @attribute <ElementTag.not>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns the opposite of the element
        // IE, true returns false and false returns true.
        // -->
        registerTag("not", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(!object.element.equalsIgnoreCase("true"));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.and[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether both the element and the second element are true.
        // -->
        registerTag("and", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(object.element.equalsIgnoreCase("true") && attribute.getContext(1).equalsIgnoreCase("true"));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.or[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether either the element or the second element are true.
        // -->
        registerTag("or", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(object.element.equalsIgnoreCase("true") || attribute.getContext(1).equalsIgnoreCase("true"));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.xor[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element and the second element are true and false (exclusive or).
        // -->
        registerTag("xor", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(object.element.equalsIgnoreCase("true") != attribute.getContext(1).equalsIgnoreCase("true"));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.starts_with[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element starts with a specified element.
        // -->
        registerTag("starts_with", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(CoreUtilities.toLowerCase(object.element).startsWith(CoreUtilities.toLowerCase(attribute.getContext(1))));
            }
        });
        registerTag("startswith", tagProcessor.registeredObjectTags.get("starts_with"));

        // <--[tag]
        // @attribute <ElementTag.index_of[<element>]>
        // @returns ElementTag(Number)
        // @group element checking
        // @description
        // Returns the index of the first occurrence of a specified element.
        // Returns 0 if the element never occurs within the element.
        // -->
        registerTag("index_of", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.index_of[...] must have a value.");
                    return null;
                }
                return new ElementTag(CoreUtilities.toLowerCase(object.element)
                        .indexOf(CoreUtilities.toLowerCase(attribute.getContext(1))) + 1);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.last_index_of[<element>]>
        // @returns ElementTag(Number)
        // @group element checking
        // @description
        // Returns the index of the last occurrence of a specified element.
        // Returns 0 if the element never occurs within the element.
        // -->
        registerTag("last_index_of", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.last_index_of[...] must have a value.");
                    return null;
                }
                return new ElementTag(CoreUtilities.toLowerCase(object.element)
                        .lastIndexOf(CoreUtilities.toLowerCase(attribute.getContext(1))) + 1);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.char_at[<#>]>
        // @returns ElementTag
        // @group element checking
        // @description
        // Returns the character at a specified index.
        // Returns null if the index is outside the range of the element.
        // -->
        registerTag("char_at", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.char_at[...] must have a value.");
                    return null;
                }
                int index = attribute.getIntContext(1) - 1;
                if (index < 0 || index >= object.element.length()) {
                    return null;
                }
                else {
                    return new ElementTag(String.valueOf(object.element.charAt(index)));
                }
            }
        });

        /////////////////////
        //   ELEMENT MANIPULATION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <ElementTag.after_last[<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the portion of an element after the last occurrence of a specified element.
        // For example: abcabc .after_last[b] returns c.
        // -->
        registerTag("after_last", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.after_last[...] must have a value.");
                    return null;
                }
                String delimiter = attribute.getContext(1);
                if (CoreUtilities.toLowerCase(object.element).contains(CoreUtilities.toLowerCase(delimiter))) {
                    return new ElementTag(object.element.substring
                            (CoreUtilities.toLowerCase(object.element).lastIndexOf(CoreUtilities.toLowerCase(delimiter)) + delimiter.length()));
                }
                else {
                    return new ElementTag("");
                }
            }
        });

        // <--[tag]
        // @attribute <ElementTag.after[<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the portion of an element after the first occurrence of a specified element.
        // For example: HelloWorld .after[Hello] returns World.
        // -->
        registerTag("after", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.after[...] must have a value.");
                    return null;
                }
                String delimiter = attribute.getContext(1);
                if (CoreUtilities.toLowerCase(object.element).contains(CoreUtilities.toLowerCase(delimiter))) {
                    return new ElementTag(object.element.substring
                            (CoreUtilities.toLowerCase(object.element).indexOf(CoreUtilities.toLowerCase(delimiter)) + delimiter.length()));
                }
                else {
                    return new ElementTag("");
                }
            }
        });

        // <--[tag]
        // @attribute <ElementTag.before_last[<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the portion of an element before the last occurrence of a specified element.
        // For example: abcabc .before_last[b] returns abca.
        // -->
        registerTag("before_last", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.before_last[...] must have a value.");
                    return null;
                }
                String delimiter = attribute.getContext(1);
                if (CoreUtilities.toLowerCase(object.element).contains(CoreUtilities.toLowerCase(delimiter))) {
                    return new ElementTag(object.element.substring
                            (0, CoreUtilities.toLowerCase(object.element).lastIndexOf(CoreUtilities.toLowerCase(delimiter))));
                }
                else {
                    return new ElementTag(object.element);
                }
            }
        });

        // <--[tag]
        // @attribute <ElementTag.before[<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the portion of an element before the first occurrence of specified element.
        // For example: abcd .before[c] returns ab.
        // -->
        registerTag("before", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.before[...] must have a value.");
                    return null;
                }
                String delimiter = attribute.getContext(1);
                if (CoreUtilities.toLowerCase(object.element).contains(CoreUtilities.toLowerCase(delimiter))) {
                    return new ElementTag(object.element.substring
                            (0, CoreUtilities.toLowerCase(object.element).indexOf(CoreUtilities.toLowerCase(delimiter))));
                }
                else {
                    return new ElementTag(object.element);
                }
            }
        });

        // <--[tag]
        // @attribute <ElementTag.replace[((first)regex:)<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the element with all instances of a element removed.
        // -->

        // <--[tag]
        // @attribute <ElementTag.replace[((first)regex:)<element>].with[<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the element with all instances of a element replaced with another.
        // Specify regex: at the start of the replace element to use Regex replacement.
        // Specify firstregex: at the start of the replace element to Regex 'replaceFirst'
        // -->
        registerTag("replace", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.replace[...] must have a value.");
                    return null;
                }
                String replace = attribute.getContext(1);
                String replacement = "";
                if (attribute.startsWith("with", 2)) {
                    if (attribute.hasContext(2)) {
                        replacement = attribute.getContext(2);
                        if (replacement == null) {
                            Debug.echoError("The tag ElementTag.replace[...].with[...] must have a value.");
                            return null;
                        }
                        attribute.fulfill(1);
                    }
                }

                if (replace.startsWith("regex:")) {
                    return new ElementTag(object.element.replaceAll(replace.substring("regex:".length()), replacement));
                }
                if (replace.startsWith("firstregex:")) {
                    return new ElementTag(object.element.replaceFirst(replace.substring("firstregex:".length()), replacement));
                }
                else {
                    return new ElementTag(object.element.replaceAll("(?i)" + Pattern.quote(replace), replacement));
                }
            }
        });
        // <--[tag]
        // @attribute <ElementTag.replace_text[((first)regex:)<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the element with all instances of a element removed.
        // -->

        // <--[tag]
        // @attribute <ElementTag.replace_text[((first)regex:)<element>].with[<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the element with all instances of a element replaced with another.
        // Specify regex: at the start of the replace element to use Regex replacement.
        // Specify firstregex: at the start of the replace element to Regex 'replaceFirst'
        // -->
        r = tagProcessor.registeredObjectTags.get("replace").clone();
        r.name = null;
        registerTag("replace_text", r);

        // <--[tag]
        // @attribute <ElementTag.format_number>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns a number reformatted for easier reading.
        // For example: 1234567 will become 1,234,567.
        // -->
        registerTag("format_number", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                try {
                    int decimal = object.element.indexOf('.');
                    String shortelement;
                    String afterdecimal;
                    if (decimal != -1) {
                        shortelement = object.element.substring(0, decimal);
                        afterdecimal = object.element.substring(decimal);
                    }
                    else {
                        shortelement = object.element;
                        afterdecimal = "";
                    }
                    String intform = Long.valueOf(shortelement.replace("%", "")).toString();
                    String negative = "";
                    if (intform.startsWith("-")) {
                        negative = "-";
                        intform = intform.substring(1);
                    }
                    for (int i = intform.length() - 3; i > 0; i -= 3) {
                        intform = intform.substring(0, i) + "," + intform.substring(i);
                    }
                    return new ElementTag(negative + intform + afterdecimal);
                }
                catch (Exception ex) {
                    Debug.echoError(ex);
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <ElementTag.to_list>
        // @returns ListTag
        // @group element manipulation
        // @description
        // Returns a ListTag of each letter in the element.
        // -->
        registerTag("to_list", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ListTag list = new ListTag();
                for (int i = 0; i < object.element.length(); i++) {
                    list.add(String.valueOf(object.element.charAt(i)));
                }
                return list;
            }
        });

        // <--[tag]
        // @attribute <ElementTag.trim>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element minus any leading or trailing whitespace.
        // -->
        registerTag("trim", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(object.element.trim());
            }
        });

        // <--[tag]
        // @attribute <ElementTag.to_uppercase>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element in all uppercase letters.
        // -->
        registerTag("to_uppercase", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(object.element.toUpperCase());
            }
        });
        registerTag("upper", tagProcessor.registeredObjectTags.get("to_uppercase"));

        // <--[tag]
        // @attribute <ElementTag.to_lowercase>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element in all lowercase letters.
        // -->
        registerTag("to_lowercase", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag(CoreUtilities.toLowerCase(object.element));
            }
        });
        registerTag("lower", tagProcessor.registeredObjectTags.get("to_lowercase"));

        // <--[tag]
        // @attribute <ElementTag.to_titlecase>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns The Value Of An ElementTag In Title Case.
        // -->
        registerTag("to_titlecase", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (object.element.length() == 0) {
                    return new ElementTag("");
                }
                StringBuilder TitleCase = new StringBuilder(object.element.length());
                String Upper = object.element.toUpperCase();
                String Lower = CoreUtilities.toLowerCase(object.element);
                TitleCase.append(Upper.charAt(0));
                for (int i = 1; i < object.element.length(); i++) {
                    if (object.element.charAt(i - 1) == ' ') {
                        TitleCase.append(Upper.charAt(i));
                    }
                    else {
                        TitleCase.append(Lower.charAt(i));
                    }
                }
                return new ElementTag(TitleCase.toString());
            }
        });
        registerTag("totitlecase", tagProcessor.registeredObjectTags.get("to_titlecase"));

        // <--[tag]
        // @attribute <ElementTag.substring[<#>(,<#>)]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the portion of an element between two element indices.
        // If no second index is specified, it will return the portion of an
        // element after the specified index.
        // -->
        registerTag("substring", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.substring[...] must have a value.");
                    return null;
                }
                int beginning_index = new ElementTag(attribute.getContext(1).split(",")[0]).asInt() - 1;
                int ending_index;
                if (attribute.getContext(1).split(",").length > 1) {
                    ending_index = new ElementTag(attribute.getContext(1).split(",")[1]).asInt();
                }
                else {
                    ending_index = object.element.length();
                }
                if (beginning_index < 0) {
                    beginning_index = 0;
                }
                if (beginning_index > object.element.length()) {
                    beginning_index = object.element.length();
                }
                if (ending_index > object.element.length()) {
                    ending_index = object.element.length();
                }
                if (ending_index < beginning_index) {
                    ending_index = beginning_index;
                }
                return new ElementTag(object.element.substring(beginning_index, ending_index));
            }
        });
        registerTag("substr", tagProcessor.registeredObjectTags.get("substring"));

        // <--[tag]
        // @attribute <ElementTag.split[(regex:)<string>]>
        // @returns ListTag
        // @group element manipulation
        // @description
        // Returns a list of portions of this element, split by the specified string.
        // If a split string is unspecified, splits by space.
        // -->
        registerTag("split", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String split_string = (attribute.hasContext(1) ? attribute.getContext(1) : " ");
                if (CoreUtilities.toLowerCase(split_string).startsWith("regex:")) {
                    split_string = split_string.split(":", 2)[1];
                }
                else {
                    split_string = "(?i)" + Pattern.quote(split_string);
                }
                String[] split;

                // <--[tag]
                // @attribute <ElementTag.split[(regex:)<string>].limit[<#>]>
                // @returns ListTag
                // @group element manipulation
                // @description
                // Returns a list of portions of this element, split by the specified string,
                // and capped at the specified number of max list items.
                // If a split string is unspecified, splits by space.
                // -->
                if (attribute.startsWith("limit", 2)) {
                    int limit = (attribute.hasContext(2) ? attribute.getIntContext(2) : 1);
                    attribute.fulfill(1);
                    split = object.element.split(split_string, limit);
                }
                else {
                    split = object.element.split(split_string);
                }
                return new ListTag(Arrays.asList(split));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.pad_left[<#>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element extended to reach a minimum specified length
        // by adding spaces to the left side.
        // -->
        registerTag("pad_left", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.pad_left[...] must have a value.");
                    return null;
                }
                String with = String.valueOf((char) 0x00A0);
                int length = attribute.getIntContext(1);

                // <--[tag]
                // @attribute <ElementTag.pad_left[<#>].with[<element>]>
                // @returns ElementTag
                // @group element manipulation
                // @description
                // Returns the value of an element extended to reach a minimum specified length
                // by adding a specific symbol to the left side.
                // -->
                if (attribute.startsWith("with", 2) && attribute.hasContext(2)) {
                    with = String.valueOf(attribute.getContext(2).charAt(0));
                    attribute.fulfill(1);
                }
                StringBuilder padded = new StringBuilder();
                length -= object.element.length();
                while (padded.length() < length) {
                    padded.append(with);
                }
                padded.append(object.element);
                return new ElementTag(padded.toString());
            }
        });

        // <--[tag]
        // @attribute <ElementTag.pad_right[<#>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element extended to reach a minimum specified length
        // by adding spaces to the right side.
        // -->
        registerTag("pad_right", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.pad_right[...] must have a value.");
                    return null;
                }
                String with = String.valueOf((char) 0x00A0);
                int length = attribute.getIntContext(1);

                // <--[tag]
                // @attribute <ElementTag.pad_right[<#>].with[<element>]>
                // @returns ElementTag
                // @group element manipulation
                // @description
                // Returns the value of an element extended to reach a minimum specified length
                // by adding a specific symbol to the right side.
                // -->
                if (attribute.startsWith("with", 2) && attribute.hasContext(2)) {
                    with = String.valueOf(attribute.getContext(2).charAt(0));
                    attribute.fulfill(1);
                }
                StringBuilder padded = new StringBuilder(object.element);
                while (padded.length() < length) {
                    padded.append(with);
                }
                return new ElementTag(padded.toString());
            }
        });

        /////////////////////
        //   MATH ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <ElementTag.abs>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the absolute value of the element.
        // -->
        registerTag("abs", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.abs(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.max[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the higher number: this element or the specified one.
        // -->
        registerTag("max", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.max(ele.asDouble(), new ElementTag(attribute.getContext(1)).asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.min[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the lower number: this element or the specified one.
        // -->
        registerTag("min", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.min(ele.asDouble(), new ElementTag(attribute.getContext(1)).asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.add_int[<#>]>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Returns the element plus a number, using integer math.
        // -->
        registerTag("add_int", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(ele.asLong() + ArgumentHelper.getLongFrom(attribute.getContext(1)));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.div_int[<#>]>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Returns the element divided by a number.
        // -->
        registerTag("div_int", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(ele.asLong() / ArgumentHelper.getLongFrom(attribute.getContext(1)));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.mul_int[<#>]>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Returns the element multiplied by a number.
        // -->
        registerTag("mul_int", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(ele.asLong() * ArgumentHelper.getLongFrom(attribute.getContext(1)));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.sub_int[<#>]>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Returns the element minus a number.
        // -->
        registerTag("sub_int", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(ele.asLong() - ArgumentHelper.getLongFrom(attribute.getContext(1)));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.add[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the element plus a number.
        // -->
        TagRunnable.ObjectForm<ElementTag> addRunnable = new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.add[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                try {
                    return new ElementTag(ele.asBigDecimal().add(ele.getBD(attribute.getContext(1))));
                }
                catch (Throwable e) {
                    return new ElementTag(ele.asDouble() + (ArgumentHelper.getDoubleFrom(attribute.getContext(1))));
                }
            }
        };
        registerTag("add", addRunnable.clone());
        registerTag("+", addRunnable.clone());

        // <--[tag]
        // @attribute <ElementTag.div[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the element divided by a number.
        // -->
        TagRunnable.ObjectForm<ElementTag> divRunnable = new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.div[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                try {
                    return new ElementTag(ele.asBigDecimal().divide(ele.getBD(attribute.getContext(1)), 64, RoundingMode.HALF_UP));
                }
                catch (Throwable e) {
                    return new ElementTag(ele.asDouble() / (ArgumentHelper.getDoubleFrom(attribute.getContext(1))));
                }
            }
        };
        registerTag("div", divRunnable.clone());
        registerTag("/", divRunnable.clone());

        // <--[tag]
        // @attribute <ElementTag.mod[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the remainder of the element divided by a number.
        // -->
        TagRunnable.ObjectForm<ElementTag> modRunnable = new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.mod[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(ele.asDouble() % (ArgumentHelper.getDoubleFrom(attribute.getContext(1))));
            }
        };
        registerTag("mod", modRunnable.clone());
        registerTag("%", modRunnable.clone());

        // <--[tag]
        // @attribute <ElementTag.mul[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the element multiplied by a number.
        // -->
        TagRunnable.ObjectForm<ElementTag> mulRunnable = new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.mul[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                try {
                    return new ElementTag(ele.asBigDecimal().multiply(ele.getBD(attribute.getContext(1))));
                }
                catch (Throwable e) {
                    return new ElementTag(ele.asDouble() * (ArgumentHelper.getDoubleFrom(attribute.getContext(1))));
                }
            }
        };
        registerTag("mul", mulRunnable.clone());
        registerTag("*", mulRunnable.clone());

        // <--[tag]
        // @attribute <ElementTag.sub[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the element minus a number.
        // -->
        TagRunnable.ObjectForm<ElementTag> subRunnable = new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.sub[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                try {
                    return new ElementTag(ele.asBigDecimal().subtract(ele.getBD(attribute.getContext(1))));
                }
                catch (Throwable e) {
                    return new ElementTag(ele.asDouble() - (ArgumentHelper.getDoubleFrom(attribute.getContext(1))));
                }
            }
        };
        registerTag("sub", subRunnable.clone());
        registerTag("-", subRunnable.clone());

        // <--[tag]
        // @attribute <ElementTag.sqrt>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the square root of the element.
        // -->
        registerTag("sqrt", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.sqrt(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.log[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the logarithm of the element, with the base of the specified number.
        // -->
        registerTag("log", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.log[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.log(ele.asDouble()) / Math.log(ArgumentHelper.getDoubleFrom(attribute.getContext(1))));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.ln>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the natural logarithm of the element.
        // -->
        registerTag("ln", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.log(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.power[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the element to the power of a number.
        // -->
        TagRunnable.ObjectForm<ElementTag> powerRunnable = new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.power[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.pow(ele.asDouble(), ArgumentHelper.getDoubleFrom(attribute.getContext(1))));
            }
        };
        registerTag("power", powerRunnable.clone());
        registerTag("^", powerRunnable.clone());

        // <--[tag]
        // @attribute <ElementTag.asin>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the arc-sine of the element.
        // -->
        registerTag("asin", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.asin(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.acos>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the arc-cosine of the element.
        // -->
        registerTag("acos", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.acos(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.atan>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the arc-tangent of the element.
        // -->
        registerTag("atan", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.atan(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.atan2[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Interprets the element to be a Y value and the input value to be an X value (meaning: <Y.atan2[X]>),
        // and returns an angle representing the vector of (X,Y).
        // -->
        registerTag("atan2", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.atan2[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.atan2(ele.asDouble(), attribute.getDoubleContext(1)));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.cos>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the cosine of the element.
        // -->
        registerTag("cos", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.cos(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.sin>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the sine of the element.
        // -->
        registerTag("sin", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.sin(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.tan>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the tangent of the element.
        // -->
        registerTag("tan", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.tan(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.to_degrees>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Converts the element from radians to degrees.
        // -->
        registerTag("to_degrees", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.toDegrees(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.to_radians>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Converts the element from degrees to radians.
        // -->
        registerTag("to_radians", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag(Math.toRadians(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.round_up>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Rounds a decimal upward.
        // -->
        registerTag("round_up", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag((long) Math.ceil(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.round_down>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Rounds a decimal downward.
        // -->
        registerTag("round_down", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag((long) Math.floor(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.round_to[<#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Rounds a decimal to the specified place.
        // For example, 0.12345 .round_to[3] returns "0.123".
        // -->
        registerTag("round_to", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.round_to[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                int ten = (int) Math.pow(10, attribute.getIntContext(1));
                return new ElementTag(((double) Math.round(ele.asDouble() * ten)) / ten);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.round>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Rounds a decimal.
        // -->
        registerTag("round", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new ElementTag((long) Math.round(ele.asDouble()));
            }
        });

        // <--[tag]
        // @attribute <ElementTag.round_to_precision[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Rounds a decimal to the specified precision.
        // For example, 0.12345 .round_to_precision[0.005] returns "0.125".
        // -->
        registerTag("round_to_precision", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.round_to_precision[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                double precision = attribute.getDoubleContext(1);
                return new ElementTag(((double) Math.round(ele.asDouble() / precision)) * precision);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.round_down_to_precision[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Rounds a decimal downward to the specified precision.
        // -->
        registerTag("round_down_to_precision", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.round_down_to_precision[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                double precision = attribute.getDoubleContext(1);
                return new ElementTag(Math.floor(ele.asDouble() / precision) * precision);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.round_up_to_precision[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Rounds a decimal upward to the specified precision.
        // -->
        registerTag("round_up_to_precision", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag ElementTag.round_up_to_precision[...] must have a value.");
                    return null;
                }
                ElementTag ele = (ElementTag) object;
                if (!ele.isDouble()) {
                    Debug.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                double precision = attribute.getDoubleContext(1);
                return new ElementTag(Math.ceil(ele.asDouble() / precision) * precision);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.base64_encode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Encodes the element using Base64 encoding.
        // -->
        registerTag("base64_encode", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String encoded = Base64.getEncoder().encodeToString(object.element.getBytes());
                return new ElementTag(encoded);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.base64_decode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Decodes the element using Base64 encoding. Must be valid Base64 input.
        // -->
        registerTag("base64_decode", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String decoded = new String(Base64.getDecoder().decode(object.element));
                return new ElementTag(decoded);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.hex_encode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Encodes the element using hexadecimal encoding.
        // -->
        registerTag("hex_encode", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String encoded = DatatypeConverter.printHexBinary(object.element.getBytes());
                return new ElementTag(encoded);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.hex_decode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Decodes the element using hexadecimal encoding. Must be valid hexadecimal input.
        // -->
        registerTag("hex_decode", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                String decoded = new String(DatatypeConverter.parseHexBinary(object.element));
                return new ElementTag(decoded);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.url_encode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Encodes the element using URL encoding.
        // -->
        registerTag("url_encode", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                try {
                    String encoded = URLEncoder.encode(object.element, "UTF-8");
                    return new ElementTag(encoded);
                }
                catch (Exception e) {
                    Debug.echoError(e);
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <ElementTag.url_decode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Decodes the element using URL encoding. Must be valid URL-encoded input.
        // -->
        registerTag("url_decode", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                try {
                    String decoded = URLDecoder.decode(object.element, "UTF-8");
                    return new ElementTag(decoded);
                }
                catch (Exception e) {
                    Debug.echoError(e);
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <ElementTag.type>
        // @returns ElementTag
        // @description
        // Always returns 'Element' for ElementTag objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->
        registerTag("type", new TagRunnable.ObjectForm<ElementTag>() {
            @Override
            public ObjectTag run(Attribute attribute, ElementTag object) {
                return new ElementTag("Element");
            }
        });

    }

    public static ObjectTagProcessor<ElementTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectForm<ElementTag> runnable) {
        tagProcessor.registerTag(name, runnable);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    public static class FailedObjectTag implements ObjectTag {
        @Override
        public String getPrefix() {
            return null;
        }
        @Override
        public boolean isUnique() {
            return false;
        }
        @Override
        public String getObjectType() {
            return null;
        }
        @Override
        public String identify() {
            return null;
        }
        @Override
        public String identifySimple() {
            return null;
        }
        @Override
        public ObjectTag setPrefix(String prefix) {
            return null;
        }
        @Override
        public ObjectTag getObjectAttribute(Attribute attribute) {
            if (!attribute.hasAlternative()) {
                Debug.echoDebug(attribute.getScriptEntry(), "Unfilled attributes '" + attribute.unfilledString() +
                        "' for tag <" + attribute.getOrigin() + ">!");
                if (attribute.seemingSuccesses.size() > 0) {
                    String almost = attribute.seemingSuccesses.get(attribute.seemingSuccesses.size() - 1);
                    if (attribute.hasContextFailed) {
                        Debug.echoDebug(attribute.getScriptEntry(), "Almost matched but failed (missing [context] parameter?): " + almost);
                    }
                    else {
                        Debug.echoDebug(attribute.getScriptEntry(), "Almost matched but failed (possibly bad input?): " + almost);
                    }
                }
            }
            if (Debug.verbose) {
                Debug.log("Element - Unfilled! Null!");
            }
            return null;
        }
    }

    public ObjectTag getNextObjectTypeDown() {
        return new FailedObjectTag();
    }
}
