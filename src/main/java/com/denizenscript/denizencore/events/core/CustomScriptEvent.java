package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class CustomScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // custom event
    //
    // @Switch id:<id> to only run the event if the given ID is used. This should almost always be specified.
    //
    // @Regex ^on custom event$
    //
    // @Group Core
    //
    // @Triggers when called by a script using <@link command customevent>.
    //
    // @Context
    // <context.id> returns the ID that was used.
    // <context.data> returns the MapTag of input data (if any! some events don't have context data).
    //
    // @Determine
    // "OUTPUT:" + Anything, to add that value to the output list (note this is an ADD, not a SET).
    //
    // -->

    public static CustomScriptEvent instance;

    public ScriptEntryData entryData = null;
    public String id;
    public MapTag contextData;

    public boolean anyMatched = false;

    public ListTag determinations;

    public CustomScriptEvent() {
        instance = this;
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        return path.eventLower.startsWith("custom event");
    }

    @Override
    public boolean matches(ScriptPath path) {
        if (!runGenericSwitchCheck(path, "id", id)) {
            return false;
        }
        anyMatched = true;
        return true;
    }

    @Override
    public ScriptEntryData getScriptEntryData() {
        return entryData;
    }

    @Override
    public String getName() {
        return "CustomEvent";
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "id": return new ElementTag(id, true);
            case "data": return contextData;
        }
        return super.getContext(name);
    }

    @Override
    public boolean applyDetermination(ScriptPath path, ObjectTag determination) {
        if (determination instanceof ElementTag && CoreUtilities.toLowerCase(determination.toString()).startsWith("output:")) {
            determinations.add(determination.toString().substring("output:".length()));
            return true;
        }
        return super.applyDetermination(path, determination);
    }

    boolean enabled = false;

    @Override
    public void init() {
        enabled = true;
    }

    @Override
    public void destroy() {
        enabled = false;
    }

    public static CustomScriptEvent runCustomEvent(ScriptEntryData data, String id, MapTag contextData) {
        if (!instance.enabled) {
            return null;
        }
        instance.cancelled = false;
        instance.determinations = new ListTag();
        instance.entryData = data;
        instance.id = id;
        instance.contextData = contextData;
        return (CustomScriptEvent) instance.fire();
    }
}
