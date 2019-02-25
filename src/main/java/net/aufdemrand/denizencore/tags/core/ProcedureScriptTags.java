package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.ObjectFetcher;
import net.aufdemrand.denizencore.objects.TagRunnable;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.objects.dScript;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.containers.core.ProcedureScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.SlowWarning;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.List;

public class ProcedureScriptTags {

    public ProcedureScriptTags() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                procedureTag(event);
            }
        }, "proc", "pr");
    }

    public SlowWarning procShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'proc' instead of 'pr' as a root tag.");

    public void procedureTag(ReplaceableTagEvent event) {

        // <--[tag]
        // @attribute <proc[ProcedureScript].context[<element>|...]>
        // @returns dObject
        // @description
        // Returns the 'determine' result of a procedure script with the given context.
        // See <@link example Using Procedure Scripts>.
        // -->

        // <--[tag]
        // @attribute <proc[ProcedureScript]>
        // @returns dObject
        // @description
        // Returns the 'determine' result of a procedure script.
        // See <@link example Using Procedure Scripts>.
        // -->
        if (!event.matches("proc", "pr")) {
            return;
        }

        if (event.matches("pr")) {
            procShorthand.warn(event.getScriptEntry());
        }

        Attribute attr = event.getAttributes();
        int attribs = 1;

        dScript script = null;
        String path = null;

        if (event.hasNameContext()) {
            if (event.getNameContext().indexOf('.') > 0) {
                String[] split = event.getNameContext().split("\\.", 2);
                path = split[1];
                script = dScript.valueOf(split[0]);

            }
            else {
                script = dScript.valueOf(event.getNameContext());
            }

        }
        else if (event.getValue() != null) {
            script = dScript.valueOf(event.getValue());

        }
        else {
            dB.echoError("Invalid procedure script tag '" + event.getValue() + "'!");
            return;
        }

        if (script == null) {
            dB.echoError("Missing script for procedure script tag '" + event.getValue() + "'!");
            return;
        }

        if (!(script.getContainer() instanceof ProcedureScriptContainer)) {
            dB.echoError("Chosen script is not a procedure script!");
            return;
        }

        // Build script entries
        List<ScriptEntry> entries;
        if (path != null) {
            entries = script.getContainer().getEntries(event.getContext().getScriptEntryData(), path);
        }
        else {
            entries = script.getContainer().getBaseEntries(event.getContext().getScriptEntryData());
        }

        // Return if no entries built
        if (entries.isEmpty()) {
            return;
        }

        // Create new ID -- this is what we will look for when determining an outcome
        long id = DetermineCommand.getNewId();

        // Add the reqId to each of the entries for referencing
        ScriptBuilder.addObjectToEntries(entries, "reqid", id);

        InstantQueue queue = InstantQueue.getQueue(ScriptQueue.getNextId(script.getContainer().getName()));
        queue.addEntries(entries);
        queue.setReqId(id);
        if (event.hasType() &&
                event.getType().equalsIgnoreCase("context") &&
                event.hasTypeContext()) {
            attribs = 2;
            int x = 1;
            dList definitions = new dList(event.getTypeContext());
            List<String> definition_names = null;
            if (script.getContainer().getContents().contains("definitions")) {
                definition_names = CoreUtilities.split(script.getContainer().getString("definitions"), '|');
            }
            for (String definition : definitions) {
                String name = definition_names != null && definition_names.size() >= x ?
                        definition_names.get(x - 1).trim() : String.valueOf(x);
                queue.addDefinition(name, definition);
                dB.echoDebug(event.getScriptEntry() == null ? (event.getScript() == null ? null :
                                event.getScript().getContainer()) : event.getScriptEntry(),
                        "Adding definition '" + name + "' as " + definition);
                x++;
            }

            queue.addDefinition("raw_context", event.getTypeContext());
        }

        queue.start();

        if (DetermineCommand.hasOutcome(id)) {
            event.setReplacedObject(CoreUtilities.autoAttrib(ObjectFetcher.pickObjectFor(DetermineCommand.getOutcome(id).get(0))
                    , attr.fulfill(attribs)));
        }
    }
}
