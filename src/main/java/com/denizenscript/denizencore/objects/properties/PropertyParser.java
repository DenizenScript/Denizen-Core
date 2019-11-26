package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.ObjectTag;

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

    @FunctionalInterface
    public interface PropertyTag<T extends Property> {
        ObjectTag run(Attribute attribute, T prop);
    }

    public static Map<Class<? extends ObjectTag>, ClassPropertiesInfo> propertiesByClass = new HashMap<>();

    public static <P extends Property> void registerTag(String name, PropertyTag<P> runnable, String... variants) {
        final PropertyParser.PropertyGetter getter = PropertyParser.currentlyRegisteringProperty;
        final Class propertyClass = PropertyParser.currentlyRegisteringPropertyClass;
        ObjectTagProcessor<?> tagProcessor = PropertyParser.currentlyRegisteringObjectType.tagProcessor;
        tagProcessor.registerTag(name, (attribute, object) -> {
            Property prop = getter.get(object);
            if (prop == null) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("Property '" + propertyClass.getSimpleName() + "' does not describe the input object.");
                }
                return null;
            }
            return runnable.run(attribute, (P) prop);
        }, variants);
    }

    public static Class currentlyRegisteringPropertyClass;

    public static PropertyGetter currentlyRegisteringProperty;

    public static ObjectFetcher.ObjectType currentlyRegisteringObjectType;

    public static void registerPropertyGetter(PropertyGetter getter, Class<? extends ObjectTag> object, String[] tags, String[] mechs, Class property) {
        currentlyRegisteringPropertyClass = property;
        currentlyRegisteringProperty = getter;
        currentlyRegisteringObjectType = ObjectFetcher.objectsByClass.get(object);
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
            propInfo.propertiesAnyTags.add(getter);
        }
        if (mechs != null) {
            for (String mech : mechs) {
                propInfo.propertiesByMechanism.put(mech, getter);
            }
            propInfo.propertiesWithMechs.add(getter);
        }
        else {
            propInfo.propertiesAnyMechs.add(getter);
            propInfo.propertiesWithMechs.add(getter);
        }
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

    public static String getPropertiesString(ObjectTag object) {
        ClassPropertiesInfo properties = propertiesByClass.get(object.getObjectTagClass());
        if (properties == null) {
            return "";
        }
        StringBuilder prop_string = new StringBuilder(properties.propertiesWithMechs.size() * 10);
        for (PropertyGetter getter : properties.propertiesWithMechs) {
            Property property = getter.get(object);
            if (property != null) {
                String description = property.getPropertyString();
                if (description != null) {
                    prop_string.append(property.getPropertyId()).append('=')
                            .append(description.replace(';', (char) 0x2011)).append(';');
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

    public static List<Property> empty = new ArrayList<>();

    public static List<Property> getProperties(ObjectTag object, String attribLow) {
        ClassPropertiesInfo properties = propertiesByClass.get(object.getObjectTagClass());
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
        ClassPropertiesInfo properties = propertiesByClass.get(object.getObjectTagClass());
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
}
