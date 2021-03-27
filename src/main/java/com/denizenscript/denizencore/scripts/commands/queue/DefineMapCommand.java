package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public class DefineMapCommand extends AbstractCommand {

    public DefineMapCommand() {
        setName("definemap");
        setSyntax("definemap [<name>] [<key>:<value> ...]");
        setRequiredArguments(2, -1);
        isProcedural = true;
    }

    // <--[command]
    // @Name DefineMap
    // @Syntax definemap [<name>] [<key>:<value> ...]
    // @Required 2
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
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        MapTag value = new MapTag();
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("definition")
                    && !arg.hasPrefix()) {
                scriptEntry.addObject("definition", new ElementTag(CoreUtilities.toLowerCase(arg.getValue())));
            }
            else if (arg.hasPrefix()) {
                value.putObject(arg.getPrefix().getRawValue(), arg.object);
            }
            else {
                arg.reportUnhandled();
            }
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
            Debug.report(scriptEntry, getName(), ArgumentHelper.debugObj("queue", scriptEntry.getResidingQueue().id)
                    + definition.debug()
                    + value.debug());
        }
        scriptEntry.getResidingQueue().addDefinition(definition.asString(), value.duplicate());
    }
}
