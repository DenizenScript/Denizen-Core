package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ReflectionHelper {

    public static boolean hasInitialized = false;

    private static final Map<Class, CheckingFieldMap> cachedFields = new HashMap<>();

    private static final Map<Class, Map<String, MethodHandle>> cachedFieldSetters = new HashMap<>();

    public static void echoError(String message) {
        if (hasInitialized) {
            Debug.echoError("[ReflectionHelper]" + message);
        }
        else {
            System.err.println("[Denizen] [ReflectionHelper]: " + message);
        }
    }

    public static void echoError(Throwable ex) {
        if (hasInitialized) {
            Debug.echoError(ex);
        }
        else {
            ex.printStackTrace();
        }
    }

    public static void setFieldValue(Class clazz, String fieldName, Object object, Object value) {
        try {
            getFields(clazz).get(fieldName).set(object, value);
        }
        catch (Throwable ex) {
            echoError(ex);
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
        catch (Throwable ex) {
            echoError(ex);
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
            echoError("Reflection field missing - Tried to find field of type '" + fieldClazz.getCanonicalName() + "' of class '" + clazz.getCanonicalName() + "'.");
            return null;
        }

        @Override
        public Field get(Object name) {
            Field f = super.get(name);
            if (f == null) {
                echoError("Reflection field missing - Tried to read field '" + name + "' of class '" + clazz.getCanonicalName() + "'.");
            }
            return f;
        }

        public Field get(String name, Class expected) {
            Field f = get(name);
            if (f == null) {
                return null;
            }
            if (f.getType() != expected) {
                echoError("Reflection field incorrect type - read field '" + name + "' from class '" + clazz.getCanonicalName() + "', expected type '" + expected.getCanonicalName() + "' but is type '" + f.getType().getCanonicalName() + "'");
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
            echoError(ex);
        }
        if (f == null) {
            echoError("Reflection method missing - Tried to read method '" + method + "' of class '" + clazz.getCanonicalName() + "'.");
        }
        return f;
    }

    public static MethodHandle getConstructor(Class<?> clazz, Class<?>... params) {
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor(params);
            ctor.setAccessible(true);
            MethodHandle result = LOOKUP.unreflectConstructor(ctor);
            if (result != null) {
                return result;
            }
        }
        catch (Throwable ex) {
            echoError(ex);
        }
        echoError("[ReflectionHelper]: Cannot find constructor for class '" + clazz.getCanonicalName() + "' with params: ["
                + Arrays.stream(params).map(Class::getCanonicalName).collect(Collectors.joining(", ")) + "]");
        return null;
    }

    public static MethodHandle getMethodHandle(Class<?> clazz, String method, Class<?>... params) {
        try {
            return LOOKUP.unreflect(getMethod(clazz, method, params));
        }
        catch (Throwable ex) {
            echoError(ex);
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
        return getFinalSetter(clazz, field, null);
    }

    public static MethodHandle getFinalSetter(Class<?> clazz, String field, Class expected) {
        Map<String, MethodHandle> map = cachedFieldSetters.computeIfAbsent(clazz, k -> new HashMap<>());
        MethodHandle result = map.get(field);
        if (result != null) {
            return result;
        }
        Field f = getFields(clazz).get(field);
        if (f == null) {
            echoError("Create get final setter for unknown field '" + field + "' (for class '" + clazz.getName() + "')");
            return null;
        }
        if (expected != null && f.getType() != expected) {
            echoError("[ReflectionHelper] field type mismatch in getFinalSetter: field '" + field + "' in class '" + clazz.getName() + "' returns type '" + f.getType().getCanonicalName() + "' but expected '" + expected.getCanonicalName() + "'");
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
                        echoError("Cannot create a setter for primitive type '" + f.getType().getName() + "'");
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
            echoError(ex);
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
                echoError(ex);
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
