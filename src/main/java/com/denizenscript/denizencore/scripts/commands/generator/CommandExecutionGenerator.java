package com.denizenscript.denizencore.scripts.commands.generator;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.codegen.CodeGenUtil;
import com.denizenscript.denizencore.utilities.codegen.MethodGenerator;
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

    public static final String COMMAND_EXECUTOR_INTERFACE_PATH = Type.getInternalName(CommandExecutor.class);
    public static final Method COMMAND_EXECUTOR_NTERFACE_EXECUTE_METHOD = ReflectionHelper.getMethod(CommandExecutor.class, "execute", ScriptEntry.class);
    public static final String COMMAND_EXECUTORINTERFACE_EXECUTE_DESCRIPTOR = Type.getMethodDescriptor(COMMAND_EXECUTOR_NTERFACE_EXECUTE_METHOD);
    public static final Method HELPER_PREFIX_ENTRY_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixEntryArg", ScriptEntry.class, PrefixArgData.class);
    public static final Method SCRIPTENTRY_SHOULDDEBUG_METHOD = ReflectionHelper.getMethod(ScriptEntry.class, "dbCallShouldDebug");
    public static final Method DEBUG_REPORT_METHOD = ReflectionHelper.getMethod(Debug.class, "report", Debuggable.class, String.class, Object[].class);

    public static abstract class ArgData {
        public Class type;
        public boolean required;
    }

    public static class PrefixArgData extends ArgData {
        public String prefix;
        public boolean throwTypeError;
    }

    /** Used for generated calls. */
    public static ObjectTag helperPrefixEntryArg(ScriptEntry entry, PrefixArgData arg) {
        if (arg.required) {
            return entry.requiredArgForPrefix(arg.prefix, arg.type);
        }
        return entry.argForPrefix(arg.prefix, arg.type, arg.throwTypeError);
    }

    public static CommandExecutor generateExecutorFor(Class<? extends AbstractCommand> cmdClass, AbstractCommand cmd) {
        try {
            Method method = Arrays.stream(cmdClass.getDeclaredMethods()).filter(m -> Modifier.isStatic(m.getModifiers()) && m.getName().equals("autoExecute")).findFirst().orElse(null);
            if (method == null) {
                return null;
            }
            // ====== Gen class ======
            String cmdCleanName = CodeGenUtil.TAG_NAME_PERMITTED.trimToMatches(cmdClass.getSimpleName().replace('.', '_'));
            if (cmdCleanName.length() > 50) {
                cmdCleanName = cmdCleanName.substring(0, 50);
            }
            String className = CodeGenUtil.COMMAND_GEN_PACKAGE + "CommandExecutor" + (totalGenerated++) + "_" + cmdCleanName;
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", new String[]{COMMAND_EXECUTOR_INTERFACE_PATH});
            cw.visitSource("GENERATED_CMD_EXEC", null);
            MethodGenerator.genDefaultConstructor(cw, className);
            // ====== Gen 'execute' method ======
            ArrayList<ArgData> args = new ArrayList<>();
            ArrayList<MethodGenerator.Local> argLocals = new ArrayList<>();
            {
                MethodGenerator gen = MethodGenerator.generateMethod(className, cw, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "execute", COMMAND_EXECUTORINTERFACE_EXECUTE_DESCRIPTOR);
                MethodGenerator.Local scriptEntryLocal = gen.addLocal("scriptEntry", ScriptEntry.class);
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
                            MethodGenerator.Local argLocal = gen.addLocal("arg_" + args.size() + "_" + CodeGenUtil.TAG_NAME_PERMITTED.trimToMatches(prefixArg.prefix()), paramType);
                            gen.loadLocal(scriptEntryLocal);
                            gen.loadStaticField(className, argLocal.name, PrefixArgData.class);
                            gen.invokeStatic(HELPER_PREFIX_ENTRY_METHOD);
                            gen.cast(paramType);
                            gen.storeLocal(argLocal);
                            PrefixArgData argData = new PrefixArgData();
                            argData.prefix = prefixArg.prefix();
                            argData.required = prefixArg.required();
                            argData.throwTypeError = prefixArg.throwTypeError();
                            argData.type = paramType;
                            argLocals.add(argLocal);
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
                gen.advanceAndLabel();
                Label afterDebugLabel = new Label();
                gen.loadLocal(scriptEntryLocal);
                gen.invokeVirtual(SCRIPTENTRY_SHOULDDEBUG_METHOD);
                gen.jumpIfFalseTo(afterDebugLabel);
                gen.loadLocal(scriptEntryLocal);
                gen.loadString(cmd.getName());
                gen.loadInt(args.size());
                gen.createArray(Object.class);
                for (int i = 0; i < argLocals.size(); i++) {
                    gen.stackDuplicate();
                    gen.loadInt(i);
                    gen.loadLocal(argLocals.get(i));
                    gen.arrayStore(Object.class);
                }
                gen.invokeStatic(DEBUG_REPORT_METHOD);
                gen.advanceAndLabel(afterDebugLabel);
                if (hasScriptEntry) {
                    gen.loadLocal(scriptEntryLocal);
                }
                for (MethodGenerator.Local local : argLocals) {
                    gen.loadLocal(local);
                }
                gen.invokeStatic(method);
                gen.advanceAndLabel();
                gen.returnNone();
                gen.end();
            }
            for (int i = 0; i < argLocals.size(); i++) {
                cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, argLocals.get(i).name, Type.getDescriptor(args.get(i).getClass()), null, null);
            }
            // ====== Compile and return ======
            cw.visitEnd();
            byte[] compiled = cw.toByteArray();
            Class<?> generatedClass = CodeGenUtil.loader.define(className.replace('/', '.'), compiled);
            for (int i = 0; i < argLocals.size(); i++) {
                ReflectionHelper.setFieldValue(generatedClass, argLocals.get(i).name, null, args.get(i));
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
