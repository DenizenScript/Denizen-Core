package net.aufdemrand.denizencore.utilities.debugging;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.OldEventManager;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dObject;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

    // <--[event]
    // @Events
    // console output
    //
    // @Warning Disable debug on this event or you'll get an infinite loop!
    //
    // @Triggers when any message is printed to console. (Requires <@link mechanism system.redirect_logging> be set true.)
    // @Context
    // <context.message> returns the messsage that is being printed to console.
    //
    // @Determine
    // "CANCELLED" to disable the output.
    //
    // -->
    @Override
    public void print(String s) {
        HashMap<String, dObject> context = new HashMap<String, dObject>();
        context.put("message", new Element(DenizenCore.getImplementation().cleanseLogString(s)));
        List<String> Determinations = OldEventManager.doEvents(Arrays.asList("console output"), // TODO: ScriptEvent
                DenizenCore.getImplementation().getEmptyScriptEntryData(), context);
        for (String str : Determinations) {
            if (str.equalsIgnoreCase("cancelled")) {
                return;
            }
        }
        super.print(s);
    }

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
