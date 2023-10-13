package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.*;

public class ArgumentHelper {

    // <--[language]
    // @name Number and Decimal
    // @group Common Terminology
    // @description
    // Many arguments in Denizen require the use of a 'number', or 'decimal'. Sometimes shorthanded to '#' or '#.#',
    // this kind of input can generally be filled with any reasonable positive or negative number.
    // 'decimal' inputs allow (but don't require) a decimal point in the number.
    // 'number' inputs will be rounded, so avoiding a decimal point is better. For example, '3.1' will be interpreted as just '3'.
    // -->

    /**
     * Turns a list of string arguments (separated by buildArgs) into Argument
     * Objects for easy matching and ObjectTag creation throughout Denizen.
     *
     * @param args a list of string arguments
     * @return a list of Arguments
     */
    public static List<Argument> interpret(ScriptEntry entry, List<String> args) {
        List<Argument> arg_list = new ArrayList<>(args.size());
        for (String string : args) {
            Argument newArg = new Argument(string);
            newArg.scriptEntry = entry;
            arg_list.add(newArg);
        }
        return arg_list;
    }

    /**
     * Builds an arguments array, recognizing items in quotes as a single item, but
     * otherwise splitting on a space.
     */
    public static String[] buildArgs(String stringArgs, boolean tagsContainSpaces) {
        if (stringArgs == null) {
            return null;
        }
        stringArgs = stringArgs.trim();
        stringArgs = stringArgs.replace('\r', ' ').replace('\n', ' ');
        ArrayList<String> matchList = new ArrayList<>(stringArgs.length() / 6);
        int start = 0;
        int len = stringArgs.length();
        char currentQuote = 0;
        int inTags = 0, inTagParams = 0;
        boolean currentTagHasFallback = false;
        for (int i = 0; i < len; i++) {
            char c = stringArgs.charAt(i);
            if (c == ' ' && currentQuote == 0 && inTags == 0 && !currentTagHasFallback) {
                if (i > start) {
                    matchList.add(stringArgs.substring(start, i));
                }
                start = i + 1;
            }
            else if (c == '<' && tagsContainSpaces) {
                if (i + 1 < len && TagManager.validTagFirstCharacter.isMatch(stringArgs.charAt(i + 1))) {
                    inTags++;
                }
            }
            else if (c == '>' && inTags > 0) {
                inTags--;
                if (inTags == 0) {
                    currentTagHasFallback = false;
                }
            }
            else if (c == '[' && inTags > 0) {
                inTagParams++;
            }
            else if (c == ']' && inTagParams > 0) {
                inTagParams--;
            }
            else if (c == '|' && i > 0 && stringArgs.charAt(i - 1) == '|' && inTags == 1) {
                currentTagHasFallback = true;
            }
            else if (c == '"' || c == '\'') {
                if (currentQuote == 0 && inTagParams == 0) {
                    if (i == 0 || stringArgs.charAt(i - 1) == ' ') {
                        currentQuote = c;
                        start = i + 1;
                    }
                }
                else if (currentQuote == c) {
                    if (i + 1 >= len || stringArgs.charAt(i + 1) == ' ') {
                        currentQuote = 0;
                        if (i >= start) {
                            matchList.add(stringArgs.substring(start, i));
                        }
                        i++;
                        start = i + 1;
                    }
                }
            }
        }
        if (start < len) {
            matchList.add(stringArgs.substring(start));
        }
        if (CoreConfiguration.debugScriptBuilder) {
            Debug.log("Constructed args: " + Arrays.toString(matchList.toArray()));
        }
        return matchList.toArray(new String[0]);
    }

    public static String debuggable(Object value) {
        if (value instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            for (Object obj : (Collection) value) {
                sb.append(obj == null ? "null" : debuggable(obj)).append("<G>,<Y> ");
            }
            if (sb.length() == 0) {
                return debuggable(sb);
            }
            else {
                return debuggable("[" + sb.substring(0, sb.length() - "<G>, ".length()) + "<Y>]");
            }
        }
        else if (value instanceof Double) {
            value = CoreUtilities.doubleToString((Double) value);
        }
        else if (value instanceof Float) {
            value = CoreUtilities.floatToCleanString((Float) value);
        }
        return (value != null ? (value instanceof ObjectTag ? ((ObjectTag) value).debuggable() : value.toString()) : "null");
    }

    public static String debugObj(String prefix, Object value) {
        return "<G>" + prefix + "='<Y>" + debuggable(value) + "<G>'  ";
    }

    public static String DIGITS = "0123456789", PREFIXES = "+-", DOUBLE_CHARS = "eE";
    public static AsciiMatcher DIGIT_MATCHER = new AsciiMatcher(DIGITS);
    public static AsciiMatcher INTEGER_MATCHER = new AsciiMatcher(DIGITS + PREFIXES);
    public static AsciiMatcher DOUBLE_SPECIAL_MATCHER = new AsciiMatcher(DOUBLE_CHARS);
    public static AsciiMatcher PREFIX_MATCHER = new AsciiMatcher(PREFIXES);
    public static AsciiMatcher HEX_MATCHER = new AsciiMatcher("abcdefABCDEF0123456789");

    public static boolean matchesDouble(String arg) {
        if (arg.length() == 0) {
            return false;
        }
        if (!INTEGER_MATCHER.isMatch(arg.charAt(0))) {
            return false;
        }
        if (!DIGIT_MATCHER.containsAnyMatch(arg)) {
            return false;
        }
        boolean hadDoubleSyntax = false;
        boolean hadDecimal = false;
        for (int i = 1; i < arg.length(); i++) {
            if (!DIGIT_MATCHER.isMatch(arg.charAt(i))) {
                if (hadDoubleSyntax) {
                    return false;
                }
                if (arg.charAt(i) == '.' && !hadDecimal) {
                    hadDecimal = true;
                }
                else if (i + 1 < arg.length() && DOUBLE_SPECIAL_MATCHER.isMatch(arg.charAt(i))
                        && PREFIX_MATCHER.isMatch(arg.charAt(i + 1))) {
                    hadDoubleSyntax = true;
                    i++;
                }
                else {
                    return false;
                }
            }
        }
        return true;
    }

    private static final int MIN_LONG_LENGTH = Long.toString(Long.MIN_VALUE).length();
    private static final int MAX_LONG_LENGTH = Long.toString(Long.MAX_VALUE).length();

    public static boolean matchesInteger(String arg) {
        if (!INTEGER_MATCHER.isOnlyMatches(arg)) {
            return false;
        }
        if (!matchesDouble(arg)) {
            return false;
        }
        char firstChar = arg.charAt(0);
        if (arg.length() > ((firstChar == '-' || firstChar == '+') ? MIN_LONG_LENGTH : MAX_LONG_LENGTH)) {
            return false;
        }
        return true;
    }
}
