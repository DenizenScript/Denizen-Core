package net.aufdemrand.denizencore.objects.properties;

import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.*;

public class PropertyParser {

    @FunctionalInterface
    public interface PropertyGetter {

        Property get(dObject obj);
    }

    public static class ClassPropertiesInfo {
        public List<PropertyGetter> allProperties = new ArrayList<>();

        public List<PropertyGetter> propertiesAnyTags = new ArrayList<>();

        public List<PropertyGetter> propertiesAnyMechs = new ArrayList<>();

        public List<PropertyGetter> propertiesWithMechs = new ArrayList<>();

        public Map<String, PropertyGetter> propertiesByTag = new HashMap<>();

        public Map<String, PropertyGetter> propertiesByMechanism = new HashMap<>();
    }

    public static Map<Class<? extends dObject>, ClassPropertiesInfo> propertiesByClass = new HashMap<>();

    public static void registerPropertyGetter(PropertyGetter getter, Class<? extends dObject> object, String[] tags, String[] mechs) {
        ClassPropertiesInfo propInfo = propertiesByClass.get(object);
        if (propInfo == null) {
            propInfo = new ClassPropertiesInfo();
            propertiesByClass.put(object, propInfo);
        }
        propInfo.allProperties.add(getter);
        if (tags != null) {
            for (String tag : tags) {
                propInfo.propertiesByTag.put(tag, getter);
            }
        }
        else {
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
            dB.echoError("Invalid property field '" + fieldName + "' for property class '" + property.getSimpleName() + "': field is not a Set: " + e.getMessage() + "!");
        }
        catch (NoSuchFieldException e) {
            // Ignore this exception.
        }
        return null;
    }

    public static void registerProperty(final Class property, Class<? extends dObject> object, PropertyGetter getter) {
        registerPropertyGetter(getter, object, getStringField(property, "handledTags"), getStringField(property, "handledMechs"));
    }

    public static void registerProperty(final Class property, Class<? extends dObject> object) {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            CallSite site = LambdaMetafactory.metafactory(lookup, "get", // PropertyGetter#get
                    MethodType.methodType(PropertyGetter.class), // Signature of invoke method
                    MethodType.methodType(Property.class, dObject.class), // signature of PropertyGetter#get
                    lookup.findStatic(property, "getFrom", MethodType.methodType(property, dObject.class)), // signature of getFrom
                    MethodType.methodType(property, dObject.class)); // Signature of getFrom again
            PropertyGetter getter = (PropertyGetter) site.getTarget().invoke();
            registerProperty(property, object, getter);
        }
        catch (Throwable e) {
            dB.echoError("Unable to register property '" + property.getSimpleName() + "'!");
            dB.echoError(e);
        }
    }

    public static String getPropertiesString(dObject object) {
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

    public static List<Property> getProperties(dObject object, String attribLow) {
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

    public static List<Property> getProperties(dObject object) {
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
}
