package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.Debug;
import sun.misc.Unsafe;

import java.lang.invoke.*;
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

    private static final Map<Class, FieldCache> cachedFields = new HashMap<>();

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
        FieldCache cache = getFields(clazz);
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

    public static class FieldCache {

        public Class<?> clazz;
        public Field[] allFields;
        public Map<String, Field> fieldCache = new HashMap<>();

        public FieldCache(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Field[] getAllFields() {
            if (allFields == null) {
                allFields = clazz.getDeclaredFields();
                for (Field field : allFields) {
                    field.setAccessible(true);
                }
            }
            return allFields;
        }

        public Field getFirstOfType(Class fieldClazz) {
            for (Field f : getAllFields()) {
                if (f.getType().equals(fieldClazz)) {
                    return f;
                }
            }
            echoError("Reflection field missing - Tried to find field of type '" + fieldClazz.getCanonicalName() + "' of class '" + clazz.getCanonicalName() + "'.");
            return null;
        }

        public Field get(Object name) {
            Field f = getNoCheck(name.toString());
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
            return fieldCache.computeIfAbsent(name, fieldName -> {
                try {
                    Field found = clazz.getDeclaredField(fieldName);
                    found.setAccessible(true);
                    return found;
                }
                catch (NoSuchFieldException ignored) {
                    return null;
                }
            });
        }
    }

    public static FieldCache getFields(Class clazz) {
        return cachedFields.computeIfAbsent(clazz, FieldCache::new);
    }

    public static Method getMethod(Class<?> clazz, String method, Class<?>... params) {
        Method f = null;
        try {
            f = clazz.getDeclaredMethod(method, params);
        }
        catch (Exception ex) {
            echoError(ex);
        }
        if (f == null) {
            echoError("Reflection method missing - Tried to read method '" + method + "' of class '" + clazz.getCanonicalName() + "'.");
            return null;
        }
        f.setAccessible(true);
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

    private static void enableUnsafe() {
        if (UNSAFE == null) {
            try {
                UNSAFE = (Unsafe) getFields(sun.misc.Unsafe.class).get("theUnsafe").get(null);
            }
            catch (Throwable ex) {
                echoError(ex);
            }
        }
    }

    private static void validateUnsafe() {
        if (!haveLoadedUnsafeMethods) {
            haveLoadedUnsafeMethods = true;
            enableUnsafe();
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

    public static void giveReflectiveAccess(Class<?> targetClass, Class<?> ourClass) {
        try {
            if (GET_MODULE == null) {
                enableUnsafe();
                Class<?> module = Class.forName("java.lang.Module");
                GET_MODULE = Class.class.getMethod("getModule");
                ADD_OPENS = module.getDeclaredMethod("implAddOpens", String.class, module);
                UNSAFE.putBoolean(ADD_OPENS, OVERRIDE_OFFSET, true);
            }
            ADD_OPENS.invoke(GET_MODULE.invoke(targetClass), targetClass.getPackage().getName(), GET_MODULE.invoke(ourClass));
        }
        catch (Exception ex) {
            echoError(ex);
        }
    }

    public static <T> T getStaticLambda(Class<T> lambdaType, String lambdaName, Class<?> type, String methodName) {
        Method targetMethod = Arrays.stream(type.getDeclaredMethods()).filter(m -> m.getName().equals(methodName)).findFirst().get();
        return getStaticLambda(lambdaType, lambdaName, type, methodName, targetMethod);
    }

    public static <T> T getStaticLambda(Class<T> lambdaType, String lambdaName, Class<?> type, String methodName, Method targetMethod) {
        try {
            Method funcMethod = Arrays.stream(lambdaType.getDeclaredMethods()).filter(m -> m.getName().equals(lambdaName)).findFirst().get();
            MethodType funcMethodType = MethodType.methodType(funcMethod.getReturnType(), funcMethod.getParameterTypes()).unwrap();
            MethodType targetMethodType = MethodType.methodType(targetMethod.getReturnType(), targetMethod.getParameterTypes()).unwrap();
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            CallSite site = LambdaMetafactory.metafactory(lookup, lambdaName, MethodType.methodType(lambdaType), funcMethodType, lookup.findStatic(type, methodName, targetMethodType), targetMethodType);
            return (T) site.getTarget().invoke();
        }
        catch (Throwable ex) {
            echoError(ex);
        }
        return null;
    }

    static {
        giveReflectiveAccess(Field.class, ReflectionHelper.class);
        MODIFIERS_FIELD = getFields(Field.class).getNoCheck("modifiers");
    }

    private static final long OVERRIDE_OFFSET = 12; // Field offset of 'override' AKA 'setAccessible'. May change arbitrarily from JDK updates. Required for reflection, so we can't use reflection to read it.
    private static Method ADD_OPENS;
    private static Method GET_MODULE;
    private static MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static Field MODIFIERS_FIELD;
    private static Unsafe UNSAFE;
    private static boolean haveLoadedUnsafeMethods = false;
    private static MethodHandle UNSAFE_FIELD_OFFSET, UNSAFE_PUT_OBJECT, UNSAFE_PUT_FLOAT, UNSAFE_PUT_DOUBLE, UNSAFE_PUT_INT, UNSAFE_PUT_LONG, UNSAFE_PUT_BOOL, UNSAFE_STATIC_FIELD_OFFSET;
}
