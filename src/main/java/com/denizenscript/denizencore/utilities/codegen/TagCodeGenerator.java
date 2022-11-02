package com.denizenscript.denizencore.utilities.codegen;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.*;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class TagCodeGenerator {

    public static long totalGenerated = 0;

    public static final Method ATTRIBUTE_FULFILLONE_METHOD = ReflectionHelper.getMethod(Attribute.class, "fulfillOne", ObjectTag.class);
    public static final Method ATTRIBUTE_TRACKLASTTAGFAILURE_METHOD = ReflectionHelper.getMethod(Attribute.class, "trackLastTagFailure");
    public static final Field ATTRIBUTE_HADMANUALFULFILL_FIELD = ReflectionHelper.getFields(Attribute.class).get("hadManualFulfill", boolean.class);

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
        boolean canBeStatic = data.tagBase.isStatic && (hasStaticContext(pieces[0], genContext) || data.tagBase.doesStaticOverride);
        int staticParts = canBeStatic ? 1 : 0;
        int applicableParts = 0;
        for (int i = 1; i < pieces.length; i++) {
            ObjectTagProcessor.TagData<?,?> piece = pieces[i].data;
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
            try {
                ReplaceableTagEvent staticParseEvent = new ReplaceableTagEvent(data, toParse.content, genContext);
                Attribute staticParseAttrib = staticParseEvent.getAttributes();
                TagManager.isStaticParsing = true;
                staticParseResult = data.tagBase.baseForm.run(staticParseAttrib);
                TagManager.isStaticParsing = false;
                if (staticParseResult == null) {
                    staticParts = 0;
                }
                else {
                    staticParseAttrib.fulfillOne(staticParseResult);
                    for (int i = 1; i < staticParts; i++) {
                        TagRunnable.ObjectInterface<ObjectTag, ObjectTag> runner = (TagRunnable.ObjectInterface<ObjectTag, ObjectTag>) pieces[i].data.runner;
                        TagManager.isStaticParsing = true;
                        ObjectTag newResult = runner.run(staticParseAttrib, staticParseResult);
                        TagManager.isStaticParsing = false;
                        if (newResult != null) {
                            staticParseResult = newResult;
                            staticParseAttrib.fulfillOne(staticParseResult);
                        }
                        else {
                            staticParts = i;
                            break;
                        }
                    }
                    if (staticParts == pieces.length) {
                        if (genContext.shouldDebug()) {
                            Debug.echoDebug(genContext, "<Y>+> [Static Tag Processing] <G>Pre-Filled tag <<W>" + toParse.content + "<G>> with '<W>" + staticParseResult + "<G>', and cached result.");
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
                        Debug.echoDebug(genContext, "<Y>+> [Static Tag Processing] <G>Pre-Filled partial tag '<W>" + piecesText + "..<G>' with '<W>" + staticParseResult + "<G>', and cached result.");
                    }
                    data.skippable = staticParts;
                }
            }
            catch (Throwable ex) {
                Debug.echoError("Static tag pre-parse failed for: " + toParse.content);
                Debug.echoError(ex);
            }
            finally {
                TagManager.isStaticParsing = false;
            }
        }
        if (applicableParts == 0 && staticParts == 0) {
            return null;
        }
        try {
            // ====== Gen class ======
            String tagFullName = CodeGenUtil.cleanName(data.rawTag.replace('.', '_'));
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
            MethodGenerator.genDefaultConstructor(cw, className);
            // ====== Gen 'run' method ======
            {
                MethodGenerator gen = MethodGenerator.generateMethod(className, cw, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "run", TagNamer.BASE_INTERFACE_RUN_DESCRIPTOR);
                MethodGenerator.Local attributeLocal = gen.addLocal("attribute", Attribute.class);
                MethodGenerator.Local objectLocal = gen.addLocal("currentObject", ObjectTag.class);
                Label returnLabel = new Label();
                Label failLabel = new Label();
                // Run the initial tag base
                if (staticParseResult != null) {
                    gen.loadStaticField(className, "staticParseResult", ObjectTag.class);
                    gen.storeLocal(objectLocal);
                }
                else {
                    gen.loadLocal(attributeLocal);
                    gen.invokeStatic(Type.getInternalName(data.tagBase.baseForm.getClass()), "staticRun", TagNamer.BASE_INTERFACE_RUN_DESCRIPTOR);
                    gen.storeLocal(objectLocal);
                    // If tag base returned null, fail
                    gen.loadLocal(objectLocal);
                    gen.jumpIfNullTo(failLabel);
                    // otherwise, fulfill one
                    gen.loadLocal(attributeLocal);
                    gen.loadLocal(objectLocal);
                    gen.invokeVirtual(ATTRIBUTE_FULFILLONE_METHOD);
                }
                for (int i = staticParseResult == null ? 1 : staticParts; i < applicableParts; i++) {
                    ObjectTagProcessor.TagData<?,?> piece = pieces[i].data;
                    // Run sub-tag
                    gen.advanceAndLabel();
                    gen.loadLocal(attributeLocal);
                    gen.loadLocal(objectLocal);
                    gen.invokeStatic(Type.getInternalName(piece.runner.getClass()), "staticRun", TagNamer.OBJECT_INTERFACE_RUN_DESCRIPTOR);
                    gen.storeLocal(objectLocal);
                    // If null return, fail
                    gen.advanceAndLabel();
                    gen.loadLocal(objectLocal);
                    gen.jumpIfNullTo(failLabel);
                    // otherwise, fulfill one
                    gen.advanceAndLabel();
                    gen.loadLocal(attributeLocal);
                    gen.loadLocal(objectLocal);
                    gen.invokeVirtual(ATTRIBUTE_FULFILLONE_METHOD);
                    // If manual fulfill happened, a legacy multi-part tag handler was used, so code gen is no longer trustworthy - exit and let legacy handler run
                    gen.advanceAndLabel();
                    gen.loadLocal(attributeLocal);
                    gen.loadInstanceField(ATTRIBUTE_HADMANUALFULFILL_FIELD);
                    gen.jumpIfTrueTo(returnLabel);
                }
                gen.jumpTo(returnLabel);
                gen.advanceAndLabel(failLabel);
                gen.loadLocal(attributeLocal);
                gen.invokeVirtual(ATTRIBUTE_TRACKLASTTAGFAILURE_METHOD);
                gen.advanceAndLabel(returnLabel);
                gen.loadLocal(objectLocal);
                gen.returnValue(ObjectTag.class);
                gen.end();
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
