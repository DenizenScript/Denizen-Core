package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.JavaReflectedObjectTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.ReflectionRefuse;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.function.Function;

public class ReflectionSetCommand extends AbstractCommand {

    public ReflectionSetCommand() {
        setName("reflectionset");
        setSyntax("reflectionset [object:<object>] [field:<name>] (value:<value>)");
        setRequiredArguments(2, 3);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name ReflectionSet
    // @Syntax reflectionset [object:<object>] [field:<name>] (value:<value>)
    // @Required 2
    // @Maximum 3
    // @Short Sets a field on an object to a given value, or null.
    // @Group core
    //
    // @Description
    // Give a <@link objecttype JavaReflectedObjectTag> as the object, a field name, and a value (or leave off for null) to set the value of a field on that object.
    //
    // Uses reflection to set, and so can bypass 'private' or 'final' field limits if permitted by config.
    //
    // If the value is fed as a general ObjectTag, automatic conversion will be attempted.
    // If automatic conversion is not possible, you must pass a <@link objecttype JavaReflectedObjectTag> with the appropriate type as the value.
    //
    // Requires config setting "Reflection.Allow set command".
    //
    // @Tags
    // <ObjectTag.reflected_internal_object>
    //
    // @Usage
    // Use to change Bukkit's reference to a world's environment to the_end.
    // - narrate <world[world].environment>
    // - define obj <world[world].reflected_internal_object>
    // - narrate <[obj].reflect_field[environment].interpret>
    // - reflectionset object:<[obj]> field:environment value:the_end
    // - narrate <world[world].environment>
    // - narrate <[obj].reflect_field[environment].interpret>
    // -->

    public static HashMap<Class<?>, Function<ObjectTag, Object>> typeConverters = new HashMap<>();

    static {
        typeConverters.put(byte.class, (o) -> (byte) o.asElement().asInt());
        typeConverters.put(Byte.class, (o) -> (byte) o.asElement().asInt());
        typeConverters.put(short.class, (o) -> (short) o.asElement().asInt());
        typeConverters.put(Short.class, (o) -> (short) o.asElement().asInt());
        typeConverters.put(int.class, (o) -> o.asElement().asInt());
        typeConverters.put(Integer.class, (o) -> o.asElement().asInt());
        typeConverters.put(long.class, (o) -> o.asElement().asLong());
        typeConverters.put(Long.class, (o) -> o.asElement().asLong());
        typeConverters.put(float.class, (o) -> o.asElement().asFloat());
        typeConverters.put(Float.class, (o) -> o.asElement().asFloat());
        typeConverters.put(double.class, (o) -> o.asElement().asDouble());
        typeConverters.put(Double.class, (o) -> o.asElement().asDouble());
        typeConverters.put(boolean.class, (o) -> o.asElement().asBoolean());
        typeConverters.put(Boolean.class, (o) -> o.asElement().asBoolean());
        typeConverters.put(String.class, (o) -> o.asElement().asString());
        // TODO: what other types should be tracked here?
    }

    public static Object convertObjectTypeFor(Class<?> type, ObjectTag value) {
        if (value == null) {
            return null;
        }
        if (value instanceof JavaReflectedObjectTag) {
            return ((JavaReflectedObjectTag) value).object;
        }
        Object javaForm = value.getJavaObject();
        if (javaForm != null && type.isAssignableFrom(javaForm.getClass())) {
            return javaForm;
        }
        Function<ObjectTag, Object> converter = typeConverters.get(type);
        if (converter != null) {
            Object res = converter.apply(value);
            if (res != null) {
                return res;
            }
        }
        if (type.isEnum()) {
            Object enumVal = value.asElement().asEnum((Class<? extends Enum>) type);
            if (enumVal == null) {
                Debug.echoError("Cannot convert value '" + value + "' to type '" + type.getName() + "' - value is not recognized as an enum constant.");
            }
            return enumVal;
        }
        Debug.echoError("Cannot convert value '" + value + "' to type '" + type.getName() + "' - no known conversion registered.");
        return null;
    }

    public static void autoExecute(
            @ArgPrefixed @ArgName("object") JavaReflectedObjectTag object,
            @ArgPrefixed @ArgName("field") String fieldName,
            @ArgPrefixed @ArgName("value") @ArgDefaultNull ObjectTag value) {
        if (!CoreConfiguration.allowReflectionSet) {
            Debug.echoError("The 'reflectionset' command is disabled in the Denizen config.");
            return;
        }
        Class<?> clazz;
        Field field = null;
        if (object.object instanceof Class) {
            clazz = (Class<?>) object.object;
            field = ReflectionHelper.getFields(clazz).get(fieldName);
            if (field == null) {
                Debug.echoError("Field '" + fieldName + "' does not exist in class: " + ((Class<?>) object.object).getName());
                return;
            }
        }
        else {
            clazz = object.object.getClass();
            while (field == null && clazz != Object.class) {
                field = ReflectionHelper.getFields(clazz).get(fieldName);
                if (field == null) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (field == null) {
                Debug.echoError("Field '" + fieldName + "' does not exist in class: " + object.object.getClass().getName());
                return;
            }
        }
        if (field.isAnnotationPresent(ReflectionRefuse.class) || field.getType().isAnnotationPresent(ReflectionRefuse.class)) {
            Debug.echoError("Cannot ReflectionSet field '" + field + "' because it is marked for reflection refusal.");
            return;
        }
        if (!Modifier.isPublic(field.getModifiers()) && !CoreConfiguration.allowReflectionSetPrivate) { // Intentionally use !isPublic rather than isPrivate to account for other limits like protected or package-local
            Debug.echoError("Cannot ReflectionSet field '" + field + "' because it is private, and modifying private fields is disabled in the Denizen config.");
            return;
        }
        if (Modifier.isFinal(field.getModifiers()) && !CoreConfiguration.allowReflectionSetFinal) {
            Debug.echoError("Cannot ReflectionSet field '" + field + "' because it is final, and modifying private fields is disabled in the Denizen config.");
            return;
        }
        Object setVal = convertObjectTypeFor(field.getType(), value);
        if (setVal == null && value != null) {
            return;
        }
        MethodHandle handle = ReflectionHelper.getFinalSetter(clazz, field.getName());
        try {
            if (object.object instanceof Class) {
                handle.invoke(setVal);
            }
            else {
                handle.invoke(object.object, setVal);
            }
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
        }
    }
}
