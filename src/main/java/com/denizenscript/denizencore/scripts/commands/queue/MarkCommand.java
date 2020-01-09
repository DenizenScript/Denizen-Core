package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class MarkCommand extends AbstractCommand {

    // <--[command]
    // @Name Mark
    // @Syntax mark [<name>]
    // @Required 1
    // @Short Marks a location for <@link command goto>.
    // @Group queue
    //
    // @Description
    // Marks a location for the goto command. See <@link command goto> for details.
    //
    // @Tags
    //
    // None
    //
    // @Usage
    // Use to mark a location.
    // - mark potato
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        // Interpret arguments
        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (!scriptEntry.hasObject("m_name")) {
                scriptEntry.addObject("m_name", arg.asElement());
            }

            else {
                arg.reportUnhandled();
            }
        }

        // Check for required information
        if (!scriptEntry.hasObject("m_name")) {
            throw new InvalidArgumentsException("Must have a mark name!");
        }

    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        // Fetch required objects
        ElementTag mName = scriptEntry.getElement("m_name");

        // Debug the execution
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), mName.debug());
        }
    }
}
