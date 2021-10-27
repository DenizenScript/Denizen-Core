package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReflectionHelper {

    private static final Map<Class, CheckingFieldMap> cachedFields = new HashMap<>();

    private static final Map<Class, Map<String, MethodHandle>> cachedFieldSetters = new HashMap<>();

    public static void setFieldValue(Class clazz, String fieldName, Object object, Object value) {
        try {
            getFields(clazz).get(fieldName).set(object, value);
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }

    public static <T> T getFieldValue(Class clazz, String fieldName, Object object) {
        Map<String, Field> cache = getFields(clazz);
        try {
            Field field = cache.get(fieldName);
            if (field == null) {
                return null;
            }
            return (T) field.get(object);
        }
        catch (Exception ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public static class CheckingFieldMap extends HashMap<String, Field> {

        public Class<?> clazz;

        public CheckingFieldMap(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Field getFirstOfType(Class fieldClazz) {
            for (Field f : super.values()) {
                if (f.getType().equals(fieldClazz)) {
                    return f;
                }
            }
            String err = "Reflection field missing - Tried to find field of type '" + fieldClazz.getCanonicalName() + "' of class '" + clazz.getCanonicalName() + "'.";
            System.err.println("[Denizen] [ReflectionHelper]: " + err);
            Debug.echoError(err);
            return null;
        }

        @Override
        public Field get(Object name) {
            Field f = super.get(name);
            if (f == null) {
                String err = "Reflection field missing - Tried to read field '" + name + "' of class '" + clazz.getCanonicalName() + "'.";
                System.err.println("[Denizen] [ReflectionHelper]: " + err);
                Debug.echoError(err);
            }
            return f;
        }

        public Field getNoCheck(String name) {
            return super.get(name);
        }
    }

    public static CheckingFieldMap getFields(Class clazz) {
        CheckingFieldMap fields = cachedFields.get(clazz);
        if (fields != null) {
            return fields;
        }
        fields = new CheckingFieldMap(clazz);
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            fields.put(field.getName(), field);
        }
        cachedFields.put(clazz, fields);
        return fields;
    }

    public static Method getMethod(Class<?> clazz, String method, Class<?>... params) {
        Method f = null;
        try {
            if (method == null) {
                for (Method possible : clazz.getDeclaredMethods()) {
                    if (possible.getParameterCount() == params.length && Arrays.equals(possible.getParameterTypes(), params)) {
                        f = possible;
                        break;
                    }
                }
            }
            else {
                f = clazz.getDeclaredMethod(method, params);
                f.setAccessible(true);
            }
        }
        catch (Exception ex) {
            Debug.echoError(ex);
        }
        if (f == null) {
            String err = "Reflection method missing - Tried to read method '" + method + "' of class '" + clazz.getCanonicalName() + "'.";
            System.err.println("[Denizen] [ReflectionHelper]: " + err);
            Debug.echoError(err);
        }
        return f;
    }

    public static MethodHandle getConstructor(Class<?> clazz, Class<?>... params) {
        try {
            return LOOKUP.unreflectConstructor(clazz.getConstructor(params));
        }
        catch (Exception ex) {
            Debug.echoError(ex);
        }
        return null;
    }

    public static MethodHandle getMethodHandle(Class<?> clazz, String method, Class<?>... params) {
        try {
            return LOOKUP.unreflect(getMethod(clazz, method, params));
        }
        catch (Exception ex) {
            Debug.echoError(ex);
        }
        return null;
    }

    public static MethodHandle getFinalSetterForFirstOfType(Class<?> clazz, Class<?> fieldType) {
        Field field = getFields(clazz).getFirstOfType(fieldType);
        if (field == null) {
            return null;
        }
        return getFinalSetter(clazz, field.getName());
    }

    public static MethodHandle getFinalSetter(Class<?> clazz, String field) {
        Map<String, MethodHandle> map = cachedFieldSetters.computeIfAbsent(clazz, k -> new HashMap<>());
        MethodHandle result = map.get(field);
        if (result != null) {
            return result;
        }
        Field f = getFields(clazz).get(field);
        if (f == null) {
            Debug.echoError("Create get final setter for unknown field '" + field + "' (for class '" + clazz.getName() + "')");
            return null;
        }
        int mod = f.getModifiers();
        try {
            if (MODIFIERS_FIELD == null) {
                validateUnsafe();
                boolean isStatic = Modifier.isStatic(mod);
                long offset = (long) (isStatic ? UNSAFE_STATIC_FIELD_OFFSET.invoke(f) : UNSAFE_FIELD_OFFSET.invoke(f));
                MethodHandle method = UNSAFE_PUT_OBJECT;
                if (f.getType().isPrimitive()) {
                    if (f.getType() == float.class) {
                        method = UNSAFE_PUT_FLOAT;
                    }
                    else if (f.getType() == double.class) {
                        method = UNSAFE_PUT_DOUBLE;
                    }
                    else if (f.getType() == int.class) {
                        method = UNSAFE_PUT_INT;
                    }
                    else if (f.getType() == long.class) {
                        method = UNSAFE_PUT_LONG;
                    }
                    else if (f.getType() == boolean.class) {
                        method = UNSAFE_PUT_BOOL;
                    }
                    else {
                        Debug.echoError("Cannot create a setter for primitive type '" + f.getType().getName() + "'");
                        return null;
                    }
                }
                result = isStatic ? MethodHandles.insertArguments(method, 0, clazz, offset)
                        : MethodHandles.insertArguments(method, 1, offset);
            }
            else {
                if (Modifier.isFinal(mod)) {
                    MODIFIERS_FIELD.setInt(f, mod & ~Modifier.FINAL);
                }
                result = LOOKUP.unreflectSetter(f);
            }
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
        if (result == null) {
            return null;
        }
        cachedFieldSetters.get(clazz).put(field, result);
        return result;
    }

    private static void validateUnsafe() {
        if (UNSAFE == null) {
            try {
                UNSAFE = getFields(Class.forName("sun.misc.Unsafe")).get("theUnsafe").get(null);
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
            UNSAFE_STATIC_FIELD_OFFSET = getMethodHandle(UNSAFE.getClass(), "staticFieldOffset", Field.class).bindTo(UNSAFE);
            UNSAFE_FIELD_OFFSET = getMethodHandle(UNSAFE.getClass(), "objectFieldOffset", Field.class).bindTo(UNSAFE);
            UNSAFE_PUT_OBJECT = getMethodHandle(UNSAFE.getClass(), "putObject", Object.class, long.class, Object.class).bindTo(UNSAFE);
            UNSAFE_PUT_FLOAT = getMethodHandle(UNSAFE.getClass(), "putFloat", Object.class, long.class, float.class).bindTo(UNSAFE);
            UNSAFE_PUT_DOUBLE = getMethodHandle(UNSAFE.getClass(), "putDouble", Object.class, long.class, double.class).bindTo(UNSAFE);
            UNSAFE_PUT_INT = getMethodHandle(UNSAFE.getClass(), "putInt", Object.class, long.class, int.class).bindTo(UNSAFE);
            UNSAFE_PUT_LONG = getMethodHandle(UNSAFE.getClass(), "putLong", Object.class, long.class, long.class).bindTo(UNSAFE);
            UNSAFE_PUT_BOOL = getMethodHandle(UNSAFE.getClass(), "putBoolean", Object.class, long.class, boolean.class).bindTo(UNSAFE);
        }
    }

    public static void giveReflectiveAccess(Class<?> from, Class<?> to) {
        try {
            if (GET_MODULE == null) {
                Class<?> module = Class.forName("java.lang.Module");
                GET_MODULE = Class.class.getMethod("getModule");
                ADD_OPENS = module.getMethod("addOpens", String.class, module);
            }
            ADD_OPENS.invoke(GET_MODULE.invoke(from), from.getPackage().getName(), GET_MODULE.invoke(to));
        }
        catch (Exception e) {
        }
    }

    static {
        giveReflectiveAccess(Field.class, ReflectionHelper.class);
        MODIFIERS_FIELD = getFields(Field.class).getNoCheck("modifiers");
    }

    private static Method ADD_OPENS;
    private static Method GET_MODULE;
    private static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static Field MODIFIERS_FIELD;
    private static Object UNSAFE;
    private static MethodHandle UNSAFE_FIELD_OFFSET, UNSAFE_PUT_OBJECT, UNSAFE_PUT_FLOAT, UNSAFE_PUT_DOUBLE, UNSAFE_PUT_INT, UNSAFE_PUT_LONG, UNSAFE_PUT_BOOL, UNSAFE_STATIC_FIELD_OFFSET;
}
