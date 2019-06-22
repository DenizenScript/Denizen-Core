package net.aufdemrand.denizencore.objects;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptRegistry;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The dScript Argument Helper will aid you in parsing and formatting arguments from a
 * dScript argument string (such as those found in a ScriptEntry.getArguments() method).
 */
public class aH {


    ////////////////////
    // Patterns and Enumerations
    /////////////////

    public enum PrimitiveType {Float, Double, Integer, Boolean, String, Word, Percentage}

    final static Pattern floatPrimitive =
            Pattern.compile("^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$");

    // <--[language]
    // @name Number and Decimal
    // @group Common Terminology
    // @description
    // Many arguments in Denizen require the use of a 'number', or 'decimal'. Sometimes shorthanded to '#' or '#.#',
    // this kind of input can generally be filled with any reasonable positive or negative number.
    // 'decimal' inputs allow (but don't require) a decimal point in the number.
    // 'number' inputs will be rounded, so avoided a decimal point is better. For example, '3.1' will be interpreted as just '3'.
    // Numbers can be verified with the 'if' commands' 'matches' functionality.
    // For example: "- if <number> matches number" ... will return true if <number> is a valid number.
    // -->
    final static Pattern doublePrimitive = floatPrimitive;

    // <--[language]
    // @name Percentage
    // @group Common Terminology
    // @description
    // Promotes the usage of a 'percentage' format to be used in applicable arguments. The 'percentage' in Denizen is
    // much like the 'number', except arguments which utilize percentages instead of numbers can also include a %.
    // Percentage arguments can generally be filled with any reasonable positive or negative number with or without a
    // decimal point and/or percentage sign. Arguments and other usages will typically refer to a percentage as
    // #.#% or <percentage>. Percentages can be verified with the 'if' commands' 'matches' functionality.
    // For example: - if <percentage> matches percentage ... will return true if <percentage> is a valid percentage.
    //
    // Generally it's best to not include the '%' symbol, and some percentage inputs will actually not accept a '%'.
    //
    // While most things explicitly labeled as being a percentage scale from zero to one hundred (0 - 100)
    // others may go from zero to one (0.0 - 1.0).
    // To translate between the two formats, you only need to multiply or divide by one hundred (100).
    //
    // -->
    final static Pattern percentagePrimitive =
            Pattern.compile("-?(?:\\d+)?(\\.\\d+)?(%)?");

    final static Pattern integerPrimitive =
            Pattern.compile("(-)?\\d+");

    final static Pattern booleanPrimitive =
            Pattern.compile("true|false", Pattern.CASE_INSENSITIVE);

    final static Pattern wordPrimitive =
            Pattern.compile("\\w+");


    ////////////////////
    // Argument Object
    //////////////////

    public static class Argument implements Cloneable {

        @Override
        public Argument clone() {
            try {
                return (Argument) super.clone();
            }
            catch (CloneNotSupportedException ex) {
                dB.echoError(ex);
                return null;
            }
        }

        public String raw_value;
        public String prefix = null;
        public String lower_prefix = null;
        public String value;
        public String lower_value;

        public dObject object = null;

        public boolean needsFill = false;
        public boolean hasSpecialPrefix = false;

        public ScriptEntry scriptEntry = null;

        public String generateRaw() {
            return prefix == null ? value : prefix + ":" + value;
        }

        public Argument(String prefix, String value) {
            this.prefix = prefix;
            this.value = TagManager.cleanOutputFully(value);
            if (prefix != null) {
                if (prefix.equals("no_prefix")) {
                    this.prefix = null;
                    raw_value = this.value;
                }
                else {
                    raw_value = prefix + ":" + this.value;
                    lower_prefix = CoreUtilities.toLowerCase(prefix);
                }
            }
            else {
                raw_value = this.value;
            }
            lower_value = CoreUtilities.toLowerCase(this.value);
            object = new Element(this.value);
        }

        public Argument(String prefix, dObject value) {
            this.prefix = prefix;
            this.value = TagManager.cleanOutputFully(value.toString());
            if (prefix != null) {
                if (prefix.equals("no_prefix")) {
                    this.prefix = null;
                    raw_value = this.value;
                }
                else {
                    raw_value = prefix + ":" + this.value;
                    lower_prefix = CoreUtilities.toLowerCase(prefix);
                }
            }
            else {
                raw_value = this.value;
            }
            lower_value = CoreUtilities.toLowerCase(this.value);
            if (value instanceof Element) {
                object = new Element(this.value);
            }
            else {
                object = value;
            }
        }

        public Argument(dObject obj) {
            object = obj;
            if (obj instanceof Element) {
                fillStr(obj.toString());
            }
            else {
                raw_value = TagManager.cleanOutputFully(obj.toString()); // TODO: Avoid for non-elements
                value = raw_value;
                lower_value = CoreUtilities.toLowerCase(value);
            }
        }

        void fillStr(String string) {
            string = TagManager.cleanOutputFully(string);
            raw_value = string;

            int first_colon = string.indexOf(':');
            int first_space = string.indexOf(' ');

            if ((first_space > -1 && first_space < first_colon) || first_colon == -1) {
                value = string;
                if (object == null) {
                    object = new Element(value);
                }
            }
            else {
                prefix = string.substring(0, first_colon);
                if (prefix.equals("no_prefix")) {
                    prefix = null;
                }
                else {
                    lower_prefix = CoreUtilities.toLowerCase(prefix);
                }
                value = string.substring(first_colon + 1);
                object = new Element(value);
            }
            lower_value = CoreUtilities.toLowerCase(value);
        }

        // Construction
        public Argument(String string) {
            fillStr(string);
        }


        public static Argument valueOf(String string) {
            return new Argument(string);
        }


        public boolean startsWith(String string) {
            return lower_value.startsWith(CoreUtilities.toLowerCase(string));
        }


        public boolean hasPrefix() {
            return prefix != null;
        }


        public Argument getPrefix() {
            if (prefix == null) {
                return null;
            }
            return valueOf(prefix);
        }


        // TODO: REMOVE IN 1.0
        public boolean matches(String values) {
            if (!CoreUtilities.contains(values, ',')) {
                return CoreUtilities.toLowerCase(value).equals(lower_value);
            }
            for (String value : CoreUtilities.split(values, ',')) {
                if (CoreUtilities.toLowerCase(value.replace(" ", "")).equals(lower_value)) {
                    return true;
                }
            }
            return false;
        }

        public boolean matchesOne(String value) {
            return CoreUtilities.toLowerCase(value).equals(lower_value);
        }

        public boolean matches(String... values) {
            for (String value : values) {
                if (CoreUtilities.toLowerCase(value).equals(lower_value)) {
                    return true;
                }
            }
            return false;
        }


        public void replaceValue(String string) {
            value = string;
            lower_value = CoreUtilities.toLowerCase(value);
        }


        public String getValue() {
            return value;
        }

        public dList getList() {
            if (object instanceof dList) {
                return (dList) object;
            }
            return dList.valueOf(value);
        }

        public static HashSet<String> precalcEnum(Enum<?>[] values) {
            HashSet<String> toRet = new HashSet<>(values.length);
            for (int i = 0; i < values.length; i++) {
                toRet.add(values[i].name().toUpperCase().replace("_", ""));
            }
            return toRet;
        }

        public boolean matchesEnum(HashSet<String> values) {
            String upper = value.replace("_", "").toUpperCase();
            return values.contains(upper);
        }

        public boolean matchesEnumList(HashSet<String> values) {
            dList list = getList();
            for (String string : list) {
                String tval = string.replace("_", "").toUpperCase();
                if (values.contains(tval)) {
                    return true;
                }
            }
            return false;
        }

        public boolean matchesEnum(Enum<?>[] values) {
            String upper = value.replace("_", "").toUpperCase();
            for (Enum<?> value : values) {
                if (value.name().replace("_", "").equals(upper)) {
                    return true;
                }
            }
            return false;
        }

        public boolean matchesEnumList(Enum<?>[] values) {
            dList list = getList();
            for (String string : list) {
                String tval = string.replace("_", "").toUpperCase();
                for (Enum<?> value : values) {
                    if (value.name().replace("_", "").equalsIgnoreCase(tval)) {
                        return true;
                    }
                }
            }
            return false;
        }


        // TODO: REMOVE IN 1.0
        public boolean matchesPrefix(String values) {
            if (!hasPrefix()) {
                return false;
            }
            if (!CoreUtilities.contains(values, ',')) {
                return CoreUtilities.toLowerCase(value).equals(lower_prefix);
            }
            for (String value : CoreUtilities.split(values, ',')) {
                if (CoreUtilities.toLowerCase(value.trim()).equals(lower_prefix)) {
                    return true;
                }
            }
            return false;
        }

        public boolean matchesOnePrefix(String value) {
            if (!hasPrefix()) {
                return false;
            }
            return CoreUtilities.toLowerCase(value).equals(lower_prefix);
        }

        public boolean matchesPrefix(String... values) {
            if (!hasPrefix()) {
                return false;
            }
            for (String value : values) {
                if (CoreUtilities.toLowerCase(value).equals(lower_prefix)) {
                    return true;
                }
            }
            return false;
        }


        public boolean matchesPrimitive(PrimitiveType argumentType) {
            if (value == null) {
                return false;
            }

            switch (argumentType) {
                case Word:
                    return wordPrimitive.matcher(value).matches();

                case Integer:
                    return doublePrimitive.matcher(value).matches();

                case Double:
                    return doublePrimitive.matcher(value).matches();

                case Float:
                    return floatPrimitive.matcher(value).matches();

                case Boolean:
                    return booleanPrimitive.matcher(value).matches();

                case Percentage:
                    return percentagePrimitive.matcher(value).matches();

                case String:
                    return true;
            }

            return false;
        }


        // Check if this argument matches a certain dObject type
        public boolean matchesArgumentType(Class<? extends dObject> dClass) {
            return CoreUtilities.canPossiblyBeType(object, dClass);
        }


        // Check if this argument matches any of multiple dObject types
        public boolean matchesArgumentTypes(Class<? extends dObject>... dClasses) {

            for (Class<? extends dObject> c : dClasses) {
                if (matchesArgumentType(c)) {
                    return true;
                }
            }

            return false;
        }


        // Check if this argument matches a dList of a certain dObject
        public boolean matchesArgumentList(Class<? extends dObject> dClass) {

            dList list = getList();

            return list.isEmpty() || list.containsObjectsFrom(dClass);
        }


        public Element asElement() {
            if (object instanceof Element) {
                return (Element) object;
            }
            return new Element(prefix, value);
        }


        public <T extends dObject> T asType(Class<T> clazz) {
            T arg = CoreUtilities.asType(object, clazz, DenizenCore.getImplementation().getTagContext(scriptEntry));
            if (arg != null) {
                arg.setPrefix(prefix);
                return arg;
            }
            return null;
        }


        public void reportUnhandled() {
            dB.echoError('\'' + raw_value + "' is an unknown argument!");
        }


        @Override
        public String toString() {
            return raw_value;
        }
    }

    /////////////////
    // Static Methods
    ///////////////

    public static List<Argument> interpretObjects(List<dObject> args) {
        List<Argument> arg_list = new ArrayList<>(args.size());
        for (dObject obj : args) {
            arg_list.add(new Argument(obj));
        }
        return arg_list;
    }

    public static List<String> specialInterpretTrickStrings = null;

    public static List<Argument> specialInterpretTrickObjects = null;

    public static List<Argument> interpretArguments(List<Argument> args) {
        for (Argument arg : args) {
            if (arg.needsFill || arg.hasSpecialPrefix) {
                if (arg.object instanceof Element && arg.prefix == null) {
                    arg.fillStr(arg.object.toString());
                }
                else {
                    arg.value = arg.object.toString();
                    arg.lower_value = CoreUtilities.toLowerCase(arg.value);
                    arg.raw_value = arg.generateRaw();
                }
            }
        }
        return args;
    }

    /**
     * Turns a list of string arguments (separated by buildArgs) into Argument
     * Objects for easy matching and dObject creation throughout Denizen.
     *
     * @param args a list of string arguments
     * @return a list of Arguments
     */
    public static List<Argument> interpret(List<String> args) {
        if (args == specialInterpretTrickStrings) {
            return interpretArguments(specialInterpretTrickObjects);
        }
        List<Argument> arg_list = new ArrayList<>(args.size());
        for (String string : args) {
            arg_list.add(new Argument(string));
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
        ArrayList<String> matchList = new ArrayList<>();
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

        if (dB.showScriptBuilder) {
            dB.log("Constructed args: " + Arrays.toString(matchList.toArray()));
        }

        return matchList.toArray(new String[matchList.size()]);
    }

    /**
     * To be used with the dBuggers' .report to provide debug output for
     * objects that don't extend dObject.
     *
     * @param prefix name/type/simple description of the object being reported
     * @param value  object being reported will report the value of toString()
     * @return color coded debug report
     */
    public static String debugObj(String prefix, Object value) {
        return "<G>" + prefix + "='<Y>" + (value != null ? value.toString() : "null") + "<G>'  ";
    }

    public static <T extends dObject> String debugList(String prefix, Collection<T> objects) {
        if (objects == null) {
            return debugObj(prefix, null);
        }
        StringBuilder sb = new StringBuilder();
        for (dObject obj : objects) {
            String output = obj.debug();
            sb.append(output.substring((obj.getPrefix() + "='<A>").length(), output.length() - 6)).append(", ");
        }
        if (sb.length() == 0) {
            return debugObj(prefix, sb);
        }
        else {
            return debugObj(prefix, "[" + sb.substring(0, sb.length() - 2) + "]");
        }
    }

    /**
     * To be used with the dBuggers' .report to provide debug output for
     * objects that may have some kind of id or type also associated with
     * the object.
     *
     * @param prefix name/type/simple description of the object being reported
     * @param id     additional id/type of the object
     * @param value  object being reported will report the value of toString()
     * @return color coded debug report
     */
    public static String debugUniqueObj(String prefix, String id, Object value) {
        return "<G>" + prefix + "='<A>" + id + "<Y>(" + (value != null ? value.toString() : "null") + ")<G>'  ";
    }


    public enum ArgumentType {
        LivingEntity, Item, Boolean, Custom, Double, Float,
        Integer, String, Word, Location, Script, Duration
    }


    /**
     * <p>Used to determine if a argument string matches a non-valued custom argument.
     * If a dScript valued argument (such as TARGET:NAME) is passed, this method
     * will always return false. Also supports multiple argument names, separated by a
     * comma (,) character. This method will trim() each name specified.</p>
     * <p/>
     * <b>Example use of '<tt>aH.matchesArg("NOW, LATER", arg)</tt>':</b>
     * <ol>
     * <tt>arg = "NOW"</tt> will return true.<br>
     * <tt>arg = "NEVER"</tt> will return false.<br>
     * <tt>arg = "LATER:8PM"</tt> will return false.<br>
     * <tt>arg = "LATER"</tt> will return true.
     * </ol>
     *
     * @param names      the valid argument names to match
     * @param string_arg the dScript argument string
     * @return true if matched, false if not
     */
    public static boolean matchesArg(String names, String string_arg) {
        String[] parts = names.split(",");
        if (parts.length == 1) {
            if (string_arg.toUpperCase().equals(names.toUpperCase())) {
                return true;
            }
        }
        else {
            for (String string : parts) {
                if (string_arg.split(":")[0].equalsIgnoreCase(string.trim())) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * <p>Used to match a custom argument with a value. In practice, the standard
     * arguments should be used whenever possible to keep things consistent across
     * the entire 'dScript experience'. Should you need to use custom arguments,
     * however, this method provides some support. After all, while using standard
     * arguments is nice, you should never reach. Arguments should make as much
     * sense to the user/script writer as possible.</p>
     * <p/>
     * <b>Small code example:</b>
     * <ol>
     * <tt>0 if (aH.matchesValueArg("HARDNESS", arg, ArgumentType.Word))</tt><br>
     * <tt>1     try { </tt><br>
     * <tt>2        hardness = Hardness.valueOf(aH.getStringFrom(arg).toUpperCase());</tt><br>
     * <tt>3     } catch (Exception e) { </tt><br>
     * <tt>4        dB.echoError("Invalid HARDNESS!") </tt><br>
     * <tt>5 }</tt><br>
     * </ol>
     * <p/>
     * <p>Note: Like {@link #matchesArg(String, String)}, matchesValueArg(String)
     * supports multiple argument names, separated by a comma (,) character. This method
     * will trim() each name specified.</p>
     * <p/>
     * <p>Also requires a specified ArgumentType, which will filter the type of value
     * to match to. If anything should be excepted as the value, or you plan
     * on parsing the value yourself, use ArgumentType.Custom, otherwise use an
     * an appropriate ArgumentType. See: {@link ArgumentType}.</p>
     * <p/>
     * <b>Example use of '<tt>aH.matchesValueArg("TIME", arg, ArgumentType.Integer)</tt>':</b>
     * <ol>
     * <tt>arg = "TIME:60"</tt> will return true.<br>
     * <tt>arg = "90"</tt> will return false.<br>
     * <tt>arg = "TIME:8 o'clock"</tt> will return false.<br>
     * <tt>arg = "TIME:0"</tt> will return true.
     * </ol>
     *
     * @param names      the desired name variations of the argument
     * @param string_arg the dScript argument string
     * @param type       a valid ArgumentType, used for matching values
     * @return true if matched, false otherwise
     */
    @Deprecated
    public static boolean matchesValueArg(String names, String string_arg, ArgumentType type) {
        if (string_arg == null) {
            return false;
        }

        int firstColonIndex = string_arg.indexOf(':');
        if (firstColonIndex == -1) {
            return false;
        }

        String[] commaParts = names.split(",");

        if (commaParts.length == 1) {
            if (!string_arg.substring(0, firstColonIndex).equalsIgnoreCase(names)) {
                return false;
            }
        }

        else {
            boolean matched = false;
            for (String string : commaParts) {
                if (string_arg.substring(0, firstColonIndex).equalsIgnoreCase(string.trim())) {
                    matched = true;
                }
            }
            if (!matched) {
                return false;
            }
        }

        string_arg = string_arg.split(":", 2)[1];

        switch (type) {
            case Word:
                return wordPrimitive.matcher(string_arg).matches();

            case Integer:
                return doublePrimitive.matcher(string_arg).matches();

            case Double:
                return doublePrimitive.matcher(string_arg).matches();

            case Float:
                return floatPrimitive.matcher(string_arg).matches();

            case Boolean:
                return booleanPrimitive.matcher(string_arg).matches();

            case Script:
                // return dScript.matches(string_arg);
                return true;

            /* TODO: MOVE OUT OF CORE:
             case Location:
             return dLocation.matches(string_arg);

             case Item:
             return dItem.matches(string_arg);

             case LivingEntity:
             return dEntity.matches(string_arg);

             case Duration:
             return Duration.matches(string_arg);
             */
            case String:
                return true;

            case Custom:
                return true;

            default:
                dB.echoError("Invalid or temporarily unavailable matches value!");

        }

        dB.echoError("While parsing '" + string_arg + "', Denizen has run into a problem. While the " +
                "prefix is correct, the value is not valid. Check documentation for valid value." +
                "Perhaps a replaceable tag has failed to fill in a value?");

        return false;
    }

    public static boolean getBooleanFrom(String arg) {
        return Boolean.valueOf(getStringFrom(arg));
    }

    public static double getDoubleFrom(String arg) {
        try {
            return Double.valueOf(getStringFrom(arg));
        }
        catch (NumberFormatException e) {
            return 0D;
        }
    }

    public static float getFloatFrom(String arg) {
        try {
            return Float.valueOf(getStringFrom(arg));
        }
        catch (NumberFormatException e) {
            return 0f;
        }
    }

    public static int getIntegerFrom(String arg) {
        try {
            return Double.valueOf(getStringFrom(arg)).intValue();
        }
        catch (NumberFormatException e) {
            return 0;
        }
    }

    @Deprecated
    public static dList getListFrom(String arg) {
        return dList.valueOf(aH.getStringFrom(arg));
    }

    public static long getLongFrom(String arg) {
        try {
            return Long.valueOf(arg);
        }
        catch (NumberFormatException ex) {
            try {
                return Double.valueOf(getStringFrom(arg)).longValue();
            }
            catch (NumberFormatException e) {
                return 0;
            }
        }
    }

    @Deprecated
    public static dScript getScriptFrom(String arg) {
        arg = CoreUtilities.toLowerCase(arg).replace("script:", "");
        return dScript.valueOf(arg);
    }

    public static String getStringFrom(String arg) {
        String[] parts = arg.split(":", 2);
        return parts.length >= 2 ? parts[1] : arg;
    }

    @Deprecated
    public static Duration getDurationFrom(String arg) {
        arg = CoreUtilities.toLowerCase(arg).replace("duration:", "").replace("delay:", "");
        return Duration.valueOf(arg);
    }

    public static boolean matchesDouble(String arg) {
        return doublePrimitive.matcher(arg).matches();
    }

    @Deprecated
    public static boolean matchesDuration(String arg) {
        arg = CoreUtilities.toLowerCase(arg).replace("duration:", "").replace("delay:", "");
        return Duration.matches(arg);
    }

    public static boolean matchesInteger(String arg) {
        return doublePrimitive.matcher(arg).matches();
    }

    @Deprecated
    public static boolean matchesItem(String arg) {
        if (arg.length() > 5 && arg.toUpperCase().startsWith("ITEM:")) {
            return true;
        }
        return false;
    }

    @Deprecated
    public static boolean matchesContext(String arg) {
        if (arg.toUpperCase().startsWith("CONTEXT:") ||
                arg.toUpperCase().startsWith("DEFINE:")) {
            return true;
        }
        return false;
    }

    @Deprecated
    public static Map<String, String> getContextFrom(String arg) {
        Map<String, String> context = new HashMap<>();
        int x = 1;
        for (String ctxt : aH.getListFrom(arg)) {
            context.put(String.valueOf(x), ctxt.trim());
            x++;
        }
        return context;
    }

    @Deprecated
    public static boolean matchesLocation(String arg) {
        return arg.toUpperCase().startsWith("LOCATION:");
    }

    @Deprecated
    public static boolean matchesQuantity(String arg) {
        return arg.toUpperCase().startsWith("QTY:");
    }

    @Deprecated
    public static boolean matchesQueue(String arg) {
        return arg.toUpperCase().startsWith("QUEUE:");
    }

    @Deprecated
    public static boolean matchesScript(String arg) {
        Matcher m = matchesScriptPtrn.matcher(arg);
        if (m.matches()) {
            if (ScriptRegistry.containsScript(m.group(1))) {
                return true;
            }
            else {
                dB.echoError("While parsing '" + arg + "', Denizen has run into a problem. This " +
                        "argument's format is correct, but Denizen couldn't locate a script " +
                        "named '" + m.group(1) + "'. Is it spelled correctly?");
            }
        }
        return false;
    }

    @Deprecated
    public static boolean matchesState(String arg) {
        final Pattern m = Pattern.compile("(state|toggle):(true|false|toggle)");
        if (m.matcher(arg).matches()) {
            return true;
        }
        else if (arg.toUpperCase().startsWith("(state|toggle):")) {
            dB.echoError("While parsing '" + arg + "', Denizen has run into a problem. While the prefix is " +
                    "correct, the value is not valid. 'STATE' requires a value of TRUE, FALSE, or TOGGLE. ");
        }

        return false;
    }

    final static Pattern matchesScriptPtrn = Pattern.compile("script:(.+)", Pattern.CASE_INSENSITIVE);
}
