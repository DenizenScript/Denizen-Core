package com.denizenscript.denizencore.scripts.commands.generator;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.EnumHelper;
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
    public static final Method HELPER_PREFIX_ENTRY_ARG_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixEntryArg", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_BOOLEAN_ARG_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperBooleanArg", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_ENUM_ARG_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperEnumArg", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_PREFIX_STRING_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixString", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_PREFIX_BOOLEAN_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixBoolean", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_PREFIX_INTEGER_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixInteger", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_PREFIX_LONG_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixLong", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_PREFIX_FLOAT_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixFloat", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_PREFIX_DOUBLE_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixDouble", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_PREFIX_ENUM_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperPrefixEnum", ScriptEntry.class, ArgData.class);
    public static final Method HELPER_DEBUG_FORMAT_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperDebugFormat", Object.class, ArgData.class);
    public static final Method SCRIPTENTRY_SHOULDDEBUG_METHOD = ReflectionHelper.getMethod(ScriptEntry.class, "dbCallShouldDebug");
    public static final Method DEBUG_REPORT_METHOD = ReflectionHelper.getMethod(Debug.class, "report", Debuggable.class, String.class, Object[].class);

    public static class ArgData {
        public Class type;
        public boolean required;
        public String prefix;
        public String defaultValue;
        public int index;
    }

    /** Used for generated calls. */
    public static ObjectTag helperPrefixEntryArg(ScriptEntry entry, ArgData arg) {
        Integer index = entry.internal.prefixedArgMapper[arg.index];
        if (index == null) {
            if (arg.required) {
                throw new InvalidArgumentsRuntimeException("Must specify input to '" + arg.prefix + "' argument. Did you forget an argument? Check meta docs!");
            }
            return null;
        }
        Argument givenArg = entry.argAtIndex(entry.internal.all_arguments, index);
        ObjectTag output = givenArg.asType(arg.type);
        if (output == null) {
            throw new InvalidArgumentsRuntimeException("Invalid input to '" + arg.prefix + "': '" + givenArg.getValue() + "': not a valid " + arg.type.getSimpleName());
        }
        return output;
    }

    /** Used for generated calls. */
    public static boolean helperBooleanArg(ScriptEntry entry, ArgData arg) {
        ScriptEntry.BooleanArg givenArg = entry.internal.booleans[arg.index];
        if (givenArg == null) {
            return false;
        }
        if (givenArg.rawValue != null) {
            return givenArg.rawValue;
        }
        Argument dynamicArg = entry.argAtIndex(entry.internal.all_arguments, givenArg.argIndex);
        ElementTag value = dynamicArg.asElement();
        if (CoreUtilities.equalsIgnoreCase(value.asString(), "false")) {
            return false;
        }
        if (CoreUtilities.equalsIgnoreCase(value.asString(), "true")) {
            return true;
        }
        throw new InvalidArgumentsRuntimeException("Input to boolean argument '" + arg.prefix + "' of '" + value + "' is invalid: must specify either 'true' or 'false'!");
    }

    /** Used for generated calls. */
    public static Enum helperEnumArg(ScriptEntry entry, ArgData arg) {
        ScriptEntry.EnumArg givenArg = entry.internal.enumVals[arg.index];
        if (givenArg == null) {
            throw new InvalidArgumentsRuntimeException("Must specify input to '" + arg.prefix + "' argument. Did you forget an argument? Check meta docs!");
        }
        if (givenArg.rawValue != null) {
            return givenArg.rawValue;
        }
        Argument dynamicArg = entry.argAtIndex(entry.internal.all_arguments, givenArg.argIndex);
        ElementTag value = dynamicArg.asElement();
        if (value == null && arg.required) {
            throw new InvalidArgumentsRuntimeException("Must specify input to '" + arg.prefix + "' argument. Did you forget an argument? Check meta docs!");
        }
        if (value != null) {
            Enum enumVal = value.asEnum(arg.type);
            if (enumVal == null) {
                throw new InvalidArgumentsRuntimeException("Invalid input to '" + arg.prefix + "' argument. Does not match enum of value options. Must be one of: " + String.join(", ", EnumHelper.get(arg.type).valuesMapLower.keySet()));
            }
            return enumVal;
        }
        return arg.defaultValue == null ? null : ElementTag.asEnum(arg.type, arg.defaultValue);
    }

    public static ElementTag getElementForPrefix(ScriptEntry entry, ArgData arg) {
        Integer index = entry.internal.prefixedArgMapper[arg.index];
        if (index == null) {
            if (arg.required) {
                throw new InvalidArgumentsRuntimeException("Must specify input to '" + arg.prefix + "' argument. Did you forget an argument? Check meta docs!");
            }
            return null;
        }
        return entry.argAtIndex(entry.internal.all_arguments, index).asElement();
    }

    /** Used for generated calls. */
    public static String helperPrefixString(ScriptEntry entry, ArgData arg) {
        ElementTag value = getElementForPrefix(entry, arg);
        if (value == null) {
            return arg.defaultValue;
        }
        return value.asString();
    }

    /** Used for generated calls. */
    public static Enum helperPrefixEnum(ScriptEntry entry, ArgData arg) {
        ElementTag value = getElementForPrefix(entry, arg);
        if (value != null) {
            return value.asEnum(arg.type);
        }
        return arg.defaultValue == null ? null : ElementTag.asEnum(arg.type, arg.defaultValue);
    }

    /** Used for generated calls. */
    public static boolean helperPrefixBoolean(ScriptEntry entry, ArgData arg) {
        ElementTag value = getElementForPrefix(entry, arg);
        if (value == null) {
            return arg.defaultValue != null && CoreUtilities.equalsIgnoreCase(arg.defaultValue, "true");
        }
        if (CoreUtilities.equalsIgnoreCase(value.asString(), "false")) {
            return false;
        }
        if (CoreUtilities.equalsIgnoreCase(value.asString(), "true")) {
            return true;
        }
        throw new InvalidArgumentsRuntimeException("Input to boolean argument '" + arg.prefix + "' of '" + value + "' is invalid: must specify either 'true' or 'false'!");
    }

    /** Used for generated calls. */
    public static int helperPrefixInteger(ScriptEntry entry, ArgData arg) {
        return (int) helperPrefixLong(entry, arg);
    }

    /** Used for generated calls. */
    public static long helperPrefixLong(ScriptEntry entry, ArgData arg) {
        ElementTag value = getElementForPrefix(entry, arg);
        if (value == null) {
            return Long.parseLong(arg.defaultValue);
        }
        try {
            return Long.parseLong(value.cleanedForLong());
        }
        catch (NumberFormatException ex) {
            throw new InvalidArgumentsRuntimeException("Input to integer argument '" + arg.prefix + "' of '" + value + "' is invalid: must specify an integer number!");
        }
    }

    /** Used for generated calls. */
    public static float helperPrefixFloat(ScriptEntry entry, ArgData arg) {
        return (float) helperPrefixDouble(entry, arg);
    }

    /** Used for generated calls. */
    public static double helperPrefixDouble(ScriptEntry entry, ArgData arg) {
        ElementTag value = getElementForPrefix(entry, arg);
        if (value == null) {
            return Double.parseDouble(arg.defaultValue);
        }
        try {
            return Double.parseDouble(ElementTag.percentageMatcher.trimToNonMatches(value.asString()));
        }
        catch (NumberFormatException ex) {
            throw new InvalidArgumentsRuntimeException("Input to decimal argument '" + arg.prefix + "' of '" + value + "' is invalid: must specify a decimal number!");
        }
    }

    /** Used for generated calls. */
    public static String helperDebugFormat(Object value, ArgData arg) {
        if (value == null) {
            return "";
        }
        return ArgumentHelper.debugObj(arg.prefix, value);
    }

    public static CommandExecutor generateExecutorFor(Class<? extends AbstractCommand> cmdClass, AbstractCommand cmd) {
        try {
            Method method = Arrays.stream(cmdClass.getDeclaredMethods()).filter(m -> Modifier.isStatic(m.getModifiers()) && m.getName().equals("autoExecute")).findFirst().orElse(null);
            if (method == null) {
                return null;
            }
            // ====== Gen class ======
            String cmdCleanName = CodeGenUtil.cleanName(cmdClass.getSimpleName().replace('.', '_'));
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
                    ArgName argName = param.getAnnotation(ArgName.class);
                    ArgPrefixed argPrefixed = param.getAnnotation(ArgPrefixed.class);
                    ArgDefaultText argDefaultText = param.getAnnotation(ArgDefaultText.class);
                    LinearArg linearArg = param.getAnnotation(LinearArg.class);
                    if (argName == null) {
                        Debug.echoError("Cannot generate executor for command '" + cmdClass.getName() + "': autoExecute method has param '" + param.getName() + "' which lacks a proper naming parameter.");
                        return null;
                    }
                    MethodGenerator.Local argLocal = gen.addLocal("arg_" + args.size() + "_" + CodeGenUtil.cleanName(argName.value()), paramType);
                    Method argMethod = null;
                    boolean doCast = false;
                    ArgData argData = new ArgData();
                    if (argPrefixed != null) {
                        argData.index = cmd.setPrefixHandled(argName.value());
                        if (ObjectTag.class.isAssignableFrom(paramType)) {
                            argMethod = HELPER_PREFIX_ENTRY_ARG_METHOD;
                            doCast = true;
                        }
                        else if (Enum.class.isAssignableFrom(paramType)) {
                            argMethod = HELPER_PREFIX_ENUM_METHOD;
                            doCast = true;
                        }
                        else if (paramType == String.class) {
                            argMethod = HELPER_PREFIX_STRING_METHOD;
                        }
                        else if (paramType == boolean.class) {
                            argMethod = HELPER_PREFIX_BOOLEAN_METHOD;
                        }
                        else if (paramType == int.class) {
                            argMethod = HELPER_PREFIX_INTEGER_METHOD;
                        }
                        else if (paramType == long.class) {
                            argMethod = HELPER_PREFIX_LONG_METHOD;
                        }
                        else if (paramType == float.class) {
                            argMethod = HELPER_PREFIX_FLOAT_METHOD;
                        }
                        else if (paramType == double.class) {
                            argMethod = HELPER_PREFIX_DOUBLE_METHOD;
                        }
                    }
                    else if (paramType == boolean.class) {
                        argMethod = HELPER_BOOLEAN_ARG_METHOD;
                        argData = new ArgData();
                        argData.index = cmd.setBooleanHandled(argName.value());
                    }
                    else if (Enum.class.isAssignableFrom(paramType)) {
                        argMethod = HELPER_ENUM_ARG_METHOD;
                        argData = new ArgData();
                        argData.index = cmd.setEnumHandled(argName.value(), (Class<? extends Enum>) paramType);
                        doCast = true;
                    }
                    if (linearArg != null) {
                        // TODO
                    }
                    if (argMethod == null) {
                        Debug.echoError("Cannot generate executor for command '" + cmdClass.getName() + "': autoExecute method has param '" + argName.value() + "' of type '" + paramType.getName() + "' which is not supported.");
                        return null;
                    }
                    gen.loadLocal(scriptEntryLocal);
                    gen.loadStaticField(className, argLocal.name, argData.getClass());
                    gen.invokeStatic(argMethod);
                    if (doCast) {
                        gen.cast(paramType);
                    }
                    gen.storeLocal(argLocal);
                    argData.type = paramType;
                    argData.prefix = argName.value();
                    if (!param.isAnnotationPresent(ArgDefaultNull.class)) {
                        if (argDefaultText == null) {
                            argData.required = true;
                        }
                        else {
                            argData.defaultValue = argDefaultText.value();
                        }
                    }
                    argLocals.add(argLocal);
                    args.add(argData);
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
                    MethodGenerator.Local local = argLocals.get(i);
                    gen.stackDuplicate();
                    gen.loadInt(i);
                    gen.loadLocal(local);
                    gen.autoBox(local.descriptor);
                    gen.loadStaticField(className, local.name, args.get(i).getClass());
                    gen.invokeStatic(HELPER_DEBUG_FORMAT_METHOD);
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
