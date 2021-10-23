package com.denizenscript.denizencore.utilities.codegen;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.objectweb.asm.*;

import java.lang.reflect.Method;

public class TagNamer {

    public static final String TAG_GEN_PACKAGE = "com/denizenscript/tag_gen/";
    public static final String OBJECT_INTERFACE_PATH = Type.getInternalName(TagRunnable.ObjectInterface.class);
    public static final String OBJECT_TAG_PATH = Type.getInternalName(ObjectTag.class);
    public static final String[] OBJECT_INTERFACE_TYPE = new String[] { OBJECT_INTERFACE_PATH };
    public static String OBJECT_INTERFACE_DESCRIPTOR = "L" + OBJECT_INTERFACE_PATH + ";";
    public static final String ATTRIBUTE_LOCAL_TYPE = "L" + Type.getInternalName(Attribute.class) + ";";
    public static final String OBJECT_LOCAL_TYPE = "L" + OBJECT_TAG_PATH + ";";
    public static final Method OBJECTINTERFACE_RUN_METHOD = ReflectionHelper.getMethod(TagRunnable.ObjectInterface.class, "run", Attribute.class, ObjectTag.class);
    public static final String OBJECTINTERFACE_RUN_DESCRIPTOR = Type.getMethodDescriptor(OBJECTINTERFACE_RUN_METHOD);

    public static DynamicClassLoader loader = new DynamicClassLoader();

    public static AsciiMatcher TAG_NAME_PERMITTED = new AsciiMatcher(AsciiMatcher.LETTERS_LOWER + AsciiMatcher.LETTERS_UPPER + AsciiMatcher.DIGITS + "_");

    public static long tagsGenerated = 0;

    public static <T extends ObjectTag, R extends ObjectTag> TagRunnable.ObjectInterface<T, R> nameTagInterface(Class<T> mainType, String tagName, TagRunnable.ObjectInterface<T, R> tag) {
        try {
            // ====== Gen class ======
            String className = TAG_GEN_PACKAGE + "Tag" + (tagsGenerated++) + "_" + mainType.getSimpleName() + "_" + TAG_NAME_PERMITTED.trimToMatches(tagName);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, className, null, "java/lang/Object", OBJECT_INTERFACE_TYPE);
            cw.visitSource("GENERATED_TAG", null);
            cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "runnable", OBJECT_INTERFACE_DESCRIPTOR, null, null);
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
            // ====== Gen 'run' method ======
            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "run", OBJECTINTERFACE_RUN_DESCRIPTOR, null, null);
                mv.visitCode();
                Label startLabel = new Label();
                mv.visitLabel(startLabel);
                mv.visitLineNumber(1, startLabel);
                mv.visitFieldInsn(Opcodes.GETSTATIC, className, "runnable", OBJECT_INTERFACE_DESCRIPTOR);
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, OBJECT_INTERFACE_PATH, "run", OBJECTINTERFACE_RUN_DESCRIPTOR, true);
                mv.visitLocalVariable("attribute", ATTRIBUTE_LOCAL_TYPE, null, startLabel, startLabel, 0);
                mv.visitLocalVariable("object", OBJECT_LOCAL_TYPE, null, startLabel, startLabel, 1);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            // ====== Compile and return ======
            cw.visitEnd();
            byte[] compiled = cw.toByteArray();
            Class<?> generatedClass = loader.define(className.replace('/', '.'), compiled);
            TagRunnable.ObjectInterface<T, R> result = (TagRunnable.ObjectInterface<T, R>) generatedClass.getConstructors()[0].newInstance();
            ReflectionHelper.setFieldValue(generatedClass, "runnable", result, tag);
            return result;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return tag;
        }
    }

    public static class DynamicClassLoader extends ClassLoader {
        public Class<?> define(String className, byte[] bytecode) {
            Class<?> clazz = super.defineClass(className, bytecode, 0, bytecode.length);
            resolveClass(clazz);
            DenizenCore.getImplementation().saveClassToLoader(clazz);
            return clazz;
        }
        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            try {
                return Class.forName(name);
            }
            catch (ClassNotFoundException ex) {
                Debug.echoError(ex);
                return super.findClass(name);
            }
        }
    }
}
