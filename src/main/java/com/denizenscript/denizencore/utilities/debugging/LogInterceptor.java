package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.core.ConsoleOutputScriptEvent;

import java.io.PrintStream;

/**
 * Intercepts system.out operations for the sake of blocking messages at request.
 * Disabled by default in config.yml
 */
public class LogInterceptor extends PrintStream {
    boolean redirected = false;
    public PrintStream standardOut;

    public LogInterceptor() {
        super(System.out, true);
    }

    @Override
    public void print(String s) {
        if (antiLoop) {
            super.print(s);
            return;
        }
        antiLoop = true;
        ConsoleOutputScriptEvent event = ConsoleOutputScriptEvent.instance;
        event.message = DenizenCore.getImplementation().cleanseLogString(s);
        event.fire();
        if (!event.cancelled) {
            super.print(s);
        }
        antiLoop = false;
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    @Override
    public void print(char[] buf) {
        print(new String(buf));
    }

    private boolean antiLoop = false;

    public void redirectOutput() {
        if (redirected) {
            return;
        }
        standardOut = System.out;
        System.setOut(this);
    }

    public void standardOutput() {
        if (!redirected) {
            return;
        }
        System.setOut(standardOut);
    }
}
