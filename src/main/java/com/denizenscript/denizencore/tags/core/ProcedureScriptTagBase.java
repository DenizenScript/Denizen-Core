package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.containers.core.ProcedureScriptContainer;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.Map;

public class ProcedureScriptTagBase {

    public ProcedureScriptTagBase() {

        // <--[tag]
        // @attribute <proc[<procedure_script_name>]>
        // @returns ObjectTag
        // @description
        // Returns the 'determine' result of a procedure script.
        // -->
        TagManager.registerTagHandler(ObjectTag.class, ElementTag.class, "proc", (attribute, param) -> {
            ScriptTag script;
            String path = null;
            if (param.asString().indexOf('.') > 0) {
                String[] split = param.asString().split("\\.", 2);
                path = split[1];
                script = ScriptTag.valueOf(split[0], attribute.context);
            }
            else {
                script = param.asType(ScriptTag.class, attribute.context);
            }
            if (script == null) {
                attribute.echoError("Missing script for procedure script tag '" + param.asString() + "'!");
                return null;
            }
            if (!(script.getContainer() instanceof ProcedureScriptContainer)) {
                attribute.echoError("Chosen script is not a procedure script!");
                return null;
            }
            ListTag definitions = null;
            MapTag mappedDefinitions;

            // <--[tag]
            // @attribute <proc[<procedure_script_name>].context[<object>|...]>
            // @returns ObjectTag
            // @description
            // Returns the 'determine' result of a procedure script with the given context.
            // -->
            if (attribute.startsWith("context", 2)) {
                mappedDefinitions = null;
                definitions = attribute.contextAsType(2, ListTag.class);
                attribute.fulfill(1);
            }

            // <--[tag]
            // @attribute <proc[<procedure_script_name>].context_map[<map>]>
            // @returns ObjectTag
            // @description
            // Returns the 'determine' result of a procedure script with the given context.
            // -->
            else if (attribute.startsWith("context_map", 2)) {
                mappedDefinitions = attribute.contextAsType(2, MapTag.class);
                attribute.fulfill(1);
            }
            else {
                mappedDefinitions = null;
            }
            ScriptQueue queue = ScriptUtilities.createAndStartQueue(script.getContainer(), path, attribute.context.getScriptEntryData(), null, (q) -> {
                if (mappedDefinitions != null) {
                    for (Map.Entry<StringHolder, ObjectTag> val : mappedDefinitions.entrySet()) {
                        q.addDefinition(val.getKey().str, val.getValue());
                    }
                }
                q.procedural = true;
            }, new DurationTag(0), null, definitions, script.getContainer());
            if (queue == null) {
                attribute.echoError("Procedure queue start failed.");
                return null;
            }
            if (queue.determinations == null || queue.determinations.isEmpty()) {
                attribute.echoError("Procedure call did not determine any value.");
                return null;
            }
            return queue.determinations.getObject(0);
        });

    }
}
