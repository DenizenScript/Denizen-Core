package com.denizenscript.denizencore.utilities.codegen;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.objectweb.asm.*;

import java.lang.reflect.Method;

public class TagNamer {

    public static long tagsGenerated = 0;

    public static final String OBJECT_INTERFACE_PATH = Type.getInternalName(TagRunnable.ObjectInterface.class);
    public static String OBJECT_INTERFACE_DESCRIPTOR = "L" + OBJECT_INTERFACE_PATH + ";";
    public static final Method OBJECT_INTERFACE_RUN_METHOD = ReflectionHelper.getMethod(TagRunnable.ObjectInterface.class, "run", Attribute.class, ObjectTag.class);
    public static final String OBJECT_INTERFACE_RUN_DESCRIPTOR = Type.getMethodDescriptor(OBJECT_INTERFACE_RUN_METHOD);

    public static <T extends ObjectTag, R extends ObjectTag> TagRunnable.ObjectInterface<T, R> nameTagInterface(Class<T> mainType, String tagName, TagRunnable.ObjectInterface<T, R> tag) {
        String fullTagName = mainType.getSimpleName() + "_" + tagName;
        return (TagRunnable.ObjectInterface<T, R>) nameInternal(fullTagName, tag, OBJECT_INTERFACE_PATH, OBJECT_INTERFACE_DESCRIPTOR, true, OBJECT_INTERFACE_RUN_DESCRIPTOR);
    }

    public static final String BASE_INTERFACE_PATH = Type.getInternalName(TagRunnable.BaseInterface.class);
    public static String BASE_INTERFACE_DESCRIPTOR = "L" + BASE_INTERFACE_PATH + ";";
    public static final Method BASE_NTERFACE_RUN_METHOD = ReflectionHelper.getMethod(TagRunnable.BaseInterface.class, "run", Attribute.class);
    public static final String BASE_INTERFACE_RUN_DESCRIPTOR = Type.getMethodDescriptor(BASE_NTERFACE_RUN_METHOD);

    public static <R extends ObjectTag> TagRunnable.BaseInterface<R> nameBaseInterface(String tagName, TagRunnable.BaseInterface<R> tag) {
        return (TagRunnable.BaseInterface<R>) nameInternal("base_" + tagName, tag, BASE_INTERFACE_PATH, BASE_INTERFACE_DESCRIPTOR, false, BASE_INTERFACE_RUN_DESCRIPTOR);
    }

    public static Object nameInternal(String fullTagName, Object tag, String typePath, String typeDescription, boolean hasObject, String runDescriptor) {
        try {
            // ====== Gen class ======
            String className = CodeGenUtil.TAG_GEN_PACKAGE + "Tag" + (tagsGenerated++) + "_" + CodeGenUtil.cleanName(fullTagName);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", new String[] { typePath });
            cw.visitSource("GENERATED_TAG", null);
            cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "runnable", typeDescription, null, null);
            MethodGenerator.genDefaultConstructor(cw, className);
            // ====== Gen 'staticRun' method ======
            {
                MethodGenerator gen = MethodGenerator.generateMethod(className, cw, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "staticRun", runDescriptor);
                MethodGenerator.Local attributeLocal = gen.addLocal("attribute", Attribute.class);
                MethodGenerator.Local objectLocal = gen.addLocal("object", ObjectTag.class);
                gen.loadStaticField(className, "runnable", typeDescription);
                gen.loadLocal(attributeLocal);
                if (hasObject) {
                    gen.loadLocal(objectLocal);
                }
                gen.invokeInterface(typePath, "run", runDescriptor);
                gen.returnValue(ObjectTag.class);
                gen.end();
            }
            // ====== Gen 'run' method ======
            {
                MethodGenerator gen = MethodGenerator.generateMethod(className, cw, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "run", runDescriptor);
                MethodGenerator.Local attributeLocal = gen.addLocal("attribute", Attribute.class);
                MethodGenerator.Local objectLocal = gen.addLocal("object", ObjectTag.class);
                gen.loadStaticField(className, "runnable", typeDescription);
                gen.loadLocal(attributeLocal);
                if (hasObject) {
                    gen.loadLocal(objectLocal);
                }
                gen.invokeInterface(typePath, "run", runDescriptor);
                gen.returnValue(ObjectTag.class);
                gen.end();
            }
            // ====== Compile and return ======
            cw.visitEnd();
            byte[] compiled = cw.toByteArray();
            Class<?> generatedClass = CodeGenUtil.loader.define(className.replace('/', '.'), compiled);
            Object result = generatedClass.getConstructors()[0].newInstance();
            ReflectionHelper.setFieldValue(generatedClass, "runnable", result, tag);
            return result;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return tag;
        }
    }
}
