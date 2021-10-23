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
            String className = CodeGenUtil.TAG_GEN_PACKAGE + "Tag" + (tagsGenerated++) + "_" + CodeGenUtil.TAG_NAME_PERMITTED.trimToMatches(fullTagName);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", new String[] { typePath });
            cw.visitSource("GENERATED_TAG", null);
            cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "runnable", typeDescription, null, null);
            // ====== Gen constructor ======
            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                mv.visitCode();
                Label startLabel = new Label();
                mv.visitLabel(startLabel);
                mv.visitLineNumber(0, startLabel);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitLocalVariable("this", "L" + className + ";", null, startLabel, startLabel, 0);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            // ====== Gen 'staticRun' method ======
            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "staticRun", runDescriptor, null, null);
                mv.visitCode();
                Label startLabel = new Label();
                mv.visitLabel(startLabel);
                mv.visitLineNumber(1, startLabel);
                mv.visitFieldInsn(Opcodes.GETSTATIC, className, "runnable", typeDescription);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                if (hasObject) {
                    mv.visitVarInsn(Opcodes.ALOAD, 1);
                }
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, typePath, "run", runDescriptor, true);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLocalVariable("attribute", CodeGenUtil.ATTRIBUTE_LOCAL_TYPE, null, startLabel, startLabel, 0);
                if (hasObject) {
                    mv.visitLocalVariable("object", CodeGenUtil.OBJECT_LOCAL_TYPE, null, startLabel, startLabel, 1);
                }
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            // ====== Gen 'run' method ======
            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "run", runDescriptor, null, null);
                mv.visitCode();
                Label startLabel = new Label();
                mv.visitLabel(startLabel);
                mv.visitLineNumber(1, startLabel);
                mv.visitFieldInsn(Opcodes.GETSTATIC, className, "runnable", typeDescription);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                if (hasObject) {
                    mv.visitVarInsn(Opcodes.ALOAD, 2);
                }
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, typePath, "run", runDescriptor, true);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLocalVariable("attribute", CodeGenUtil.ATTRIBUTE_LOCAL_TYPE, null, startLabel, startLabel, 0);
                if (hasObject) {
                    mv.visitLocalVariable("object", CodeGenUtil.OBJECT_LOCAL_TYPE, null, startLabel, startLabel, 1);
                }
                mv.visitMaxs(0, 0);
                mv.visitEnd();
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
