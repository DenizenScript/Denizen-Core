package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreConfiguration;

import java.util.Stack;

public class Debug {

    /** Current debug recording text, if recording enabled, for submission to paste server. */
    public static StringBuilder debugRecording = new StringBuilder();

    /** Current main thread context, maintained automatically by stacked calls, for error handling. */
    public static TagContext currentContext = null;

    /** Stack trace helper for current error context. */
    public static Stack<Object> errorContextStack = new Stack<>();

    /** Push an error context object to the stack. */
    public static void pushErrorContext(Object context) {
        if (DenizenCore.isMainThread()) {
            errorContextStack.push(context);
        }
    }

    // NOTE: the '2' and '3' pushes are intentionally separate methods, for naming reasons (you must know the exact number) and performance reasons (using '...' causes an object construction).

    /** Push 2 error context objects to the stack. */
    public static void push2ErrorContexts(Object context1, Object context2) {
        if (DenizenCore.isMainThread()) {
            errorContextStack.push(context1);
            errorContextStack.push(context2);
        }
    }

    /** Push 3 error context objects to the stack. */
    public static void push3ErrorContexts(Object context1, Object context2, Object context3) {
        if (DenizenCore.isMainThread()) {
            errorContextStack.push(context1);
            errorContextStack.push(context2);
            errorContextStack.push(context3);
        }
    }

    /** Pop the top of the error context stack. */
    public static void popErrorContext() {
        popErrorContext(1);
    }

    /** Pop the top X entries of the error context stack. */
    public static void popErrorContext(int count) {
        if (DenizenCore.isMainThread()) {
            for (int i = 0; i < count; i++) {
                errorContextStack.pop();
            }
        }
    }

    /** Start debug recording, for submission to paste server. */
    public static void startRecording() {
        if (!CoreConfiguration.debugRecordingAllowed) {
            Debug.echoError("Cannot start debug recording: forbidden by config.");
            return;
        }
        CoreConfiguration.shouldRecordDebug = true;
        debugRecording = new StringBuilder();
    }

    /** Stop debug recording, to cancel a startRecording. */
    public static void stopRecording() {
        CoreConfiguration.shouldRecordDebug = false;
        debugRecording = new StringBuilder();
    }

    /**
     * Can be used with echoDebug(...) to output a header, footer,
     * or a spacer.
     * <p/>
     * DebugElement.Header = +- string description ------+
     * DebugElement.Spacer =
     * DebugElement.Footer = +--------------+
     * <p/>
     * Also includes color.
     */
    public enum DebugElement {
        Header, Footer, Spacer
    }

    /** Echos an error message, using automatically gathered context. */
    public static void echoError(String error) {
        echoError(currentContext, error);
    }

    /** Echos an error message, using manually specified context. */
    public static void echoError(TagContext context, String error) {
        echoError(context, null, error);
    }

    /** Echos an error message, using manually specified context and optional extra text context. */
    public static void echoError(TagContext context, String addedContext, String error) {
        if (context == null) {
            context = currentContext;
        }
        if (context != null && context.entry != null) {
            echoError(context.entry, addedContext, error);
        }
        else if (context != null && context.script != null) {
            echoError(context.script.getContainer(), addedContext, error);
        }
        else {
            DenizenCore.runOnMainThread(() -> DebugInternals.echoErrorInternal(null, addedContext, error, true));
        }
    }

    /** Echos an error message, using a specific script container as the context source. */
    public static void echoError(ScriptContainer script, String error) {
        echoError(script, null, error);
    }

    /** Echos an error message, using a specific script container as the context source with optional extra text content. */
    public static void echoError(ScriptContainer script, String addedContext, String error) {
        Debug.pushErrorContext(script);
        try {
            DenizenCore.runOnMainThread(() -> DebugInternals.echoErrorInternal(null, addedContext, error, true));
        }
        finally {
            Debug.popErrorContext();
        }
    }

    /** Echos an error message, using a specific script entry as the context source. */
    public static void echoError(ScriptEntry entry, String error) {
        echoError(entry, null, error);
    }

    /** Echos an error message, using a specific script entry as the context source with optional extra text content. */
    public static void echoError(ScriptEntry entry, String addedContext, String error) {
        DenizenCore.runOnMainThread(() -> DebugInternals.echoErrorInternal(entry, addedContext, error, true));
    }

    /** Echos an exception error message, with automatic context filling. */
    public static void echoError(Throwable ex) {
        DenizenCore.runOnMainThread(() -> DebugInternals.echoExceptionInternal(currentContext == null ? null : currentContext.entry, ex));
    }
    /** Echos an exception error message, with a specific script entry as the context source. */
    public static void echoError(ScriptEntry entry, Throwable error) {
        DenizenCore.runOnMainThread(() -> DebugInternals.echoExceptionInternal(entry, error));
    }

    /** Logs output text only if Verbose is enabled. */
    public static void verboseLog(String message) {
        if (CoreConfiguration.debugVerbose) {
            String caller = DebugInternals.getCaller();
            DenizenCore.runOnMainThread(() -> DebugInternals.logInternal(caller, message));
        }
    }

    /** Logs contextless output information with a generated caller classname label. */
    public static void log(String message) {
        String caller = DebugInternals.getCaller();
        DenizenCore.runOnMainThread(() -> DebugInternals.logInternal(caller, message));
    }

    /** Logs contextless output information with a specified caller label. */
    public static void log(String caller, String message) {
        DenizenCore.runOnMainThread(() -> DebugInternals.logInternal(caller, message));
    }

    /** Logs contextless output information with a given formatting element. */
    public static void log(DebugElement element, String message) {
        if (!CoreConfiguration.shouldShowDebug) {
            return;
        }
        switch (element) {
            case Footer:
                message = "<LP>+---------------------+";
                break;
            case Header:
                message = "<LP>+- " + message + "<LP> ---------+";
                break;
        }
        final String finalMessage = message;
        DenizenCore.runOnMainThread(() -> DebugInternals.finalOutputDebugText(finalMessage, null, true));
    }

    /** Echos a contextless "Okay!" message. */
    public static void echoApproval(String message) {
        DenizenCore.runOnMainThread(() -> {
            if (!CoreConfiguration.shouldShowDebug) {
                return;
            }
            DebugInternals.finalOutputDebugText("<GR>OKAY! <W>" + message, null, true);
        });
    }

    /** Echoes general debug on a given caller. Only shows if debug is enabled for the caller. */
    public static void echoDebug(Debuggable entry, DebugElement element) {
        if (!CoreConfiguration.shouldShowDebug || !shouldDebug(entry)) {
            return;
        }
        echoDebug(entry, element, null);
    }

    /** Echoes general debug on a given caller. Only shows if debug is enabled for the caller. */
    public static void echoDebug(Debuggable entry, String message) {
        if (!CoreConfiguration.shouldShowDebug || !shouldDebug(entry)) {
            return;
        }
        DenizenCore.runOnMainThread(() -> {
            DebugInternals.echo("<W>" + DebugInternals.trimMessage(message), entry);
            if (CoreConfiguration.debugVerbose && entry != null) {
                DebugInternals.echo("<LG>(Verbose) Caller = " + entry, entry);
            }
        });
    }

    /** Echoes general debug on a given caller. Only shows if debug is enabled for the caller. */
    public static void echoDebug(Debuggable entry, DebugElement element, String message) {
        if (!CoreConfiguration.shouldShowDebug || !shouldDebug(entry)) {
            return;
        }
        switch (element) {
            case Footer:
                message = "<LP>+---------------------+";
                break;
            case Header:
                message = "<LP>+- " + message + "<LP> ---------+";
                break;
        }
        final String finalMessage = message;
        DenizenCore.runOnMainThread(() -> DebugInternals.echo(finalMessage, entry));
    }

    /** Reports a command's output to debug, if debug is enabled for the entry. */
    public static void report(Debuggable caller, String name, String message) {
        if (!CoreConfiguration.shouldShowDebug || !shouldDebug(caller)) {
            return;
        }
        DenizenCore.runOnMainThread(() -> DebugInternals.echo("<Y>+> <G>Executing '<Y>" + name + "<G>': " + DebugInternals.trimMessage(message), caller));
    }

    /** Reports a command's output to debug, if debug is enabled for the entry. */
    public static void report(Debuggable caller, String name, Object... values) {
        if (!CoreConfiguration.shouldShowDebug || !shouldDebug(caller)) {
            return;
        }
        StringBuilder output = new StringBuilder();
        for (Object obj : values) {
            if (obj == null) {
                continue;
            }
            if (obj instanceof ObjectTag) {
                ObjectTag objTag = (ObjectTag) obj;
                output.append("<G>").append(objTag.getPrefix()).append("='<Y>").append(objTag.debuggable()).append("<G>'  ");
            }
            else {
                output.append(obj);
            }
        }
        DenizenCore.runOnMainThread(() -> DebugInternals.echo("<Y>+> <G>Executing '<Y>" + name + "<G>': " + DebugInternals.trimMessage(output.toString()), caller));
    }

    /** Returns truie if the debuggable object should output debug. */
    public static boolean shouldDebug(Debuggable caller) {
        if (CoreConfiguration.debugOverride) {
            return true;
        }
        if (!CoreConfiguration.shouldShowDebug) {
            return false;
        }
        if (caller != null) {
            return caller.shouldDebug();
        }
        return true;
    }
}
