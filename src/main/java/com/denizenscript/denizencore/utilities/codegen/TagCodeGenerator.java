package com.denizenscript.denizencore.utilities.codegen;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.objectweb.asm.*;

public class TagCodeGenerator {

    public static long totalGenerated = 0;

    private static final int LOCAL_ATTRIBUTE = 1, LOCAL_CURRENTOBJECT = 2;

    public static String FULFILL_ONE_RUN_DESCRIPTOR = "(L" + CodeGenUtil.OBJECT_TAG_PATH + ";)V";

    public static TagRunnable.BaseInterface<? extends ObjectTag> generatePartialTag(ReplaceableTagEvent.ReferenceData data) {
        if (data == null || data.tagBase == null || data.tagBase.baseForm == null || data.attribs.attributes.length <= 1) {
            return null;
        }
        if (data.compiledStart != null) {
            return data.compiledStart;
        }
        Attribute.AttributeComponent[] pieces = data.attribs.attributes;
        int applicableParts = 0;
        for (int i = 1; i < pieces.length; i++) {
            ObjectTagProcessor.TagData piece = pieces[i].data;
            if (piece == null || piece.runner == null) {
                break;
            }
            applicableParts++;
        }
        if (applicableParts == 0) {
            return null;
        }
        try {
            // ====== Gen class ======
            String tagFullName = CodeGenUtil.TAG_NAME_PERMITTED.trimToMatches(data.rawTag.replace('.', '_'));
            if (tagFullName.length() > 50) {
                tagFullName = tagFullName.substring(0, 50);
            }
            String className = CodeGenUtil.TAG_GEN_PACKAGE + "UserTag" + (totalGenerated++) + "_" + tagFullName;
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", new String[] {TagNamer.BASE_INTERFACE_PATH});
            cw.visitSource("GENERATED_TAG", null);
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
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "run", TagNamer.BASE_INTERFACE_RUN_DESCRIPTOR, null, null);
                mv.visitCode();
                Label returnLabel = new Label();
                Label failLabel = new Label();
                // Run the initial tag base
                Label startLabel = new Label();
                mv.visitLabel(startLabel);
                int line = 1;
                mv.visitLineNumber(line++, startLabel);
                mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ATTRIBUTE);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(data.tagBase.baseForm.getClass()), "staticRun", TagNamer.BASE_INTERFACE_RUN_DESCRIPTOR, false);
                mv.visitVarInsn(Opcodes.ASTORE, LOCAL_CURRENTOBJECT);
                // If tag base returned null, fail
                mv.visitVarInsn(Opcodes.ALOAD, LOCAL_CURRENTOBJECT);
                mv.visitJumpInsn(Opcodes.IFNULL, failLabel);
                // otherwise, fulfill one
                mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ATTRIBUTE);
                mv.visitVarInsn(Opcodes.ALOAD, LOCAL_CURRENTOBJECT);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CodeGenUtil.ATTRIBUTE_TYPE_PATH, "fulfillOne", FULFILL_ONE_RUN_DESCRIPTOR, false);
                for (int i = 1; i < pieces.length; i++) {
                    ObjectTagProcessor.TagData piece = pieces[i].data;
                    if (piece == null || piece.runner == null) {
                        break;
                    }
                    // Run sub-tag
                    Label methodLabel = new Label();
                    mv.visitLabel(methodLabel);
                    mv.visitLineNumber(line++, methodLabel);
                    mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ATTRIBUTE);
                    mv.visitVarInsn(Opcodes.ALOAD, LOCAL_CURRENTOBJECT);
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(piece.runner.getClass()), "staticRun", TagNamer.OBJECT_INTERFACE_RUN_DESCRIPTOR, false);
                    mv.visitVarInsn(Opcodes.ASTORE, LOCAL_CURRENTOBJECT);
                    // If null return, fail
                    Label checkLabel1 = new Label();
                    mv.visitLabel(checkLabel1);
                    mv.visitLineNumber(line++, checkLabel1);
                    mv.visitVarInsn(Opcodes.ALOAD, LOCAL_CURRENTOBJECT);
                    mv.visitJumpInsn(Opcodes.IFNULL, failLabel);
                    // otherwise, fulfill one
                    Label fulfillLabel = new Label();
                    mv.visitLabel(fulfillLabel);
                    mv.visitLineNumber(line++, fulfillLabel);
                    mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ATTRIBUTE);
                    mv.visitVarInsn(Opcodes.ALOAD, LOCAL_CURRENTOBJECT);
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CodeGenUtil.ATTRIBUTE_TYPE_PATH, "fulfillOne", FULFILL_ONE_RUN_DESCRIPTOR, false);
                    // If manual fulfill happened, a legacy multi-part tag handler was used, so code gen is no longer trustworthy - exit and let legacy handler run
                    Label checkLabel2 = new Label();
                    mv.visitLabel(checkLabel2);
                    mv.visitLineNumber(line++, checkLabel2);
                    mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ATTRIBUTE);
                    mv.visitFieldInsn(Opcodes.GETFIELD, CodeGenUtil.ATTRIBUTE_TYPE_PATH, "hadManualFulfill", "Z");
                    mv.visitJumpInsn(Opcodes.IFNE, returnLabel);
                }
                mv.visitJumpInsn(Opcodes.GOTO, returnLabel);
                mv.visitLabel(failLabel);
                mv.visitLineNumber(line++, failLabel);
                mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ATTRIBUTE);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CodeGenUtil.ATTRIBUTE_TYPE_PATH, "trackLastTagFailure", "()V", false);
                mv.visitLabel(returnLabel);
                mv.visitLineNumber(line, returnLabel);
                mv.visitVarInsn(Opcodes.ALOAD, LOCAL_CURRENTOBJECT);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLocalVariable("attribute", CodeGenUtil.ATTRIBUTE_LOCAL_TYPE, null, startLabel, startLabel, LOCAL_ATTRIBUTE);
                mv.visitLocalVariable("currentObject", CodeGenUtil.OBJECT_LOCAL_TYPE, null, startLabel, startLabel, LOCAL_CURRENTOBJECT);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            // ====== Compile and return ======
            cw.visitEnd();
            byte[] compiled = cw.toByteArray();
            Class<?> generatedClass = CodeGenUtil.loader.define(className.replace('/', '.'), compiled);
            Object result = generatedClass.getConstructors()[0].newInstance();
            return (TagRunnable.BaseInterface<? extends ObjectTag>) result;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }
}
