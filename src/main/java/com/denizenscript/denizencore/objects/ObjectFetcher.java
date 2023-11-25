package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.tags.CoreObjectTags;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

import java.lang.reflect.Method;
import java.util.*;

public class ObjectFetcher {

    public static Map<String, ObjectType<? extends ObjectTag>> objectsByPrefix = new HashMap<>();
    public static Map<String, ObjectType<? extends ObjectTag>> objectsByName = new HashMap<>();
    public static Map<Class<? extends ObjectTag>, ObjectType<? extends ObjectTag>> objectsByClass = new HashMap<>();
    public static Map<Class<? extends ObjectTag>, List<Class<? extends ObjectTag>>> customSubtypeList = new HashMap<>();
    public static HashSet<Class<? extends ObjectTag>> realObjectClassSet = new HashSet<>();

    public static <T extends ObjectTag> ObjectType<T> getType(Class<T> type) {
        return (ObjectType<T>) objectsByClass.get(type);
    }

    private static ArrayList<Class<? extends ObjectTag>> createList(Class<? extends ObjectTag> clazz) {
        ArrayList<Class<? extends ObjectTag>> classes = new ArrayList<>();
        classes.add(clazz);
        classes.add(ElementTag.class);
        return classes;
    }

    public static void registerCrossType(Class<? extends ObjectTag> a, Class<? extends ObjectTag> b) {
        List<Class<? extends ObjectTag>> listA = customSubtypeList.computeIfAbsent(a, ObjectFetcher::createList);
        List<Class<? extends ObjectTag>> listB = customSubtypeList.computeIfAbsent(b, ObjectFetcher::createList);
        listA.add(b);
        listB.add(a);
    }

    public static Collection<Class<? extends ObjectTag>> getAllApplicableSubTypesFor(Class<? extends ObjectTag> type) {
        if (type == ObjectTag.class) {
            return realObjectClassSet;
        }
        List<Class<? extends ObjectTag>> customSet = customSubtypeList.get(type);
        if (customSet != null) {
            return customSet;
        }
        if (type == ElementTag.class) {
            return Collections.singleton(ElementTag.class);
        }
        return Arrays.asList(type, ElementTag.class);
    }

    public static ObjectType<BinaryTag> TYPE_BINARY;
    public static ObjectType<ColorTag> TYPE_COLOR;
    public static ObjectType<CustomObjectTag> TYPE_CUSTOM;
    public static ObjectType<DurationTag> TYPE_DURATION;
    public static ObjectType<ElementTag> TYPE_ELEMENT;

    public static ObjectType<ImageTag> TYPE_IMAGE;
    public static ObjectType<JavaReflectedObjectTag> TYPE_REFLECTEDOBJECT;
    public static ObjectType<ListTag> TYPE_LIST;
    public static ObjectType<MapTag> TYPE_MAP;
    public static ObjectType<QuaternionTag> TYPE_QUATERNION;
    public static ObjectType<QueueTag> TYPE_QUEUE;
    public static ObjectType<ScriptTag> TYPE_SCRIPT;
    public static ObjectType<SecretTag> TYPE_SECRET;
    public static ObjectType<TimeTag> TYPE_TIME;

    public static void registerCoreObjects() {

        // <--[tag]
        // @attribute <binary[<binary>]>
        // @returns BinaryTag
        // @description
        // Returns a BinaryTag constructed from the input binary data in hexadecimal format.
        // Refer to <@link objecttype BinaryTag>.
        // -->
        TYPE_BINARY = registerWithObjectFetcher(BinaryTag.class, BinaryTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // binary@

        // <--[tag]
        // @attribute <color[<color>]>
        // @returns ColorTag
        // @description
        // Returns a color object constructed from the input value.
        // Refer to <@link objecttype ColorTag>.
        // -->
        TYPE_COLOR = ObjectFetcher.registerWithObjectFetcher(ColorTag.class, ColorTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // co@

        // <--[tag]
        // @attribute <custom_object[<custom-object>]>
        // @returns CustomObjectTag
        // @description
        // Returns a custom object constructed from the input value.
        // Refer to <@link ObjectType CustomObjectTag>.
        // -->
        TYPE_CUSTOM = registerWithObjectFetcher(CustomObjectTag.class, CustomObjectTag.tagProcessor).setAsNOtherCode().generateBaseTag(); // custom@

        // <--[tag]
        // @attribute <duration[<duration>]>
        // @returns DurationTag
        // @description
        // Returns a duration object constructed from the input value.
        // Refer to <@link ObjectType DurationTag>.
        // -->
        TYPE_DURATION = registerWithObjectFetcher(DurationTag.class, DurationTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // d@

        // <--[tag]
        // @attribute <element[<element>]>
        // @returns ElementTag
        // @description
        // Returns an element constructed from the input value.
        // Refer to <@link objecttype ElementTag>.
        // -->
        TYPE_ELEMENT = registerWithObjectFetcher(ElementTag.class, ElementTag.tagProcessor).setCanConvertStatic().generateBaseTag(); // el@
        TYPE_ELEMENT.typeChecker = ObjectType.TypeComparisonRunnable.trueAlways;
        TYPE_ELEMENT.typeConverter = (obj, c) -> obj.asElement();

        // <--[tag]
        // @attribute <image[<image>]>
        // @returns ImageTag
        // @description
        // Returns an ImageTag constructed from the input value.
        // Refer to <@link ObjectType ImageTag>.
        // -->
        TYPE_IMAGE = registerWithObjectFetcher(ImageTag.class, ImageTag.tagProcessor).setCanConvertStatic().generateBaseTag(); // image@

        // <--[tag]
        // @attribute <reflected[<reflected-tag>]>
        // @returns JavaReflectedObjectTag
        // @description
        // Returns a JavaReflectedObjectTag constructed from the input reference ID lookup.
        // Refer to <@link objecttype JavaReflectedObjectTag>.
        // -->
        TYPE_REFLECTEDOBJECT = registerWithObjectFetcher(JavaReflectedObjectTag.class, JavaReflectedObjectTag.tagProcessor).generateBaseTag(); // reflected@

        // Tag generated externally as input is optional
        TYPE_LIST = registerWithObjectFetcher(ListTag.class, ListTag.tagProcessor).setCanConvertStatic(); // li@
        TYPE_LIST.typeChecker = ObjectType.TypeComparisonRunnable.trueAlways;
        TYPE_LIST.typeConverter = ListTag::getListFor;

        // Tag generated externally as input is optional
        TYPE_MAP = registerWithObjectFetcher(MapTag.class, MapTag.tagProcessor).setCanConvertStatic(); // map@
        TYPE_MAP.typeConverter = MapTag::getMapFor;
        TYPE_MAP.typeChecker = (inp) -> {
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
        };

        // <--[tag]
        // @attribute <quaternion[<quaternion>]>
        // @returns QuaternionTag
        // @description
        // Returns a QuaternionTag object constructed from the input value.
        // Refer to <@link ObjectType QuaternionTag>.
        // -->
        TYPE_QUATERNION = registerWithObjectFetcher(QuaternionTag.class, QuaternionTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // quaternion@

        // Tag generated externally as input is optional
        TYPE_QUEUE = registerWithObjectFetcher(QueueTag.class, QueueTag.tagProcessor).setAsNOtherCode(); // q@

        // Tag generated externally as input is optional
        TYPE_SCRIPT = registerWithObjectFetcher(ScriptTag.class, ScriptTag.tagProcessor).setAsNOtherCode(); // s@

        // <--[tag]
        // @attribute <secret[<secret>]>
        // @returns SecretTag
        // @description
        // Returns a SecretTag object constructed from the input value.
        // Refer to <@link ObjectType SecretTag>.
        // @Example
        // - webget <secret[my_secret_url]> "post:Message to secret address!"
        // -->
        TYPE_SECRET = registerWithObjectFetcher(SecretTag.class, SecretTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // secret@

        // <--[tag]
        // @attribute <time[<time>]>
        // @returns TimeTag
        // @description
        // Returns a time object constructed from the input value.
        // Refer to <@link ObjectType TimeTag>.
        // -->
        TYPE_TIME = registerWithObjectFetcher(TimeTag.class, TimeTag.tagProcessor).setAsNOtherCode().setCanConvertStatic().generateBaseTag(); // time@
    }

    public static <T extends ObjectTag> ObjectType.MatchesInterface getMatchesFor(Class<T> clazz) {
        try {
            ObjectType.MatchesInterface result = ReflectionHelper.getStaticLambda(ObjectType.MatchesInterface.class, "matches", clazz, "matches", clazz.getDeclaredMethod("matches", String.class));
            if (result == null) {
                ReflectionHelper.echoError("Failed to get matches for " + clazz.getCanonicalName());
            }
            return result;
        }
        catch (Throwable ex) {
            ReflectionHelper.echoError(ex);
        }
        return null;
    }

    public static <T extends ObjectTag> ObjectType.ValueOfInterface<T> getValueOfFor(Class<T> clazz) {
        try {
            ObjectType.ValueOfInterface<T> result = ReflectionHelper.getStaticLambda(ObjectType.ValueOfInterface.class, "valueOf", clazz, "valueOf", clazz.getDeclaredMethod("valueOf", String.class, TagContext.class));
            if (result == null) {
                ReflectionHelper.echoError("Failed to get valueOf for " + clazz.getCanonicalName());
            }
            return result;
        }
        catch (Throwable ex) {
            ReflectionHelper.echoError(ex);
        }
        return null;
    }

    @Deprecated
    public static void registerWithObjectFetcher(Class<? extends ObjectTag> objectTag) {
        registerWithObjectFetcher(objectTag, null);
    }

    public static <T extends ObjectTag> ObjectType<T> registerWithObjectFetcher(Class<T> objectTag, ObjectTagProcessor<T> processor) {
        String className = DebugInternals.getClassNameOpti(objectTag);
        String shortName = null;
        if (className.endsWith("Tag")) {
            shortName = className.substring(0, className.length() - "Tag".length());
        }
        return registerWithObjectFetcher(objectTag, processor, shortName, className);
    }

    public static <T extends ObjectTag> ObjectType<T> registerWithObjectFetcher(Class<T> objectTag, ObjectTagProcessor<T> processor, String shortName, String longName) {
        ObjectType<T> newType = new ObjectType<>();
        newType.clazz = objectTag;
        if (processor != null) {
            processor.type = objectTag;
            CoreObjectTags.generateCoreTags(processor);
            newType.tagProcessor = processor;
        }
        newType.longName = longName;
        newType.shortName = shortName;
        newType.isAdjustable = Adjustable.class.isAssignableFrom(objectTag);
        objectsByClass.put(objectTag, newType);
        realObjectClassSet.add(objectTag);
        try {
            Method valueOfMethod = objectTag.getMethod("valueOf", String.class, TagContext.class);
            if (valueOfMethod.isAnnotationPresent(Fetchable.class)) {
                String identifier = valueOfMethod.getAnnotation(Fetchable.class).value();
                objectsByPrefix.put(CoreUtilities.toLowerCase(identifier.trim()), newType);
                objectsByName.put(CoreUtilities.toLowerCase(longName), newType);
                if (shortName != null) {
                    objectsByName.put(CoreUtilities.toLowerCase(shortName), newType);
                }
                newType.prefix = identifier;
            }
            else {
                Debug.echoError("Type '" + DebugInternals.getClassNameOpti(objectTag) + "' registered as an object type, but doesn't have a fetcher prefix.");
            }
            newType.matches = getMatchesFor(objectTag);
            newType.valueOf = getValueOfFor(objectTag);
            for (Method registerMethod : objectTag.getDeclaredMethods()) {
                if ((registerMethod.getName().equals("register") || registerMethod.getName().equals("registerTags")) && registerMethod.getParameterCount() == 0) {
                    registerMethod.invoke(null);
                    break;
                }
            }
        }
        catch (Throwable ex) {
            Debug.echoError("Failed to initialize an object type(" + DebugInternals.getClassNameOpti(objectTag) + "): ");
            Debug.echoError(ex);
        }
        return newType;
    }

    public static boolean canFetch(String id) {
        return objectsByPrefix.containsKey(CoreUtilities.toLowerCase(id));
    }

    public static boolean isObjectWithProperties(String input) {
        return input.indexOf('[') != -1 && input.lastIndexOf(']') == input.length() - 1;
    }

    public static boolean checkMatch(Class<? extends ObjectTag> dClass, String value) {
        return checkMatch(getType(dClass), value);
    }

    public static boolean checkMatch(ObjectType<? extends ObjectTag> objType, String value) {
        if (value == null || objType == null) {
            return false;
        }
        int firstBracket = value.indexOf('[');
        if (firstBracket != -1 && value.lastIndexOf(']') == value.length() - 1) {
            value = value.substring(0, firstBracket);
        }
        try {
            return objType.matches.matches(value);
        }
        catch (Exception e) {
            Debug.echoError(e);
        }

        return false;

    }

    public static List<String> separateProperties(String input) {
        if (!isObjectWithProperties(input)) {
            return null;
        }
        ArrayList<String> output = new ArrayList<>(input.length() / 7);
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
        if (dClass == ObjectTag.class) {
            return (T) pickObjectFor(value, context);
        }
        return getObjectFrom(getType(dClass), value, context);
    }

    public static <T extends ObjectTag> T getObjectFromWithProperties(Class<T> dClass, String value, TagContext context) {
        return getObjectFromWithProperties(getType(dClass), value, context);
    }

    public static String partialUnescape(String description) {
        if (description.indexOf('&') != -1) {
            description = CoreUtilities.replace(description, "&sc", ";");
            description = CoreUtilities.replace(description, "&lb", "[");
            description = CoreUtilities.replace(description, "&rb", "]");
            description = CoreUtilities.replace(description, "&eq", "=");
            description = CoreUtilities.replace(description, "&amp", "&");
        }
        return description;
    }

    public static String unescapeProperty(String description) {
        if (description.indexOf('&') == -1) {
            return description;
        }
        int openBracket = description.indexOf('[');
        if (openBracket == -1) {
            return partialUnescape(description);
        }
        int length = description.length();
        StringBuilder result = new StringBuilder(length);
        int start = 0;
        int brackets = 0;
        for (int i = openBracket; i < length; i++) {
            char c = description.charAt(i);
            if (c == '[') {
                brackets++;
                if (brackets == 1) {
                    result.append(partialUnescape(description.substring(start, i)));
                    start = i;
                }
            }
            else if (c == ']') {
                brackets--;
                if (brackets == 0) {
                    result.append(description, start, i);
                    start = i;
                    i = description.indexOf('[', start) - 1;
                    if (i < 0) {
                        break;
                    }
                }
            }
        }
        result.append(partialUnescape(description.substring(start)));
        return result.toString();
    }

    public static void applyPropertySet(Adjustable object, TagContext context, List<String> properties) {
        for (int i = 1; i < properties.size(); i++) {
            List<String> data = CoreUtilities.split(properties.get(i), '=', 2);
            if (data.size() != 2) {
                Debug.echoError("Invalid property string '" + properties.get(i) + "'!");
                continue;
            }
            String description = unescapeProperty(data.get(1));
            object.safeApplyProperty(new Mechanism(data.get(0), new ElementTag(description), context));
        }
    }

    public static <T extends ObjectTag> T getObjectFrom(ObjectType<T> type, String value, TagContext context) {
        try {
            return type.valueOf.valueOf(value, context);
        }
        catch (Exception e) {
            Debug.echoError(e);
        }

        return null;
    }

    public static <T extends ObjectTag> T getObjectFromWithProperties(ObjectType<T> type, String value, TagContext context) {
        try {
            List<String> matches = separateProperties(value);
            boolean matched = matches != null && type.isAdjustable;
            T gotten = type.valueOf.valueOf(matched ? matches.get(0) : value, context);
            if (gotten != null && matched) {
                applyPropertySet((Adjustable) gotten, context, matches);
                gotten = (T) gotten.fixAfterProperties();
            }
            return gotten;
        }
        catch (Exception e) {
            Debug.echoError(e);
        }

        return null;
    }

    public static ObjectTag pickObjectFor(String value, TagContext context) {
        if (value == null) {
            return null;
        }
        if (CoreUtilities.contains(value, '@')) {
            String type = value.split("@", 2)[0];
            ObjectType<? extends ObjectTag> toFetch = objectsByPrefix.get(type);
            if (toFetch != null && (toFetch.canConvertStatic || !TagManager.isStaticParsing)) {
                ObjectTag fetched = getObjectFrom(toFetch, value, context);
                if (fetched != null) {
                    return fetched;
                }
            }
        }
        return new ElementTag(value);
    }
}
