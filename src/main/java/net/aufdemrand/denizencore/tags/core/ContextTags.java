package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptRegistry;
import net.aufdemrand.denizencore.scripts.containers.core.TaskScriptContainer;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.HashMap;
import java.util.Map;

public class ContextTags {

    public ContextTags() {
        TagManager.registerTagEvents(this);
    }


    // Get scriptqueue context!
    @TagManager.TagEvents
    public void contextTags(ReplaceableTagEvent event) {
        if (!event.matches("context", "c") || event.getScriptEntry() == null) {
            return;
        }

        String object = event.getType();

        // First, check queue object context.
        dObject obj = event.getScriptEntry().getResidingQueue().getContext(object);
        if (obj != null) {
            Attribute attribute = event.getAttributes();
            event.setReplaced(obj.getAttribute(attribute.fulfill(2)));
            return;
        }

        if (!event.hasAlternative()) {
            dB.echoError(event.getScriptEntry() != null ? event.getScriptEntry().getResidingQueue() : null, "Invalid context ID '" + object + "'!");
        }
    }


    // Get a saved script entry!
    @TagManager.TagEvents
    public void savedEntryTags(ReplaceableTagEvent event) {
        if (!event.matches("entry", "e")
                || event.getScriptEntry() == null
                || !event.hasNameContext()) {
            return;
        }

        // <e[entry_id].entity.blah.blah>
        if (event.getScriptEntry().getResidingQueue() != null) {

            // Get the entry_id from name context
            String id = event.getNameContext();

            Attribute attribute = event.getAttributes();
            ScriptEntry held = event.getScriptEntry().getResidingQueue().getHeldScriptEntry(id);
            if (held == null) { // Check if the ID is bad
                if (!event.hasAlternative()) {
                    dB.echoDebug(event.getScriptEntry(), "Bad saved entry ID '" + id + "'");
                }

            }
            else {
                if (!held.hasObject(attribute.getAttribute(2))
                        || held.getdObject(attribute.getAttribute(2)) == null) {
                    if (!event.hasAlternative()) {
                        dB.echoDebug(event.getScriptEntry(), "Missing saved entry object '" + attribute.getAttribute(2) + "'");
                    }

                }
                else { // Okay, now it's safe!
                    event.setReplaced(held.getdObject(attribute.getAttribute(2)).getAttribute(attribute.fulfill(2)));
                }
            }
        }

        //else event.setReplaced("null");
    }
}
