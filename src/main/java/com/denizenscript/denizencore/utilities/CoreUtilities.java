package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptBuilder;
import com.denizenscript.denizencore.scripts.ScriptHelper;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.function.Function;

public class CoreUtilities {

    /**
     * No debug, no errors.
     */
    public static TagContext noDebugContext;
    /**
     * Debug and errors shown.
     */
    public static TagContext basicContext;
    /**
     * No debug, yes errors.
     */
    public static TagContext errorButNoDebugContext;

    public static DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols(Locale.US);

    public static final char NBSP_Char = (char) 0x00A0;

    public static final String NBSP = String.valueOf(NBSP_Char);

    public static String clearNBSPs(String input) {
        return input.replace(NBSP_Char, ' ');
    }

    public static ObjectTag objectToTagForm(Object obj, TagContext context) {
        return objectToTagForm(obj, context, false);
    }

    public static ObjectTag objectToTagForm(Object obj, TagContext context, boolean scriptStrip) {
        return objectToTagForm(obj, context, scriptStrip, false);
    }

    public static List<Function<Object, ObjectTag>> objectConversions = new ArrayList<>();

    public static ObjectTag objectToTagForm(Object obj, TagContext context, boolean scriptStrip, boolean doParse) {
        return objectToTagForm(obj, context, scriptStrip, doParse, true);
    }

    public static ObjectTag objectToTagForm(Object obj, TagContext context, boolean scriptStrip, boolean doParse, boolean canPick) {
        if (obj == null) {
            return new ElementTag("null");
        }
        if (obj instanceof YamlConfiguration) {
            obj = ((YamlConfiguration) obj).contents;
        }
        if (obj instanceof ObjectTag) {
            return (ObjectTag) obj;
        }
        else if (obj instanceof Map) {
            MapTag result = new MapTag();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) obj).entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (scriptStrip) {
                    key = ScriptBuilder.stripLinePrefix(key);
                }
                result.putObject(key, objectToTagForm(entry.getValue(), context, scriptStrip, doParse, canPick));
            }
            return result;
        }
        if (obj instanceof Iterable) {
            ListTag listResult = new ListTag();
            for (Object subObj : (Iterable) obj) {
                listResult.addObject(objectToTagForm(subObj, context, scriptStrip, doParse, canPick));
            }
            return listResult;
        }
        else {
            for (Function<Object, ObjectTag> func : objectConversions) {
                ObjectTag result = func.apply(obj);
                if (result != null) {
                    return result.duplicate();
                }
            }
            String result = obj.toString();
            if (scriptStrip) {
                result = ScriptBuilder.stripLinePrefix(result);
            }
            if (doParse) {
                return TagManager.tagObject(result, context);
            }
            if (canPick) {
                return ObjectFetcher.pickObjectFor(result, context);
            }
            else {
                return new ElementTag(result, true);
            }
        }
    }


    public static Object objectTagToJavaForm(ObjectTag obj, boolean stringHolder, boolean nativeTypes) {
        if (obj == null) {
            return null;
        }
        else if (obj instanceof ListTag) {
            List<Object> output = new ArrayList<>(((ListTag) obj).size());
            for (ObjectTag entry : ((ListTag) obj).objectForms) {
                output.add(objectTagToJavaForm(entry, stringHolder, nativeTypes));
            }
            return output;
        }
        else if (obj instanceof MapTag) {
            Map<Object, Object> output = new LinkedHashMap<>();
            for (Map.Entry<StringHolder, ObjectTag> entry : ((MapTag) obj).map.entrySet()) {
                output.put(stringHolder ? entry.getKey() : entry.getKey().str, objectTagToJavaForm(entry.getValue(), stringHolder, nativeTypes));
            }
            return output;
        }
        else {
            String raw = obj.toString();
            if (nativeTypes) {
                if (raw.equals("true")) {
                    return Boolean.TRUE;
                }
                else if (raw.equals("false")) {
                    return Boolean.FALSE;
                }
                else if (ArgumentHelper.matchesDouble(raw)) {
                    try {
                        if (ArgumentHelper.matchesInteger(raw)) {
                            return Long.parseLong(raw);
                        }
                        return Double.parseDouble(raw);
                    }
                    catch (NumberFormatException ex) { /* IGNORE */ }
                }
            }
            return raw;
        }
    }

    public static String splitLinesByCharacterCount(String str, int length) {
        if (length < 3) {
            return str;
        }
        StringBuilder output = new StringBuilder(str.length() * 2);
        int curLineLen = 0;
        int lineStart = 0;
        mainloop:
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\n') {
                output.append(str, lineStart, i + 1);
                curLineLen = 0;
                lineStart = i + 1;
                continue;
            }
            curLineLen++;
            if (curLineLen > length) {
                for (int x = i - 1; x > lineStart; x--) {
                    char xc = str.charAt(x);
                    if (xc == ' ') {
                        output.append(str, lineStart, x).append("\n");
                        curLineLen = 0;
                        lineStart = x + 1;
                        i = x;
                        continue mainloop;
                    }
                }
                output.append(str, lineStart, i).append("\n");
                curLineLen = 0;
                lineStart = i;
            }
        }
        output.append(str, lineStart, str.length());
        return output.toString();
    }

    public static void fixNewLinesToListSeparation(List<String> list) {
        for (int i = 0; i < list.size(); i++) {
            String line = list.get(i);
            if (line.contains("\n")) {
                List<String> split = split(line, '\n');
                list.set(i, split.get(0));
                for (int x = 1; x < split.size(); x++) {
                    list.add(i + x, split.get(x));
                }
            }
        }
    }

    public static String replace(String original, String findMe, String swapMeIn) {
        int firstIndex = original.indexOf(findMe);
        if (firstIndex < 0) {
            return original;
        }
        int lastIndex = original.lastIndexOf(findMe);
        if (firstIndex == lastIndex) {
            return original.substring(0, firstIndex) + swapMeIn + original.substring((lastIndex + findMe.length()));
        }
        StringBuilder output = new StringBuilder(original.length() * 2);
        int prevIndex = 0;
        while (firstIndex != -1) {
            output.append(original, prevIndex, firstIndex).append(swapMeIn);
            prevIndex = firstIndex + findMe.length();
            firstIndex = original.indexOf(findMe, prevIndex);
        }
        output.append(original, prevIndex, original.length());
        return output.toString();
    }

    public static String join(String delim, List objects) {
        StringBuilder output = new StringBuilder(objects.size() * 5);
        for (int i = 0; i < objects.size(); i++) {
            output.append(objects.get(i));
            if (i + 1 < objects.size()) {
                output.append(delim);
            }
        }
        return output.toString();
    }

    public static String stringifyNullPass(Object obj) {
        return obj == null ? null : obj.toString();
    }

    public static ObjectTag fixType(ObjectTag input, TagContext context) {
        if (input instanceof ElementTag && !((ElementTag) input).isPlainText) {
            return ObjectFetcher.pickObjectFor(input.toString(), context);
        }
        return input.refreshState();
    }

    public static void autoPropertyMechanism(ObjectTag object, Mechanism mechanism) {
        if (mechanism.fulfilled()) {
            return;
        }
        PropertyParser.ClassPropertiesInfo properties = PropertyParser.propertiesByClass.get(object.getClass());
        if (properties == null) {
            return;
        }
        PropertyParser.PropertyGetter specificGetter = properties.propertiesByMechanism.get(mechanism.getName());
        if (specificGetter != null) {
            Property prop = specificGetter.get(object);
            if (prop == null) {
                mechanism.echoError("Cannot apply property mechanism - object does not fit property requirements?");
                return;
            }
            prop.adjust(mechanism);
            return;
        }
        for (PropertyParser.PropertyGetter listGetter : properties.propertiesAnyMechs) {
            Property prop = listGetter.get(object);
            if (prop != null) {
                prop.adjust(mechanism);
                if (mechanism.fulfilled()) {
                    return;
                }
            }
        }
    }

    public static ObjectTag autoPropertyTagObject(ObjectTag object, Attribute attribute) {
        if (attribute.isComplete()) {
            return null;
        }
        PropertyParser.ClassPropertiesInfo properties = PropertyParser.propertiesByClass.get(object.getClass());
        if (properties == null) {
            return null;
        }
        String tagName = attribute.getAttributeWithoutParam(1);
        PropertyParser.PropertyGetter specificGetter = properties.propertiesByTag.get(tagName);
        if (specificGetter != null) {
            Property prop = specificGetter.get(object);
            if (prop == null) {
                String propName = properties.propertyNamesByTag.get(tagName);
                attribute.seemingSuccesses.add(attribute.getAttributeWithoutParam(1) + " - property " + propName + " matched, but is not valid for the object.");
                return null;
            }
            return prop.getObjectAttribute(attribute);
        }
        for (PropertyParser.PropertyGetter listGetter : properties.propertiesAnyTags) {
            Property prop = listGetter.get(object);
            if (prop != null) {
                ObjectTag returned = prop.getObjectAttribute(attribute);
                if (returned != null) {
                    return returned;
                }
            }
        }
        return null;
    }

    public static ObjectTag autoAttribTyped(ObjectTag inp, Attribute attribute) {
        return autoAttrib(fixType(inp, attribute.context), attribute);
    }

    public static ObjectTag autoAttrib(ObjectTag inp, Attribute attribute) {
        if (inp == null) {
            Debug.echoError("Tag parse failed (null return) for tag <LG><" + attribute.toString() + "<LG>><W>!");
            return null;
        }
        if (attribute.isComplete()) {
            return inp;
        }
        return inp.getObjectAttribute(attribute);
    }

    public static <T extends ObjectTag> T asType(ObjectTag inp, Class<T> type, TagContext context) {
        if (inp.getClass() == type) {
            return (T) inp;
        }
        TagTypeConverter converter = typeConverters.get(type);
        if (converter != null) {
            return (T) converter.convert(inp, context);
        }
        return ObjectFetcher.getObjectFrom(type, inp.toString(), context);
    }

    @FunctionalInterface
    public interface TypeComparisonRunnable {
        boolean doesCompare(ObjectTag inp);
    }

    @FunctionalInterface
    public interface TagTypeConverter {
        ObjectTag convert(ObjectTag inp, TagContext context);
    }

    public static Map<Class<? extends ObjectTag>, TypeComparisonRunnable> typeCheckers = new HashMap<>();

    public static Map<Class<? extends ObjectTag>, TypeComparisonRunnable> typeShouldBeCheckers = new HashMap<>();

    public static Map<Class<? extends ObjectTag>, TagTypeConverter> typeConverters = new HashMap<>();

    static {
        registerTypeAsTrueAlways(ElementTag.class);
        registerTypeAsTrueAlways(ListTag.class);
        registerTypeAsNoOtherTypeCode(CustomObjectTag.class, "custom");
        registerTypeAsNoOtherTypeCode(DurationTag.class, "d");
        registerTypeAsNoOtherTypeCode(BinaryTag.class, "binary");
        registerTypeAsNoOtherTypeCode(MapTag.class, "map");
        registerTypeAsNoOtherTypeCode(QueueTag.class, "q");
        registerTypeAsNoOtherTypeCode(ScriptTag.class, "s");
        registerTypeAsNoOtherTypeCode(TimeTag.class, "d");
        typeConverters.put(ObjectTag.class, (obj, c) -> obj);
        typeConverters.put(ElementTag.class, (obj, c) -> obj.asElement());
        typeConverters.put(ListTag.class, ListTag::getListFor);
        typeConverters.put(MapTag.class, MapTag::getMapFor);
        typeCheckers.put(MapTag.class, (inp) -> {
            if (inp == null) {
                return false;
            }
            if (inp instanceof MapTag) {
                return true;
            }
            if (!(inp instanceof ElementTag)) {
                return false;
            }
            String simple = inp.toString();
            if (simple.startsWith("map@")) {
                return true;
            }
            if (simple.startsWith("[") && simple.endsWith("]") && simple.contains("=")) {
                return true;
            }
            return false;
        });
    }

    public static void registerTypeAsNoOtherTypeCode(Class<? extends ObjectTag> type, final String knownCode) {
        typeCheckers.put(type, (inp) -> {
            if (inp == null) {
                return false;
            }
            Class<? extends ObjectTag> inpType = inp.getClass();
            if (inpType == type) {
                return true;
            }
            if (inpType == ElementTag.class) {
                String simple = inp.toString();
                int atIndex = simple.indexOf('@');
                if (atIndex != -1) {
                    String code = simple.substring(0, atIndex);
                    if (!code.equals(knownCode) && !code.equals("el")) {
                        if (ObjectFetcher.objectsByPrefix.containsKey(code)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        });
    }

    public static void registerTypeAsTrueAlways(Class<? extends ObjectTag> type) {
        typeCheckers.put(type, (inp) -> true);
    }

    public static boolean shouldBeType(ObjectTag inp, Class<? extends ObjectTag> type) {
        if (type == ElementTag.class || type == ObjectTag.class) {
            return true;
        }
        if (inp.getClass() == type) {
            return true;
        }
        TypeComparisonRunnable comp = typeShouldBeCheckers.get(type);
        if (comp != null) {
            return comp.doesCompare(inp);
        }
        if (!(inp instanceof ElementTag)) {
            return false;
        }
        if (((ElementTag) inp).isPlainText || ((ElementTag) inp).isRawInput) {
            return false;
        }
        String raw = inp.toString();
        int atSign = raw.indexOf('@');
        if (atSign == -1) {
            return false;
        }
        ObjectFetcher.ObjectType<?> typeData = ObjectFetcher.objectsByClass.get(type);
        return typeData.prefix.equals(raw.substring(0, atSign));
    }

    public static boolean canPossiblyBeType(ObjectTag inp, Class<? extends ObjectTag> type) {
        if (type == ObjectTag.class) {
            return true;
        }
        if (inp.getClass() == type) {
            return true;
        }
        TypeComparisonRunnable comp = typeCheckers.get(type);
        if (comp != null && !comp.doesCompare(inp)) {
            return false;
        }
        return ObjectFetcher.checkMatch(type, inp.toString());
    }

    public static void deleteDirectory(File directory) throws IOException {
        Files.walkFileTree(directory.toPath(),
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    public static void copyDirectory(File source, File destination, HashSet<String> excludeExtensions) throws IOException {
        copyDirectory(source.toPath(), destination.toPath(), excludeExtensions);
    }

    public static void copyDirectory(Path source, Path destination, HashSet<String> excludeExtensions) throws IOException {
        Files.walk(source).forEach(file -> {
            try {
                if (excludeExtensions != null) {
                    String name = file.getFileName().toString();
                    int dot = name.indexOf('.');
                    if (dot >= 0) {
                        String ext = toLowerCase(name.substring(dot + 1));
                        if (excludeExtensions.contains(ext)) {
                            return;
                        }
                    }
                }
                Path destPath = destination.resolve(source.relativize(file));
                destPath.getParent().toFile().mkdirs();
                Files.copy(file, destPath);
            }
            catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    static Random random = new Random();

    public static Random getRandom() {
        return random;
    }

    public static String bigDecToString(BigDecimal input) {
        String temp = input.toString();
        if (contains(temp, '.')) {
            for (int i = temp.length() - 1; i >= 0; i--) {
                if (temp.charAt(i) != '0') {
                    if (temp.charAt(i) == '.') {
                        return temp.substring(0, i);
                    }
                    return temp.substring(0, i + 1);
                }
            }
        }
        if (temp.startsWith("0E")) {
            return "0";
        }
        return temp;
    }

    public static DecimalFormat df = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
    public static DecimalFormat floatFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

    static {
        df.setMaximumFractionDigits(340);
        floatFormat.setMaximumFractionDigits(8);
    }

    public static String doubleToString(float input) {
        if (Float.isNaN(input)) {
            return "NaN";
        }
        else if (Float.isInfinite(input)) {
            if (input < 0) {
                return "-infinity";
            }
            return "infinity";
        }
        return floatFormat.format(input);
    }

    public static String doubleToString(double input) {
        if (Double.isNaN(input)) {
            return "NaN";
        }
        else if (Double.isInfinite(input)) {
            if (input < 0) {
                return "-infinity";
            }
            return "infinity";
        }
        return df.format(input);
    }

    static boolean isScriptFilename(String fileName) {
        if (fileName.startsWith(".")) {
            return false;
        }

        String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
        if (ext.equalsIgnoreCase("DSCRIPT")) {
            Debug.echoError("Script '" + fileName + "' has invalid '.dscript' file extension.");
            Deprecations.dscriptFileExtension.warn();
            return false;
        }
        if (ext.equalsIgnoreCase("YML")) {
            Debug.echoError("Script '" + fileName + "' has legacy '.yml' file extension.");
            Deprecations.ymlFileExtension.warn();
            return true;
        }
        return ext.equalsIgnoreCase("dsc");
    }

    public static List<File> listDScriptFiles(File dir) {
        List<File> files = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (isScriptFilename(file.getName())) {
                files.add(file);
            }
            if (file.isDirectory()) {
                files.addAll(listDScriptFiles(file));
            }
        }
        return files;
    }

    public static boolean contains(String str, char c) {
        return str.indexOf(c) >= 0;
    }

    public static String concat(List<String> str, String split) {
        StringBuilder sb = new StringBuilder();
        if (str.size() > 0) {
            sb.append(str.get(0));
        }
        for (int i = 1; i < str.size(); i++) {
            sb.append(split).append(str.get(i));
        }
        return sb.toString();
    }

    public static List<String> split(String str, char c) {
        List<String> strings = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                strings.add(str.substring(start, i));
                start = i + 1;
            }
        }
        strings.add(str.substring(start));
        return strings;
    }

    public static List<String> split(String str, char c, int max) {
        List<String> strings = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                strings.add(str.substring(start, i));
                start = i + 1;
                if (strings.size() + 1 == max) {
                    break;
                }
            }
        }
        strings.add(str.substring(start));
        if (CoreConfiguration.debugVerbose) {
            Debug.log("Splitting " + str + " around " + c + " limited to " + max + " returns " + concat(strings, ":::"));
        }
        return strings;
    }

    public static boolean equalsIgnoreCase(String input, String compared) {
        int inLength = input.length();
        if (inLength != compared.length()) {
            return false;
        }
        for (int i = 0; i < inLength; i++) {
            char a = input.charAt(i);
            char b = compared.charAt(i);
            if (a >= 'A' && a <= 'Z') {
                a -= 'A' - 'a';
            }
            if (b >= 'A' && b <= 'Z') {
                b -= 'A' - 'a';
            }
            if (a != b) {
                return false;
            }
        }
        return true;
    }

    public static String toLowerCase(String input) {
        int len = input.length();
        boolean any = false;
        char c;
        for (int i = 0; i < len; i++) {
            c = input.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                any = true;
                break;
            }
        }
        if (!any) {
            return input;
        }
        char[] data = input.toCharArray();
        for (int i = 0; i < data.length; i++) {
            if (data[i] >= 'A' && data[i] <= 'Z') {
                data[i] -= 'A' - 'a';
            }
        }
        return new String(data);
    }

    public static String getXthArg(int argc, String args) {
        char[] data = args.toCharArray();
        StringBuilder nArg = new StringBuilder();
        int arg = 0;
        for (int i = 0; i < data.length; i++) {
            if (data[i] == ' ') {
                arg++;
                if (arg > argc) {
                    return nArg.toString();
                }
            }
            else if (arg == argc) {
                nArg.append(data[i]);
            }
        }
        return nArg.toString();
    }

    public static String getClosestOption(List<String> strs, String opt) {
        int minDist = Integer.MAX_VALUE;
        opt = CoreUtilities.toLowerCase(opt);
        String closest = "";
        for (String cmd : strs) {
            String comp = CoreUtilities.toLowerCase(cmd);
            int distance = getLevenshteinDistance(opt, comp);
            if (minDist > distance) {
                minDist = distance;
                closest = cmd;
            }
        }

        return closest;
    }

    public static int indexOfAny(String str, int start, char... chars) {
        int earliest = -1;
        for (char c : chars) {
            int index = str.indexOf(c, start);
            if (index != -1 && (earliest == -1 || index < earliest)) {
                earliest = index;
            }
        }
        return earliest;
    }

    public static int getLevenshteinDistance(String s, String t) {
        if (s == null || t == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        int n = s.length(); // length of s
        int m = t.length(); // length of t

        if (n == 0) {
            return m;
        }
        else if (m == 0) {
            return n;
        }

        int[] p = new int[n + 1]; // 'previous' cost array, horizontally
        int[] d = new int[n + 1]; // cost array, horizontally
        int[] _d; // placeholder to assist in swapping p and d

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        char t_j; // jth character of t

        int cost; // cost

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            t_j = t.charAt(j - 1);
            d[0] = j;

            for (i = 1; i <= n; i++) {
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left
                // and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }

    private static byte[] valueOfHex = new byte[256];

    public static char[] charForByte = "0123456789abcdef".toCharArray();

    static {
        for (byte i = 0; i < 10; i++) {
            valueOfHex['0' + i] = i;
        }
        for (byte i = 0; i < 6; i++) {
            valueOfHex['a' + i] = (byte) (i + 10);
            valueOfHex['A' + i] = (byte) (i + 10);
        }
    }

    public static byte[] hexDecode(String str) {
        byte[] output = new byte[str.length() >> 1];
        for (int i = 0; i < output.length; i++) {
            char a = str.charAt(i << 1);
            char b = str.charAt((i << 1) + 1);
            byte valA = (byte) (valueOfHex[a] << 4);
            byte valB = valueOfHex[b];
            output[i] = (byte) (valA + valB);
        }
        return output;
    }

    public static String hexEncode(byte[] value) {
        char[] output = new char[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            byte valA = (byte) ((value[i] & 0xF0) >> 4);
            byte valB = (byte) (value[i] & 0x0F);
            output[i << 1] = charForByte[valA];
            output[(i << 1) + 1] = charForByte[valB];
        }
        return new String(output);
    }

    public static void journallingFileSave(String filePath, String contents) {
        File saveToFile = new File(filePath + "~1");
        try {
            saveToFile.getParentFile().mkdirs();
            Charset charset = CoreConfiguration.scriptEncoding == null ? null : CoreConfiguration.scriptEncoding.charset();
            FileOutputStream fiout = new FileOutputStream(saveToFile);
            OutputStreamWriter writer;
            if (charset == null) {
                writer = new OutputStreamWriter(fiout);
            }
            else {
                writer = new OutputStreamWriter(fiout, charset);
            }
            writer.write(contents);
            writer.close();
            File bakFile = new File(filePath + "~2");
            File realFile = new File(filePath);
            if (realFile.exists()) {
                realFile.renameTo(bakFile);
            }
            saveToFile.renameTo(realFile);
            if (bakFile.exists()) {
                bakFile.delete();
            }
        }
        catch (Throwable ex) {
            Debug.echoError("Failed to save data to path '" + filePath + "'");
            Debug.echoError(ex);
        }
    }

    public static String journallingLoadFile(String filePath) {
        try {
            File realPath;
            File flagFile = new File(filePath);
            if (flagFile.exists()) {
                realPath = flagFile;
            }
            else {
                File bakFile = new File(filePath + "~2");
                if (bakFile.exists()) {
                    realPath = bakFile;
                }
                // Note: ~1 are likely corrupted, so ignore them.
                else {
                    return null;
                }
            }
            FileInputStream fis = new FileInputStream(realPath);
            String str = ScriptHelper.convertStreamToString(fis);
            fis.close();
            return str;
        }
        catch (Throwable ex) {
            Debug.echoError("Failed to load data for path '" + filePath + "'");
            Debug.echoError(ex);
            return null;
        }
    }

    public static Collection<ObjectTag> objectToList(ObjectTag list, TagContext context) {
        if (list instanceof MapTag) {
            for (StringHolder key : ((MapTag) list).map.keySet()) {
                if (!ArgumentHelper.matchesInteger(key.str)) {
                    return Collections.singletonList(list);
                }
            }
            return ((MapTag) list).map.values();
        }
        else if (list instanceof ListTag) {
            return ((ListTag) list).objectForms;
        }
        else {
            String raw = list.toString();
            if (raw.startsWith("map@") || raw.startsWith("[")) {
                MapTag map = MapTag.valueOf(raw, context);
                if (map != null) {
                    for (StringHolder key : map.map.keySet()) {
                        if (!ArgumentHelper.matchesInteger(key.str)) {
                            return Collections.singletonList(list);
                        }
                    }
                    return map.map.values();
                }
            }
            return ListTag.valueOf(raw, context).objectForms;
        }
    }

    public static long monotonicMillis() {
        return System.nanoTime() / 1000000;
    }

    public static long monotonicMillisToReal(long monotonic) {
        return System.currentTimeMillis() + (monotonic - monotonicMillis());
    }

    public static String hash_md5(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(bytes, 0, bytes.length);
            return new BigInteger(1, md.digest()).toString(16);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
        return null;
    }
}
