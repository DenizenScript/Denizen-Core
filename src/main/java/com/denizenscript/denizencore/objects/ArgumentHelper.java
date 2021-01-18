package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
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
     *
     * @param stringArgs the line of arguments that need split
     * @return an array of arguments
     */
    public static String[] buildArgs(String stringArgs) {
        if (stringArgs == null) {
            return null;
        }
        stringArgs = stringArgs.trim();
        stringArgs = stringArgs.replace('\r', ' ').replace('\n', ' ');
        ArrayList<String> matchList = new ArrayList<>(stringArgs.length() / 7);
        int start = 0;
        int len = stringArgs.length();
        char currentQuote = 0;
        for (int i = 0; i < len; i++) {
            char c = stringArgs.charAt(i);
            if (c == ' ' && currentQuote == 0) {
                if (i > start) {
                    matchList.add(stringArgs.substring(start, i));
                }
                start = i + 1;
            }
            else if (c == '"' || c == '\'') {
                if (currentQuote == 0) {
                    if (i - 1 < 0 || stringArgs.charAt(i - 1) == ' ') {
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

        if (Debug.showScriptBuilder) {
            Debug.log("Constructed args: " + Arrays.toString(matchList.toArray()));
        }

        return matchList.toArray(new String[0]);
    }

    public static String debugObj(String prefix, Object value) {
        return "<G>" + prefix + "='<Y>" + (value != null ? (value instanceof ObjectTag ? ((ObjectTag) value).debuggable() : value.toString()) : "null") + "<G>'  ";
    }

    public static <T extends ObjectTag> String debugList(String prefix, Collection<T> objects) {
        if (objects == null) {
            return debugObj(prefix, null);
        }
        StringBuilder sb = new StringBuilder();
        for (ObjectTag obj : objects) {
            sb.append(obj.debuggable()).append("<G>, ");
        }
        if (sb.length() == 0) {
            return debugObj(prefix, sb);
        }
        else {
            return debugObj(prefix, "[" + sb.substring(0, sb.length() - "<G>, ".length()) + "<Y>]");
        }
    }

    public static String debugUniqueObj(String prefix, String id, Object value) {
        return "<G>" + prefix + "='<A>" + id + "<Y>(" + (value != null ? value.toString() : "null") + ")<G>'  ";
    }

    private static String DIGITS = "0123456789", PREFIXES = "+-", DOUBLE_CHARS = "eE";
    private static AsciiMatcher DIGIT_MATCHER = new AsciiMatcher(DIGITS);
    private static AsciiMatcher INTEGER_MATCHER = new AsciiMatcher(DIGITS + PREFIXES);
    private static AsciiMatcher DOUBLE_SPECIAL_MATCHER = new AsciiMatcher(DOUBLE_CHARS);
    private static AsciiMatcher PREFIX_MATCHER = new AsciiMatcher(PREFIXES);

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
