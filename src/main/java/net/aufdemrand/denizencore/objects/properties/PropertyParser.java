package net.aufdemrand.denizencore.objects.properties;

import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class PropertyParser {

    public static abstract class PropertyGetter {

        public abstract Property get(dObject obj);
    }

    static Map<Class<? extends dObject>, Map<String, List<PropertyGetter>>> propgetters = new HashMap<Class<? extends dObject>, Map<String, List<PropertyGetter>>>();


    public static void registerProperty(PropertyGetter getter, Class<? extends dObject> object) {
        registerProperty(getter, object, (String) null);
    }

    public static void insert(Map<String, List<PropertyGetter>> gettyByTag, String attrl, PropertyGetter getter) {
        List<PropertyGetter> getters = gettyByTag.get(attrl);
        if (getters == null) {
            getters = new ArrayList<PropertyGetter>();
            gettyByTag.put(attrl, getters);
        }
        getters.add(getter);
    }

    public static void registerProperty(PropertyGetter getter, Class<? extends dObject> object, String... attrLow) {
        Map<String, List<PropertyGetter>> gettyByTag = propgetters.get(object);
        if (gettyByTag == null) {
            gettyByTag = new HashMap<String, List<PropertyGetter>>();
            propgetters.put(object, gettyByTag);
        }
        for (String attrl : attrLow) {
            insert(gettyByTag, attrl, getter);
        }
        insert(gettyByTag, "_all_", getter);
    }

    public static void registerProperty(final Class property, Class<? extends dObject> object) {
        try {
            final Method getf = property.getMethod("getFrom", dObject.class);
            getf.setAccessible(true);
            PropertyGetter getter = new PropertyGetter() {
                @Override
                public Property get(dObject obj) {
                    try {
                        Object o = getf.invoke(null, obj);
                        if (o instanceof Property) {
                            return (Property) o;
                        }
                    }
                    catch (IllegalAccessException e) {
                        dB.echoError(e);
                    }
                    catch (InvocationTargetException e) {
                        dB.echoError(e);
                    }
                    return null;
                }
            };
            registerProperty(getter, object);
        }
        catch (NoSuchMethodException e) {
            dB.echoError("Unable to register property '" + property.getSimpleName() + "'!");
        }
    }

    public static String getPropertiesString(dObject object) {
        StringBuilder prop_string = new StringBuilder();

        // Iterate through each property associated with the dObject type, invoke 'describes'
        // and if 'true', add property string from the property to the prop_string.
        for (Property property : getProperties(object)) {
            String description = property.getPropertyString();
            if (description != null) {
                prop_string.append(property.getPropertyId()).append('=')
                        .append(description.replace(';', (char) 0x2011)).append(';');
            }
        }

        // Return the list of properties
        if (prop_string.length() > 0) // Remove final semicolon
        {
            return "[" + prop_string.substring(0, prop_string.length() - 1) + "]";
        }
        else {
            return "";
        }
    }

    public static List<Property> empty = new ArrayList<Property>();

    public static List<Property> getProperties(dObject object, String attribLow) {
        Map<String, List<PropertyGetter>> gettyByTag = propgetters.get(object.getClass());
        if (gettyByTag != null) {
            List<Property> returnMe = empty;
            List<PropertyGetter> getty = gettyByTag.get(attribLow);
            if (getty != null) {
                returnMe = new ArrayList<Property>(getty.size());
                for (PropertyGetter property : getty) {
                    Property propGot = property.get(object);
                    if (propGot != null) {
                        returnMe.add(propGot);
                    }
                }
            }
            getty = gettyByTag.get(null);
            if (getty != null) {
                if (returnMe == empty) {
                    returnMe = new ArrayList<Property>(getty.size());
                }
                for (PropertyGetter property : getty) {
                    Property propGot = property.get(object);
                    if (propGot != null) {
                        returnMe.add(propGot);
                    }
                }
            }
            return returnMe;
        }
        return empty;
    }

    public static List<Property> getProperties(dObject object) {
        Map<String, List<PropertyGetter>> gettyByTag = propgetters.get(object.getClass());
        if (gettyByTag != null) {
            List<PropertyGetter> getty = gettyByTag.get("_all_");
            if (getty != null) {
                List<Property> props = new ArrayList<Property>();
                for (PropertyGetter property : getty) {
                    Property propGot = property.get(object);
                    if (propGot != null) {
                        props.add(propGot);
                    }
                }
                return props;
            }
        }
        return empty;
    }
}
