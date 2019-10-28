package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.tags.TagContext;

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

    public interface ValueOfInterface<T extends ObjectTag> {

        T valueOf(String str, TagContext context);
    }

    public static class ObjectType<T extends ObjectTag> {

        public Class<T> clazz;

        public MatchesInterface matches;

        public ValueOfInterface<T> valueOf;

        public ObjectTagProcessor<T> tagProcessor;

        public String prefix;
    }

    public static Map<String, ObjectType<? extends ObjectTag>> objectsByPrefix = new HashMap<>();
    public static Map<Class<? extends ObjectTag>, ObjectType<? extends ObjectTag>> objectsByClass = new HashMap<>();

    public static void registerCoreObjects() {

        // Initialize the ObjectFetcher
        registerWithObjectFetcher(CustomObjectTag.class, CustomObjectTag.tagProcessor); // custom@
        registerWithObjectFetcher(ListTag.class, ListTag.tagProcessor);        // li@
        registerWithObjectFetcher(ScriptTag.class, ScriptTag.tagProcessor);      // s@
        registerWithObjectFetcher(ElementTag.class, ElementTag.tagProcessor);      // el@
        registerWithObjectFetcher(DurationTag.class, DurationTag.tagProcessor);     // d@
        registerWithObjectFetcher(QueueTag.class, QueueTag.tagProcessor);  // q@

    }

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

    @Deprecated
    public static void registerWithObjectFetcher(Class<? extends ObjectTag> objectTag) {
        registerWithObjectFetcher(objectTag, null);
    }

    public static <T extends ObjectTag> void registerWithObjectFetcher(Class<T> objectTag, ObjectTagProcessor<T> processor) {
        ObjectType newType = new ObjectType();
        newType.clazz = objectTag;
        newType.tagProcessor = processor;
        objectsByClass.put(objectTag, newType);
        try {
            Method valueOfMethod = objectTag.getMethod("valueOf", String.class, TagContext.class);
            if (valueOfMethod.isAnnotationPresent(Fetchable.class)) {
                String identifier = valueOfMethod.getAnnotation(Fetchable.class).value();
                objectsByPrefix.put(CoreUtilities.toLowerCase(identifier.trim()), newType);
                Debug.log("Registered: " + objectTag.getSimpleName() + " as " + identifier);
                newType.prefix = identifier;
            }
            else {
                Debug.echoError("Type '" + objectTag.getSimpleName() + "' registered as an object type, but doesn't have a fetcher prefix.");
            }
            newType.matches = getMatchesFor(objectTag);
            newType.valueOf = getValueOfFor(objectTag);
            for (Method registerMethod: objectTag.getDeclaredMethods()) {
                if (registerMethod.getName().equals("registerTags") && registerMethod.getParameterCount() == 0) {
                    registerMethod.invoke(null);
                }
            }
        }
        catch (Throwable e) {
            Debug.echoError("Failed to initialize an object type(" + objectTag.getSimpleName() + "): ");
            Debug.echoError(e);
        }
    }

    public static boolean canFetch(String id) {
        return objectsByPrefix.containsKey(CoreUtilities.toLowerCase(id));
    }

    public static Class getObjectClass(String id) {
        if (canFetch(id)) {
            return objectsByPrefix.get(CoreUtilities.toLowerCase(id)).clazz;
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
        value = m.matches() ? m.group(1) : value;
        try {
            return objectsByClass.get(dClass).matches.matches(value);
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
        return getObjectFrom((ObjectType<T>) objectsByClass.get(dClass), value, context);
    }

    public static <T extends ObjectTag> T getObjectFrom(ObjectType<T> type, String value, TagContext context) {
        try {
            List<String> matches = separateProperties(value);
            boolean matched = matches != null && Adjustable.class.isAssignableFrom(type.clazz);
            T gotten = type.valueOf.valueOf(matched ? matches.get(0) : value, context);
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
        if (value.contains("@")) {
            String type = value.split("@", 2)[0];
            ObjectType<? extends ObjectTag> toFetch = objectsByPrefix.get(type);
            if (toFetch != null) {
                ObjectTag fetched = getObjectFrom(toFetch, value, context);
                if (fetched != null) {
                    return fetched;
                }
            }
        }
        return new ElementTag(value);
    }
}
