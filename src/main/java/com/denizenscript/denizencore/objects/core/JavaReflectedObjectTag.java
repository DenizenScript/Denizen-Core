package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.*;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

public class JavaReflectedObjectTag implements ObjectTag {

    // <--[ObjectType]
    // @name JavaReflectedObjectTag
    // @prefix reflected
    // @base ElementTag
    // @ExampleTagBase reflected[some_reflected_obj]
    // @ExampleValues <reflected[some_reflected_obj]>
    // @ExampleForReturns
    // - narrate <%VALUE%.full_class_name>
    // @format
    // The identity format for JavaReflectedObjectTag is a random UUID that is associated with a temporary lookup to reduce reparsing risk.
    //
    // @description
    // JavaReflectedObjectTag represent raw Java objects for reflection-based interactions in Denizen.
    // This is only useful in certain interop edge cases, and should usually be avoided.
    // They have no persistent identity, and instead only use a temporary generated UUID for the identity to allow reconstruction by lookup.
    // Two different JavaReflectedObjectTag might simultaneously refer to the same underlying Java object, despite having different IDs.
    // A Java object should not be retained in flags or other long term storage, as they will necessarily become invalid quickly.
    //
    // -->

    public static AsciiMatcher VALID_UUID_SYMBOLS = new AsciiMatcher(AsciiMatcher.DIGITS + "abcdefABCDEF-");

    @Fetchable("reflected")
    public static JavaReflectedObjectTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (!string.startsWith("reflected@")) {
            return null;
        }
        String id = string.substring("reflected@".length());
        if (!id.contains("-") || id.length() != 36 || !VALID_UUID_SYMBOLS.isOnlyMatches(id)) {
            return null;
        }
        clearOldRefs();
        try {
            UUID uuid = UUID.fromString(id);
            return persistedReferences.get(uuid);
        }
        catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static boolean matches(String string) {
        return string.startsWith("reflected@");
    }

    public static HashMap<UUID, JavaReflectedObjectTag> persistedReferences = new HashMap<>();

    private static ArrayList<UUID> toRemoveHelper = new ArrayList<>();

    public static long lastCleared;

    public static long clearRateSeconds = 60;

    public static void clearOldRefs() {
        long time = CoreUtilities.monotonicMillis();
        if (lastCleared + clearRateSeconds * 500 > time) {
            return;
        }
        lastCleared = time;
        for (JavaReflectedObjectTag ref : persistedReferences.values()) {
            if (ref.lastIdentified + clearRateSeconds * 1000 < time) {
                toRemoveHelper.add(ref.id);
            }
        }
        for (UUID toRemove : toRemoveHelper) {
            persistedReferences.remove(toRemove);
        }
    }

    public Object object;
    public UUID id;
    public long lastIdentified;

    public JavaReflectedObjectTag(Object object) {
        if (object == null) {
            throw new NullPointerException();
        }
        if (object.getClass().isAnnotationPresent(ReflectionRefuse.class)) {
            throw new RuntimeException("Cannot reflect into object of class " + object.getClass().getName() + " as it has a reflection refusal marked.");
        }
        this.object = object;
        id = UUID.randomUUID();
    }

    public void persist() {
        clearOldRefs();
        lastIdentified = CoreUtilities.monotonicMillis();
        persistedReferences.put(id, this);
    }

    private String prefix = "Reflected";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String debuggable() {
        return "<LG>reflected@<GR>" + id + " <LG>(<GR>" + object + "<LG>)";
    }

    @Override
    public String identify() {
        persist();
        return "reflected@" + id;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public Object getJavaObject() {
        return object;
    }

    public static void register() {

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.simple_class_name>
        // @returns ElementTag
        // @description
        // Returns the simple/short class name of the reflected object, such as "JavaReflectedObjectTag".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "simple_class_name", (attribute, object) -> {
            return new ElementTag(DebugInternals.getClassNameOpti(object.object.getClass()));
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.super_classes>
        // @returns ListTag
        // @description
        // Returns the list of super classes of this object, in order, starting with its primary class's super, down to Object, as a ListTag of full class names.
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "super_classes", (attribute, object) -> {
            ListTag classes = new ListTag();
            Class<?> clazz = object.object.getClass();
            while (clazz != Object.class) {
                clazz = clazz.getSuperclass();
                classes.add(clazz.getName());
            }
            return classes;
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.full_class_name>
        // @returns ElementTag
        // @description
        // Returns the full class name of the reflected object with package info included, such as "com.denizenscript.denizencore.objects.core.JavaReflectedObjectTag"
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "full_class_name", (attribute, object) -> {
            return new ElementTag(object.object.getClass().getName());
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.interpret>
        // @returns ObjectTag
        // @description
        // Interprets the object to Denizen object format.
        // -->
        tagProcessor.registerTag(ObjectTag.class, "interpret", (attribute, object) -> {
            if (!CoreConfiguration.allowReflectedCoreMethods) {
                attribute.echoError("Core-reflected-method-calling tags are forbidden by current Denizen config.");
                return null;
            }
            return CoreUtilities.objectToTagForm(object.object, attribute.context);
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.to_string>
        // @returns ElementTag
        // @description
        // Returns the result of the Java toString() call on the object.
        // -->
        tagProcessor.registerTag(ObjectTag.class, "to_string", (attribute, object) -> {
            if (!CoreConfiguration.allowReflectedCoreMethods) {
                attribute.echoError("Core-reflected-method-calling tags are forbidden by current Denizen config.");
                return null;
            }
            return new ElementTag(object.object.toString());
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.hash_code>
        // @returns ElementTag(Number)
        // @description
        // Returns the result of the Java hashCode() call on the object.
        // -->
        tagProcessor.registerTag(ObjectTag.class, "hash_code", (attribute, object) -> {
            if (!CoreConfiguration.allowReflectedCoreMethods) {
                attribute.echoError("Core-reflected-method-calling tags are forbidden by current Denizen config.");
                return null;
            }
            return new ElementTag(object.object.hashCode());
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.java_equals[<reflected_object>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns the result of the Java equals(<object>) call on the object with another JavaReflectedObjectTag.
        // -->
        tagProcessor.registerTag(ObjectTag.class, JavaReflectedObjectTag.class, "java_equals", (attribute, object, compareTo) -> {
            if (!CoreConfiguration.allowReflectedCoreMethods) {
                attribute.echoError("Core-reflected-method-calling tags are forbidden by current Denizen config.");
                return null;
            }
            return new ElementTag(object.object.equals(compareTo.object));
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.memory_equals[<reflected_object>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns the result of the Java "==" exact memory equality call on the object with another JavaReflectedObjectTag.
        // -->
        tagProcessor.registerStaticTag(ObjectTag.class, JavaReflectedObjectTag.class, "memory_equals", (attribute, object, compareTo) -> {
            if (!CoreConfiguration.allowReflectedCoreMethods) {
                attribute.echoError("Core-reflected-method-calling tags are forbidden by current Denizen config.");
                return null;
            }
            return new ElementTag(object.object == compareTo.object);
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.field_names>
        // @returns ListTag
        // @description
        // Returns a list of all field names on the object (in its current class or any super classes).
        // -->
        tagProcessor.registerStaticTag(ListTag.class, "field_names", (attribute, object) -> {
            if (!CoreConfiguration.allowReflectionFieldReads) {
                attribute.echoError("Field-reading tags are forbidden by current Denizen config.");
                return null;
            }
            ListTag fields = new ListTag();
            Class<?> clazz = object.object.getClass();
            while (clazz != Object.class) {
                fields.addAll(ReflectionHelper.getFields(clazz).keySet());
                clazz = clazz.getSuperclass();
            }
            return fields;
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.field_is_public[<name>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the field for the given name on this object is a public field.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "field_is_public", (attribute, object, fieldName) -> {
            if (denyFieldTag(attribute)) {
                return null;
            }
            Field f = object.getFieldForTag(attribute::echoError, fieldName.asString());
            if (f == null) {
                return null;
            }
            return new ElementTag(Modifier.isPublic(f.getModifiers()));
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.field_is_private[<name>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the field for the given name on this object is a private field.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "field_is_private", (attribute, object, fieldName) -> {
            if (denyFieldTag(attribute)) {
                return null;
            }
            Field f = object.getFieldForTag(attribute::echoError, fieldName.asString());
            if (f == null) {
                return null;
            }
            return new ElementTag(Modifier.isPrivate(f.getModifiers()));
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.field_is_protected[<name>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the field for the given name on this object is a protected field.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "field_is_protected", (attribute, object, fieldName) -> {
            if (denyFieldTag(attribute)) {
                return null;
            }
            Field f = object.getFieldForTag(attribute::echoError, fieldName.asString());
            if (f == null) {
                return null;
            }
            return new ElementTag(Modifier.isProtected(f.getModifiers()));
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.field_is_static[<name>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the field for the given name on this object is a static field.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "field_is_static", (attribute, object, fieldName) -> {
            if (denyFieldTag(attribute)) {
                return null;
            }
            Field f = object.getFieldForTag(attribute::echoError, fieldName.asString());
            if (f == null) {
                return null;
            }
            return new ElementTag(Modifier.isStatic(f.getModifiers()));
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.field_is_final[<name>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the field for the given name on this object is a final field.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "field_is_final", (attribute, object, fieldName) -> {
            if (denyFieldTag(attribute)) {
                return null;
            }
            Field f = object.getFieldForTag(attribute::echoError, fieldName.asString());
            if (f == null) {
                return null;
            }
            return new ElementTag(Modifier.isFinal(f.getModifiers()));
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.field_class_type[<name>]>
        // @returns ElementTag
        // @description
        // Returns the full class name of the field of the given name on this object.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ElementTag.class, "field_class_type", (attribute, object, fieldName) -> {
            if (denyFieldTag(attribute)) {
                return null;
            }
            Field f = object.getFieldForTag(attribute::echoError, fieldName.asString());
            if (f == null) {
                return null;
            }
            return new ElementTag(f.getType().getName());
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.read_field[<name>]>
        // @returns ObjectTag
        // @description
        // Reads the field of the given name on the object and returns the value in its Denizen-valid format.
        // See also <@link tag JavaReflectedObjectTag.reflect_field>
        // -->
        tagProcessor.registerTag(ObjectTag.class, ElementTag.class, "read_field", (attribute, object, fieldName) -> {
            if (denyFieldTag(attribute)) {
                return null;
            }
            Object val = object.readFieldForTag(attribute, fieldName.asString());
            if (val == null) {
                return null;
            }
            return CoreUtilities.objectToTagForm(val, attribute.context, false, false, false);
        });

        // <--[tag]
        // @attribute <JavaReflectedObjectTag.reflect_field[<name>]>
        // @returns JavaReflectedObjectTag
        // @description
        // Reads the field of the given name on the object and returns the value as another reflected tag.
        // See also <@link tag JavaReflectedObjectTag.read_field>
        // -->
        tagProcessor.registerTag(JavaReflectedObjectTag.class, ElementTag.class, "reflect_field", (attribute, object, fieldName) -> {
            if (denyFieldTag(attribute)) {
                return null;
            }
            Object val = object.readFieldForTag(attribute, fieldName.asString());
            if (val == null) {
                return null;
            }
            return new JavaReflectedObjectTag(val);
        });
    }

    public Field getFieldForTag(Consumer<String> error, String fieldName) {
        Field field = null;
        if (object instanceof Class) {
            field = ReflectionHelper.getFields((Class<?>) object).getNoCheck(fieldName);
            if (field == null) {
                error.accept("Field '" + fieldName + "' does not exist in class: " + ((Class<?>) object).getName());
            }
        }
        else {
            Class<?> clazz = object.getClass();
            while (field == null && clazz != Object.class) {
                field = ReflectionHelper.getFields(clazz).getNoCheck(fieldName);
                if (field == null) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (field == null) {
                error.accept("Field '" + fieldName + "' does not exist in class: " + object.getClass().getName());
            }
        }
        return field;
    }

    public static boolean denyFieldTag(Attribute attribute) {
        if (!CoreConfiguration.allowReflectionFieldReads) {
            attribute.echoError("Field-reading tags are forbidden by current Denizen config.");
            return true;
        }
        if (!attribute.hasParam()) {
            return true;
        }
        return false;
    }

    public Object readFieldForTag(Attribute attribute, String fieldName) {
        Field field = getFieldForTag(attribute::echoError, fieldName);
        if (field == null) {
            return null;
        }
        if (field.isAnnotationPresent(ReflectionRefuse.class) || field.getType().isAnnotationPresent(ReflectionRefuse.class)) {
            attribute.echoError("Cannot read field '" + field.getName() + "' in class '" + field.getDeclaringClass().getName() + "' because it is marked for reflection refusal.");
            return null;
        }
        if (object instanceof Class && !Modifier.isStatic(field.getModifiers())) {
            attribute.echoError("Cannot read field '" + field.getName() + "' in class '" + field.getDeclaringClass().getName() + "' because the field is not static.");
            return null;
        }
        return ReflectionHelper.getFieldValue(field.getDeclaringClass(), fieldName, Modifier.isStatic(field.getModifiers()) || object instanceof Class ? null : object);
    }

    public static ObjectTagProcessor<JavaReflectedObjectTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }
}
