package net.aufdemrand.denizencore.objects;

import net.aufdemrand.denizencore.scripts.commands.core.Comparable;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.tags.core.EscapeTags;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.SQLEscaper;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import javax.xml.bind.DatatypeConverter;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// <--[language]
// @name Element
// @group Object System
// @description
// Elements are simple objects that contain either a boolean (true/false),
// string, or number value. Their main usage is within the replaceable tag
// system, often times returned from the use of another tag that isn't returning
// a specific object type, such as a location or entity. For example,
// <player.name> or <li@item|item2|item3.as_cslist> will both return Elements.
//
// Pluses to the Element system is the ability to utilize its attributes that
// can provide a range of functionality that should be familiar from any other
// programming language, such as 'to_uppercase', 'split', 'replace', 'contains',
// as_int, any many more. See 'element' tags for more information.
//
// While information fetched from other tags resulting in an Element is often
// times automatically handled, it may be desirable to utilize element
// attributes from strings/numbers/etc. that aren't already an element object.
// To accomplish this, the object fetcher can be used to create a new element.
// Element has a constructor, el@val[element_value], that will allow the
// creation of a new element. For example: <el@val[This_is_a_test.].to_uppercase>
// will result in the value 'THIS_IS_A_TEST.' Note that while other objects often
// return their object identifier (el@, li@, e@, etc.), elements do not.

// -->


public class Element implements dObject, dObject.ObjectAttributable {

    public final static Element TRUE = new Element(Boolean.TRUE);
    public final static Element FALSE = new Element(Boolean.FALSE);
    public final static Element SERVER = new Element("server");
    public final static Element NULL = new Element("null");

    final static Pattern VALUE_PATTERN =
            Pattern.compile("el@val(?:ue)?\\[([^\\[\\]]+)\\].*",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);


    public static Element valueOf(String string) {
        return valueOf(string, null);
    }

    /**
     * @param string the string or dScript argument String
     * @return a dScript dList
     */
    @Fetchable("el")
    public static Element valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }

        Matcher m = VALUE_PATTERN.matcher(string);

        // Allow construction of elements with el@val[<value>]
        if (m.matches()) {
            String value = m.group(1);
            return new Element(value);
        }

        return new Element(CoreUtilities.toLowerCase(string).startsWith("el@") ? string.substring(3) : string);
    }

    public static boolean matches(String string) {
        return string != null;
    }

    /**
     * Handle null dObjects appropriately for potentionally null tags.
     * Will show a dB error message and return Element.NULL for null objects.
     *
     * @param tag    The input string that produced a potentially null object, for debugging.
     * @param object The potentially null object.
     * @param type   The type of object expected, for debugging. (EG: 'dNPC')
     * @return The object or Element.NULL if the object is null.
     */
    public static <T extends dObject> T handleNull(String tag, T object, String type, boolean has_fallback) {
        if (object == null) {
            if (!has_fallback) {
                dB.echoError("'" + tag + "' is an invalid " + type + "!");
            }
            return null;
        }
        return object;
    }

    private final String element;

    public Element(String string) {
        this.prefix = "element";
        if (string == null) {
            if (dB.verbose) {
                try {
                    throw new RuntimeException("Trace");
                }
                catch (Exception ex) {
                    dB.echoError(ex);
                }
                dB.log("Element - Null construction!");
            }
            this.element = "null";
        }
        else {
            this.element = TagManager.cleanOutput(string);
        }
    }

    public Element(boolean bool) {
        this.prefix = "boolean";
        this.element = String.valueOf(bool);
    }

    public Element(int integer) {
        this.prefix = "number";
        this.element = String.valueOf(integer);
    }

    public Element(byte byt) {
        this.prefix = "number";
        this.element = String.valueOf(byt);
    }

    public Element(short shrt) {
        this.prefix = "number";
        this.element = String.valueOf(shrt);
    }

    public Element(long lng) {
        this.prefix = "number";
        this.element = String.valueOf(lng);
    }

    public Element(BigDecimal bdl) {
        this.prefix = "decimal";
        this.element = CoreUtilities.bigDecToString(bdl);
    }

    public Element(double dbl) {
        this.prefix = "decimal";
        this.element = CoreUtilities.doubleToString(dbl);
    }

    public Element(float flt) {
        this.prefix = "decimal";
        this.element = CoreUtilities.doubleToString(flt);
    }

    public Element(String prefix, String string) {
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
            dB.echoError("Unreasonably large number detected!");
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
            dB.echoError("'" + element + "' is not a valid integer!");
            return 0;
        }
    }

    public long asLong() {
        try {
            return Long.valueOf(element.replaceAll("(%)|(\\.\\d+)", ""));
        }
        catch (NumberFormatException ex) {
            dB.echoError("'" + element + "' is not a valid integer!");
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

    public boolean matchesType(Class<? extends dObject> dClass) {
        return ObjectFetcher.checkMatch(dClass, element);
    }

    public <T extends dObject> T asType(Class<T> dClass) {
        return ObjectFetcher.getObjectFrom(dClass, element);
    }

    public <T extends dObject> T asType(Class<T> dClass, TagContext context) {
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
    public dObject setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debug() {
        return (prefix + "='<A>" + identify() + "<G>'  ");
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

        registerTag("is", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {

                // <--[tag]
                // @attribute <el@element.is[<operator>].to[<element>]>
                // @returns Element(Boolean)
                // @group comparison
                // @description
                // Takes an operator, and compares the value of the element to the supplied
                // element. Returns the outcome of the comparable, either true or false. For
                // information on operators, see <@link language operator>.
                // Equivalent to <@link tag el@element.is[<operator>].than[<element>]>
                // -->

                // <--[tag]
                // @attribute <el@element.is[<operator>].than[<element>]>
                // @returns Element(Boolean)
                // @group comparison
                // @description
                // Takes an operator, and compares the value of the element to the supplied
                // element. Returns the outcome of the comparable, either true or false. For
                // information on operators, see <@link language operator>.
                // Equivalent to <@link tag el@element.is[<operator>].to[<element>]>
                // -->
                if (attribute.hasContext(1)
                        && (attribute.startsWith("to", 2) || attribute.startsWith("than", 2)) && attribute.hasContext(2)) {

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

                        return new Element(com.determineOutcome()).getObjectAttribute(attribute.fulfill(2));
                    }
                    else {
                        net.aufdemrand.denizencore.utilities.debugging.dB.echoError("Unknown operator '" + operator + "'.");
                    }
                }

                return null;
            }
        });
        // <--[tag]
        // @attribute <el@element.as_element>
        // @returns Element
        // @group conversion
        // @description
        // Returns the element as itself.
        // For use in special cases, generally not very useful.
        // -->
        registerTag("as_element", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return ((Element) object).getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("aselement", registeredObjectTags.get("as_element"));

        // <--[tag]
        // @attribute <el@element.as_boolean>
        // @returns Element(Boolean)
        // @group conversion
        // @description
        // Returns the element as true/false.
        // -->
        registerTag("as_boolean", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                return new Element(element.equalsIgnoreCase("true")
                        || element.equalsIgnoreCase("t")
                        || element.equalsIgnoreCase("1"))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("asboolean", registeredObjectTags.get("as_boolean"));

        // <--[tag]
        // @attribute <el@element.as_decimal>
        // @returns Element(Decimal)
        // @group conversion
        // @description
        // Returns the element as a decimal number, or shows an error.
        // -->
        registerTag("as_decimal", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                try {
                    return new Element(Double.valueOf(element))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (NumberFormatException e) {
                    if (!attribute.hasAlternative()) {
                        dB.echoError("'" + element + "' is not a valid decimal number.");
                    }
                    return null;
                }
            }
        });
        registerTag("as_double", registeredObjectTags.get("as_decimal"));
        registerTag("asdouble", registeredObjectTags.get("as_decimal"));

        // <--[tag]
        // @attribute <el@element.as_int>
        // @returns Element(Number)
        // @group conversion
        // @description
        // Returns the element as a number without a decimal. Rounds decimal values.
        // NOTE: Please use .round_down instead of .as_int!
        // -->
        registerTag("as_int", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                try {
                    return new Element(Double.valueOf(element).longValue())
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (NumberFormatException e) {
                    if (!attribute.hasAlternative()) {
                        dB.echoError("'" + element + "' is not a valid decimal number.");
                    }
                    return null;
                }
            }
        });
        registerTag("asint", registeredObjectTags.get("as_int"));

        // <--[tag]
        // @attribute <el@element.as_money>
        // @returns Element(Decimal)
        // @group conversion
        // @description
        // Returns the element as a number with two decimal places.
        // -->
        registerTag("as_money", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                try {
                    DecimalFormat d = new DecimalFormat("0.00");
                    return new Element(d.format(Double.valueOf(element)))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (NumberFormatException e) {
                    if (!attribute.hasAlternative()) {
                        dB.echoError("'" + element + "' is not a valid decimal number.");
                    }
                    return null;
                }
            }
        });
        registerTag("asmoney", registeredObjectTags.get("as_money"));

        // <--[tag]
        // @attribute <el@element.as_list>
        // @returns dList
        // @group conversion
        // @description
        // Returns the element as a dList.
        // -->
        registerTag("as_list", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                dList obj = handleNull(element, dList.valueOf(element), "dList", attribute.hasAlternative());
                if (obj != null) {
                    return obj.getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });
        registerTag("aslist", registeredObjectTags.get("as_list"));

        // <--[tag]
        // @attribute <el@element.as_custom>
        // @returns dList
        // @group conversion
        // @description
        // Returns the element as a custom object.
        // -->
        registerTag("as_custom", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                CustomObject obj = handleNull(element, CustomObject.valueOf(element, null), "Custom", attribute.hasAlternative());
                if (obj != null) {
                    return obj.getObjectAttribute(attribute.fulfill(1));
                }
                return null;
            }
        });
        registerTag("ascustom", registeredObjectTags.get("as_custom"));

        // <--[tag]
        // @attribute <el@element.as_script>
        // @returns dScript
        // @group conversion
        // @description
        // Returns the element as a dScript.
        // Note: the value must be a valid script.
        // -->
        registerTag("as_script", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                dScript obj = handleNull(element, dScript.valueOf(element), "dScript", attribute.hasAlternative());
                if (obj != null) {
                    return CoreUtilities.autoAttrib(obj, attribute.fulfill(1));
                }
                return null;
            }
        });
        registerTag("asscript", registeredObjectTags.get("as_script"));

        // <--[tag]
        // @attribute <el@element.as_queue>
        // @returns ScriptQueue
        // @group conversion
        // @description
        // Returns the element as a ScriptQueue.
        // Note: the value must be a valid ScriptQueue.
        // -->
        registerTag("as_queue", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                ScriptQueue obj = handleNull(element, ScriptQueue.valueOf(element), "ScriptQueue", attribute.hasAlternative());
                if (obj != null) {
                    return CoreUtilities.autoAttrib(obj, attribute.fulfill(1));
                }
                return null;
            }
        });
        registerTag("asqueue", registeredObjectTags.get("as_queue"));

        // <--[tag]
        // @attribute <el@element.as_duration>
        // @returns Duration
        // @group conversion
        // @description
        // Returns the element as a Duration.
        // Note: the value must be a valid Duration.
        // -->
        registerTag("as_duration", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                Duration obj = handleNull(element, Duration.valueOf(element), "Duration", attribute.hasAlternative());
                if (obj != null) {
                    return CoreUtilities.autoAttrib(obj, attribute.fulfill(1));
                }
                return null;
            }
        });
        registerTag("asduration", registeredObjectTags.get("as_duration"));

        // <--[tag]
        // @attribute <el@element.escaped>
        // @returns Element
        // @group conversion
        // @description
        // Returns the element, escaped for safe reuse.
        // Inverts <@link tag el@element.unescaped>
        // See <@link language property escaping>
        // -->
        registerTag("escaped", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                return new Element(EscapeTags.escape(element)).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.sql_escaped>
        // @returns Element
        // @group conversion
        // @description
        // Returns the element, escaped for safe use in SQL.
        // -->
        registerTag("sql_escaped", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                return new Element(SQLEscaper.escapeSQL(element)).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.unescaped>
        // @returns Element
        // @group conversion
        // @description
        // Returns the element, unescaped.
        // Inverts <@link tag el@element.escaped>
        // See <@link language property escaping>
        // -->
        registerTag("unescaped", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                return new Element(EscapeTags.unEscape(element)).getObjectAttribute(attribute.fulfill(1));
            }
        });

        /////////////////////
        //   DEBUG ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <el@element.debug>
        // @returns Element
        // @group debug
        // @description
        // Returns a standard debug representation of the Element.
        // -->
        registerTag("debug", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(object.debug())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.prefix>
        // @returns Element
        // @group debug
        // @description
        // Returns the prefix of the element.
        // -->
        registerTag("prefix", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(object.getPrefix())
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        /////////////////////
        //   ELEMENT CHECKING ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <el@element.difference[<element>]>
        // @returns Element(Number)
        // @group element checking
        // @description
        // Returns a number representing the difference between the two elements. (Uses Levenshtein logic).
        // -->
        registerTag("difference", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                String two = attribute.getContext(1);
                return new Element(CoreUtilities.getLevenshteinDistance(element, two))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.contains_any_case_sensitive_text[<element>|...]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains any of a list of specified elements, case sensitive.
        // -->
        registerTag("contains_any_case_sensitive", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                dList list = dList.valueOf(attribute.getContext(1));
                for (String list_element : list) {
                    if (element.contains(list_element)) {
                        return Element.TRUE.getObjectAttribute(attribute.fulfill(1));
                    }
                }
                return Element.FALSE.getObjectAttribute(attribute.fulfill(1));
            }
        });
        TagRunnable.ObjectForm r = registeredObjectTags.get("contains_any_case_sensitive").clone();
        r.name = null;
        registerTag("contains_any_case_sensitive_text", r);

        // <--[tag]
        // @attribute <el@element.contains_any_text[<element>|...]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains any of a list of specified elements, case insensitive.
        // -->
        registerTag("contains_any", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                dList list = dList.valueOf(CoreUtilities.toLowerCase(attribute.getContext(1)));
                String ellow = CoreUtilities.toLowerCase(element);
                for (String list_element : list) {
                    if (ellow.contains(list_element)) {
                        return Element.TRUE.getObjectAttribute(attribute.fulfill(1));
                    }
                }
                return Element.FALSE.getObjectAttribute(attribute.fulfill(1));
            }
        });
        r = registeredObjectTags.get("contains_any").clone();
        r.name = null;
        registerTag("contains_any_text", r);

        // <--[tag]
        // @attribute <el@element.contains_case_sensitive_text[<element>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains a specified element, case sensitive.
        // -->
        registerTag("contains_case_sensitive", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                String contains = attribute.getContext(1);
                if (element.contains(contains)) {
                    return new Element("true").getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return new Element("false").getObjectAttribute(attribute.fulfill(1));
                }
            }
        });
        r = registeredObjectTags.get("contains_case_sensitive").clone();
        r.name = null;
        registerTag("contains_case_sensitive_text", r);

        // <--[tag]
        // @attribute <el@element.contains_text[<element>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains a specified element, case insensitive. Can use
        // regular expression by prefixing the element with regex:
        // -->
        registerTag("contains", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                String contains = attribute.getContext(1);

                if (CoreUtilities.toLowerCase(contains).startsWith("regex:")) {

                    if (Pattern.compile(contains.substring(("regex:").length()), Pattern.CASE_INSENSITIVE).matcher(element).matches()) {
                        return new Element("true").getObjectAttribute(attribute.fulfill(1));
                    }
                    else {
                        return new Element("false").getObjectAttribute(attribute.fulfill(1));
                    }
                }
                else if (CoreUtilities.toLowerCase(element).contains(CoreUtilities.toLowerCase(contains))) {
                    return new Element("true").getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return new Element("false").getObjectAttribute(attribute.fulfill(1));
                }
            }
        });
        r = registeredObjectTags.get("contains").clone();
        r.name = null;
        registerTag("contains_text", r);

        // <--[tag]
        // @attribute <el@element.contains_all_text[<element>|...]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains all of the specified strings, case insensitive.
        // -->
        registerTag("contains_all", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                dList list = dList.valueOf(CoreUtilities.toLowerCase(attribute.getContext(1)));
                String ellow = CoreUtilities.toLowerCase(element);
                for (String list_element : list) {
                    if (!ellow.contains(list_element)) {
                        return new Element("false").getObjectAttribute(attribute.fulfill(1));
                    }
                }
                return new Element("true").getObjectAttribute(attribute.fulfill(1));
            }
        });
        r = registeredObjectTags.get("contains_all").clone();
        r.name = null;
        registerTag("contains_all_text", r);

        // <--[tag]
        // @attribute <el@element.contains_all_case_sensitive_text[<element>|...]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains all of the specified strings, case sensitive.
        // -->
        registerTag("contains_all_case_sensitive", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String element = ((Element) object).element;
                dList list = dList.valueOf(attribute.getContext(1));
                for (String list_element : list) {
                    if (!element.contains(list_element)) {
                        return new Element("false").getObjectAttribute(attribute.fulfill(1));
                    }
                }
                return new Element("true").getObjectAttribute(attribute.fulfill(1));
            }
        });
        r = registeredObjectTags.get("contains_all_case_sensitive").clone();
        r.name = null;
        registerTag("contains_all_case_sensitive_text", r);

        // <--[tag]
        // @attribute <el@element.ends_with[<element>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element ends with a specified element.
        // -->
        registerTag("ends_with", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(CoreUtilities.toLowerCase(((Element) object).element).
                        endsWith(CoreUtilities.toLowerCase(attribute.getContext(1))))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("endswith", registeredObjectTags.get("ends_with"));

        // <--[tag]
        // @attribute <el@element.equals_case_sensitive[<element>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element matches another element, case-sensitive.
        // -->
        registerTag("equals_case_sensitive", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.equals_case_sensitive[...] must have a value.");
                    return null;
                }
                return new Element(((Element) object).element.equals(attribute.getContext(1)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("equals_with_case", registeredObjectTags.get("equals_case_sensitive"));

        // <--[tag]
        // @attribute <el@element.matches[<regex>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element matches a regex input.
        // -->
        registerTag("matches", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.matches[...] must have a value.");
                    return null;
                }
                return new Element(((Element) object).element.matches(attribute.getContext(1))).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.regex[<regex>].group[<group>]>
        // @returns Element
        // @group element checking
        // @description
        // Returns the specific group from a regex match.
        // Specify group 0 for the whole match.
        // For example, <el@val[hello5world].regex[.*(\d).*].group[1]> returns '5'.
        // -->
        registerTag("regex", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1) || !attribute.hasContext(2)) {
                    dB.echoError("The tag el@element.regex[...] must have a value.");
                    return null;
                }
                String regex = attribute.getContext(1);
                Matcher m = Pattern.compile(regex).matcher(((Element) object).element);
                if (!m.matches()) {
                    return null;
                }
                int group = new Element(attribute.getContext(2)).asInt();
                if (group < 0) {
                    group = 0;
                }
                if (group > m.groupCount()) {
                    group = m.groupCount();
                }
                return new Element(m.group(group)).getObjectAttribute(attribute.fulfill(2));
            }
        });

        // <--[tag]
        // @attribute <el@element.length>
        // @returns Element(Number)
        // @group element checking
        // @description
        // Returns the length of the element.
        // -->
        registerTag("length", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(((Element) object).element.length()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.not>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns the opposite of the element
        // IE, true returns false and false returns true.
        // -->
        registerTag("not", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(!((Element) object).element.equalsIgnoreCase("true")).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.and[<element>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether both the element and the second element are true.
        // -->
        registerTag("and", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(((Element) object).element.equalsIgnoreCase("true") && attribute.getContext(1).equalsIgnoreCase("true"))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.or[<element>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether either the element or the second element are true.
        // -->
        registerTag("or", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(((Element) object).element.equalsIgnoreCase("true") || attribute.getContext(1).equalsIgnoreCase("true"))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.xor[<element>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element and the second element are true and false (exclusive or).
        // -->
        registerTag("xor", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(((Element) object).element.equalsIgnoreCase("true") != attribute.getContext(1).equalsIgnoreCase("true"))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.starts_with[<element>]>
        // @returns Element(Boolean)
        // @group element checking
        // @description
        // Returns whether the element starts with a specified element.
        // -->
        registerTag("starts_with", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(CoreUtilities.toLowerCase(((Element) object).element).startsWith(CoreUtilities.toLowerCase(attribute.getContext(1))))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("startswith", registeredObjectTags.get("starts_with"));

        // <--[tag]
        // @attribute <el@element.index_of[<element>]>
        // @returns Element(Number)
        // @group element checking
        // @description
        // Returns the index of the first occurrence of a specified element.
        // Returns 0 if the element never occurs within the element.
        // -->
        registerTag("index_of", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.index_of[...] must have a value.");
                    return null;
                }
                return new Element(CoreUtilities.toLowerCase(((Element) object).element)
                        .indexOf(CoreUtilities.toLowerCase(attribute.getContext(1))) + 1)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.last_index_of[<element>]>
        // @returns Element(Number)
        // @group element checking
        // @description
        // Returns the index of the last occurrence of a specified element.
        // Returns 0 if the element never occurs within the element.
        // -->
        registerTag("last_index_of", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.last_index_of[...] must have a value.");
                    return null;
                }
                return new Element(CoreUtilities.toLowerCase(((Element) object).element)
                        .lastIndexOf(CoreUtilities.toLowerCase(attribute.getContext(1))) + 1)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.char_at[<#>]>
        // @returns Element
        // @group element checking
        // @description
        // Returns the character at a specified index.
        // Returns null if the index is outside the range of the element.
        // -->
        registerTag("char_at", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.char_at[...] must have a value.");
                    return null;
                }
                int index = attribute.getIntContext(1) - 1;
                if (index < 0 || index >= ((Element) object).element.length()) {
                    return null;
                }
                else {
                    return new Element(String.valueOf(((Element) object).element.charAt(index)))
                            .getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        /////////////////////
        //   ELEMENT MANIPULATION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <el@element.after_last[<element>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the portion of an element after the last occurrence of a specified element.
        // For example: abcabc .after_last[b] returns c.
        // -->
        registerTag("after_last", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.after_last[...] must have a value.");
                    return null;
                }
                String delimiter = attribute.getContext(1);
                if (CoreUtilities.toLowerCase(((Element) object).element).contains(CoreUtilities.toLowerCase(delimiter))) {
                    return new Element(((Element) object).element.substring
                            (CoreUtilities.toLowerCase(((Element) object).element).lastIndexOf(CoreUtilities.toLowerCase(delimiter)) + delimiter.length()))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return new Element("").getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.after[<element>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the portion of an element after the first occurrence of a specified element.
        // For example: HelloWorld .after[Hello] returns World.
        // -->
        registerTag("after", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.after[...] must have a value.");
                    return null;
                }
                String delimiter = attribute.getContext(1);
                if (CoreUtilities.toLowerCase(((Element) object).element).contains(CoreUtilities.toLowerCase(delimiter))) {
                    return new Element(((Element) object).element.substring
                            (CoreUtilities.toLowerCase(((Element) object).element).indexOf(CoreUtilities.toLowerCase(delimiter)) + delimiter.length()))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return new Element("").getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.before_last[<element>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the portion of an element before the last occurrence of a specified element.
        // For example: abcabc .before_last[b] returns abca.
        // -->
        registerTag("before_last", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.before_last[...] must have a value.");
                    return null;
                }
                String delimiter = attribute.getContext(1);
                if (CoreUtilities.toLowerCase(((Element) object).element).contains(CoreUtilities.toLowerCase(delimiter))) {
                    return new Element(((Element) object).element.substring
                            (0, CoreUtilities.toLowerCase(((Element) object).element).lastIndexOf(CoreUtilities.toLowerCase(delimiter))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return new Element(((Element) object).element).getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.before[<element>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the portion of an element before the first occurrence of specified element.
        // For example: abcd .before[c] returns ab.
        // -->
        registerTag("before", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.before[...] must have a value.");
                    return null;
                }
                String delimiter = attribute.getContext(1);
                if (CoreUtilities.toLowerCase(((Element) object).element).contains(CoreUtilities.toLowerCase(delimiter))) {
                    return new Element(((Element) object).element.substring
                            (0, CoreUtilities.toLowerCase(((Element) object).element).indexOf(CoreUtilities.toLowerCase(delimiter))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                else {
                    return new Element(((Element) object).element).getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.replace[((first)regex:)<element>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the element with all instances of a element removed.
        // -->

        // <--[tag]
        // @attribute <el@element.replace[((first)regex:)<element>].with[<element>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the element with all instances of a element replaced with another.
        // Specify regex: at the start of the replace element to use Regex replacement.
        // Specify firstregex: at the start of the replace element to Regex 'replaceFirst'
        // -->
        registerTag("replace", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.replace[...] must have a value.");
                    return null;
                }
                String replace = attribute.getContext(1);
                String replacement = "";
                attribute.fulfill(1);
                if (attribute.startsWith("with")) {
                    if (attribute.hasContext(1)) {
                        replacement = attribute.getContext(1);
                        if (replacement == null) {
                            replacement = "";
                        }
                        attribute.fulfill(1);
                    }
                }

                if (replace.startsWith("regex:")) {
                    return new Element(((Element) object).element.replaceAll(replace.substring("regex:".length()), replacement))
                            .getObjectAttribute(attribute);
                }
                if (replace.startsWith("firstregex:")) {
                    return new Element(((Element) object).element.replaceFirst(replace.substring("firstregex:".length()), replacement))
                            .getObjectAttribute(attribute);
                }
                else {
                    return new Element(((Element) object).element.replaceAll("(?i)" + Pattern.quote(replace), replacement))
                            .getObjectAttribute(attribute);
                }
            }
        });
        // <--[tag]
        // @attribute <el@element.replace_text[((first)regex:)<element>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the element with all instances of a element removed.
        // -->

        // <--[tag]
        // @attribute <el@element.replace_text[((first)regex:)<element>].with[<element>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the element with all instances of a element replaced with another.
        // Specify regex: at the start of the replace element to use Regex replacement.
        // Specify firstregex: at the start of the replace element to Regex 'replaceFirst'
        // -->
        r = registeredObjectTags.get("replace").clone();
        r.name = null;
        registerTag("replace_text", r);

        // <--[tag]
        // @attribute <el@element.format_number>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns a number reformatted for easier reading.
        // For example: 1234567 will become 1,234,567.
        // -->
        registerTag("format_number", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                try {
                    int decimal = ((Element) object).element.indexOf('.');
                    String shortelement;
                    String afterdecimal;
                    if (decimal != -1) {
                        shortelement = ((Element) object).element.substring(0, decimal);
                        afterdecimal = ((Element) object).element.substring(decimal);
                    }
                    else {
                        shortelement = ((Element) object).element;
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
                    return new Element(negative + intform + afterdecimal).getObjectAttribute(attribute.fulfill(1));
                }
                catch (Exception ex) {
                    dB.echoError(ex);
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <el@element.to_list>
        // @returns dList
        // @group element manipulation
        // @description
        // Returns a dList of each letter in the element.
        // -->
        registerTag("to_list", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                dList list = new dList();
                for (int i = 0; i < ((Element) object).element.length(); i++) {
                    list.add(String.valueOf(((Element) object).element.charAt(i)));
                }
                return list.getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.trim>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the value of an element minus any leading or trailing whitespace.
        // -->
        registerTag("trim", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(((Element) object).element.trim()).getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.to_uppercase>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the value of an element in all uppercase letters.
        // -->
        registerTag("to_uppercase", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(((Element) object).element.toUpperCase()).getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("upper", registeredObjectTags.get("to_uppercase"));

        // <--[tag]
        // @attribute <el@element.to_lowercase>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the value of an element in all lowercase letters.
        // -->
        registerTag("to_lowercase", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(CoreUtilities.toLowerCase(((Element) object).element)).getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("lower", registeredObjectTags.get("to_lowercase"));

        // <--[tag]
        // @attribute <el@element.to_titlecase>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns The Value Of An Element In Title Case.
        // -->
        registerTag("to_titlecase", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (((Element) object).element.length() == 0) {
                    return new Element("").getObjectAttribute(attribute.fulfill(1));
                }
                StringBuilder TitleCase = new StringBuilder(((Element) object).element.length());
                String Upper = ((Element) object).element.toUpperCase();
                String Lower = CoreUtilities.toLowerCase(((Element) object).element);
                TitleCase.append(Upper.charAt(0));
                for (int i = 1; i < ((Element) object).element.length(); i++) {
                    if (((Element) object).element.charAt(i - 1) == ' ') {
                        TitleCase.append(Upper.charAt(i));
                    }
                    else {
                        TitleCase.append(Lower.charAt(i));
                    }
                }
                return new Element(TitleCase.toString()).getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("totitlecase", registeredObjectTags.get("to_titlecase"));

        // <--[tag]
        // @attribute <el@element.substring[<#>(,<#>)]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the portion of an element between two element indices.
        // If no second index is specified, it will return the portion of an
        // element after the specified index.
        // -->
        registerTag("substring", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.substring[...] must have a value.");
                    return null;
                }
                int beginning_index = new Element(attribute.getContext(1).split(",")[0]).asInt() - 1;
                int ending_index;
                if (attribute.getContext(1).split(",").length > 1) {
                    ending_index = new Element(attribute.getContext(1).split(",")[1]).asInt();
                }
                else {
                    ending_index = ((Element) object).element.length();
                }
                if (beginning_index < 0) {
                    beginning_index = 0;
                }
                if (beginning_index > ((Element) object).element.length()) {
                    beginning_index = ((Element) object).element.length();
                }
                if (ending_index > ((Element) object).element.length()) {
                    ending_index = ((Element) object).element.length();
                }
                if (ending_index < beginning_index) {
                    ending_index = beginning_index;
                }
                return new Element(((Element) object).element.substring(beginning_index, ending_index))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });
        registerTag("substr", registeredObjectTags.get("substring"));

        // <--[tag]
        // @attribute <el@element.pad_left[<#>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the value of an element extended to reach a minimum specified length
        // by adding spaces to the left side.
        // -->
        registerTag("pad_left", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.pad_left[...] must have a value.");
                    return null;
                }
                String with = String.valueOf((char) 0x00A0);
                int length = attribute.getIntContext(1);
                attribute = attribute.fulfill(1);
                // <--[tag]
                // @attribute <el@element.pad_left[<#>].with[<element>]>
                // @returns Element
                // @group string manipulation
                // @description
                // Returns the value of an element extended to reach a minimum specified length
                // by adding a specific symbol to the left side.
                // -->
                if (attribute.startsWith("with")
                        && attribute.hasContext(1)) {
                    with = String.valueOf(attribute.getContext(1).charAt(0));
                    attribute = attribute.fulfill(1);
                }
                String padded = ((Element) object).element;
                while (padded.length() < length) {
                    padded = with + padded;
                }
                return new Element(padded).getObjectAttribute(attribute);
            }
        });

        // <--[tag]
        // @attribute <el@element.pad_right[<#>]>
        // @returns Element
        // @group element manipulation
        // @description
        // Returns the value of an element extended to reach a minimum specified length
        // by adding spaces to the right side.
        // -->
        registerTag("pad_right", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.pad_right[...] must have a value.");
                    return null;
                }
                String with = String.valueOf((char) 0x00A0);
                int length = attribute.getIntContext(1);
                attribute = attribute.fulfill(1);
                // <--[tag]
                // @attribute <el@element.pad_right[<#>].with[<element>]>
                // @returns Element
                // @group element manipulation
                // @description
                // Returns the value of an element extended to reach a minimum specified length
                // by adding a specific symbol to the right side.
                // -->
                if (attribute.startsWith("with")
                        && attribute.hasContext(1)) {
                    with = String.valueOf(attribute.getContext(1).charAt(0));
                    attribute = attribute.fulfill(1);
                }
                StringBuilder padded = new StringBuilder(((Element) object).element);
                while (padded.length() < length) {
                    padded.append(with);
                }
                return new Element(padded.toString()).getObjectAttribute(attribute);
            }
        });

        /////////////////////
        //   MATH ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <el@element.abs>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the absolute value of the element.
        // -->
        registerTag("abs", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.abs(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.max[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the higher number: this element or the specified one.
        // -->
        registerTag("max", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.max(ele.asDouble(), new Element(attribute.getContext(1)).asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.min[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the lower number: this element or the specified one.
        // -->
        registerTag("min", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.min(ele.asDouble(), new Element(attribute.getContext(1)).asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.add_int[<#>]>
        // @returns Element(Number)
        // @group math
        // @description
        // Returns the element plus a number, using integer math.
        // -->
        registerTag("add_int", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(ele.asLong() + aH.getLongFrom(attribute.getContext(1)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.div_int[<#>]>
        // @returns Element(Number)
        // @group math
        // @description
        // Returns the element divided by a number.
        // -->
        registerTag("div_int", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(ele.asLong() / aH.getLongFrom(attribute.getContext(1)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.mul_int[<#>]>
        // @returns Element(Number)
        // @group math
        // @description
        // Returns the element multiplied by a number.
        // -->
        registerTag("mul_int", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(ele.asLong() * aH.getLongFrom(attribute.getContext(1)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.sub_int[<#>]>
        // @returns Element(Number)
        // @group math
        // @description
        // Returns the element minus a number.
        // -->
        registerTag("sub_int", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(ele.asLong() - aH.getLongFrom(attribute.getContext(1)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.add[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the element plus a number.
        // -->
        registerTag("add", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.add[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                try {
                    return new Element(ele.asBigDecimal().add(ele.getBD(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (Throwable e) {
                    return new Element(ele.asDouble() + (aH.getDoubleFrom(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.div[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the element divided by a number.
        // -->
        registerTag("div", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.div[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                try {
                    return new Element(ele.asBigDecimal().divide(ele.getBD(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (Throwable e) {
                    return new Element(ele.asDouble() / (aH.getDoubleFrom(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.mod[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the remainder of the element divided by a number.
        // -->
        registerTag("mod", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.mod[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(ele.asDouble() % (aH.getDoubleFrom(attribute.getContext(1))))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.mul[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the element multiplied by a number.
        // -->
        registerTag("mul", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.mul[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                try {
                    return new Element(ele.asBigDecimal().multiply(ele.getBD(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (Throwable e) {
                    return new Element(ele.asDouble() * (aH.getDoubleFrom(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.sub[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the element minus a number.
        // -->
        registerTag("sub", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.sub[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                try {
                    return new Element(ele.asBigDecimal().subtract(ele.getBD(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (Throwable e) {
                    return new Element(ele.asDouble() - (aH.getDoubleFrom(attribute.getContext(1))))
                            .getObjectAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.sqrt>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the square root of the element.
        // -->
        registerTag("sqrt", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.sqrt(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.log[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the logarithm of the element, with the base of the specified number.
        // -->
        registerTag("log", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.log[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.log(ele.asDouble()) / Math.log(aH.getDoubleFrom(attribute.getContext(1))))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.ln>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the natural logarithm of the element.
        // -->
        registerTag("ln", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.log(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.power[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the element to the power of a number.
        // -->
        registerTag("power", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.power[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.pow(ele.asDouble(), aH.getDoubleFrom(attribute.getContext(1))))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.asin>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the arc-sine of the element.
        // -->
        registerTag("asin", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.asin(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.acos>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the arc-cosine of the element.
        // -->
        registerTag("acos", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.acos(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.atan>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the arc-tangent of the element.
        // -->
        registerTag("atan", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.atan(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.cos>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the cosine of the element.
        // -->
        registerTag("cos", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.cos(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.sin>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the sine of the element.
        // -->
        registerTag("sin", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.sin(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.tan>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Returns the tangent of the element.
        // -->
        registerTag("tan", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.tan(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.to_degrees>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Converts the element from radians to degrees.
        // -->
        registerTag("to_degrees", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.toDegrees(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.to_radians>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Converts the element from degrees to radians.
        // -->
        registerTag("to_radians", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.toRadians(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.round_up>
        // @returns Element(Number)
        // @group math
        // @description
        // Rounds a decimal upward.
        // -->
        registerTag("round_up", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element((long) Math.ceil(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.round_down>
        // @returns Element(Number)
        // @group math
        // @description
        // Rounds a decimal downward.
        // -->
        registerTag("round_down", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element((long) Math.floor(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.atan2[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Interprets the element to be a Y value and the input value to be an X value (meaning: <Y.atan2[X]>),
        // and returns an angle representing the vector of (X,Y).
        // -->
        registerTag("atan2", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.atan2[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element(Math.atan2(ele.asDouble(), attribute.getDoubleContext(1)))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.round_to[<#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Rounds a decimal to the specified place.
        // -->
        registerTag("round_to", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.round_to[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                int ten = (int) Math.pow(10, attribute.getIntContext(1));
                return new Element(((double) Math.round(ele.asDouble() * ten)) / ten)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.round>
        // @returns Element(Number)
        // @group math
        // @description
        // Rounds a decimal.
        // -->
        registerTag("round", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                return new Element((long) Math.round(ele.asDouble()))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.round_to_precision[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Rounds a decimal to the specified precision.
        // -->
        registerTag("round_to_precision", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.round_to_precision[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                double precision = attribute.getDoubleContext(1);
                return new Element(((double) Math.round(ele.asDouble() / precision)) * precision)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.round_down_to_precision[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Rounds a decimal downward to the specified precision.
        // -->
        registerTag("round_down_to_precision", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.round_down_to_precision[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                double precision = attribute.getDoubleContext(1);
                return new Element(Math.floor(ele.asDouble() / precision) * precision)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.round_up_to_precision[<#.#>]>
        // @returns Element(Decimal)
        // @group math
        // @description
        // Rounds a decimal upward to the specified precision.
        // -->
        registerTag("round_up_to_precision", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag el@element.round_up_to_precision[...] must have a value.");
                    return null;
                }
                Element ele = (Element) object;
                if (!ele.isDouble()) {
                    dB.echoError("Element '" + ele + "' is not a valid decimal number!");
                    return null;
                }
                double precision = attribute.getDoubleContext(1);
                return new Element(Math.ceil(ele.asDouble() / precision) * precision)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.base64_encode>
        // @returns Element
        // @group conversion
        // @description
        // Encodes the element using Base64 encoding.
        // -->
        registerTag("base64_encode", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String encoded = Base64.getEncoder().encodeToString(((Element) object).element.getBytes());
                return new Element(encoded)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.base64_decode>
        // @returns Element
        // @group conversion
        // @description
        // Decodes the element using Base64 encoding. Must be valid Base64 input.
        // -->
        registerTag("base64_decode", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String decoded = new String(Base64.getDecoder().decode(((Element) object).element));
                return new Element(decoded)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.hex_encode>
        // @returns Element
        // @group conversion
        // @description
        // Encodes the element using hexadecimal encoding.
        // -->
        registerTag("hex_encode", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String encoded = DatatypeConverter.printHexBinary(((Element) object).element.getBytes());
                return new Element(encoded)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.hex_decode>
        // @returns Element
        // @group conversion
        // @description
        // Decodes the element using hexadecimal encoding. Must be valid hexadecimal input.
        // -->
        registerTag("hex_decode", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                String decoded = new String(DatatypeConverter.parseHexBinary(((Element) object).element));
                return new Element(decoded)
                        .getObjectAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <el@element.url_encode>
        // @returns Element
        // @group conversion
        // @description
        // Encodes the element using URL encoding.
        // -->
        registerTag("url_encode", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                try {
                    String encoded = URLEncoder.encode(((Element) object).element, "UTF-8");
                    return new Element(encoded)
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (Exception e) {
                    dB.echoError(e);
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.url_decode>
        // @returns Element
        // @group conversion
        // @description
        // Decodes the element using URL encoding. Must be valid URL-encoded input.
        // -->
        registerTag("url_decode", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                try {
                    String decoded = URLDecoder.decode(((Element) object).element, "UTF-8");
                    return new Element(decoded)
                            .getObjectAttribute(attribute.fulfill(1));
                }
                catch (Exception e) {
                    dB.echoError(e);
                    return null;
                }
            }
        });

        // <--[tag]
        // @attribute <el@element.type>
        // @returns Element
        // @description
        // Always returns 'Element' for Element objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->
        registerTag("type", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element("Element").getObjectAttribute(attribute.fulfill(1));
            }
        });

    }

    //public static HashMap<String, TagRunnable> registeredTags = new HashMap<String, TagRunnable>();

    public static HashMap<String, TagRunnable.ObjectForm> registeredObjectTags = new HashMap<>();

    public static void registerTag(String name, TagRunnable.ObjectForm runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registeredObjectTags.put(name, runnable);
    }

    public static void registerTag(String name, final TagRunnable runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registerTag(name, new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                return new Element(runnable.run(attribute, object)).getObjectAttribute(attribute);
            }
        });
    }

    @Override
    public <T extends dObject> T asObjectType(Class<T> type, TagContext context) {
        return null;
    }

    @Override
    public String getAttribute(Attribute attribute) {
        return CoreUtilities.stringifyNullPass(getObjectAttribute(attribute));
    }

    @Override
    public dObject getObjectAttribute(Attribute attribute) {

        if (attribute == null) {
            if (dB.verbose) {
                dB.log("Element - Attribute null!");
            }
            return null;
        }

        if (attribute.isComplete()) {
            if (dB.verbose) {
                dB.log("Element - Attribute complete! Self return! " + element);
            }
            return this;
        }

        // TODO: Scrap getAttribute, make this functionality a core system
        String attrLow = CoreUtilities.toLowerCase(attribute.getAttributeWithoutContext(1));
        TagRunnable.ObjectForm otr = registeredObjectTags.get(attrLow);
        if (otr != null) {
            if (!otr.name.equals(attrLow)) {
                dB.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + otr.name + "': '" + attrLow + "'.");
            }
            if (dB.verbose) {
                dB.log("Element - run tag " + otr.name);
            }
            return otr.run(attribute, this);
        }
        /*
        TagRunnable tr = registeredTags.get(attrLow);
        if (tr != null) {
            if (!tr.name.equals(attrLow)) {
                dB.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + tr.name + "': '" + attrLow + "'.");
            }
            return new Element(tr.run(attribute, this));
        }*/


        // <--[tag]
        // @attribute <el@element.split[(regex:)<string>].limit[<#>]>
        // @returns dList
        // @group string manipulation
        // @description
        // Returns a list of portions of this element, split by the specified string,
        // and capped at the specified number of max list items.
        // -->
        if (attribute.startsWith("split") && attribute.startsWith("limit", 2)) {
            String split_string = (attribute.hasContext(1) ? attribute.getContext(1) : " ");
            Integer limit = (attribute.hasContext(2) ? attribute.getIntContext(2) : 1);
            if (CoreUtilities.toLowerCase(split_string).startsWith("regex:")) {
                return new dList(Arrays.asList(element.split(split_string.split(":", 2)[1], limit)))
                        .getObjectAttribute(attribute.fulfill(2));
            }
            else {
                return new dList(Arrays.asList(element.split("(?i)" + Pattern.quote(split_string), limit)))
                        .getObjectAttribute(attribute.fulfill(2));
            }
        }

        // <--[tag]
        // @attribute <el@element.split[(regex:)<string>]>
        // @returns dList
        // @group string manipulation
        // @description
        // Returns a list of portions of this element, split by the specified string.
        // -->
        if (attribute.startsWith("split")) {
            String split_string = (attribute.hasContext(1) ? attribute.getContext(1) : " ");
            if (CoreUtilities.toLowerCase(split_string).startsWith("regex:")) {
                return new dList(Arrays.asList(element.split(split_string.split(":", 2)[1])))
                        .getObjectAttribute(attribute.fulfill(1));
            }
            else {
                return new dList(Arrays.asList(element.split("(?i)" + Pattern.quote(split_string))))
                        .getObjectAttribute(attribute.fulfill(1));
            }
        }

        dObject returned = CoreUtilities.autoPropertyTagObject(this, attribute);
        if (returned != null) {
            return returned;
        }

        if (attribute.isComplete()) {
            if (dB.verbose) {
                dB.log("Element - Secondary complete! Self return! " + element);
            }
            return this;
        }

        // Unfilled attributes past this point probably means the tag is spelled
        // incorrectly. So instead of just passing through what's been resolved
        // so far, 'null' shall be returned with a debug message.

        if (!attribute.hasAlternative()) {
            dB.echoDebug(attribute.getScriptEntry(), "Unfilled attributes '" + attribute.unfilledString() +
                    "' for tag <" + attribute.getOrigin() + ">!");
        }
        if (dB.verbose) {
            dB.log("Element - Unfilled! Null!");
        }
        return null;
    }
}
