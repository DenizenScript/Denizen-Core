package com.denizenscript.denizencore.scripts.commands.generator;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.codegen.CodeGenUtil;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;
import org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;

public class CommandExecutionGenerator {

    public interface CommandExecutor {
        void execute(ScriptEntry scriptEntry);
    }

    public static long totalGenerated = 0;

    public static final String COMMAND_EXECUTION_GENERATOR_TYPENAME = Type.getInternalName(CommandExecutionGenerator.class);
    public static final String COMMAND_EXECUTOR_INTERFACE_PATH = Type.getInternalName(CommandExecutor.class);
    public static final Method COMMAND_EXECUTOR_NTERFACE_EXECUTE_METHOD = ReflectionHelper.getMethod(CommandExecutor.class, "execute", ScriptEntry.class);
    public static final String COMMAND_EXECUTORINTERFACE_EXECUTE_DESCRIPTOR = Type.getMethodDescriptor(COMMAND_EXECUTOR_NTERFACE_EXECUTE_METHOD);

    private static final int LOCAL_SCRIPTENTRY = 1;

    public static abstract class ArgData {
        public Class type;
        public boolean required;
    }

    public static class PrefixArgData extends ArgData {
        public String prefix;
        public boolean throwTypeError;
    }
    public static final String PREFIXARGDATA_PATH = Type.getInternalName(PrefixArgData.class);
    public static final String PREFIXARGDATA_DESCRIPTOR = "L" + PREFIXARGDATA_PATH + ";";

    public static ObjectTag helperPrefixEntryArg(ScriptEntry entry, PrefixArgData arg) {
        if (arg.required) {
            return entry.requiredArgForPrefix(arg.prefix, arg.type);
        }
        return entry.argForPrefix(arg.prefix, arg.type, arg.throwTypeError);
    }

    public static Method HELPER_PREFIX_ENTRY_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixEntryArg", ScriptEntry.class, PrefixArgData.class);
    public static final String HELPER_PREFIX_ENTRY_DESCRIPTOR = Type.getMethodDescriptor(HELPER_PREFIX_ENTRY_METHOD);

    public static final String SCRIPTENTRY_SHOULDDEBUG_DESCRIPTOR = Type.getMethodDescriptor(ReflectionHelper.getMethod(ScriptEntry.class, "dbCallShouldDebug"));
    public static final String DEBUG_REPORT_DESCRIPTOR = Type.getMethodDescriptor(ReflectionHelper.getMethod(Debug.class, "report", Debuggable.class, String.class, Object[].class));

    public static CommandExecutor generateExecutorFor(Class<? extends AbstractCommand> cmdClass, AbstractCommand cmd) {
        try {
            Method method = Arrays.stream(cmdClass.getDeclaredMethods()).filter(m -> Modifier.isStatic(m.getModifiers()) && m.getName().equals("autoExecute")).findFirst().orElse(null);
            if (method == null) {
                return null;
            }
            // ...

            // ====== Gen class ======
            String cmdCleanName = CodeGenUtil.TAG_NAME_PERMITTED.trimToMatches(cmdClass.getSimpleName().replace('.', '_'));
            if (cmdCleanName.length() > 50) {
                cmdCleanName = cmdCleanName.substring(0, 50);
            }
            String className = CodeGenUtil.COMMAND_GEN_PACKAGE + "CommandExecutor" + (totalGenerated++) + "_" + cmdCleanName;
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", new String[]{COMMAND_EXECUTOR_INTERFACE_PATH});
            cw.visitSource("GENERATED_CMD_EXEC", null);
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
            // ====== Gen 'execute' method ======
            ArrayList<ArgData> args = new ArrayList<>();
            {
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "execute", COMMAND_EXECUTORINTERFACE_EXECUTE_DESCRIPTOR, null, null);
                mv.visitCode();
                Label returnLabel = new Label();
                int line = 1;
                Label startLabel = new Label();
                mv.visitLabel(startLabel);
                mv.visitLineNumber(line++, startLabel);
                boolean hasScriptEntry = false;

                for (Parameter param : method.getParameters()) {
                    Class<?> paramType = param.getType();
                    if (paramType == ScriptEntry.class && !hasScriptEntry) {
                        hasScriptEntry = true;
                        continue;
                    }
                    PrefixedArg prefixArg = param.getAnnotation(PrefixedArg.class);
                    if (prefixArg != null) {
                        cmd.setPrefixesHandled(prefixArg.prefix());
                        if (ObjectTag.class.isAssignableFrom(paramType)) {
                            mv.visitVarInsn(Opcodes.ALOAD, LOCAL_SCRIPTENTRY);
                            mv.visitFieldInsn(Opcodes.GETSTATIC, className, "staticArgHolder" + (args.size()), PREFIXARGDATA_DESCRIPTOR);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, COMMAND_EXECUTION_GENERATOR_TYPENAME, "helperPrefixEntryArg", HELPER_PREFIX_ENTRY_DESCRIPTOR, false);
                            mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(paramType));
                            mv.visitVarInsn(Opcodes.ASTORE, 2 + args.size());
                            PrefixArgData argData = new PrefixArgData();
                            argData.prefix = prefixArg.prefix();
                            argData.required = prefixArg.required();
                            argData.throwTypeError = prefixArg.throwTypeError();
                            argData.type = paramType;
                            args.add(argData);
                            continue;
                        }
                        else if (paramType == boolean.class) {
                            // TODO
                        }
                        else {
                            // TODO
                        }
                    }
                    BooleanArg booleanArg = param.getAnnotation(BooleanArg.class);
                    if (booleanArg != null && paramType == boolean.class) {
                        cmd.setBooleansHandled(booleanArg.name());
                        // TODO
                    }
                    LinearArg linearArg = param.getAnnotation(LinearArg.class);
                    if (linearArg != null) {
                        // TODO
                    }
                    Debug.echoError("Cannot generate executor for command '" + cmdClass.getName() + "': autoExecute method has param '" + param.getName() + "' of type '" + paramType.getName() + "' which is not supported.");
                    return null;
                }

                mv.visitLineNumber(line++, startLabel);
                Label afterDebugLabel = new Label();
                mv.visitVarInsn(Opcodes.ALOAD, LOCAL_SCRIPTENTRY);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, CodeGenUtil.SCRIPTENTRY_PATH, "dbCallShouldDebug", SCRIPTENTRY_SHOULDDEBUG_DESCRIPTOR, false);
                mv.visitJumpInsn(Opcodes.IFEQ, afterDebugLabel);
                mv.visitVarInsn(Opcodes.ALOAD, LOCAL_SCRIPTENTRY);
                mv.visitLdcInsn(cmd.getName());
                mv.visitIntInsn(Opcodes.SIPUSH, args.size());
                mv.visitTypeInsn(Opcodes.ANEWARRAY, CodeGenUtil.JAVA_OBJECT_PATH);
                for (int i = 0; i < args.size(); i++) {
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitIntInsn(Opcodes.SIPUSH, i);
                    mv.visitVarInsn(Opcodes.ALOAD, 2 + i);
                    mv.visitInsn(Opcodes.AASTORE);
                }
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, CodeGenUtil.DEBUG_PATH, "report", DEBUG_REPORT_DESCRIPTOR, false);
                mv.visitLabel(afterDebugLabel);
                mv.visitLineNumber(line++, afterDebugLabel);

                if (hasScriptEntry) {
                    mv.visitVarInsn(Opcodes.ALOAD, LOCAL_SCRIPTENTRY);
                }
                for (int i = 0; i < args.size(); i++) {
                    mv.visitVarInsn(Opcodes.ALOAD, 2 + i);
                }

                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(cmdClass), method.getName(), Type.getMethodDescriptor(method), false);

                mv.visitLabel(returnLabel);
                mv.visitLineNumber(line, returnLabel);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitLocalVariable("scriptEntry", CodeGenUtil.SCRIPTENTRYT_LOCAL_TYPE, null, startLabel, returnLabel, LOCAL_SCRIPTENTRY);
                for (int i = 0; i < args.size(); i++) {
                    mv.visitLocalVariable("arg" + i, "L" + Type.getInternalName(args.get(i).type) + ";", null, startLabel, returnLabel, 2 + i);
                }
                mv.visitMaxs(0, 0);
                mv.visitEnd();
            }
            for (int i = 0; i < args.size(); i++) {
                cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "staticArgHolder" + i, "L" + Type.getInternalName(args.get(i).getClass()) + ";", null, null);
            }
            // ====== Compile and return ======
            cw.visitEnd();
            byte[] compiled = cw.toByteArray();
            Class<?> generatedClass = CodeGenUtil.loader.define(className.replace('/', '.'), compiled);
            for (int i = 0; i < args.size(); i++) {
                ReflectionHelper.setFieldValue(generatedClass, "staticArgHolder" + i, null, args.get(i));
            }
            Object result = generatedClass.getConstructors()[0].newInstance();
            return (CommandExecutor) result;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }
}
