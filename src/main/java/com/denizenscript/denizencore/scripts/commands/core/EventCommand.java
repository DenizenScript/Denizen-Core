package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.events.OldEventManager;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventCommand extends AbstractCommand {

    public EventCommand() {
        setName("event");
        setSyntax("event [<event name>|...] (context:<name>|<object>|...)");
        setRequiredArguments(1, 2);
        isProcedural = false;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("context")
                    && arg.matchesPrefix("context", "c")) {
                scriptEntry.addObject("context", arg.asType(ListTag.class));
            }
            else if (!scriptEntry.hasObject("events")) {
                scriptEntry.addObject("events", arg.asType(ListTag.class));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("events")) {
            throw new InvalidArgumentsException("Must specify a list of event names!");
        }
        scriptEntry.defaultObject("context", new ListTag());
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        Deprecations.eventCommand.warn(scriptEntry);
        ListTag events = scriptEntry.getObjectTag("events");
        ListTag context = scriptEntry.getObjectTag("context");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), events, context);
        }
        if (context.size() % 2 == 1) { // Size is uneven!
            context.add("null");
        }
        // Change the context input to a list of objects
        Map<String, ObjectTag> context_map = new HashMap<>();
        for (int i = 0; i < context.size(); i += 2) {
            context_map.put(context.get(i), ObjectFetcher.pickObjectFor(context.get(i + 1), scriptEntry.getContext()));
        }
        List<String> Determination = OldEventManager.doEvents(events,
                scriptEntry.entryData, context_map, true);
        scriptEntry.addObject("determinations", new ListTag(Determination));
    }
}
