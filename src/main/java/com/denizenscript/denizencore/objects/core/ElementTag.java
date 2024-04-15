package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.tags.*;
import com.denizenscript.denizencore.tags.core.EscapeTagUtil;
import com.denizenscript.denizencore.utilities.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElementTag implements ObjectTag {

    // NOTE: Explicitly no example value
    // <--[ObjectType]
    // @name ElementTag
    // @prefix el
    // @base None
    // @ExampleTagBase element[hello_world]
    // @ExampleForReturns
    // - narrate %VALUE%
    // @format
    // Just the plain text of the element value, no prefix or formatting.
    //
    // @description
    // ElementTags are simple objects that contain a simple bit of text.
    // Their main usage is within the replaceable tag system,
    // often times returned from the use of another tag that isn't returning a specific object type, such as a location or entity.
    // For example, <player.name> or <list[item1|item2|item3].comma_separated> will both return ElementTags.
    //
    // Pluses to the ElementTag system is the ability to utilize its tag attributes,
    // which can provide a range of functionality that should be familiar from any other programming language,
    // such as 'to_uppercase', 'split', 'replace', 'contains', and many more.
    // See 'ElementTag.*' tags for more information.
    //
    // While information fetched from other tags resulting in an ElementTag is often times automatically handled,
    // it may be desirable to utilize element attributes from text/numbers/etc. that aren't already an element object.
    // To accomplish this, the standard 'element' tag base can be used for the creation of a new element.
    // For example: <element[This_is_a_test].to_uppercase>
    // will result in the value 'THIS_IS_A_TEST'.
    //
    // Note that while other objects often return their object identifier (p@, li@, e@, etc.), elements usually do not (except special type-validation circumstances).
    // They will, however, recognize the object notation "el@" if it is used.
    //
    // @Matchable
    // ElementTag matchers, often used as a default when other object types aren't available
    // "integer": plaintext: matches if the element is an integer number.
    // "decimal": plaintext: matches if the element is a decimal number.
    // "boolean": plaintext: matches if the element is a valid boolean ("true" or "false").
    //
    // -->

    // <--[language]
    // @name ElementTag(Boolean)
    // @group Object System
    // @description
    // When "ElementTag(Boolean)" appears in meta documentation, this means the input/output is an ElementTag
    // (refer to <@link objecttype ElementTag>) that is a boolean.
    // Boolean means either a "true" or a "false".
    // -->

    // <--[language]
    // @name ElementTag(Number)
    // @group Object System
    // @description
    // When "ElementTag(Number)" appears in meta documentation, this means the input/output is an ElementTag
    // (refer to <@link objecttype ElementTag>) that is an integer number.
    // That is, for example: 0, 1, 5, -4, 10002325 or any other number.
    // This does NOT include decimal numbers (like 1.5). Those will be documented as <@link language ElementTag(Decimal)>.
    //
    // In some cases, this will also be documented as "<#>".
    // -->

    // <--[language]
    // @name ElementTag(Decimal)
    // @group Object System
    // @description
    // When "ElementTag(Decimal)" appears in meta documentation, this means the input/output is an ElementTag
    // (refer to <@link objecttype ElementTag>) that is a decimal number.
    // That is, for example: 0, 1, 5, -4, 10002325, 4.2, -18.281241 or any other number.
    // While this is specifically for decimal numbers, the decimal itself is optional (will be assumed as ".0").
    //
    // In some cases, this will also be documented as "<#.#>".
    // -->

    /**
     * Should never be called directly, exists only for internal compatibility reasons.
     */
    @Fetchable("el")
    public static ElementTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        return new ElementTag(CoreUtilities.toLowerCase(string).startsWith("el@") ? string.substring(3) : string);
    }

    /**
     * Should never be called directly, exists only for internal compatibility reasons.
     */
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
            if (!has_fallback && !TagManager.isStaticParsing) {
                Debug.echoError("'" + tag + "' is an invalid " + type + "!");
            }
            return null;
        }
        return object;
    }

    private final String element;

    private String prefix;

    /**
     * If true, this element is plain text only, even if it might look like an object, and so should not be reinterpreted.
     */
    public boolean isPlainText;

    /**
     * If true, the input was raw text exactly given by a user, or constructed by multiple tags. If not, it was constructed by a tag or the system.
     */
    public boolean isRawInput;

    public ElementTag(String string, boolean isPlain) {
        this(string);
        this.isPlainText = isPlain;
    }

    public ElementTag(Enum<?> enumVal) {
        this(enumVal.name());
        this.isPlainText = true;
    }

    public ElementTag(String string) {
        this.prefix = "element";
        if (string == null) {
            if (CoreConfiguration.debugVerbose) {
                Debug.echoError("Element - Null construction!");
            }
            this.element = "null";
        }
        else {
            this.element = string;
        }
    }

    public ElementTag(boolean bool) {
        this.prefix = "boolean";
        this.element = String.valueOf(bool);
        this.isPlainText = true;
    }

    public ElementTag(int integer) {
        this.prefix = "number";
        this.element = String.valueOf(integer);
        this.isPlainText = true;
    }

    public ElementTag(byte byt) {
        this.prefix = "number";
        this.element = String.valueOf(byt);
        this.isPlainText = true;
    }

    public ElementTag(short shrt) {
        this.prefix = "number";
        this.element = String.valueOf(shrt);
        this.isPlainText = true;
    }

    public ElementTag(long lng) {
        this.prefix = "number";
        this.element = String.valueOf(lng);
        this.isPlainText = true;
    }

    public ElementTag(BigDecimal bdl) {
        this.prefix = "decimal";
        this.element = CoreUtilities.bigDecToString(bdl);
        this.isPlainText = true;
    }

    public ElementTag(double dbl) {
        this.prefix = "decimal";
        this.element = CoreUtilities.doubleToString(dbl);
        this.isPlainText = true;
    }

    public ElementTag(float flt) {
        this.prefix = "decimal";
        this.element = CoreUtilities.doubleToString(flt);
        this.isPlainText = true;
    }

    public ElementTag(String prefix, String string) {
        this(string);
        if (prefix != null) {
            this.prefix = prefix;
        }
    }

    static final BigDecimal max = new BigDecimal("10E1000");

    private BigDecimal getBD(String text) {
        BigDecimal bd = new BigDecimal(text);
        if (bd.compareTo(max) >= 1) {
            Debug.echoError("Unreasonably large number detected!");
            return max;
        }
        if (bd.scale() < 50) {
            bd = bd.setScale(50);
        }
        return bd;
    }

    public static AsciiMatcher percentageMatcher = new AsciiMatcher("%");

    public BigDecimal asBigDecimal() {
        return getBD(percentageMatcher.trimToNonMatches(element));
    }

    public double asDouble() {
        return Double.parseDouble(percentageMatcher.trimToNonMatches(element));
    }

    public float asFloat() {
        return Float.parseFloat(percentageMatcher.trimToNonMatches(element));
    }

    public int asInt() {
        return (int) asLong();
    }

    public String cleanedForLong() {
        String cleaned = percentageMatcher.trimToNonMatches(element);
        int dot = cleaned.indexOf('.');
        if (dot > 0) {
            cleaned = cleaned.substring(0, dot);
        }
        return cleaned;
    }

    public long asLong() {
        try {
            return Long.parseLong(cleanedForLong());
        }
        catch (NumberFormatException ex) {
            Debug.echoError("'" + element + "' is not a valid integer!");
            return 0;
        }
    }

    public boolean asBoolean() {
        return CoreUtilities.equalsIgnoreCase(element, "true");
    }

    public String asString() {
        return element;
    }

    public String asLowerString() {
        return CoreUtilities.toLowerCase(element);
    }

    public boolean isBoolean() {
        return CoreUtilities.equalsIgnoreCase(element, "true") || CoreUtilities.equalsIgnoreCase(element, "false");
    }

    public boolean isDouble() {
        try {
            if (!ArgumentHelper.matchesDouble(element)) {
                return false;
            }
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
            if (!ArgumentHelper.matchesDouble(element)) {
                return false;
            }
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
            return ArgumentHelper.matchesInteger(element);
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

    @Override
    public boolean shouldBeType(Class<? extends ObjectTag> type) {
        if (type == ElementTag.class || type == ObjectTag.class) {
            return true;
        }
        if (isPlainText || isRawInput) {
            return false;
        }
        String raw = toString();
        int atSign = raw.indexOf('@');
        if (atSign == -1) {
            return false;
        }
        ObjectType<?> typeData = ObjectFetcher.getType(type);
        return typeData.prefix.equals(raw.substring(0, atSign));
    }

    @Override
    public ElementTag asElement() {
        return this;
    }

    public boolean matchesEnum(Class<? extends Enum> clazz) {
        return EnumHelper.get(clazz).valuesMapLower.containsKey(EnumHelper.cleanKey(element));
    }

    public static <T extends Enum> T asEnum(Class<T> clazz, String value) {
        return (T) EnumHelper.get(clazz).valuesMapLower.get(EnumHelper.cleanKey(value));
    }

    public <T extends Enum> T asEnum(Class<T> clazz) {
        return (T) EnumHelper.get(clazz).valuesMapLower.get(EnumHelper.cleanKey(element));
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
    public String savable() {
        if (element.indexOf('@') == -1) {
            return element;
        }
        return "el@" + element;
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
        return element;
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public boolean isTruthy() {
        if (element.equals("") || CoreUtilities.equalsIgnoreCase(element, "null") || CoreUtilities.equalsIgnoreCase(element, "false")) {
            return false;
        }
        if (ArgumentHelper.matchesDouble(element)) {
            try {
                if (asDouble() == 0) {
                    return false;
                }
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
        }
        return true;
    }

    public static void register() {

        /////////////////////
        //   CONVERSION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <ElementTag.equals[<element>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is equal to another element.
        // Equivalent to if comparison: ==
        // You should never ever use this tag inside any 'if', 'while', etc. command.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "equals", (attribute, object, compareVal) -> {
            return new ElementTag(CoreUtilities.equalsIgnoreCase(object.asString(), compareVal.asString()));
        });

        // <--[tag]
        // @attribute <ElementTag.is_more_than[<number>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether this decimal number is greater than the input decimal number.
        // Equivalent to if comparison: >
        // You should never ever use this tag inside any 'if', 'while', etc. command.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "is_more_than", (attribute, object, compareVal) -> {
            return new ElementTag(object.asBigDecimal().compareTo(compareVal.asBigDecimal()) > 0);
        });

        // <--[tag]
        // @attribute <ElementTag.is_less_than[<number>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether this decimal number is less than the input decimal number.
        // Equivalent to if comparison: <
        // You should never ever use this tag inside any 'if', 'while', etc. command.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "is_less_than", (attribute, object, compareVal) -> {
            return new ElementTag(object.asBigDecimal().compareTo(compareVal.asBigDecimal()) < 0);
        });

        // <--[tag]
        // @attribute <ElementTag.is_more_than_or_equal_to[<number>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether this decimal number is greater than or equal to the input decimal number.
        // Equivalent to if comparison: >=
        // You should never ever use this tag inside any 'if', 'while', etc. command.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "is_more_than_or_equal_to", (attribute, object, compareVal) -> {
            return new ElementTag(object.asBigDecimal().compareTo(compareVal.asBigDecimal()) >= 0);
        });

        // <--[tag]
        // @attribute <ElementTag.is_less_than_or_equal_to[<number>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether this decimal number is less than or equal to the input decimal number.
        // Equivalent to if comparison: <=
        // You should never ever use this tag inside any 'if', 'while', etc. command.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "is_less_than_or_equal_to", (attribute, object, compareVal) -> {
            return new ElementTag(object.asBigDecimal().compareTo(compareVal.asBigDecimal()) <= 0);
        });

        // <--[tag]
        // @attribute <ElementTag.is_boolean>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is a boolean ('true' or 'false').
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_boolean", (attribute, object) -> {
            return new ElementTag(object.isBoolean());
        });

        // <--[tag]
        // @attribute <ElementTag.is_integer>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is an integer number (a number without a decimal point), within the limits of a Java "long" (64-bit signed integer).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_integer", (attribute, object) -> {
            if (!ArgumentHelper.matchesInteger(object.element)) {
                return new ElementTag(false);
            }
            try {
                Long.parseLong(object.element);
                return new ElementTag(true);
            }
            catch (NumberFormatException ex) {
                return new ElementTag(false);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.is_decimal>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is a valid decimal number (the decimal point is optional).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_decimal", (attribute, object) -> {
            if (!ArgumentHelper.matchesDouble(object.element)) {
                return new ElementTag(false);
            }
            try {
                return new ElementTag(object.asBigDecimal() != null);
            }
            catch (NumberFormatException ex) {
                return new ElementTag(false);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.is_odd>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is an odd-valued decimal number. Returns 'false' for non-numbers.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_odd", (attribute, object) -> {
            String element = object.element;
            return new ElementTag(ArgumentHelper.matchesDouble(element) && (object.asBigDecimal().longValue() % 2) != 0);
        });

        // <--[tag]
        // @attribute <ElementTag.is_even>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether the element is an even-valued decimal number. Returns 'false' for non-numbers.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_even", (attribute, object) -> {
            String element = object.element;
            return new ElementTag(ArgumentHelper.matchesDouble(element) && (object.asBigDecimal().longValue() % 2) == 0);
        });

        // <--[tag]
        // @attribute <ElementTag.as_element>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element as itself.
        // For use in special cases, generally not very useful.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "as_element", (attribute, object) -> {
            return object;
        }, "aselement");

        // <--[tag]
        // @attribute <ElementTag.as_boolean>
        // @returns ElementTag(Boolean)
        // @group conversion
        // @description
        // Returns the element as true/false.
        // 'true', 't', or '1' become 'true', anything else becomes 'false'.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "as_boolean", (attribute, object) -> {
            String element = object.element;
            return new ElementTag(element.equalsIgnoreCase("true")
                    || element.equalsIgnoreCase("t")
                    || element.equalsIgnoreCase("1"));
        }, "asboolean");

        // <--[tag]
        // @attribute <ElementTag.as_decimal>
        // @returns ElementTag(Decimal)
        // @group conversion
        // @description
        // Returns the element as a decimal number, or shows an error.
        // Essentially an error-check-in-a-tag. Produces no functional output change in most cases.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "as_decimal", (attribute, object) -> {
            String element = object.element;
            try {
                return new ElementTag(Double.parseDouble(element));
            }
            catch (NumberFormatException e) {
                if (!attribute.hasAlternative()) {
                    attribute.echoError("'" + element + "' is not a valid decimal number.");
                }
                return null;
            }
        }, "as_double", "asdouble");

        // <--[tag]
        // @attribute <ElementTag.truncate>
        // @returns ElementTag(Number)
        // @group conversion
        // @description
        // Returns the element as a number without a decimal by way of stripping the decimal value off the end.
        // That is, rounds towards zero.
        // This is an extremely special case tag that should only be used in very specific situations.
        // If at all unsure, this is probably the wrong tag. Consider <@link tag elementtag.round> or <@link tag elementtag.round_down> instead.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "truncate", (attribute, object) -> {
            try {
                return new ElementTag(object.asBigDecimal().longValue());
            }
            catch (NumberFormatException e) {
                if (!attribute.hasAlternative()) {
                    attribute.echoError("'" + object.element + "' is not a valid decimal number.");
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <ElementTag.as_money>
        // @returns ElementTag(Decimal)
        // @group conversion
        // @description
        // Returns the element as a number with two decimal places.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "as_money", (attribute, object) -> {
            String element = object.element;
            try {
                DecimalFormat d = new DecimalFormat("0.00", CoreUtilities.decimalFormatSymbols);
                return new ElementTag(d.format(Double.valueOf(element)));
            }
            catch (NumberFormatException e) {
                if (!attribute.hasAlternative()) {
                    attribute.echoError("'" + element + "' is not a valid decimal number.");
                }
                return null;
            }
        }, "asmoney");

        // <--[tag]
        // @attribute <ElementTag.as_list>
        // @returns ListTag
        // @group conversion
        // @deprecated use as[list]
        // @description
        // Deprecated in favor of <@link tag ObjectTag.as>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "as_list", (attribute, object) -> {
            Deprecations.asXTags.warn(attribute.context);
            String element = object.element;
            return handleNull(element, ListTag.valueOf(element, attribute.context), "ListTag", attribute.hasAlternative());
        }, "aslist");

        // <--[tag]
        // @attribute <ElementTag.as_map>
        // @returns MapTag
        // @group conversion
        // @deprecated use as[map]
        // @description
        // Deprecated in favor of <@link tag ObjectTag.as>
        // -->
        tagProcessor.registerStaticTag(MapTag.class, "as_map", (attribute, object) -> {
            Deprecations.asXTags.warn(attribute.context);
            String element = object.element;
            return handleNull(element, MapTag.valueOf(element, attribute.context), "MapTag", attribute.hasAlternative());
        });

        // <--[tag]
        // @attribute <ElementTag.as_custom>
        // @returns CustomObjectTag
        // @group conversion
        // @deprecated use as[custom]
        // @description
        // Deprecated in favor of <@link tag ObjectTag.as>
        // -->
        tagProcessor.registerTag(CustomObjectTag.class, "as_custom", (attribute, object) -> {
            Deprecations.asXTags.warn(attribute.context);
            String element = object.element;
            return handleNull(element, CustomObjectTag.valueOf(element, attribute.context), "Custom", attribute.hasAlternative());
        }, "ascustom");

        // <--[tag]
        // @attribute <ElementTag.as_script>
        // @returns ScriptTag
        // @group conversion
        // @deprecated use as[script]
        // @description
        // Deprecated in favor of <@link tag ObjectTag.as>
        // -->
        tagProcessor.registerStaticTag(ScriptTag.class, "as_script", (attribute, object) -> {
            Deprecations.asXTags.warn(attribute.context);
            String element = object.element;
            return handleNull(element, ScriptTag.valueOf(element, attribute.context), "ScriptTag", attribute.hasAlternative());
        }, "asscript");

        // <--[tag]
        // @attribute <ElementTag.as_queue>
        // @returns QueueTag
        // @group conversion
        // @deprecated use as[queue]
        // @description
        // Deprecated in favor of <@link tag ObjectTag.as>
        // -->
        tagProcessor.registerTag(QueueTag.class, "as_queue", (attribute, object) -> {
            Deprecations.asXTags.warn(attribute.context);
            String element = object.element;
            return handleNull(element, QueueTag.valueOf(element, attribute.context), "QueueTag", attribute.hasAlternative());
        }, "asqueue");

        // <--[tag]
        // @attribute <ElementTag.as_duration>
        // @returns DurationTag
        // @group conversion
        // @deprecated use as[duration]
        // @description
        // Deprecated in favor of <@link tag ObjectTag.as>
        // -->
        tagProcessor.registerStaticTag(DurationTag.class, "as_duration", (attribute, object) -> {
            Deprecations.asXTags.warn(attribute.context);
            String element = object.element;
            return handleNull(element, DurationTag.valueOf(element, attribute.context), "DurationTag", attribute.hasAlternative());
        }, "asduration");

        // <--[tag]
        // @attribute <ElementTag.escaped>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element, escaped for safe reuse.
        // Inverts <@link tag ElementTag.unescaped>.
        // See <@link language Escaping System>.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "escaped", (attribute, object) -> {
            String element = object.element;
            return new ElementTag(EscapeTagUtil.escape(element));
        });

        // <--[tag]
        // @attribute <ElementTag.html_escaped>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element, escaped for safe use in HTML.
        // -->
        AsciiMatcher htmlEscapable = new AsciiMatcher("&<>'\"");
        tagProcessor.registerStaticTag(ElementTag.class, "html_escaped", (attribute, object) -> {
            if (!htmlEscapable.containsAnyMatch(object.element)) {
                return object;
            }
            return new ElementTag(object.element.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"));

        });

        // <--[tag]
        // @attribute <ElementTag.sql_escaped>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element, escaped for safe use in SQL.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "sql_escaped", (attribute, object) -> {
            String element = object.element;
            return new ElementTag(SQLEscaper.escapeSQL(element));
        });

        // <--[tag]
        // @attribute <ElementTag.unescaped>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns the element, unescaped.
        // Inverts <@link tag ElementTag.escaped>.
        // See <@link language Escaping System>.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "unescaped", (attribute, object) -> {
            String element = object.element;
            return new ElementTag(EscapeTagUtil.unEscape(element));
        });

        // <--[tag]
        // @attribute <ElementTag.parsed>
        // @returns ObjectTag
        // @group conversion
        // @description
        // Returns the element, with any contained tags parsed.
        // WARNING: THIS TAG IS DANGEROUS TO USE, DO NOT USE IT UNLESS
        // YOU KNOW WHAT YOU ARE DOING. USE AT YOUR OWN RISK.
        // -->
        tagProcessor.registerTag(ObjectTag.class, "parsed", (attribute, object) -> {
            return TagManager.tagObject(object.element, attribute.context);
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
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "difference", (attribute, object, compareVal) -> {
            return new ElementTag(CoreUtilities.getLevenshteinDistance(object.asString(), compareVal.asString()));
        });

        // <--[tag]
        // @attribute <ElementTag.contains_any_case_sensitive_text[<element>|...]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains any of a list of specified elements, case sensitive.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ListTag.class, "contains_any_case_sensitive_text", (attribute, object, list) -> {
            String element = object.element;
            for (String value : list) {
                if (element.contains(value)) {
                    return new ElementTag(true);
                }
            }
            return new ElementTag(false);
        });
        tagProcessor.registerFutureTagDeprecation("contains_any_case_sensitive_text", "contains_any_case_sensitive");

        // <--[tag]
        // @attribute <ElementTag.contains_any_text[<element>|...]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains any of a list of specified elements, case insensitive.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ListTag.class, "contains_any_text", (attribute, object, list) -> {
            String low = object.asLowerString();
            for (String value : list) {
                if (low.contains(CoreUtilities.toLowerCase(value))) {
                    return new ElementTag(true);
                }
            }
            return new ElementTag(false);
        });
        tagProcessor.registerFutureTagDeprecation("contains_any_text", "contains_any");

        // <--[tag]
        // @attribute <ElementTag.contains_case_sensitive_text[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains a specified element, case sensitive.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "contains_case_sensitive_text", (attribute, object, contains) -> {
            return new ElementTag(object.asString().contains(contains.asString()));
        });
        tagProcessor.registerFutureTagDeprecation("contains_case_sensitive_text", "contains_case_sensitive");

        // <--[tag]
        // @attribute <ElementTag.contains_text[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains a specified element, case insensitive.
        // Can use regular expression by prefixing the element with 'regex:'.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "contains_text", (attribute, object, contains) -> {
            String contLow = contains.asLowerString();
            if (contLow.startsWith("regex:")) {
                return new ElementTag(Pattern.compile(contains.asString().substring("regex:".length()), Pattern.CASE_INSENSITIVE).matcher(object.asString()).find());
            }
            return new ElementTag(object.asLowerString().contains(contLow));
        });
        tagProcessor.registerFutureTagDeprecation("contains_text", "contains");

        // <--[tag]
        // @attribute <ElementTag.contains_all_text[<element>|...]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains all of the specified elements, case insensitive.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ListTag.class, "contains_all_text", (attribute, object, list) -> {
            String low = object.asLowerString();
            for (String value : list) {
                if (!low.contains(CoreUtilities.toLowerCase(value))) {
                    return new ElementTag(false);
                }
            }
            return new ElementTag(true);
        });
        tagProcessor.registerFutureTagDeprecation("contains_all_text", "contains_all");

        // <--[tag]
        // @attribute <ElementTag.contains_all_case_sensitive_text[<element>|...]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element contains all of the specified elements, case sensitive.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ListTag.class, "contains_all_case_sensitive_text", (attribute, object, list) -> {
            String element = object.element;
            for (String value : list) {
                if (!element.contains(value)) {
                    return new ElementTag(false);
                }
            }
            return new ElementTag(true);
        });
        tagProcessor.registerFutureTagDeprecation("contains_all_case_sensitive_text", "contains_all_case_sensitive");

        // <--[tag]
        // @attribute <ElementTag.ends_with[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element ends with a specified element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "ends_with", (attribute, object, compare) -> {
            return new ElementTag(CoreUtilities.toLowerCase(object.element).
                    endsWith(compare.asLowerString()));
        }, "endswith");

        // <--[tag]
        // @attribute <ElementTag.equals_case_sensitive[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element matches another element, case-sensitive.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "equals_case_sensitive", (attribute, object, compare) -> {
            return new ElementTag(object.element.equals(compare.asString()));
        }, "equals_with_case");

        // <--[tag]
        // @attribute <ElementTag.regex_matches[<regex>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element matches a regex input.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "regex_matches", (attribute, object, regex) -> {
            return new ElementTag(object.element.matches(regex.asString()));
        }, "matches");

        // <--[tag]
        // @attribute <ElementTag.regex[<regex>].group[<group>]>
        // @returns ElementTag
        // @group element checking
        // @description
        // Returns the specific group from a regex match.
        // Specify group 0 for the whole match.
        // For example, <element[hello5world].regex[.*(\d).*].group[1]> returns '5'.
        // -->
        tagProcessor.registerTag(ElementTag.class, "regex", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam() || !attribute.hasContext(2)) {
                return null;
            }
            String regex = attribute.getParam();
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
        });

        // <--[tag]
        // @attribute <ElementTag.is_in[<list>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element is contained by a list.
        // Essentially equivalent to <@link tag ListTag.contains_single>, but with input order reversed.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ListTag.class, "is_in", (attribute, object, list) -> {
            for (String element : list) {
                if (CoreUtilities.equalsIgnoreCase(element, object.asString())) {
                    return new ElementTag(true);
                }
            }
            return new ElementTag(false);
        });

        // <--[tag]
        // @attribute <ElementTag.length>
        // @returns ElementTag(Number)
        // @group element checking
        // @description
        // Returns the length of the element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "length", (attribute, object) -> {
            return new ElementTag(object.element.length());
        });

        // <--[tag]
        // @attribute <ElementTag.not>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns the opposite of the element
        // IE, true returns false and false returns true.
        // You should never ever use this tag inside any 'if', 'while', etc. command (instead, use the '!' negation prefix).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "not", (attribute, object) -> {
            String elem = object.asLowerString();
            if (elem.equals("true")) {
                return new ElementTag(false);
            }
            else if (elem.equals("false")) {
                return new ElementTag(true);
            }
            else {
                attribute.echoError("Invalid input to 'not' tag, '" + elem + "' is neither 'true' nor 'false'");
                return new ElementTag(true); // treat invalid as though it were 'false', so !invalid = true
            }
        });

        // <--[tag]
        // @attribute <ElementTag.and[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether both the element and the second element are true.
        // You should never ever use this tag inside any 'if', 'while', etc. command.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "and", (attribute, object) -> { // Intentional don't register param to allow short-circuit-eval
            return new ElementTag(object.asBoolean() && attribute.getParamElement().asBoolean());
        });

        // <--[tag]
        // @attribute <ElementTag.or[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether either the element or the second element are true.
        // You should never ever use this tag inside any 'if', 'while', etc. command.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "or", (attribute, object) -> { // Intentional don't register param to allow short-circuit-eval
            return new ElementTag(object.asBoolean() || attribute.getParamElement().asBoolean());
        });

        // <--[tag]
        // @attribute <ElementTag.xor[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element and the second element are true and false (exclusive or).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "xor", (attribute, object, compare) -> {
            return new ElementTag(object.element.equalsIgnoreCase("true") != compare.asString().equalsIgnoreCase("true"));
        });

        // <--[tag]
        // @attribute <ElementTag.starts_with[<element>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns whether the element starts with a specified element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "starts_with", (attribute, object, compare) -> {
            return new ElementTag(CoreUtilities.toLowerCase(object.element).startsWith(compare.asLowerString()));
        }, "startswith");

        // <--[tag]
        // @attribute <ElementTag.index_of[<element>]>
        // @returns ElementTag(Number)
        // @group element checking
        // @description
        // Returns the index of the first occurrence of a specified element.
        // Returns 0 if the element never occurs within the element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "index_of", (attribute, object, compare) -> {
            return new ElementTag(CoreUtilities.toLowerCase(object.element)
                    .indexOf(compare.asLowerString()) + 1);
        });

        // <--[tag]
        // @attribute <ElementTag.last_index_of[<element>]>
        // @returns ElementTag(Number)
        // @group element checking
        // @description
        // Returns the index of the last occurrence of a specified element.
        // Returns 0 if the element never occurs within the element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "last_index_of", (attribute, object, compare) -> {
            return new ElementTag(CoreUtilities.toLowerCase(object.element)
                    .lastIndexOf(compare.asLowerString()) + 1);
        });

        // <--[tag]
        // @attribute <ElementTag.char_at[<#>]>
        // @returns ElementTag
        // @group element checking
        // @description
        // Returns the character at a specified index.
        // Returns null if the index is outside the range of the element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "char_at", (attribute, object, indexText) -> {
            int index = indexText.asInt() - 1;
            if (index < 0 || index >= object.element.length()) {
                return null;
            }
            else {
                return new ElementTag(String.valueOf(object.element.charAt(index)));
            }
        });

        /////////////////////
        //   ELEMENT MANIPULATION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <ElementTag.repeat[<#>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns a copy of the element, repeated the specified number of times.
        // For example, "hello" .repeat[3] returns "hellohellohello"
        // An input value or zero or a negative number will result in an empty element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "repeat", (attribute, object, countText) -> {
            int repeatTimes = countText.asInt();
            if (repeatTimes <= 0) {
                return new ElementTag("");
            }
            StringBuilder result = new StringBuilder(object.element.length() * repeatTimes);
            for (int i = 0; i < repeatTimes; i++) {
                result.append(object.element);
            }
            return new ElementTag(result.toString());
        });

        // <--[tag]
        // @attribute <ElementTag.after_last[<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the portion of an element after the last occurrence of a specified element.
        // For example: abcabc .after_last[b] returns c.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "after_last", (attribute, object, delimiter) -> {
            if (CoreUtilities.toLowerCase(object.element).contains(delimiter.asLowerString())) {
                return new ElementTag(object.element.substring
                        (CoreUtilities.toLowerCase(object.element).lastIndexOf(delimiter.asLowerString()) + delimiter.asString().length()));
            }
            else {
                return new ElementTag("");
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
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "after", (attribute, object, delimiter) -> {
            if (CoreUtilities.toLowerCase(object.element).contains(delimiter.asLowerString())) {
                return new ElementTag(object.element.substring
                        (CoreUtilities.toLowerCase(object.element).indexOf(delimiter.asLowerString()) + delimiter.asString().length()));
            }
            else {
                return new ElementTag("");
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
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "before_last", (attribute, object, delimiter) -> {
            if (CoreUtilities.toLowerCase(object.element).contains(delimiter.asLowerString())) {
                return new ElementTag(object.element.substring
                        (0, CoreUtilities.toLowerCase(object.element).lastIndexOf(delimiter.asLowerString())));
            }
            else {
                return new ElementTag(object.element);
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
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "before", (attribute, object, delimiter) -> {
            if (CoreUtilities.toLowerCase(object.element).contains(delimiter.asLowerString())) {
                return new ElementTag(object.element.substring
                        (0, CoreUtilities.toLowerCase(object.element).indexOf(delimiter.asLowerString())));
            }
            else {
                return new ElementTag(object.element);
            }
        });

        // <--[tag]
        // @attribute <ElementTag.replace_text[((first)regex:)<element>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the element with all instances of an element removed.
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
        tagProcessor.registerTag(ElementTag.class, "replace_text", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam()) {
                attribute.echoError("The tag ElementTag.replace[...] must have a value.");
                return null;
            }
            String replace = attribute.getParam();
            String replacement = "";
            if (attribute.startsWith("with", 2)) {
                if (attribute.hasContext(2)) {
                    replacement = attribute.getContext(2);
                    if (replacement == null) {
                        attribute.echoError("The tag ElementTag.replace[...].with[...] must have a value.");
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
                return new ElementTag(object.element.replaceAll("(?i)" + Pattern.quote(replace), Matcher.quoteReplacement(replacement)));
            }
        });
        tagProcessor.registerFutureTagDeprecation("replace_text", "replace");

        // <--[tag]
        // @attribute <ElementTag.format_number[(<format>)]>
        // @returns ElementTag
        // @group element manipulation
        // @synonyms ElementTag.number_with_commas, ElementTag.thousands_separated
        // @description
        // Returns a number reformatted for easier reading.
        // For example: 1234567 will become 1,234,567.
        // Optionally, specify a standard number format code to instead use that.
        // For information on that optional input, refer to <@link url https://docs.oracle.com/javase/7/docs/api/java/text/DecimalFormat.html>.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "format_number", (attribute, object) -> {
            try {
                if (attribute.hasParam()) {
                    DecimalFormat format = new DecimalFormat(attribute.getParam(), CoreUtilities.decimalFormatSymbols);
                    return new ElementTag(format.format(object.asBigDecimal()));
                }
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
                attribute.echoError(ex);
            }
            return null;
        });

        // <--[tag]
        // @attribute <ElementTag.to_list>
        // @returns ListTag
        // @group element manipulation
        // @description
        // Returns a ListTag of each letter in the element.
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "to_list", (attribute, object) -> {
            ListTag list = new ListTag(object.element.length());
            for (int i = 0; i < object.element.length(); i++) {
                list.addObject(new ElementTag(String.valueOf(object.element.charAt(i)), true));
            }
            return list;
        });

        // <--[tag]
        // @attribute <ElementTag.trim>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element minus any leading or trailing whitespace.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "trim", (attribute, object) -> {
            return new ElementTag(object.element.trim());
        });

        // <--[tag]
        // @attribute <ElementTag.split_lines[<#>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the element split into separate lines based on a maximum number of characters per line.
        // This does not account for character width, so for example 20 "W"s and 20 "i"s will be treated as the same number of characters.
        // Spaces will be preferred to become newlines, unless a line does not contain any spaces.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "split_lines", (attribute, object, countText) -> {
            int characterCount = countText.asInt();
            return new ElementTag(CoreUtilities.splitLinesByCharacterCount(object.element, characterCount));
        });

        // <--[tag]
        // @attribute <ElementTag.is_uppercase>
        // @returns ElementTag(Boolean)
        // @group element manipulation
        // @description
        // Returns whether all characters in the element are uppercase letters.
        // Numbers and symbols will return false.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_uppercase", (attribute, object) -> {
            for (char c : object.element.toCharArray()) {
                if (!Character.isUpperCase(c)) {
                    return new ElementTag(false);
                }
            }
            return new ElementTag(true);
        });

        // <--[tag]
        // @attribute <ElementTag.is_lowercase>
        // @returns ElementTag(Boolean)
        // @group element manipulation
        // @description
        // Returns whether all characters in the element are lowercase.
        // Numbers and symbols will return false.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_lowercase", (attribute, object) -> {
            for (char c : object.element.toCharArray()) {
                if (!Character.isLowerCase(c)) {
                    return new ElementTag(false);
                }
            }
            return new ElementTag(true);
        });

        // <--[tag]
        // @attribute <ElementTag.to_uppercase>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element in all uppercase letters.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_uppercase", (attribute, object) -> {
            // Intentionally do not use CoreUtilities here as users may expect multi-language compat.
            return new ElementTag(object.element.toUpperCase());
        }, "upper");

        // <--[tag]
        // @attribute <ElementTag.to_lowercase>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element in all lowercase letters.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_lowercase", (attribute, object) -> {
            // Intentionally do not use CoreUtilities here as users may expect multi-language compat.
            return new ElementTag(object.element.toLowerCase());
        }, "lower");

        // <--[tag]
        // @attribute <ElementTag.to_titlecase>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns The Value Of An ElementTag In Title Case.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_titlecase", (attribute, object) -> {
            if (object.element.length() == 0) {
                return new ElementTag("");
            }
            StringBuilder TitleCase = new StringBuilder(object.element.length());
            // Intentionally do not use CoreUtilities here as users may expect multi-language compat.
            String Upper = object.element.toUpperCase();
            String Lower = object.element.toLowerCase();
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
        }, "totitlecase");

        // <--[tag]
        // @attribute <ElementTag.to_sentence_case>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value in sentence case (the first letter capitalized, the rest lowercase).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_sentence_case", (attribute, object) -> {
            if (object.element.length() == 0) {
                return new ElementTag("");
            }
            return new ElementTag(Character.toUpperCase(object.element.charAt(0)) + object.element.substring(1).toLowerCase());
        });

        // <--[tag]
        // @attribute <ElementTag.to_roman_numerals>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the element in roman numeral form. Must be in the range of 1 and 4000 (inclusive).
        // For example: <element[1169].to_roman_numerals> returns MCLXIX.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_roman_numerals", (attribute, object) -> {
            if (!object.isInt()) {
                attribute.echoError("Element '" + object + "' is not a valid number.");
                return null;
            }
            int n = object.asInt();
            if (n < 1 || n > 4000) {
                attribute.echoError("Invalid range! Must be in the range of 1 and 4000 (inclusive).");
                return null;
            }
            return new ElementTag(RomanNumerals.arabicToRoman(object.asInt()));
        });

        // <--[tag]
        // @attribute <ElementTag.from_roman_numerals>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the roman numeral string in integer form.
        // For example: <element[MCLXIX].from_roman_numerals> returns 1169.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "from_roman_numerals", (attribute, object) -> {
            int result = RomanNumerals.romanToArabic(object.element);
            if (result == -1) {
                attribute.echoError("Invalid roman numeral string!");
                return null;
            }
            return new ElementTag(result);
        });

        // <--[tag]
        // @attribute <ElementTag.substring[<#>(,<#>)]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the portion of an element between two element indices.
        // If no second index is specified, it will return the portion of an
        // element after the specified index.
        // For example: <element[hello].substring[2,4]> returns "ell"
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "substring", (attribute, object, indices) -> {
            String[] split = indices.asString().split(",");
            int beginning_index = new ElementTag(split[0]).asInt() - 1;
            int ending_index;
            if (split.length > 1) {
                ending_index = new ElementTag(split[1]).asInt();
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
        }, "substr");

        // <--[tag]
        // @attribute <ElementTag.split_args>
        // @returns ListTag
        // @group element manipulation
        // @description
        // Returns a list of portions of this element, split the same way command arguments are split.
        // That is, split by spaces but respecting the use of "quotes" to contain spaces within a single argument.
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "split_args", (attribute, object) -> {
            return new ListTag(Arrays.asList(ArgumentHelper.buildArgs(object.element, false)));
        });

        // <--[tag]
        // @attribute <ElementTag.split[((regex:)<string>)]>
        // @returns ListTag
        // @group element manipulation
        // @description
        // Returns a list of portions of this element, split by the specified string.
        // If a split string is unspecified, splits by space.
        // -->
        tagProcessor.registerTag(ListTag.class, "split", (attribute, object) -> { // non-static due to hacked sub-tag
            String split_string = (attribute.hasParam() ? attribute.getParam() : " ");
            if (CoreUtilities.toLowerCase(split_string).startsWith("regex:")) {
                split_string = split_string.split(":", 2)[1];
            }
            else {
                split_string = "(?i)" + Pattern.quote(split_string);
            }
            if (split_string.isEmpty()) {
                attribute.echoError("Cannot split over empty value. Did you mean to use 'to_list'?");
            }
            String[] split;

            // <--[tag]
            // @attribute <ElementTag.split[((regex:)<string>)].limit[<#>]>
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
        });

        // <--[tag]
        // @attribute <ElementTag.pad_left[<#>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element extended to reach a minimum specified length by adding spaces to the left side.
        // -->
        tagProcessor.registerTag(ElementTag.class, ElementTag.class, "pad_left", (attribute, object, lengthText) -> { // non-static due to hacked sub-tag
            String with = CoreUtilities.NBSP;
            int length = lengthText.asInt();

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
        });

        // <--[tag]
        // @attribute <ElementTag.pad_right[<#>]>
        // @returns ElementTag
        // @group element manipulation
        // @description
        // Returns the value of an element extended to reach a minimum specified length by adding spaces to the right side.
        // -->
        tagProcessor.registerTag(ElementTag.class, ElementTag.class, "pad_right", (attribute, object, lengthText) -> { // non-static due to hacked sub-tag
            String with = CoreUtilities.NBSP;
            int length = lengthText.asInt();

            // <--[tag]
            // @attribute <ElementTag.pad_right[<#>].with[<element>]>
            // @returns ElementTag
            // @group element manipulation
            // @description
            // Returns the value of an element extended to reach a minimum specified length by adding a specific symbol to the right side.
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
        });

        /////////////////////
        //   MATH ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <ElementTag.abs>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.absolute_value
        // @group math
        // @description
        // Returns the absolute value of the element.
        // For example: <element[-5].abs> returns 5.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "abs", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.abs(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.max[<#.#>]>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.larger,ElementTag.greater,ElementTag.higher,ElementTag.bigger,ElementTag.maximum
        // @group math
        // @description
        // Returns the higher number: this element or the specified one.
        // For example: <element[5].max[10]> returns 10.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "max", (attribute, ele, second) -> {
            if (!ele.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + ele + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.max(ele.asDouble(), second.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.min[<#.#>]>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.smaller,ElementTag.lesser,ElementTag.lower,ElementTag.minimum
        // @group math
        // @description
        // Returns the lower number: this element or the specified one.
        // For example: <element[5].min[10]> returns 5.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "min", (attribute, ele, second) -> {
            if (!ele.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + ele + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.min(ele.asDouble(), second.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.add_int[<#>]>
        // @returns ElementTag(Number)
        // @group math
        // @deprecated This tag hasn't conferred any real benefit for years.
        // @description
        // Don't use this, just use "add".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "add_int", (attribute, ele, second) -> {
            Deprecations.intTagVariants.warn(attribute.context);
            if (!ele.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + ele + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(ele.asLong() + second.asLong());
        });

        // <--[tag]
        // @attribute <ElementTag.div_int[<#>]>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Returns the element divided by a number.
        // This is a special-case Java Long Integer logic tag, and generally you should use the variant without "_int" instead.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "div_int", (attribute, ele, second) -> {
            if (!ele.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + ele + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(ele.asLong() / second.asLong());
        });

        // <--[tag]
        // @attribute <ElementTag.mul_int[<#>]>
        // @returns ElementTag(Number)
        // @group math
        // @deprecated This tag hasn't conferred any real benefit for years.
        // @description
        // Don't use this, just use "mul".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "mul_int", (attribute, ele, second) -> {
            Deprecations.intTagVariants.warn(attribute.context);
            if (!ele.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + ele + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(ele.asLong() * second.asLong());
        });

        // <--[tag]
        // @attribute <ElementTag.sub_int[<#>]>
        // @returns ElementTag(Number)
        // @group math
        // @deprecated This tag hasn't conferred any real benefit for years.
        // @description
        // Don't use this, just use "sub".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "sub_int", (attribute, ele, second) -> {
            Deprecations.intTagVariants.warn(attribute.context);
            if (!ele.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + ele + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(ele.asLong() - second.asLong());
        });

        // <--[tag]
        // @attribute <ElementTag.add[<#.#>]>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.plus,ElementTag.addition,ElementTag.+
        // @group math
        // @description
        // Returns the element plus a number.
        // -->
        TagRunnable.ObjectWithParamInterface<ElementTag, ElementTag, ElementTag> addRunnable = (attribute, object, second) -> {
            if (!object.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + object + "' is not a valid decimal number!");
                return null;
            }
            try {
                return new ElementTag(object.asBigDecimal().add(second.asBigDecimal()));
            }
            catch (Throwable e) {
                return new ElementTag(object.asDouble() + second.asDouble());
            }
        };
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "add", addRunnable);
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "+", addRunnable);

        // <--[tag]
        // @attribute <ElementTag.div[<#.#>]>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.divide,ElementTag./
        // @group math
        // @description
        // Returns the element divided by a number.
        // -->
        TagRunnable.ObjectWithParamInterface<ElementTag, ElementTag, ElementTag> divRunnable = (attribute, object, second) -> {
            if (!object.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            try {
                return new ElementTag(object.asBigDecimal().divide(second.asBigDecimal(), 64, RoundingMode.HALF_UP));
            }
            catch (Throwable e) {
                return new ElementTag(object.asDouble() / second.asDouble());
            }
        };
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "div", divRunnable);
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "/", divRunnable);

        // <--[tag]
        // @attribute <ElementTag.mod[<#.#>]>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.modulo,ElementTag.modulus,ElementTag.remainder/ElementTag.%
        // @group math
        // @description
        // Returns the remainder of the element divided by a number.
        // -->
        TagRunnable.ObjectWithParamInterface<ElementTag, ElementTag, ElementTag> modRunnable = (attribute, object, second) -> {
            if (!object.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            try {
                // Note: "remainder" method has doc "Note that this is not the modulo operation (the result can be negative)."
                // however this doc is misleading - standard modulo with "%" allows negatives in the exact same situation (first parameter is negative).
                return new ElementTag(object.asBigDecimal().remainder(second.asBigDecimal()));
            }
            catch (Throwable e) {
                return new ElementTag(object.asDouble() % second.asDouble());
            }
        };
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "mod", modRunnable);
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "%", modRunnable);

        // <--[tag]
        // @attribute <ElementTag.mul[<#.#>]>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.multiply,ElementTag.times,ElementTag.*
        // @group math
        // @description
        // Returns the element multiplied by a number.
        // -->
        TagRunnable.ObjectWithParamInterface<ElementTag, ElementTag, ElementTag> mulRunnable = (attribute, object, second) -> {
            if (!object.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            try {
                return new ElementTag(object.asBigDecimal().multiply(second.asBigDecimal()));
            }
            catch (Throwable e) {
                return new ElementTag(object.asDouble() * second.asDouble());
            }
        };
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "mul", mulRunnable);
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "*", mulRunnable);

        // <--[tag]
        // @attribute <ElementTag.sub[<#.#>]>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.subtract,ElementTag.-
        // @group math
        // @description
        // Returns the element minus a number.
        // -->
        TagRunnable.ObjectWithParamInterface<ElementTag, ElementTag, ElementTag> subRunnable = (attribute, object, second) -> {
            if (!object.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            try {
                return new ElementTag(object.asBigDecimal().subtract(second.asBigDecimal()));
            }
            catch (Throwable e) {
                return new ElementTag(object.asDouble() - second.asDouble());
            }
        };
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "sub", subRunnable);
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "-", subRunnable);

        // <--[tag]
        // @attribute <ElementTag.sqrt>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.square_root
        // @group math
        // @description
        // Returns the square root of the element.
        // Null for negative numbers.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "sqrt", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            if (ele.asDouble() < 0) {
                return null;
            }
            return new ElementTag(Math.sqrt(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.log[<#.#>]>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.logarithm
        // @group math
        // @description
        // Returns the logarithm of the element, with the base of the specified number.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "log", (attribute, object, base) -> {
            if (!object.isDouble() || !base.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + base + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.log(object.asDouble()) / Math.log(base.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.ln>
        // @returns ElementTag(Decimal)
        // @synonyms ElementTag.natural_logarithm
        // @group math
        // @description
        // Returns the natural logarithm of the element.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "ln", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.log(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.power[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the element to the power of a number.
        // -->
        TagRunnable.ObjectWithParamInterface<ElementTag, ElementTag, ElementTag> powerRunnable = (attribute, object, factor) -> {
            if (!object.isDouble() || !factor.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + factor + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.pow(object.asDouble(), factor.asDouble()));
        };
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "power", powerRunnable);
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "^", powerRunnable);

        // <--[tag]
        // @attribute <ElementTag.asin>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the arc-sine of the element in radians.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "asin", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.asin(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.acos>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the arc-cosine of the element in radians.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "acos", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.acos(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.atan>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the arc-tangent of the element in radians.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "atan", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.atan(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.atan2[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Interprets the element to be a Y value and the input value to be an X value (meaning: <Y.atan2[X]>),
        // and returns an angle in radians representing the vector of (X,Y).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "atan2", (attribute, object, second) -> {
            if (!object.isDouble() || !second.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + second + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.atan2(object.asDouble(), second.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.cos>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the cosine of the input radian angle.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "cos", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.cos(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.sin>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the sine of the input radian angle.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "sin", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.sin(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.tan>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Returns the tangent of the input radian angle.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "tan", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.tan(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.factorial>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Returns the factorial of the element. This should only be used for small values (generally: less than 20), and will become ridiculous/unusable at larger values.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "factorial", (attribute, ele) -> {
            if (!ele.isInt()) {
                attribute.echoError("Element '" + ele + "' is not a valid number!");
                return null;
            }
            int count = ele.asInt();
            BigInteger result = BigInteger.ONE;
            for (int i = 2; i <= count; i++) {
                result = result.multiply(BigInteger.valueOf(i));
            }
            return new ElementTag(result.toString());
        });

        // <--[tag]
        // @attribute <ElementTag.to_degrees>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Converts the element from radians to degrees.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_degrees", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.toDegrees(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.to_radians>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Converts the element from degrees to radians.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "to_radians", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(Math.toRadians(ele.asDouble()));
        });

        // <--[tag]
        // @attribute <ElementTag.round_up>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Rounds a decimal upward.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "round_up", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(ele.asBigDecimal().setScale(0, RoundingMode.CEILING));
        });

        // <--[tag]
        // @attribute <ElementTag.round_down>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Rounds a decimal downward.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "round_down", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(ele.asBigDecimal().setScale(0, RoundingMode.FLOOR));
        });

        // <--[tag]
        // @attribute <ElementTag.round_to[<#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Rounds a decimal to the specified place.
        // For example, 0.12345 .round_to[3] returns "0.123".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "round_to", (attribute, object, to) -> {
            if (!object.isDouble() || !to.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + to + "' is not a valid decimal number!");
                return null;
            }
            // BigDecimal is really strange with scale values.
            BigDecimal tenVal = BigDecimal.valueOf((int) Math.pow(10, to.asInt()));
            BigDecimal prec = BigDecimal.ONE.setScale(50).divide(tenVal, RoundingMode.HALF_UP);
            BigDecimal result = object.asBigDecimal().divide(prec, RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP).multiply(prec);
            return new ElementTag(result);
        });

        // <--[tag]
        // @attribute <ElementTag.round_to_precision[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Rounds a decimal to the specified precision.
        // For example, 0.12345 .round_to_precision[0.005] returns "0.125".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "round_to_precision", (attribute, object, precText) -> {
            if (!object.isDouble() || !precText.isDouble()) {
                attribute.echoError("Element '" + object + "' or '" + precText + "' is not a valid decimal number!");
                return null;
            }
            BigDecimal prec = precText.asBigDecimal();
            BigDecimal result = object.asBigDecimal().divide(prec, RoundingMode.HALF_UP).setScale(0, RoundingMode.HALF_UP).multiply(prec);
            return new ElementTag(result);
        });

        // <--[tag]
        // @attribute <ElementTag.round_down_to_precision[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Rounds a decimal downward to the specified precision.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "round_down_to_precision", (attribute, object, precText) -> {
            if (!object.isDouble() || !precText.isDouble()) {
                attribute.echoError("Element '" + object + "' is not a valid decimal number!");
                return null;
            }
            BigDecimal prec = precText.asBigDecimal();
            BigDecimal result = object.asBigDecimal().divide(prec, RoundingMode.DOWN).setScale(0, RoundingMode.DOWN).multiply(prec);
            return new ElementTag(result);
        });

        // <--[tag]
        // @attribute <ElementTag.round_up_to_precision[<#.#>]>
        // @returns ElementTag(Decimal)
        // @group math
        // @description
        // Rounds a decimal upward to the specified precision.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "round_up_to_precision", (attribute, object, precText) -> {
            if (!object.isDouble() || !precText.isDouble()) {
                attribute.echoError("Element '" + object + "' is not a valid decimal number!");
                return null;
            }
            BigDecimal prec = precText.asBigDecimal();
            BigDecimal result = object.asBigDecimal().divide(prec, RoundingMode.UP).setScale(0, RoundingMode.UP).multiply(prec);
            return new ElementTag(result);
        });

        // <--[tag]
        // @attribute <ElementTag.round>
        // @returns ElementTag(Number)
        // @group math
        // @description
        // Rounds a decimal.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "round", (attribute, ele) -> {
            if (!ele.isDouble()) {
                attribute.echoError("Element '" + ele + "' is not a valid decimal number!");
                return null;
            }
            return new ElementTag(ele.asBigDecimal().setScale(0, RoundingMode.HALF_UP));
        });

        // <--[tag]
        // @attribute <ElementTag.number_to_hex>
        // @returns ElementTag
        // @group conversion
        // @description
        // Encodes base-10 integer number to hexadecimal (base-16) format.
        // For example input of "15" will return "F".
        // See also <@link tag ElementTag.hex_to_number>
        // Consider instead <@link tag ElementTag.integer_to_binary>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "number_to_hex", (attribute, object) -> {
            if (!object.isInt()) {
                attribute.echoError("Element '" + object + "' is not a valid number!");
                return null;
            }
            return new ElementTag(Long.toHexString(object.asLong()));
        });

        // <--[tag]
        // @attribute <ElementTag.hex_to_number>
        // @returns ElementTag(Number)
        // @group conversion
        // @description
        // Encodes base-16 hexadecimal value to an integer number.
        // For example input of "F" will return "15".
        // See also <@link tag ElementTag.number_to_hex>
        // Consider instead <@link tag BinaryTag.decode_integer>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "hex_to_number", (attribute, object) -> {
            if (!ArgumentHelper.HEX_MATCHER.isOnlyMatches(object.element)) {
                attribute.echoError("Element '" + object + "' is not a valid hexadecimal number!");
                return null;
            }
            return new ElementTag(Long.parseLong(object.element, 16));
        });

        // <--[tag]
        // @attribute <ElementTag.integer_to_binary>
        // @returns BinaryTag
        // @group conversion
        // @description
        // Returns a BinaryTag holding 8 bytes of this integer number converted to binary format using big-endian 64-bit integer twos-complement encoding.
        // @example
        // # Narrates '00000000000000ff'
        // - narrate <element[255].to_binary>
        // -->
        tagProcessor.registerStaticTag(BinaryTag.class, "integer_to_binary", (attribute, object) -> {
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(object.asLong());
            return new BinaryTag(buffer.array());
        });

        // <--[tag]
        // @attribute <ElementTag.base64_encode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Encodes some text to UTF-8 Base64.
        // Equivalent to using <@link tag ElementTag.utf8_encode> and then <@link tag BinaryTag.to_base64>.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "base64_encode", (attribute, object) -> {
            String encoded = Base64.getEncoder().encodeToString(object.element.getBytes(StandardCharsets.UTF_8));
            return new ElementTag(encoded);
        });

        // <--[tag]
        // @attribute <ElementTag.base64_decode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Decodes a Base64 UTF-8 encoded text to its original text.
        // Equivalent to using <@link tag ElementTag.base64_to_binary> and then <@link tag BinaryTag.utf8_decode>.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "base64_decode", (attribute, object) -> {
            String decoded = new String(Base64.getDecoder().decode(object.element));
            return new ElementTag(decoded);
        });

        // <--[tag]
        // @attribute <ElementTag.base64_to_binary>
        // @returns BinaryTag
        // @group conversion
        // @description
        // Converts base64 encoded text to its raw binary form.
        // See also <@link tag BinaryTag.to_base64>
        // @example
        // - define data <binary[48454c4c4f20574f524c44]>
        // - define encoded <[data].to_base64>
        // - define decoded <[encoded].base64_to_binary>
        // - if <[decoded].to_hex> == <[data].to_hex>:
        //     - narrate "Everything works!"
        // -->
        tagProcessor.registerStaticTag(BinaryTag.class, "base64_to_binary", (attribute, object) -> {
            return new BinaryTag(Base64.getDecoder().decode(object.element));
        });

        // <--[tag]
        // @attribute <ElementTag.utf8_encode>
        // @returns BinaryTag
        // @group conversion
        // @description
        // Converts the text to a binary representation encoded with the standard UTF-8 encoding.
        // See also <@link tag BinaryTag.utf8_decode>
        // @example
        // # narrates "48454c4c4f20574f524c44"
        // - narrate "<element[HELLO WORLD].utf8_encode.to_hex>"
        // -->
        tagProcessor.registerStaticTag(BinaryTag.class, "utf8_encode", (attribute, object) -> {
            return new BinaryTag(object.element.getBytes(StandardCharsets.UTF_8));
        });

        // <--[tag]
        // @attribute <ElementTag.text_encode[<encoding>]>
        // @returns BinaryTag
        // @group conversion
        // @description
        // Converts the text to a binary representation encoded using the specified encoding method.
        // Input can be for example "utf-8" or "iso-8859-1".
        // "encoding" label corresponds to the standard charset names listed at <@link url https://docs.oracle.com/javase/8/docs/api/java/nio/charset/Charset.html>, or any other charsets added by your Java environment.
        // See also <@link tag BinaryTag.text_decode>
        // @example
        // # narrates "48454c4c4f20574f524c44"
        // - narrate "<element[HELLO WORLD].text_encode[us-ascii].to_hex>"
        // -->
        tagProcessor.registerStaticTag(BinaryTag.class, ElementTag.class, "text_encode", (attribute, object, encoding) -> {
            try {
                return new BinaryTag(object.element.getBytes(encoding.asString()));
            }
            catch (UnsupportedEncodingException ex) {
                attribute.echoError("Invalid encoding '" + encoding + "'");
                return null;
            }
        });

        // <--[tag]
        // @attribute <ElementTag.hex_encode>
        // @returns ElementTag
        // @group conversion
        // @deprecated use utf8_encode
        // @description
        // Deprecated in favor of <@link tag ElementTag.utf8_encode> or <@link tag ElementTag.text_encode>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "hex_encode", (attribute, object) -> {
            Deprecations.prebinaryTags.warn(attribute.context);
            String encoded = CoreUtilities.hexEncode(object.element.getBytes());
            return new ElementTag(encoded);
        });

        // <--[tag]
        // @attribute <ElementTag.hex_decode>
        // @returns ElementTag
        // @group conversion
        // @deprecated use BinaryTag.utf8_decode
        // @description
        // Deprecated in favor of <@link tag BinaryTag.utf8_decode> or <@link tag BinaryTag.text_decode>
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "hex_decode", (attribute, object) -> {
            Deprecations.prebinaryTags.warn(attribute.context);
            String decoded = new String(CoreUtilities.hexDecode(object.element));
            return new ElementTag(decoded);
        });

        // <--[tag]
        // @attribute <ElementTag.url_encode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Encodes the element using URL encoding.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "url_encode", (attribute, object) -> {
            try {
                String encoded = URLEncoder.encode(object.element, "UTF-8");
                return new ElementTag(encoded);
            }
            catch (Exception e) {
                attribute.echoError(e);
                return null;
            }
        });

        // <--[tag]
        // @attribute <ElementTag.url_decode>
        // @returns ElementTag
        // @group conversion
        // @description
        // Decodes the element using URL encoding. Must be valid URL-encoded input.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "url_decode", (attribute, object) -> {
            try {
                String decoded = URLDecoder.decode(object.element, "UTF-8");
                return new ElementTag(decoded);
            }
            catch (Exception e) {
                attribute.echoError(e);
                return null;
            }
        });

        // <--[tag]
        // @attribute <ElementTag.matches_character_set[<characters>]>
        // @returns ElementTag(Boolean)
        // @group element checking
        // @description
        // Returns true if the element contains only symbols from the given character set.
        // The character set is expected to be ASCII only.
        // This tag is case-sensitive.
        // For example:
        // "alphabet" .matches_character_set[abcdefghijklmnopqrstuvwxyz]> returns "true",
        // "Alphabet" .matches_character_set[abcdefghijklmnopqrstuvwxyz]> returns "false" because it has a capital "A",
        // and "alphabet1" .matches_character_set[abcdefghijklmnopqrstuvwxyz]> returns "false" because it has a "1".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "matches_character_set", (attribute, object, set) -> {
            return new ElementTag(new AsciiMatcher(set.element).isOnlyMatches(object.element)); // TODO: Caching!
        });

        // <--[tag]
        // @attribute <ElementTag.trim_to_character_set[<characters>]>
        // @returns ElementTag
        // @group conversion
        // @description
        // Returns only the characters within the element that match the character set.
        // The character set is expected to be ASCII only.
        // This tag is case-sensitive.
        // For example:
        // "alphabet" .trim_to_character_set[abcdefghijklmnopqrstuvwxyz]> returns "alphabet",
        // "Alphabet" .trim_to_character_set[abcdefghijklmnopqrstuvwxyz]> returns "lphabet" without the capital "A".
        // and "alphabet1" .trim_to_character_set[abcdefghijklmnopqrstuvwxyz]> returns "alphabet" without the "1".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "trim_to_character_set", (attribute, object, set) -> {
            return new ElementTag(new AsciiMatcher(set.element).trimToMatches(object.element)); // TODO: Caching!
        });

        // <--[tag]
        // @attribute <ElementTag.if_true[<object>].if_false[<object>]>
        // @returns ObjectTag
        // @group element checking
        // @description
        // If this element is 'true', returns the first given object. If it isn't 'true', returns the second given object.
        // If the input objects are tags, only the matching tag will be parsed.
        // For example: "<player.exists.if_true[<player.name>].if_false[server]>"
        // will return the player's name if there's a player present, or if not will return 'server', and won't show any errors from the '<player.name>' tag even without a player linked.
        // -->
        tagProcessor.registerTag(ObjectTag.class, "if_true", (attribute, object) -> { // non-static due to hacked sub-tag
            if (!attribute.hasParam() || !attribute.startsWith("if_false", 2) || !attribute.hasContext(2)) {
                attribute.echoError("ElementTag.if_true[...].if_false[...] malformed and missing at least one required part.");
                return null;
            }
            ObjectTag result = attribute.getContextObject(object.asBoolean() ? 1 : 2);
            attribute.fulfill(1);
            return result;
        });

        // <--[tag]
        // @attribute <ElementTag.parse_yaml>
        // @returns MapTag
        // @description
        // Parses the input YAML or JSON text into a MapTag.
        // -->
        tagProcessor.registerStaticTag(MapTag.class, "parse_yaml", (attribute, object) -> {
            return (MapTag) CoreUtilities.objectToTagForm(YamlConfiguration.load(object.asString()).contents, attribute.context);
        });

        // <--[tag]
        // @attribute <ElementTag.millis_to_time>
        // @returns TimeTag
        // @group conversion
        // @description
        // Returns a TimeTag constructed from the given number of milliseconds after the Unix Epoch (Jan. 1st 1970).
        // See also <@link tag util.current_time_millis> and <@link tag TimeTag.epoch_millis>.
        // @example
        // # Takes an arbitrary unix timestamp from an external source, and formats it for a user-friendly date/time display message.
        // - define some_unix_timestamp <util.time_now.epoch_millis>
        // - narrate "The timestamp was <[some_unix_timestamp].millis_to_time.format>"
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "millis_to_time", (attribute, object) -> {
            return new TimeTag(object.asLong());
        });
    }

    public static ObjectTagProcessor<ElementTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ElementTag)) {
            return false;
        }
        ElementTag other = (ElementTag) o;
        return element.equals(other.element);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
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
            if (CoreConfiguration.debugVerbose) {
                Debug.log("Element - Unfilled! Null!");
            }
            return null;
        }
    }

    @Override
    public ObjectTag getNextObjectTypeDown() {
        return new FailedObjectTag();
    }

    @Override
    public boolean advancedMatches(String matcher) {
        String matcherLow = CoreUtilities.toLowerCase(matcher);
        switch (matcherLow) {
            case "integer": return isInt();
            case "decimal": return isDouble();
            case "boolean": return isBoolean();
        }
        return ScriptEvent.runGenericCheck(matcher, element);
    }
}
