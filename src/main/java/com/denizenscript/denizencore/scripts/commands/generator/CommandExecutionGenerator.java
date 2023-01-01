package com.denizenscript.denizencore.scripts.commands.generator;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.EnumHelper;
import com.denizenscript.denizencore.utilities.ReflectionHelper;
import com.denizenscript.denizencore.utilities.codegen.CodeGenUtil;
import com.denizenscript.denizencore.utilities.codegen.MethodGenerator;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;
import org.objectweb.asm.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandExecutionGenerator {

    public static abstract class CommandExecutor {
        public ArgData[] args;

        public CommandExecutor() {
            // Placeholder for default-constructor gen stability.
        }

        public abstract void execute(ScriptEntry scriptEntry);
    }

    public static long totalGenerated = 0;

    public static Method getArgHelperMethod(String name) {
        return ReflectionHelper.getMethod(CommandExecutionGenerator.class, name, ScriptEntry.class, ArgData.class);
    }

    public static final String COMMAND_EXECUTOR_PATH = Type.getInternalName(CommandExecutor.class);
    public static final Method COMMAND_EXECUTOR_EXECUTE_METHOD = ReflectionHelper.getMethod(CommandExecutor.class, "execute", ScriptEntry.class);
    public static final String COMMAND_EXECUTOR_EXECUTE_DESCRIPTOR = Type.getMethodDescriptor(COMMAND_EXECUTOR_EXECUTE_METHOD);
    public static final Method HELPER_GET_UNPARSED_ARG_LIST = getArgHelperMethod("helperGetUnparsedArgList");
    public static final Method HELPER_PREFIX_ENTRY_ARG_METHOD = getArgHelperMethod("helperPrefixEntryArg");
    public static final Method HELPER_BOOLEAN_ARG_METHOD = getArgHelperMethod("helperBooleanArg");
    public static final Method HELPER_ENUM_ARG_METHOD = getArgHelperMethod("helperEnumArg");
    public static final Method HELPER_PREFIX_LIST_OBJECT_METHOD = getArgHelperMethod("helperPrefixListObject");
    public static final Method HELPER_PREFIX_LIST_ENUM_METHOD = getArgHelperMethod("helperPrefixListEnum");
    public static final Method HELPER_PREFIX_STRING_METHOD = getArgHelperMethod("helperPrefixString");
    public static final Method HELPER_PREFIX_ELEMENT_METHOD = getArgHelperMethod("helperPrefixElement");
    public static final Method HELPER_PREFIX_BOOLEAN_METHOD = getArgHelperMethod("helperPrefixBoolean");
    public static final Method HELPER_PREFIX_INTEGER_METHOD = getArgHelperMethod("helperPrefixInteger");
    public static final Method HELPER_PREFIX_LONG_METHOD = getArgHelperMethod("helperPrefixLong");
    public static final Method HELPER_PREFIX_FLOAT_METHOD = getArgHelperMethod("helperPrefixFloat");
    public static final Method HELPER_PREFIX_DOUBLE_METHOD = getArgHelperMethod("helperPrefixDouble");
    public static final Method HELPER_PREFIX_ENUM_METHOD = getArgHelperMethod("helperPrefixEnum");
    public static final Method HELPER_DEBUG_FORMAT_METHOD = ReflectionHelper.getMethod(CommandExecutionGenerator.class, "helperDebugFormat", Object.class, ArgData.class);
    public static final Method SCRIPTENTRY_SHOULDDEBUG_METHOD = ReflectionHelper.getMethod(ScriptEntry.class, "dbCallShouldDebug");
    public static final Method DEBUG_REPORT_METHOD = ReflectionHelper.getMethod(Debug.class, "report", Debuggable.class, String.class, Object[].class);

    public static class ArgData {
        public Class type;
        public Class subType;
        public boolean required;
        public String name;
        public String defaultValue;
        public Object defaultObject;
        public int index;
        public boolean isLinear;
        public boolean getRaw;
        public boolean shouldDebug;
        public boolean shouldParse;
    }

    public static Argument getArgumentFor(ScriptEntry entry, ArgData arg) {
        if (arg.isLinear) {
            if (entry.internal.arguments_to_use.length <= arg.index) {
                if (arg.required) {
                    throw new InvalidArgumentsRuntimeException("Must specify input to linear argument '" + arg.name + "'. Did you forget an argument? Check meta docs!");
                }
                return null;
            }
            return entry.argAtIndex(true, arg.index);
        }
        else {
            Integer index = entry.internal.prefixedArgMapper[arg.index];
            if (index == null) {
                if (arg.required) {
                    throw new InvalidArgumentsRuntimeException("Must specify input to '" + arg.name + "' argument. Did you forget an argument? Check meta docs!");
                }
                return null;
            }
            return entry.argAtIndex(false, index);
        }
    }

    /** Used for generated calls. */
    public static List<ScriptEntry.InternalArgument> helperGetUnparsedArgList(ScriptEntry scriptEntry, ArgData arg) {
        List<ScriptEntry.InternalArgument> result = Arrays.asList(scriptEntry.internal.arguments_to_use);
        if (arg.index != 0) {
            result = result.subList(arg.index, result.size());
        }
        return result;
    }

    /** Used for generated calls. */
    public static ObjectTag helperPrefixEntryArg(ScriptEntry entry, ArgData arg) {
        Argument givenArg = getArgumentFor(entry, arg);
        if (givenArg == null) {
            return (ObjectTag) arg.defaultObject;
        }
        ObjectTag output = givenArg.asType(arg.type);
        if (output == null) {
            throw new InvalidArgumentsRuntimeException("Invalid input to '" + arg.name + "': '" + givenArg.getValue() + "': not a valid " + DebugInternals.getClassNameOpti(arg.type));
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
        Argument dynamicArg = entry.argAtIndex(arg.isLinear, givenArg.argIndex);
        ElementTag value = dynamicArg.asElement();
        if (CoreUtilities.equalsIgnoreCase(value.asString(), "false")) {
            return false;
        }
        if (CoreUtilities.equalsIgnoreCase(value.asString(), "true")) {
            return true;
        }
        throw new InvalidArgumentsRuntimeException("Input to boolean argument '" + arg.name + "' of '" + value + "' is invalid: must specify either 'true' or 'false'!");
    }

    /** Used for generated calls. */
    public static Enum helperEnumArg(ScriptEntry entry, ArgData arg) {
        ScriptEntry.EnumArg givenArg = entry.internal.enumVals[arg.index];
        if (givenArg != null) {
            if (givenArg.rawValue != null) {
                return givenArg.rawValue;
            }
            Argument dynamicArg = entry.argAtIndex(false, givenArg.argIndex);
            ElementTag value = dynamicArg.asElement();
            if (value != null) {
                Enum enumVal = value.asEnum(arg.type);
                if (enumVal == null) {
                    throw new InvalidArgumentsRuntimeException("Invalid input to '" + arg.name + "' argument. Does not match enum of value options. Must be one of: " + String.join(", ", EnumHelper.get(arg.type).valuesMapLower.keySet()));
                }
                return enumVal;
            }
        }
        if (arg.required) {
            throw new InvalidArgumentsRuntimeException("Must specify input to '" + arg.name + "' argument. Did you forget an argument? Check meta docs!");
        }
        return (Enum) arg.defaultObject;
    }

    public static ListTag getListFor(ScriptEntry entry, ArgData arg) {
        Argument givenArg = getArgumentFor(entry, arg);
        ListTag list;
        if (givenArg == null) {
            if (arg.defaultObject == null) {
                return null;
            }
            list = (ListTag) arg.defaultObject;
        }
        else {
            list = givenArg.asType(ListTag.class);
        }
        if (list == null) {
            throw new InvalidArgumentsRuntimeException("Invalid input to '" + arg.name + "': '" + givenArg.getValue() + "': not a valid ListTag");
        }
        return list;
    }

    /** Used for generated calls. */
    public static List<? extends ObjectTag> helperPrefixListObject(ScriptEntry entry, ArgData arg) {
        ListTag list = getListFor(entry, arg);
        if (list == null) {
            return null;
        }
        return list.filter(arg.subType, entry);
    }

    /** Used for generated calls. */
    public static List<? extends Enum> helperPrefixListEnum(ScriptEntry entry, ArgData arg) {
        ListTag list = getListFor(entry, arg);
        if (list == null) {
            return null;
        }
        List output = new ArrayList(list.size());
        for (String str : list) {
            Enum val = ElementTag.asEnum(arg.subType, str);
            if (val == null) {
                throw new InvalidArgumentsRuntimeException("Invalid input to '" + arg.name + "': '" + str + "': not a valid "
                        + DebugInternals.getClassNameOpti(arg.subType) + ", must be one of: " + String.join(", ", EnumHelper.get(arg.type).valuesMapLower.keySet()));
            }
            output.add(val);
        }
        return output;
    }

    public static ElementTag getElementForPrefix(ScriptEntry entry, ArgData arg) {
        Argument givenArg = getArgumentFor(entry, arg);
        if (givenArg == null) {
            return null;
        }
        return arg.getRaw ? givenArg.getRawElement() : givenArg.asElement();
    }

    /** Used for generated calls. */
    public static ElementTag helperPrefixElement(ScriptEntry entry, ArgData arg) {
        ElementTag value = getElementForPrefix(entry, arg);
        if (value == null) {
            return (ElementTag) arg.defaultObject;
        }
        return value;
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
            Enum result = value.asEnum(arg.type);
            if (result == null) {
                throw new InvalidArgumentsRuntimeException("Invalid input to '" + arg.name + "' argument. Does not match enum of value options. Must be one of: " + String.join(", ", EnumHelper.get(arg.type).valuesMapLower.keySet()));
            }
            return result;
        }
        return (Enum) arg.defaultObject;
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
        throw new InvalidArgumentsRuntimeException("Input to boolean argument '" + arg.name + "' of '" + value + "' is invalid: must specify either 'true' or 'false'!");
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
            throw new InvalidArgumentsRuntimeException("Input to integer argument '" + arg.name + "' of '" + value + "' is invalid: must specify an integer number!");
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
            throw new InvalidArgumentsRuntimeException("Input to decimal argument '" + arg.name + "' of '" + value + "' is invalid: must specify a decimal number!");
        }
    }

    /** Used for generated calls. */
    public static String helperDebugFormat(Object value, ArgData arg) {
        if (value == null) {
            return "";
        }
        return ArgumentHelper.debugObj(arg.name, value);
    }

    public static CommandExecutor generateExecutorFor(Class<? extends AbstractCommand> cmdClass, AbstractCommand cmd) {
        try {
            Method method = Arrays.stream(cmdClass.getDeclaredMethods()).filter(m -> Modifier.isStatic(m.getModifiers()) && m.getName().equals("autoExecute")).findFirst().orElse(null);
            if (method == null) {
                return null;
            }
            // ====== Gen class ======
            String cmdCleanName = CodeGenUtil.cleanName(DebugInternals.getClassNameOpti(cmdClass).replace('.', '_'));
            if (cmdCleanName.length() > 50) {
                cmdCleanName = cmdCleanName.substring(0, 50);
            }
            String className = CodeGenUtil.COMMAND_GEN_PACKAGE + "Cmd" + (totalGenerated++) + "_" + cmdCleanName;
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, COMMAND_EXECUTOR_PATH, new String[]{});
            cw.visitSource("GENERATED_CMD_EXEC", null);
            MethodGenerator.genDefaultConstructor(cw, className, COMMAND_EXECUTOR_PATH);
            // ====== Gen 'execute' method ======
            ArrayList<ArgData> args = new ArrayList<>();
            ArrayList<MethodGenerator.Local> argLocals = new ArrayList<>();
            {
                MethodGenerator gen = MethodGenerator.generateMethod(className, cw, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, "execute", COMMAND_EXECUTOR_EXECUTE_DESCRIPTOR);
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
                    ArgLinear argLinear = param.getAnnotation(ArgLinear.class);
                    ArgSubType argSubType = param.getAnnotation(ArgSubType.class);
                    if (argName == null) {
                        Debug.echoError("Cannot generate executor for command '" + cmdClass.getName() + "': autoExecute method has param '" + param.getName() + "' which lacks a proper naming parameter.");
                        return null;
                    }
                    ArgData argData = new ArgData();
                    argData.shouldDebug = !param.isAnnotationPresent(ArgNoDebug.class);
                    argData.getRaw = param.isAnnotationPresent(ArgRaw.class);
                    argData.shouldParse = !param.isAnnotationPresent(ArgUnparsed.class);
                    argData.subType = argSubType == null ? null : argSubType.value();
                    argData.type = paramType;
                    argData.name = argName.value();
                    if (!param.isAnnotationPresent(ArgDefaultNull.class)) {
                        if (argDefaultText == null) {
                            argData.required = true;
                        }
                        else {
                            argData.defaultValue = argDefaultText.value();
                            boolean needsConvert = true;
                            if (ObjectTag.class.isAssignableFrom(argData.type)) {
                                argData.defaultObject = new ElementTag(argData.defaultValue).asType(argData.type, CoreUtilities.noDebugContext);
                            }
                            else if (Enum.class.isAssignableFrom(argData.type)) {
                                argData.defaultObject = EnumHelper.get(argData.type).valuesMapLower.get(EnumHelper.cleanKey(argData.defaultValue));
                            }
                            else if (argData.type == List.class && argData.subType != null) {
                                argData.defaultObject = ListTag.valueOf(argData.defaultValue, CoreUtilities.noDebugContext);
                            }
                            else {
                                needsConvert = false;
                            }
                            if (needsConvert && argData.defaultObject == null) {
                                Debug.echoError("Cannot generate executor for command '" + cmdClass.getName() + "': autoExecute method has param '" + argData.name
                                        + "' which specifies default value '" + argData.defaultValue + "' which is a not a valid '" + DebugInternals.getClassNameOpti(argData.type) + "'");
                                return null;
                            }
                        }
                    }
                    MethodGenerator.Local argLocal = gen.addLocal("arg_" + args.size() + "_" + CodeGenUtil.cleanName(argData.name), paramType);
                    Method argMethod = null;
                    boolean doCast = false;
                    if (argPrefixed != null || argLinear != null) {
                        if (argPrefixed != null) {
                            argData.index = cmd.setPrefixHandled(argData.name);
                        }
                        else {
                            if (cmd.generatorInfiniteArgs) {
                                Debug.echoError("Cannot generate executor for command '" + cmdClass.getName() + "': autoExecute method has param '" + argData.name + "' which is linear, after an unlimited linear arg.");
                                return null;
                            }
                            argData.index = cmd.linearHandledCount++;
                            argData.isLinear = true;
                        }
                        if (argData.isLinear && paramType == List.class && !argData.shouldParse && argData.subType == null) {
                            cmd.generatorInfiniteArgs = true;
                            argMethod = HELPER_GET_UNPARSED_ARG_LIST;
                        }
                        else if (paramType == List.class && argData.subType != null && ObjectTag.class.isAssignableFrom(argData.subType)) {
                            argMethod = HELPER_PREFIX_LIST_OBJECT_METHOD;
                        }
                        else if (paramType == List.class && argData.subType != null && Enum.class.isAssignableFrom(argData.subType)) {
                            argMethod = HELPER_PREFIX_LIST_ENUM_METHOD;
                        }
                        else if (paramType == ElementTag.class) {
                            argMethod = HELPER_PREFIX_ELEMENT_METHOD;
                        }
                        else if (ObjectTag.class.isAssignableFrom(paramType)) {
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
                        argData.index = cmd.setBooleanHandled(argData.name);
                    }
                    else if (Enum.class.isAssignableFrom(paramType)) {
                        argMethod = HELPER_ENUM_ARG_METHOD;
                        argData.index = cmd.setEnumHandled(argData.name, (Class<? extends Enum>) paramType);
                        doCast = true;
                    }
                    else {
                        Debug.echoError("Cannot generate executor for command '" + cmdClass.getName() + "': autoExecute method has param '" + argData.name + "' which does not have a valid order specifier (Linear, Prefixed, ...).");
                        return null;
                    }
                    if (argMethod == null) {
                        Debug.echoError("Cannot generate executor for command '" + cmdClass.getName() + "': autoExecute method has param '" + argData.name + "' of type '" + paramType.getName() + "' which is not supported.");
                        return null;
                    }
                    gen.loadLocal(scriptEntryLocal);
                    gen.loadStaticField(className, argLocal.name, argData.getClass());
                    gen.invokeStatic(argMethod);
                    if (doCast) {
                        gen.cast(paramType);
                    }
                    gen.storeLocal(argLocal);
                    argLocals.add(argLocal);
                    args.add(argData);
                }
                gen.advanceAndLabel();
                if (cmd.generateDebug) {
                    Label afterDebugLabel = new Label();
                    gen.loadLocal(scriptEntryLocal);
                    gen.invokeVirtual(SCRIPTENTRY_SHOULDDEBUG_METHOD);
                    gen.jumpIfFalseTo(afterDebugLabel);
                    gen.loadLocal(scriptEntryLocal);
                    gen.loadString(cmd.getName());
                    gen.loadInt(args.size());
                    gen.createArray(Object.class);
                    for (int i = 0; i < argLocals.size(); i++) {
                        ArgData arg = args.get(i);
                        if (arg.shouldDebug) {
                            MethodGenerator.Local local = argLocals.get(i);
                            gen.stackDuplicate();
                            gen.loadInt(i);
                            gen.loadLocal(local);
                            gen.autoBox(local.descriptor);
                            gen.loadStaticField(className, local.name, arg.getClass());
                            gen.invokeStatic(HELPER_DEBUG_FORMAT_METHOD);
                            gen.arrayStore(Object.class);
                        }
                    }
                    gen.invokeStatic(DEBUG_REPORT_METHOD);
                    gen.advanceAndLabel(afterDebugLabel);
                }
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
            CommandExecutor result = (CommandExecutor) generatedClass.getConstructors()[0].newInstance();
            result.args = args.toArray(new ArgData[0]);
            return result;
        }
        catch (Throwable ex) {
            Debug.echoError(ex);
            return null;
        }
    }
}
