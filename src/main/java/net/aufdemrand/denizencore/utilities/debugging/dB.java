package net.aufdemrand.denizencore.utilities.debugging;

import net.aufdemrand.denizencore.DenizenCore;

public class dB {

    public static void echoError(String error) {
        DenizenCore.getImplementation().debugError(error);
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
}
