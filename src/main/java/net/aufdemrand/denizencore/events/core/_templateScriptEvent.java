package net.aufdemrand.denizencore.events.core;

import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.CoreUtilities;

import java.util.HashMap;

/**
 * A sample script event, use as the basis for creating new script events.
 */
public class _templateScriptEvent extends ScriptEvent {

    public _templateScriptEvent() {
        instance = this;
    }

    public static _templateScriptEvent instance;
    // public InternalEventClass event;
    public Element objectOne;
    public dList objectTwo;

    @Override
    public boolean couldMatch(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return lower.startsWith("template event"); // || ...
    }

    @Override
    public boolean matches(ScriptContainer scriptContainer, String s) {
        String lower = CoreUtilities.toLowerCase(s);
        return lower.endsWith("example")
                && checkSwitch(s, "example", "value");
    }

    @Override
    public String getName() {
        return "Template";
    }

    @Override
    public void init() {
        // Register internal events here
    }

    @Override
    public void destroy() {
        // Unregister internal events here
    }

    @Override
    public boolean applyDetermination(ScriptContainer container, String determination) {
        // Apply an input determination here.
        // Return true if sucessful, false if there was an error.
        String lower = CoreUtilities.toLowerCase(determination);
        if (lower.startsWith("example:")) {
            objectOne = new Element(determination.substring(2));
        }
        else {
            return super.applyDetermination(container, determination);
        }
        return true;
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        //return new ExampleImplementationScriptEntryData(ObjectOne, ObjectTwo);
        return null;
    }

    @Override
    public HashMap<String, dObject> getContext() {
        HashMap<String, dObject> context = super.getContext();
        context.put("object_one", objectOne);
        context.put("object_twO", objectTwo);
        return context;
    }

    // Handle internal events here
    // public void onEvent(Event event) { ... }
}
