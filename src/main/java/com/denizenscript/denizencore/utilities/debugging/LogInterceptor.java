package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.core.ConsoleOutputScriptEvent;

import java.io.PrintStream;

/**
 * Intercepts system.out operations for the sake of blocking messages at request.
 * Disabled by default in config.yml
 */
public class LogInterceptor extends PrintStream {
    public boolean redirected = false;
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
        try {
            ConsoleOutputScriptEvent event = ConsoleOutputScriptEvent.instance;
            event.message = DenizenCore.implementation.cleanseLogString(s);
            event = (ConsoleOutputScriptEvent) event.fire();
            if (!event.cancelled) {
                super.print(s);
            }
        }
        finally {
            antiLoop = false;
        }
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    @Override
    public void print(char[] buf) {
        print(new String(buf));
    }

    public boolean antiLoop = false;

    public void redirectOutput() {
        if (redirected) {
            return;
        }
        redirected = true;
        if (System.out != this) {
            standardOut = System.out;
        }
        System.setOut(this);
    }

    public void standardOutput() {
        if (!redirected) {
            return;
        }
        redirected = false;
        System.setOut(standardOut);
    }
}
