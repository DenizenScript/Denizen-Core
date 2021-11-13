package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagContext;

public class Debug {

    public static boolean showScriptBuilder = false;

    public static boolean showEventsTrimming = false;

    public static boolean verbose = false;

    public static boolean showLoading = false;

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
        DenizenCore.implementation.debugError((String) null, error);
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
            DenizenCore.implementation.debugError(addedContext, error);
        }
    }

    public static void echoError(ScriptContainer script, String error) {
        echoError(script, null, error);
    }

    public static void echoError(ScriptContainer script, String addedContext, String error) {
        if (script != null) {
            addedContext = " <LR>In script '<A>" + script.getName() + "<LR>'" + (addedContext == null ? "" : addedContext);
        }
        if (script == null) {
            DenizenCore.implementation.debugError(addedContext, error);
        }
        else {
            DenizenCore.implementation.debugError(addedContext, error);
        }
    }

    public static void echoError(ScriptEntry entry, String error) {
        echoError(entry, null, error);
    }

    public static void echoError(ScriptEntry entry, String addedContext, String error) {
        if (entry == null) {
            DenizenCore.implementation.debugError(addedContext, error);
        }
        else if (entry.getResidingQueue() == null) {
            if (entry.getScript() != null) {
                addedContext = " <LR>In script '<A>" + entry.getScript().getName() + "<LR>' on line <A>" + entry.internal.lineNumber + "<LR>" + (addedContext == null ? "" : addedContext);
            }
            DenizenCore.implementation.debugError(addedContext, error);
        }
        else {
            DenizenCore.implementation.debugError(entry.getResidingQueue(), addedContext, error);
        }
    }

    public static void echoError(ScriptQueue queue, String error) {
        DenizenCore.implementation.debugError(queue, null, error);
    }

    public static void echoError(ScriptQueue queue, Throwable error) {
        DenizenCore.implementation.debugError(queue, error);
    }

    public static void echoError(Throwable ex) {
        DenizenCore.implementation.debugException(ex);
    }

    public static void log(String message) {
        DenizenCore.implementation.debugMessage(message);
    }

    public static void log(String caller, String message) {
        DenizenCore.implementation.debugMessage(caller, message);
    }

    public static void echoApproval(String message) {
        DenizenCore.implementation.debugApproval(message);
    }

    public static void echoDebug(Debuggable entry, String message) {
        DenizenCore.implementation.debugEntry(entry, message);
    }

    public static void echoDebug(Debuggable entry, DebugElement element, String message) {
        DenizenCore.implementation.debugEntry(entry, element, message);
    }

    public static void echoDebug(Debuggable entry, DebugElement element) {
        DenizenCore.implementation.debugEntry(entry, element);
    }

    public static void report(Debuggable caller, String name, String message) {
        DenizenCore.implementation.debugReport(caller, name, message);
    }

    public static void report(Debuggable caller, String name, Object... values) {
        DenizenCore.implementation.debugReport(caller, name, values);
    }
}
