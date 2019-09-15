package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.tags.TagContext;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjectFetcher {

    @FunctionalInterface
    public interface MatchesInterface {

        boolean matches(String str);
    }

    public interface ValueOfInterface {

        ObjectTag valueOf(String str, TagContext context);
    }

    // Keep track of each Class keyed by its 'object identifier' --> i@, e@, etc.
    private static Map<String, Class> objects = new HashMap<>();

    // Keep track of the static 'matches' and 'valueOf' methods for each ObjectTag
    static Map<Class, MatchesInterface> matches = new HashMap<>();
    static Map<Class, ValueOfInterface> valueof = new HashMap<>();

    public static void _initialize() throws IOException, ClassNotFoundException {

        if (fetchable_objects.isEmpty()) {
            return;
        }

        Map<String, Class> adding = new HashMap<>();
        for (Class dClass : fetchable_objects) {
            try {
                Method method = dClass.getMethod("valueOf", String.class, TagContext.class);
                if (method.isAnnotationPresent(Fetchable.class)) {
                    String[] identifiers = method.getAnnotation(Fetchable.class).value().split(",");
                    for (String identifier : identifiers) {
                        adding.put(CoreUtilities.toLowerCase(identifier.trim()), dClass);
                        Debug.log("Registered: " + dClass.getSimpleName() + " as " + identifier);
                    }
                }
            }
            catch (Throwable e) {
                Debug.echoError("Failed to initialize an object type(" + dClass.getSimpleName() + "): ");
                Debug.echoError(e);
            }
        }

        objects.putAll(adding);
        Debug.echoApproval("Added objects to the ObjectFetcher " + adding.keySet().toString());
        fetchable_objects.clear();
    }

    public static void _registerCoreObjects() throws NoSuchMethodException, ClassNotFoundException, IOException {

        // Initialize the ObjectFetcher
        registerWithObjectFetcher(CustomObjectTag.class); // custom@
        registerWithObjectFetcher(ListTag.class);        // li@
        ListTag.registerTags(); // TODO: Automate this once all classes have tag registries
        registerWithObjectFetcher(ScriptTag.class);      // s@
        ScriptTag.registerTags(); // TODO: Automate this once all classes have tag registries
        registerWithObjectFetcher(ElementTag.class);      // el@
        ElementTag.registerTags(); // TODO: Automate this once all classes have tag registries
        registerWithObjectFetcher(DurationTag.class);     // d@
        DurationTag.registerTags(); // TODO: Automate this once all classes have tag registries
        registerWithObjectFetcher(QueueTag.class);  // q@
        QueueTag.registerTags(); // TODO: Automate this once all classes have tag registries
        _initialize();

    }

    private static ArrayList<Class> fetchable_objects = new ArrayList<>();

    public static MatchesInterface getMatchesFor(Class clazz) {

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            CallSite site = LambdaMetafactory.metafactory(lookup, "matches", // MatchesInterface#matches
                    MethodType.methodType(MatchesInterface.class), // Signature of invoke method
                    MethodType.methodType(Boolean.class, String.class).unwrap(), // signature of MatchesInterface#matches
                    lookup.findStatic(clazz, "matches", MethodType.methodType(Boolean.class, String.class).unwrap()), // signature of original matches method
                    MethodType.methodType(Boolean.class, String.class).unwrap()); // Signature of original matches again
            return (MatchesInterface) site.getTarget().invoke();
        }
        catch (Throwable ex) {
            System.err.println("Failed to get matches for " + clazz.getCanonicalName());
            ex.printStackTrace();
            Debug.echoError(ex);
            return null;
        }
    }

    public static ValueOfInterface getValueOfFor(Class clazz) {

        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            CallSite site = LambdaMetafactory.metafactory(lookup, "valueOf", // ValueOfInterface#valueOf
                    MethodType.methodType(ValueOfInterface.class), // Signature of invoke method
                    MethodType.methodType(ObjectTag.class, String.class, TagContext.class), // signature of ValueOfInterface#valueOf
                    lookup.findStatic(clazz, "valueOf", MethodType.methodType(clazz, String.class, TagContext.class)), // signature of original valueOf method
                    MethodType.methodType(clazz, String.class, TagContext.class)); // Signature of original valueOf again
            return (ValueOfInterface) site.getTarget().invoke();
        }
        catch (Throwable ex) {
            System.err.println("Failed to get valueOf for " + clazz.getCanonicalName());
            ex.printStackTrace();
            Debug.echoError(ex);
            return null;
        }
    }

    public static void registerWithObjectFetcher(Class<? extends ObjectTag> objectTag) {
        try {
            fetchable_objects.add(objectTag);
            matches.put(objectTag, getMatchesFor(objectTag));
            valueof.put(objectTag, getValueOfFor(objectTag));
        }
        catch (Throwable e) {
            Debug.echoError("Failed to register an object type (" + objectTag.getSimpleName() + "): ");
            Debug.echoError(e);
        }
    }

    public static boolean canFetch(String id) {
        return objects.containsKey(CoreUtilities.toLowerCase(id));
    }

    public static Class getObjectClass(String id) {
        if (canFetch(id)) {
            return objects.get(CoreUtilities.toLowerCase(id));
        }
        else {
            return null;
        }
    }

    final static Pattern PROPERTIES_PATTERN = Pattern.compile("([^\\[]+)\\[(.+=.+)\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    public final static Pattern DESCRIBED_PATTERN =
            Pattern.compile("[^\\[]+\\[.+=.+\\]", Pattern.DOTALL | Pattern.MULTILINE);

    public static boolean checkMatch(Class<? extends ObjectTag> dClass, String value) {
        if (value == null || dClass == null) {
            return false;
        }
        Matcher m = PROPERTIES_PATTERN.matcher(value);
        try {
            return matches.get(dClass).matches(m.matches() ? m.group(1) : value);
        }
        catch (Exception e) {
            Debug.echoError(e);
        }

        return false;

    }

    @Deprecated
    public static <T extends ObjectTag> T getObjectFrom(Class<T> dClass, String value) {
        return getObjectFrom(dClass, value, DenizenCore.getImplementation().getTagContext(null));
    }

    public static List<String> separateProperties(String input) {
        if (input.indexOf('[') == -1 || input.lastIndexOf(']') != input.length() - 1) {
            return null;
        }
        ArrayList<String> output = new ArrayList<>();
        int start = 0;
        boolean needObject = true;
        int brackets = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '[' && needObject) {
                needObject = false;
                output.add(input.substring(start, i));
                start = i + 1;
            }
            else if (input.charAt(i) == '[') {
                brackets++;
            }
            else if (input.charAt(i) == ']' && brackets > 0) {
                brackets--;
            }
            else if ((input.charAt(i) == ';' || input.charAt(i) == ']') && brackets == 0) {
                output.add((input.substring(start, i)));
                start = i + 1;
            }
        }
        return output;
    }

    public static <T extends ObjectTag> T getObjectFrom(Class<T> dClass, String value, TagContext context) {
        try {
            List<String> matches = separateProperties(value);
            boolean matched = matches != null && Adjustable.class.isAssignableFrom(dClass);
            T gotten = (T) valueof.get(dClass).valueOf(matched ? matches.get(0) : value, context);
            if (gotten != null && matched) {
                for (int i = 1; i < matches.size(); i++) {
                    List<String> data = CoreUtilities.split(matches.get(i), '=', 2);
                    if (data.size() != 2) {
                        Debug.echoError("Invalid property string '" + matches.get(i) + "'!");
                        continue;
                    }
                    ((Adjustable) gotten).safeApplyProperty(new Mechanism(new ElementTag(data.get(0)),
                            new ElementTag((data.get(1)).replace((char) 0x2011, ';')), context));
                }
            }
            return gotten;
        }
        catch (Exception e) {
            Debug.echoError(e);
        }

        return null;
    }

    /**
     * This function will return the most-valid ObjectTag for the input string.
     * If the input lacks @ notation or is not a valid object, an ElementTag will be returned.
     *
     * @param value the input string.
     * @return the most-valid ObjectTag available.
     */
    public static ObjectTag pickObjectFor(String value) {
        return pickObjectFor(value, DenizenCore.getImplementation().getEmptyScriptEntryData().getTagContext());
    }

    public static ObjectTag pickObjectFor(String value, TagContext context) {
        if (value == null) {
            return null;
        }
        // While many inputs are valid as various object types
        // (EG, 'bob' could be a player or NPC's name)
        // Only use specific objects for input with @ notation
        if (value.contains("@")) {
            String type = value.split("@", 2)[0];
            // Of course, ensure the @ notation is valid first
            if (canFetch(type)) {
                Class toFetch = getObjectClass(type);
                ObjectTag fetched = getObjectFrom(toFetch, value, context);
                // Only return if a valid object is born... otherwise, use an element.
                if (fetched != null) {
                    return fetched;
                }
            }
        }
        // If all else fails, just use a simple Element!
        return new ElementTag(value);
    }
}
