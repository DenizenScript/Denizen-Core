package net.aufdemrand.denizencore.utilities.debugging;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.core.ConsoleOutputScriptEvent;

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
        event.reset();
        event.message = DenizenCore.getImplementation().cleanseLogString(s);
        event.fire();
        if (!event.cancelled) {
            super.print(s);
        }
        antiLoop = false;
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
