package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.core.ConsoleOutputScriptEvent;
import com.denizenscript.denizencore.events.core.ScriptGeneratesErrorScriptEvent;
import com.denizenscript.denizencore.events.core.ServerGeneratesExceptionScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.CommandExecutor;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DebugInternals {

    /** Called once per tick to reset some tracked fields. */
    public static void onTick() {
        outputThisTick = 0;
        errorDuplicatePrevention = false;
        lastErrorHeader = "";
        Debug.errorContextStack.clear();
    }

    /** An implementation can replace trimming if desired. */
    public static Function<String, String> alternateTrimLogic;

    /** Some debug methods trim to keep super-long messages from hitting the console. */
    public static String trimMessage(String message) {
        if (!CoreConfiguration.debugShouldTrim) {
            return message;
        }
        if (alternateTrimLogic != null) {
            return alternateTrimLogic.apply(message);
        }
        int trimSize = CoreConfiguration.debugTrimLength;
        if (message.length() > trimSize) {
            message = message.substring(0, (trimSize / 2) - 10) + "... *snip!*..." + message.substring(message.length() - ((trimSize / 2) - 10));
        }
        return message;
    }

    /** Used to prevent error recursion. */
    public static boolean errorDuplicatePrevention = false;

    /** Outputs an error message. Main internal call. Prefer the other method overloads. */
    public static void echoErrorInternal(ScriptEntry source, String addedContext, String message, boolean reformat) {
        message = DenizenCore.implementation.applyDebugColors(message);
        if (errorDuplicatePrevention) {
            if (!CoreConfiguration.debugVerbose) {
                finalOutputDebugText("Error within error (??!!!! SOMETHING WENT SUPER WRONG!): " + message, source, reformat);
            }
            return;
        }
        errorDuplicatePrevention = true;
        ScriptQueue sourceQueue = CommandExecutor.currentQueue;
        if (source == null && sourceQueue != null) {
            source = sourceQueue.getLastEntryExecuted();
        }
        if (source != null && source.queue != null) {
            sourceQueue = source.queue;
        }
        ScriptTag sourceScript = null;
        if (source != null) {
            sourceScript = source.getScript();
        }
        if (throwErrorEvent) {
            throwErrorEvent = false;
            boolean cancel = ScriptGeneratesErrorScriptEvent.instance.handle(message, sourceQueue, sourceScript, source == null ? -1 : source.internal.lineNumber);
            throwErrorEvent = true;
            if (cancel) {
                errorDuplicatePrevention = false;
                return;
            }
        }
        if (!CoreConfiguration.shouldShowDebug) {
            errorDuplicatePrevention = false;
            return;
        }
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append(ERROR_HEADER_START);
        if (sourceScript != null) {
            headerBuilder.append(" in script '<A>").append(sourceScript.getName()).append("<LR>'");
        }
        if (sourceQueue != null) {
            headerBuilder.append(" in queue '").append(sourceQueue.debugId).append("<LR>'");
        }
        if (source != null) {
            headerBuilder.append(" while executing command '<A>").append(source.getCommandName()).append("<LR>'");
            if (sourceScript != null) {
                headerBuilder.append(" in file '<A>").append(sourceScript.getContainer().getRelativeFileName())
                        .append("<LR>' on line '<A>").append(source.internal.lineNumber).append("<LR>'");
            }
            DenizenCore.implementation.addExtraErrorHeaders(headerBuilder, source);
        }
        HashSet<Object> duplicatePrevention = new HashSet<>();
        for (Object context : Debug.errorContextStack) {
            if (context instanceof ScriptTag scriptTag) {
                context = scriptTag.getContainer();
            }
            else if (context instanceof QueueTag queueTag) {
                context = queueTag.getQueue();
            }
            else if (context instanceof Supplier<?> supplier) {
                context = supplier.get();
            }
            if (!duplicatePrevention.add(context)) {
                continue;
            }
            if (context instanceof ScriptContainer container) {
                if (sourceScript == null || context != sourceScript.getContainer()) {
                    headerBuilder.append(" in script '<A>").append(container.getName()).append("<LR>'");
                }
            }
            else if (context instanceof ScriptQueue queue) {
                if (context != sourceQueue) {
                    headerBuilder.append(" in queue '").append(queue.debugId).append("<LR>'");
                }
            }
            else if (context instanceof String str) {
                headerBuilder.append(" ").append(str);
            }
            else if (context instanceof ObjectTag objectTag) {
                headerBuilder.append(objectTag.getErrorHeaderContext());
            }
            else if (context != null) {
                headerBuilder.append("\n<FORCE_ALIGN>With non-context-valid object '<A>").append(context).append("<LR>'");
            }
        }
        if (addedContext != null) {
            headerBuilder.append("\n<FORCE_ALIGN>").append(addedContext);
        }
        headerBuilder.append(ERROR_HEADER_END);
        String header = headerBuilder.toString();
        boolean showDebugSuffix = sourceScript != null && !sourceScript.getContainer().shouldDebug();
        String headerRef = header;
        if (header.equals(lastErrorHeader)) {
            header = ADDITIONAL_ERROR_HEADER;
            showDebugSuffix = false;
        }
        finalOutputDebugText(header + message + (showDebugSuffix ? ENABLE_DEBUG_MESSAGE : ""), sourceQueue, reformat);
        errorDuplicatePrevention = false;
        if (CoreConfiguration.debugVerbose && CoreConfiguration.debugUltraVerbose && depthCorrectError == 0) {
            depthCorrectError++;
            try {
                throw new RuntimeException("Verbose info for above error");
            }
            catch (Throwable e) {
                Debug.echoError(source, e);
            }
            depthCorrectError--;
        }
        lastErrorHeader = headerRef;
    }

    public static String lastErrorHeader = "";
    public static String ENABLE_DEBUG_MESSAGE = "<LG> ... <LR>Enable debug on the script for more information.",
            ERROR_HEADER_START = "<LR> ERROR",
            ERROR_HEADER_END = "!\n<LG><FORCE_ALIGN>Error Message: <W>",
            ADDITIONAL_ERROR_HEADER = "<LG>Additional Error Info: <W>";

    /** Exception recursion prevention. */
    public static long depthCorrectError = 0;

    /** Exception recursion prevention. */
    public static boolean throwErrorEvent = true;

    /** Helper method to unroll an Exception into a clean loggable String. */
    public static String getFullExceptionMessage(Throwable ex, boolean includeBounding) {
        StringBuilder errorMessage = new StringBuilder();
        if (includeBounding) {
            errorMessage.append("Internal exception was thrown!\n");
        }
        String prefix = includeBounding ? "<LG>[Error Continued] <W>" : "";
        boolean first = true;
        while (ex != null) {
            errorMessage.append(prefix);
            if (!first) {
                errorMessage.append("Caused by: ");
            }
            errorMessage.append(ex).append('\n');
            for (StackTraceElement ste : ex.getStackTrace()) {
                errorMessage.append(prefix).append("  ").append(ste).append('\n');
            }
            if (ex.getCause() == ex) {
                break;
            }
            ex = ex.getCause();
            first = false;
        }
        return errorMessage.toString();
    }

    /** Internal exception log handling. */
    public static void echoExceptionInternal(ScriptEntry source, Throwable ex) {
        boolean wasThrowAllowed = throwErrorEvent;
        throwErrorEvent = false;
        try {
            String errorMessage = getFullExceptionMessage(ex, true);
            if (wasThrowAllowed) {
                Throwable thrown = ex;
                while (thrown.getCause() != null) {
                    thrown = thrown.getCause();
                }
                ScriptQueue sourceQueue = CommandExecutor.currentQueue;
                if (source == null && sourceQueue != null) {
                    source = sourceQueue.getLastEntryExecuted();
                }
                if (source != null && source.queue != null) {
                    sourceQueue = source.queue;
                }
                ScriptTag sourceScript = null;
                if (source != null) {
                    sourceScript = source.getScript();
                }
                boolean cancel = ServerGeneratesExceptionScriptEvent.instance.handle(thrown, errorMessage, sourceQueue, sourceScript, source == null ? -1 : source.internal.lineNumber);
                if (cancel) {
                    return;
                }
            }
            if (!CoreConfiguration.shouldShowDebug) {
                return;
            }
            if (!CoreConfiguration.debugStackTraces) {
                Debug.echoError(source, "Exception! Enable '/denizen debug -s' for the nitty-gritty.");
            }
            else {
                depthCorrectError++;
                echoErrorInternal(source, null, errorMessage, false);
                depthCorrectError--;
            }
        }
        finally {
            throwErrorEvent = wasThrowAllowed;
        }
    }

    /** A sender to use if there isn't a current queue, for limited contexts like the 'reload' command. */
    public static volatile Consumer<String> specialBackupSender = null;

    /** Gets an extra path to send debug to, if relevant. */
    public static Consumer<String> getDebugSender(Debuggable caller) {
        if (caller == null) {
            caller = CommandExecutor.currentQueue;
        }
        if (caller instanceof TagContext context && context.entry != null) {
            caller = context.entry;
        }
        if (caller instanceof ScriptEntry entry && entry.getResidingQueue() != null) {
            caller = entry.getResidingQueue();
        }
        if (caller instanceof ScriptQueue queue) {
            return queue.debugOutput;
        }
        if (specialBackupSender != null) {
            return specialBackupSender;
        }
        // ScriptContainer can't be traced to a queue
        return null;
    }

    /** Internal debug method that handles checking whether the provided debuggable should submit to the debugger, and source tracking. */
    public static void echo(String string, Debuggable caller) {
        if (!Debug.shouldDebug(caller)) {
            return;
        }
        if (!CoreConfiguration.debugShowSources || caller == null) {
            finalOutputDebugText(string, caller, true);
            return;
        }
        String callerId;
        if (caller instanceof ScriptContainer container) {
            callerId = "Script:" + container.getName();
        }
        else if (caller instanceof ScriptEntry entry) {
            if (entry.getScript() != null) {
                callerId = "Command:" + entry.getCommandName() + " in Script:" + entry.getScript().getName();
            }
            else {
                callerId = "Command:" + entry.getCommandName();
            }
        }
        else if (caller instanceof ScriptQueue queue) {
            if (queue.script != null) {
                callerId = "Queue:" + queue.id + " running Script:" + queue.script.getName();
            }
            else {
                callerId = "Queue:" + queue.id;
            }
        }
        else if (caller instanceof TagContext context) {
            if (context.entry != null) {
                ScriptEntry sent = context.entry;
                if (sent.getScript() != null) {
                    callerId = "Tag in Command:" + sent.getCommandName() + " in Script:" + sent.getScript().getName();
                }
                else {
                    callerId = "Tag in Command:" + sent.getCommandName();
                }
            }
            else if (context.script != null) {
                callerId = "Tag in Script:" + context.script.getName();
            }
            else {
                callerId = "Tag:" + caller;
            }
        }
        else {
            callerId = caller.toString();
        }
        finalOutputDebugText("<G>[Src:<LG>" + callerId + "<G>] <W>" + string, caller, true);
    }

    /** Used to ratelimit debug output. */
    public static int outputThisTick = 0;

    /** Hack-fix to block multiple footers in a row. */
    public static boolean skipFooter = false;

    /** Date format used by debug recordings. */
    public static SimpleDateFormat debugRecordDateFormat = new SimpleDateFormat("HH:mm:ss");

    /** Internal final debug output called. Should generally not be called directly - instead use echoDebug, log, echoError, ... */
    public static void finalOutputDebugText(String message, Debuggable caller, boolean reformat) {
        lastErrorHeader = "";
        outputThisTick++;
        if (outputThisTick >= CoreConfiguration.debugLimitPerTick) {
            if (outputThisTick == CoreConfiguration.debugLimitPerTick) {
                internalFinalOutputPath("... Debug rate limit per-tick hit, edit config.yml to adjust this limit...", caller, true);
            }
            return;
        }
        // 'Hack-fix' for disallowing multiple 'footers' to print in a row
        if (message.equals("<LP>+---------------------+")) {
            if (skipFooter) {
                return;
            }
            skipFooter = true;
        }
        else {
            skipFooter = false;
        }
        internalFinalOutputPath(DenizenCore.implementation.applyDebugColors(message), caller, reformat);
    }

    /** Internal final debug output called. Should generally not be called directly - instead use echoDebug, log, echoError, ... */
    public static void internalFinalOutputPath(String message, Debuggable caller, boolean reformat) {
        message = message.replace('\0', ' ');
        String formatted = message;
        if (reformat) {
            StringBuilder buffer = new StringBuilder();
            int length = 0;
            for (String word : CoreUtilities.split(formatted, ' ')) {
                // # of total chars * # of lines - timestamp
                int strippedLength = DenizenCore.implementation.stripColor(word).length() + 1;
                if (length + strippedLength < CoreConfiguration.debugLineLength) {
                    buffer.append(word).append(" ");
                    length += strippedLength;
                }
                else {
                    // Increase # of lines to account for
                    length = strippedLength;
                    // Leave spaces to account for timestamp and indent
                    buffer.append("\n<FORCE_ALIGN>").append(word).append(" ");
                }
                if (word.contains("\n")) {
                    length = 0;
                }
            }
            formatted = buffer.toString();
        }
        // Record current buffer to the to-be-submitted buffer
        if (CoreConfiguration.shouldRecordDebug) {
            try {
                //                                                         "HH:mm:ss"
                String toRecord = " " + formatted.replace("<FORCE_ALIGN>", "        ")+ "\n";
                Debug.debugRecording.append(URLEncoder.encode(debugRecordDateFormat.format(new Date()) + toRecord, StandardCharsets.UTF_8));
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
        }
        if (DenizenCore.logInterceptor.redirected) {
            if (!DenizenCore.logInterceptor.antiLoop) {
                DenizenCore.logInterceptor.antiLoop = true;
                try {
                    ConsoleOutputScriptEvent event = ConsoleOutputScriptEvent.instance;
                    event.message = message;
                    event = (ConsoleOutputScriptEvent) event.fire();
                    if (event.cancelled) {
                        return;
                    }
                }
                finally {
                    DenizenCore.logInterceptor.antiLoop = false;
                }
            }
        }
        DenizenCore.implementation.doFinalDebugOutput(formatted);
        Consumer<String> additional = getDebugSender(caller);
        if (additional != null) {
            additional.accept(message);
        }
    }

    /** Used for "log" to get class names. */
    public static final Map<Class<?>, String> classNameCache = new WeakHashMap<>();

    /** Used for "log" to get class names. */
    public static boolean canGetClass = true;

    /** Helper to get a class name with less JVM overhead using a cache. */
    public static String getClassNameOpti(Class<?> clazz) {
        return classNameCache.computeIfAbsent(clazz, k -> clazz.getSimpleName());
    }

    /** Helper to get the caller for a 'log' message. */
    public static StackWalker.StackFrame getCallerStackFrame() {
        if (!canGetClass) {
            return null;
        }
        try {
            return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(stackFrameStream -> stackFrameStream.skip(3).findFirst().orElse(null));
        }
        catch (Throwable ex) {
            canGetClass = false;
            return null;
        }
    }

    /** Helper to get the name of the calling class for a 'log' message. */
    public static String getCaller() {
        StackWalker.StackFrame callerStackFrame = getCallerStackFrame();
        if (callerStackFrame == null) {
            return "<JVM-Block>";
        }
        String callerName = getClassNameOpti(callerStackFrame.getDeclaringClass());
        return callerName.length() > 16 ? callerName.substring(0, 12) + "..." : callerName;
    }

    /** Internal path to 'log' a contextless global message, with a specified class name. */
    public static void logInternal(String caller, String message) {
        if (!CoreConfiguration.shouldShowDebug) {
            return;
        }
        finalOutputDebugText("<Y>+> [" + caller + "] <W>" + trimMessage(message), null, true);
    }
}
