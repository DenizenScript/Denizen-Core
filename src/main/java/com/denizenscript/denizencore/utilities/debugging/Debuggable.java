package com.denizenscript.denizencore.utilities.debugging;

public interface Debuggable {

    public boolean shouldDebug();

    public boolean shouldFilter(String criteria) throws Exception;
}
