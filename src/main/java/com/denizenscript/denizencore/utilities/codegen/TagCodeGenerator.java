package com.denizenscript.denizencore.utilities.codegen;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.*;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.objectweb.asm.*;

public class TagCodeGenerator {

    public static long totalGenerated = 0;

    private static final int LOCAL_ATTRIBUTE = 1, LOCAL_CURRENTOBJECT = 2;

    public static String FULFILL_ONE_RUN_DESCRIPTOR = "(L" + CodeGenUtil.OBJECT_TAG_PATH + ";)V";

    public static boolean hasStaticContext(Attribute.AttributeComponent component, TagContext genContext) {
        if (component.rawParam == null) {
            return true;
        }
        if (component.paramParsed == null) {
            component.paramParsed = TagManager.parseTextToTag(component.rawParam, genContext);
            if (component.paramParsed == null) {
                return false;
            }
        }
        return !component.paramParsed.hasTag;
    }

    public static TagRunnable.BaseInterface<? extends ObjectTag> generatePartialTag(TagManager.ParseableTagPiece toParse, TagContext genContext) {
        ReplaceableTagEvent.ReferenceData data = toParse.tagData;
        if (data == null || data.tagBase == null || data.tagBase.baseForm == null || data.attribs.attributes.length < 1) {
            return null;
        }
        if (data.compiledStart != null) {
            return data.compiledStart;
        }
        Attribute.AttributeComponent[] pieces = data.attribs.attributes;
        boolean canBeStatic = data.tagBase.isStatic && hasStaticContext(pieces[0], genContext);
        int staticParts = canBeStatic ? 1 : 0;
        int applicableParts = 0;
        for (int i = 1; i < pieces.length; i++) {
            ObjectTagProcessor.TagData piece = pieces[i].data;
            if (piece == null || piece.runner == null) {
                break;
            }
            applicableParts++;
            if (canBeStatic) {
                if (piece.isStatic && hasStaticContext(pieces[i], genContext)) {
                    staticParts++;
                }
                else {
                    canBeStatic = false;
                }
            }
        }
        ObjectTag staticParseResult = null;
        if (staticParts > 0) {
            ReplaceableTagEvent staticParseEvent = new ReplaceableTagEvent(data, toParse.content, genContext);
            Attribute staticParseAttrib = staticParseEvent.getAttributes();
            staticParseResult = data.tagBase.baseForm.run(staticParseAttrib);
            if (staticParseResult != null) {
                staticParseAttrib.fulfillOne(staticParseResult);
                for (int i = 1; i < staticParts; i++) {
                    TagRunnable.ObjectInterface<ObjectTag, ObjectTag> runner = (TagRunnable.ObjectInterface<ObjectTag, ObjectTag>) pieces[i].data.runner;
                    staticParseResult = runner.run(staticParseAttrib, staticParseResult);
                    if (staticParseResult != null) {
                        staticParseAttrib.fulfillOne(staticParseResult);
                    }
                }
                if (staticParseResult != null) {
                    if (staticParts == pieces.length) {
                        if (genContext.shouldDebug()) {
                            Debug.echoDebug(genContext, "<Y>+> [Static Tag Processing] <G>Pre-Filled tag <<W>" + toParse.content + "<G>> with '<W>" + staticParseResult.toString() + "<G>', and cached result.");
                        }
                        toParse.rawObject = staticParseResult;
                        toParse.tagData.rawObject = staticParseResult;
                        toParse.content = staticParseResult.toString();
                        toParse.isTag = false;
                        return null;
                    }
                    if (genContext.shouldDebug()) {
                        StringBuilder piecesText = new StringBuilder("<");
                        for (int i = 0; i < staticParts; i++) {
                            piecesText.append(pieces[i].toString()).append(".");
                        }
                        Debug.echoDebug(genContext, "<Y>+> [Static Tag Processing] <G>Pre-Filled partial tag '<W>" + piecesText + "..<G>' with '<W>" + staticParseResult.toString() + "<G>', and cached result.");
                    }
                    data.skippable = staticParts;
                }
            }
        }
        if (applicableParts == 0 && staticParts == 0) {
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
            if (staticParseResult != null) {
                cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "staticParseResult", CodeGenUtil.OBJECT_LOCAL_TYPE, null, null);
            }
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
                int line = 1;
                Label startLabel = new Label();
                mv.visitLabel(startLabel);
                mv.visitLineNumber(line++, startLabel);
                // Run the initial tag base
                if (staticParseResult != null) {
                    mv.visitFieldInsn(Opcodes.GETSTATIC, className, "staticParseResult", CodeGenUtil.OBJECT_LOCAL_TYPE);
                    mv.visitVarInsn(Opcodes.ASTORE, LOCAL_CURRENTOBJECT);
                }
                else {
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
                }
                for (int i = staticParseResult == null ? 1 : staticParts; i < applicableParts; i++) {
                    ObjectTagProcessor.TagData piece = pieces[i].data;
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
            if (staticParseResult != null) {
                ReflectionHelper.getFinalSetter(generatedClass, "staticParseResult").invoke(staticParseResult);
            }
            Object result = generatedClass.getConstructors()[0].newInstance();
            return (TagRunnable.BaseInterface<? extends ObjectTag>) result;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }
}
