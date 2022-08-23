package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class EnumHelper<T extends Enum> {

    public static HashMap<Class<? extends Enum>, EnumHelper<?>> helpers = new HashMap<>();

    public static <T extends Enum> EnumHelper<T> get(Class<T> enumClass) {
        return (EnumHelper<T>) helpers.computeIfAbsent(enumClass, EnumHelper::new);
    }

    public static String cleanKey(String name) {
        return CoreUtilities.toLowerCase(name).replace("_", "");
    }

    public Class<T> targetClass;

    public HashMap<String, T> valuesMapLower = new HashMap<>();

    public EnumHelper(Class<T> clazz) {
        targetClass = clazz;
        try {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == clazz) {
                    valuesMapLower.put(cleanKey(f.getName()), (T) f.get(null));
                }
            }
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }
}
