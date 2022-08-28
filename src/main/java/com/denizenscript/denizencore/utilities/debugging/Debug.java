package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreConfiguration;

public class Debug {

    public static boolean showScriptBuilder = false;

    public static boolean showEventsTrimming = false;

    public static StringBuilder debugRecording = new StringBuilder();

    public static TagContext currentContext = null;

    public static void startRecording() {
        CoreConfiguration.shouldRecordDebug = true;
        debugRecording = new StringBuilder();
    }

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

    public static void echoError(String error) {
        echoError(currentContext, error);
    }

    public static void echoError(TagContext context, String error) {
        echoError(context, null, error);
    }

    public static void echoError(TagContext context, String addedContext, String error) {
        if (context != null && context.entry != null) {
            echoError(context.entry, addedContext, error);
        }
        else if (context != null && context.script != null) {
            echoError(context.script.getContainer(), addedContext, error);
        }
        else {
            DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugError(addedContext, error));
        }
    }

    public static void echoError(ScriptContainer script, String error) {
        echoError(script, null, error);
    }

    public static void echoError(ScriptContainer script, String addedContext, String error) {
        if (script != null) {
            addedContext = " <LR>In script '<A>" + script.getName() + "<LR>'" + (addedContext == null ? "" : addedContext);
        }
        final String text = addedContext;
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugError(text, error));
    }

    public static void echoError(ScriptEntry entry, String error) {
        echoError(entry, null, error);
    }

    public static void echoError(ScriptEntry entry, String addedContext, String error) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugError(entry, addedContext, error));
    }

    public static void echoError(Throwable ex) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugException(ex));
    }
    public static void echoError(ScriptEntry entry, Throwable error) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugError(entry, error));
    }

    public static void verboseLog(String message) {
        if (CoreConfiguration.debugVerbose) {
            DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugMessage(message));
        }
    }

    public static void log(String message) {
        if (DenizenCore.isMainThread()) { // Note: 'Log' needs this check to compensate for Stack expectations in Log internals
            DenizenCore.implementation.debugMessage(message);
        }
        else {
            DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugMessage(message));
        }
    }

    public static void log(String caller, String message) {
        if (DenizenCore.isMainThread()) { // Note: 'Log' needs this check to compensate for Stack expectations in Log internals
            DenizenCore.implementation.debugMessage(caller, message);
        }
        else {
            DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugMessage(caller, message));
        }
    }

    public static void log(DebugElement element, String message) {
        if (DenizenCore.isMainThread()) { // Note: 'Log' needs this check to compensate for Stack expectations in Log internals
            DenizenCore.implementation.debugMessage(element, message);
        }
        else {
            DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugMessage(element, message));
        }
        DenizenCore.implementation.debugMessage(element, message);
    }

    public static void echoApproval(String message) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugApproval(message));
    }

    public static void echoDebug(Debuggable entry, String message) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugEntry(entry, message));
    }

    public static void echoDebug(Debuggable entry, DebugElement element, String message) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugEntry(entry, element, message));
    }

    public static void echoDebug(Debuggable entry, DebugElement element) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugEntry(entry, element));
    }

    public static void report(Debuggable caller, String name, String message) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugReport(caller, name, message));
    }

    public static void report(Debuggable caller, String name, Object... values) {
        DenizenCore.runOnMainThread(() -> DenizenCore.implementation.debugReport(caller, name, values));
    }

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
