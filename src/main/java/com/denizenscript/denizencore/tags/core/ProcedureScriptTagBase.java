package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.core.ProcedureScriptContainer;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.TagManager;

import java.util.List;

public class ProcedureScriptTagBase {

    public ProcedureScriptTagBase() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                procedureTag(event);
            }
        }, "proc", "pr");
    }

    public void procedureTag(ReplaceableTagEvent event) {

        // <--[tag]
        // @attribute <proc[ProcedureScript].context[<element>|...]>
        // @returns ObjectTag
        // @description
        // Returns the 'determine' result of a procedure script with the given context.
        // -->

        // <--[tag]
        // @attribute <proc[ProcedureScript]>
        // @returns ObjectTag
        // @description
        // Returns the 'determine' result of a procedure script.
        // -->
        if (!event.matches("proc", "pr")) {
            return;
        }

        if (event.matches("pr")) {
            Deprecations.procShorthand.warn(event.getScriptEntry());
        }

        Attribute attr = event.getAttributes();
        int attribs = 1;

        ScriptTag script = null;
        String path = null;

        if (event.hasNameContext()) {
            if (event.getNameContext().indexOf('.') > 0) {
                String[] split = event.getNameContext().split("\\.", 2);
                path = split[1];
                script = ScriptTag.valueOf(split[0]);

            }
            else {
                script = ScriptTag.valueOf(event.getNameContext());
            }

        }
        else {
            Debug.echoError("Invalid procedure script tag!");
            return;
        }

        if (script == null) {
            Debug.echoError("Missing script for procedure script tag '" + event.getNameContext() + "'!");
            return;
        }

        if (!(script.getContainer() instanceof ProcedureScriptContainer)) {
            Debug.echoError("Chosen script is not a procedure script!");
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

        InstantQueue queue = new InstantQueue(script.getContainer().getName());
        queue.addEntries(entries);
        if (event.hasType() &&
                event.getType().equalsIgnoreCase("context") &&
                event.hasTypeContext()) {
            attribs = 2;
            int x = 1;
            ListTag definitions = new ListTag(event.getTypeContext());
            List<String> definition_names = null;
            if (script.getContainer().getContents().contains("definitions")) {
                definition_names = CoreUtilities.split(script.getContainer().getString("definitions"), '|');
            }
            for (String definition : definitions) {
                String name = definition_names != null && definition_names.size() >= x ?
                        definition_names.get(x - 1).trim() : String.valueOf(x);
                queue.addDefinition(name, definition);
                Debug.echoDebug(event.getScriptEntry() == null ? (event.getScript() == null ? script.getContainer() :
                                event.getScript().getContainer()) : event.getScriptEntry(),
                        "Adding definition '" + name + "' as " + definition);
                x++;
            }

            queue.addDefinition("raw_context", event.getTypeContext());
        }

        queue.start();

        if (queue.determinations != null && queue.determinations.size() > 0) {
            event.setReplacedObject(CoreUtilities.autoAttribTyped(queue.determinations.getObject(0)
                    , attr.fulfill(attribs)));
        }
    }
}
