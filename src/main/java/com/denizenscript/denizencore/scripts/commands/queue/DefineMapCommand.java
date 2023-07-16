package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.Map;

public class DefineMapCommand extends AbstractCommand {

    public DefineMapCommand() {
        setName("definemap");
        setSyntax("definemap [<name>] [<key>:<value> ...]");
        setRequiredArguments(1, -1);
        isProcedural = true;
        allowedDynamicPrefixes = true;
        anyPrefixSymbolAllowed = true;
    }

    // <--[command]
    // @Name DefineMap
    // @Syntax definemap [<name>] [<key>:<value> ...]
    // @Required 1
    // @Maximum -1
    // @Short Creates a MapTag definition with key/value pairs constructed from the input arguments.
    // @Group queue
    // @Guide https://guide.denizenscript.com/guides/basics/definitions.html
    //
    // @Description
    // Creates a MapTag definition with key/value pairs constructed from the input arguments.
    //
    // @Tags
    // <[<id>]> to get the value assigned to an ID
    //
    // @Usage
    // Use to make a MapTag definition with three inputs.
    // - definemap my_map count:5 type:Taco smell:Tasty
    //
    // @Usage
    // Use to make a MapTag definition with complex input.
    // - definemap my_map:
    //     count: 5
    //     some_list:
    //     - a
    //     - b
    //     some_submap:
    //         some_subkey: taco
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        MapTag value = new MapTag();
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("definition")
                    && !arg.hasPrefix()) {
                scriptEntry.addObject("definition", new ElementTag(CoreUtilities.toLowerCase(arg.getValue())));
            }
            else if (arg.hasPrefix()) {
                value.putObject(arg.getPrefix().getRawValue(), arg.object);
            }
            else if (!arg.hasPrefix() && arg.getRawValue().contains(":")) {
                int colon = arg.getRawValue().indexOf(':');
                value.putObject(arg.getRawValue().substring(0, colon), new ElementTag(arg.getRawValue().substring(colon + 1)));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (scriptEntry.internal.yamlSubcontent instanceof Map) {
            MapTag map = (MapTag) CoreUtilities.objectToTagForm(scriptEntry.internal.yamlSubcontent, scriptEntry.getContext(), true, true);
            value.putAll(map);
        }
        scriptEntry.addObject("map", value);
        if (!scriptEntry.hasObject("definition") || !scriptEntry.hasObject("map")) {
            throw new InvalidArgumentsException("Must specify a definition and value!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag definition = scriptEntry.getElement("definition");
        MapTag value = scriptEntry.getObjectTag("map");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), new QueueTag(scriptEntry.getResidingQueue()), definition, value);
        }
        scriptEntry.getResidingQueue().addDefinition(definition.asString(), value.duplicate());
    }
}
