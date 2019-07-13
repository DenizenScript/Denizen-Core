package com.denizenscript.denizencore.utilities.debugging;

public interface Debuggable {

    boolean shouldDebug();

    boolean shouldFilter(String criteria) throws Exception;
}
