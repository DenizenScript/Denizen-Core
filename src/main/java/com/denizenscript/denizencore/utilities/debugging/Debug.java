package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

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
        DenizenCore.getImplementation().debugError(error);
    }

    public static void echoError(ScriptQueue queue, String error) {
        DenizenCore.getImplementation().debugError(queue, error);
    }

    public static void echoError(ScriptQueue queue, Throwable error) {
        DenizenCore.getImplementation().debugError(queue, error);
    }

    public static void echoError(Throwable ex) {
        DenizenCore.getImplementation().debugException(ex);
    }

    public static void log(String message) {
        DenizenCore.getImplementation().debugMessage(message);
    }

    public static void echoApproval(String message) {
        DenizenCore.getImplementation().debugApproval(message);
    }

    public static void echoDebug(Debuggable entry, String message) {
        DenizenCore.getImplementation().debugEntry(entry, message);
    }

    public static void echoDebug(Debuggable entry, DebugElement element, String message) {
        DenizenCore.getImplementation().debugEntry(entry, element, message);
    }

    public static void echoDebug(Debuggable entry, DebugElement element) {
        DenizenCore.getImplementation().debugEntry(entry, element);
    }

    public static void report(Debuggable caller, String name, String message) {
        DenizenCore.getImplementation().debugReport(caller, name, message);
    }
}
