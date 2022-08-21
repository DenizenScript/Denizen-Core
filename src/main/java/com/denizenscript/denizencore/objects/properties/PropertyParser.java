package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class PropertyParser {

    @FunctionalInterface
    public interface PropertyGetter {

        Property get(ObjectTag obj);
    }

    public static class ClassPropertiesInfo {

        public List<PropertyGetter> allProperties = new ArrayList<>();

        public List<PropertyGetter> propertiesAnyTags = new ArrayList<>();

        public List<PropertyGetter> propertiesAnyMechs = new ArrayList<>();

        public List<PropertyGetter> propertiesWithMechs = new ArrayList<>();

        public Map<String, PropertyGetter> propertiesByTag = new HashMap<>();

        public Map<String, PropertyGetter> propertiesByMechanism = new HashMap<>();

        public Map<String, String> propertyNamesByTag = new HashMap<>();
    }

    public static Set<String> allMechanismsEver = new HashSet<>();

    @FunctionalInterface
    public interface PropertyTagWithReturn<T extends Property, R extends ObjectTag> {
        R run(Attribute attribute, T prop);
    }

    @FunctionalInterface
    public interface PropertyTagWithReturnAndParam<T extends Property, R extends ObjectTag, P extends ObjectTag> {
        R run(Attribute attribute, T prop, P param);
    }

    public static Map<Class<? extends ObjectTag>, ClassPropertiesInfo> propertiesByClass = new HashMap<>();

    public static <T extends Property, R extends ObjectTag, P extends ObjectTag> void registerStaticTag(Class<T> propType, Class<R> returnType, Class<P> paramType, String name, PropertyTagWithReturnAndParam<T, R, P> runnable, String... variants) {
        registerTagInternal(propType, returnType, paramType, name, runnable, variants, true);
    }

    public static <T extends Property, R extends ObjectTag, P extends ObjectTag> void registerTag(Class<T> propType, Class<R> returnType, Class<P> paramType, String name, PropertyTagWithReturnAndParam<T, R, P> runnable, String... variants) {
        registerTagInternal(propType, returnType, paramType, name, runnable, variants, false);
    }

    public static <T extends Property, R extends ObjectTag, P extends ObjectTag> void registerTagInternal(Class<T> propType, Class<R> returnType, Class<P> paramType, String name, PropertyTagWithReturnAndParam<T, R, P> runnable, String[] variants, boolean isStatic) {
        // NOTE: Java compiler gets confused about generic types if this isn't split into its own var.
        PropertyTagWithReturn<T, R> altMethod = (attribute, prop) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            ObjectTag param = attribute.getParamObject();
            P result = param.asType(paramType, attribute.context);
            if (result == null) {
                attribute.echoError("Tag '<Y>" + name + "<W>' requires input of type '<Y>" + paramType.getSimpleName() + "<W>' but received input '<R>" + param + "<W>'.");
                return null;
            }
            return runnable.run(attribute, prop, result);
        };
        registerTagInternal(propType, returnType, name, altMethod, variants, isStatic);
    }

    public static <T extends Property, R extends ObjectTag> void registerStaticTag(Class<T> propType, Class<R> returnType, String name, PropertyTagWithReturn<T, R> runnable, String... variants) {
        registerTagInternal(propType, returnType, name, runnable, variants, true);
    }

    public static <T extends Property, R extends ObjectTag> void registerTag(Class<T> propType, Class<R> returnType, String name, PropertyTagWithReturn<T, R> runnable, String... variants) {
        registerTagInternal(propType, returnType, name, runnable, variants, false);
    }

    public static <T extends Property, R extends ObjectTag> void registerTagInternal(Class<T> propType, Class<R> returnType, String name, PropertyTagWithReturn<T, R> runnable, String[] variants, boolean isStatic) {
        final PropertyParser.PropertyGetter getter = PropertyParser.currentlyRegisteringProperty;
        ObjectTagProcessor<?> tagProcessor = PropertyParser.currentlyRegisteringObjectType.tagProcessor;
        tagProcessor.registerTagInternal(returnType, name, (attribute, object) -> {
            Property prop = getter.get(object);
            if (prop == null) {
                if (!attribute.hasAlternative()) {
                    attribute.echoError("Property '" + propType.getSimpleName() + "' does not describe the input object.");
                }
                return null;
            }
            return runnable.run(attribute, (T) prop);
        }, isStatic, variants);
    }

    public static Class currentlyRegisteringPropertyClass;

    public static PropertyGetter currentlyRegisteringProperty;

    public static ObjectType currentlyRegisteringObjectType;

    public static void registerPropertyGetter(PropertyGetter getter, Class<? extends ObjectTag> object, String[] tags, String[] mechs, Class property) {
        currentlyRegisteringPropertyClass = property;
        currentlyRegisteringProperty = getter;
        currentlyRegisteringObjectType = ObjectFetcher.getType(object);
        boolean didRegisterTags = false;
        try {
            for (Method registerMethod : property.getDeclaredMethods()) {
                if (registerMethod.getName().equals("registerTags") && registerMethod.getParameterCount() == 0) {
                    registerMethod.invoke(null);
                    didRegisterTags = true;
                }
            }
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
        currentlyRegisteringProperty = null;
        currentlyRegisteringObjectType = null;
        currentlyRegisteringPropertyClass = null;
        ClassPropertiesInfo propInfo = propertiesByClass.get(object);
        if (propInfo == null) {
            propInfo = new ClassPropertiesInfo();
            propertiesByClass.put(object, propInfo);
        }
        propInfo.allProperties.add(getter);
        if (tags != null) {
            String propName = property.getSimpleName();
            for (String tag : tags) {
                propInfo.propertiesByTag.put(tag, getter);
                propInfo.propertyNamesByTag.put(tag, propName);
            }
        }
        else if (!didRegisterTags) {
            Debug.log("Warning: property class '" + property.getName() + "' has unknown tag registration.");
            propInfo.propertiesAnyTags.add(getter);
        }
        if (mechs != null) {
            for (String mech : mechs) {
                propInfo.propertiesByMechanism.put(mech, getter);
                allMechanismsEver.add(mech);
            }
        }
        else {
            Debug.log("Warning: property class '" + property.getName() + "' has unknown mechanism registration.");
            propInfo.propertiesAnyMechs.add(getter);
        }
        propInfo.propertiesWithMechs.add(getter);
    }

    public static String[] getStringField(Class property, String fieldName) {
        try {
            Field f = property.getDeclaredField(fieldName);
            return (String[]) f.get(null);
        }
        catch (IllegalAccessException e) {
            Debug.echoError("Invalid property field '" + fieldName + "' for property class '" + property.getSimpleName() + "': field is not a String[]: " + e.getMessage() + "!");
        }
        catch (NoSuchFieldException e) {
            // Ignore this exception.
        }
        return null;
    }

    public static void registerProperty(final Class property, Class<? extends ObjectTag> object, PropertyGetter getter) {
        registerPropertyGetter(getter, object, getStringField(property, "handledTags"), getStringField(property, "handledMechs"), property);
    }

    public static void registerProperty(final Class property, Class<? extends ObjectTag> object) {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            CallSite site = LambdaMetafactory.metafactory(lookup, "get", // PropertyGetter#get
                    MethodType.methodType(PropertyGetter.class), // Signature of invoke method
                    MethodType.methodType(Property.class, ObjectTag.class), // signature of PropertyGetter#get
                    lookup.findStatic(property, "getFrom", MethodType.methodType(property, ObjectTag.class)), // signature of getFrom
                    MethodType.methodType(property, ObjectTag.class)); // Signature of getFrom again
            PropertyGetter getter = (PropertyGetter) site.getTarget().invoke();
            registerProperty(property, object, getter);
        }
        catch (Throwable e) {
            Debug.echoError("Unable to register property '" + property.getSimpleName() + "'!");
            Debug.echoError(e);
        }
    }

    public static AsciiMatcher needsEscapingMatcher = new AsciiMatcher("&;[]=");

    public static boolean isConsistentBrackets(String str, int start) {
        int brackets = 0;
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '[') {
                brackets++;
            }
            else if (c == ']') {
                brackets--;
                if (brackets < 0) {
                    return false;
                }
            }
        }
        return brackets == 0;
    }

    public static String escapePropertyKey(String input) {
        if (needsEscapingMatcher.containsAnyMatch(input)) {
            input = CoreUtilities.replace(input, "&", "&amp");
            input = CoreUtilities.replace(input, ";", "&sc");
            input = CoreUtilities.replace(input, "[", "&lb");
            input = CoreUtilities.replace(input, "]", "&rb");
            input = CoreUtilities.replace(input, "=", "&eq");
        }
        return input;
    }

    public static String escapePropertyValue(String input) {
        if (needsEscapingMatcher.containsAnyMatch(input)) {
            int openBracket = input.indexOf('[');
            if (openBracket != -1) {
                int closeBracket = input.lastIndexOf(']');
                if (closeBracket > openBracket && isConsistentBrackets(input, openBracket)) {
                    return escapePropertyValue(input.substring(0, openBracket)) + input.substring(openBracket, closeBracket + 1) + escapePropertyValue(input.substring(closeBracket + 1));
                }
            }
            input = CoreUtilities.replace(input, "&", "&amp");
            input = CoreUtilities.replace(input, ";", "&sc");
            input = CoreUtilities.replace(input, "[", "&lb");
            input = CoreUtilities.replace(input, "]", "&rb");
            input = CoreUtilities.replace(input, "=", "&eq");
        }
        return input;
    }

    public static String getPropertiesDebuggable(ObjectTag object) {
        ClassPropertiesInfo properties = propertiesByClass.get(object.getClass());
        if (properties == null) {
            return "";
        }
        StringBuilder prop_string = new StringBuilder(properties.propertiesWithMechs.size() * 10);
        for (PropertyGetter getter : properties.propertiesWithMechs) {
            Property property = getter.get(object);
            if (property != null) {
                String description = property.getPropertyString();
                if (description != null) {
                    prop_string.append(property.getPropertyId()).append(" <LG>=<Y> ").append(description).append("<LG>; <Y>");
                }
            }
        }
        if (prop_string.length() > 0) {
            return "<LG>[<Y>" + prop_string.substring(0, prop_string.length() - "; <Y>".length()) + "<LG>]";
        }
        else {
            return "";
        }
    }

    public static String getPropertiesString(ObjectTag object) {
        ClassPropertiesInfo properties = propertiesByClass.get(object.getClass());
        if (properties == null) {
            return "";
        }
        StringBuilder prop_string = new StringBuilder(properties.propertiesWithMechs.size() * 10);
        for (PropertyGetter getter : properties.propertiesWithMechs) {
            Property property = getter.get(object);
            if (property != null) {
                String description = property.getPropertyString();
                if (description != null) {
                    prop_string.append(property.getPropertyId()).append('=').append(escapePropertyValue(description)).append(';');
                }
            }
        }
        if (prop_string.length() > 0) {
            return "[" + prop_string.substring(0, prop_string.length() - 1) + "]";
        }
        else {
            return "";
        }
    }

    public static MapTag getPropertiesMap(ObjectTag object) {
        MapTag map = new MapTag();
        ClassPropertiesInfo properties = propertiesByClass.get(object.getClass());
        if (properties == null) {
            return map;
        }
        for (PropertyGetter getter : properties.propertiesWithMechs) {
            Property property = getter.get(object);
            if (property != null) {
                String description = property.getPropertyString();
                if (description != null) {
                    map.putObject(property.getPropertyId(), new ElementTag(description));
                }
            }
        }
        return map;
    }

    public static List<Property> empty = new ArrayList<>();

    public static List<Property> getProperties(ObjectTag object, String attribLow) {
        ClassPropertiesInfo properties = propertiesByClass.get(object.getClass());
        if (properties == null) {
            return empty;
        }
        PropertyGetter getter = properties.propertiesByTag.get(attribLow);
        if (getter != null) {
            Property prop = getter.get(object);
            if (prop == null) {
                return empty;
            }
            return Collections.singletonList(getter.get(object));
        }
        else {
            return getProperties(object);
        }
    }

    public static List<Property> getProperties(ObjectTag object) {
        ClassPropertiesInfo properties = propertiesByClass.get(object.getClass());
        if (properties == null) {
            return empty;
        }
        List<Property> props = new ArrayList<>(properties.allProperties.size());
        for (PropertyGetter getter : properties.allProperties) {
            Property prop = getter.get(object);
            if (prop != null) {
                props.add(prop);
            }
        }
        return props;
    }

    // <--[ObjectType]
    // @name PropertyHolderObject
    // @prefix None
    // @base None
    // @format
    // N/A
    //
    // @description
    // "PropertyHolderObject" is a pseudo-ObjectType that represents any object that holds properties.
    //
    // -->

    public static <T extends Adjustable> void registerPropertyTagHandlers(Class<T> type, ObjectTagProcessor<T> processor) {

        // <--[tag]
        // @attribute <PropertyHolderObject.with[<mechanism>=<value>;...]>
        // @returns PropertyHolderObject
        // @group properties
        // @description
        // Returns a copy of the object with mechanism adjustments applied.
        // Be careful with dynamic inputs, they may break from escaping flaws.
        // Consider using <@link tag PropertyHolderObject.with_single> instead.
        // -->
        processor.registerTag(type, "with", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            T instance = (T) object.duplicate();
            List<String> properties = ObjectFetcher.separateProperties("[" + attribute.getParam() + "]");
            for (int i = 1; i < properties.size(); i++) {
                List<String> data = CoreUtilities.split(properties.get(i), '=', 2);
                if (data.size() != 2) {
                    Debug.echoError("Invalid property string '" + properties.get(i) + "'!");
                }
                else {
                    instance.safeApplyProperty(new Mechanism(data.get(0), new ElementTag(data.get(1)), attribute.context));
                }
            }
            return instance;
        });

        // <--[tag]
        // @attribute <PropertyHolderObject.with_single[<mechanism>=<value>]>
        // @returns PropertyHolderObject
        // @group properties
        // @description
        // Returns a copy of the object with a single mechanism adjustment applied.
        // This avoids the risk of escaping issues.
        // -->
        processor.registerTag(type, "with_single", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            T instance = (T) object.duplicate();
            List<String> data = CoreUtilities.split(attribute.getParam(), '=', 2);
            if (data.size() != 2) {
                Debug.echoError("Invalid property string '" + attribute.getParam() + "'!");
            }
            else {
                instance.safeApplyProperty(new Mechanism(data.get(0), new ElementTag(data.get(1)), attribute.context));
            }
            return instance;
        });

        // <--[tag]
        // @attribute <PropertyHolderObject.with_map[<property-map>]>
        // @returns PropertyHolderObject
        // @group properties
        // @description
        // Returns a copy of the object with the MapTag of mechanism adjustments applied.
        // -->
        processor.registerTag(type, "with_map", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            MapTag properties = attribute.paramAsType(MapTag.class);
            T instance = (T) object.duplicate();
            for (Map.Entry<StringHolder, ObjectTag> pair : properties.map.entrySet()) {
                instance.safeApplyProperty(new Mechanism(pair.getKey().low, pair.getValue(), attribute.context));
            }
            return instance;
        });

        // <--[tag]
        // @attribute <PropertyHolderObject.supports[<property-name>]>
        // @returns ElementTag(Boolean)
        // @group properties
        // @description
        // Returns true if the property named is supported by the object.
        // This does not necessarily mean it has a valid current value, just that it's supported at all.
        // -->
        processor.registerTag(ElementTag.class, "supports", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            String propertyName = attribute.getParam();
            ClassPropertiesInfo properties = propertiesByClass.get(object.getClass());
            if (properties == null) {
                return new ElementTag(false);
            }
            PropertyGetter getter = properties.propertiesByMechanism.get(CoreUtilities.toLowerCase(propertyName));
            if (getter == null) {
                return new ElementTag(false);
            }
            return new ElementTag(getter.get(object) != null);
        });

        // <--[tag]
        // @attribute <PropertyHolderObject.property_map>
        // @returns MapTag
        // @group properties
        // @description
        // Returns the object's property map.
        // -->
        processor.registerTag(MapTag.class, "property_map", (attribute, object) -> {
            return getPropertiesMap(object);
        });
    }
}
