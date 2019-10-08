package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class DetermineCommand extends AbstractCommand {

    // <--[command]
    // @Name Determine
    // @Syntax determine (passively) [<value>]
    // @Required 1
    // @Short Sets the outcome of a world event.
    // @Group queue
    // @Guide https://guide.denizenscript.com/guides/first-steps/world-script.html
    //
    // @Description
    // TODO: Document Command Details
    //
    // @Tags
    // TODO: Document Command Details
    //
    // @Usage
    // Use to modify the result of an event
    // - determine <context.message.substring[5]>
    //
    // @Usage
    // Use to cancel an event, but continue running script commands
    // - determine passively cancelled
    //
    // -->

    // Default 'DETERMINE_NONE' value.
    public static String DETERMINE_NONE = "none";


    //
    // Command Singleton
    //

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        //
        // Parse the arguments
        //

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (arg.matches("passive", "passively")) {
                scriptEntry.addObject("passively", new ElementTag(true));
            }

            else if (!scriptEntry.hasObject("outcome")) {
                scriptEntry.addObject("outcome", arg.hasPrefix() ? new ElementTag(arg.raw_value) : arg.object);
            }

            else {
                arg.reportUnhandled();
            }
        }

        //
        // Set defaults
        //

        scriptEntry.defaultObject("passively", new ElementTag(false));
        scriptEntry.defaultObject("outcome", new ElementTag(DETERMINE_NONE));
    }


    @Override
    public void execute(ScriptEntry scriptEntry) {

        ObjectTag outcomeObj = scriptEntry.getObjectTag("outcome");
        ElementTag passively = scriptEntry.getElement("passively");

        // Report!
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), outcomeObj.debug() + passively.debug() + new QueueTag(scriptEntry.getResidingQueue()).debug());
        }

        // Store the outcome in the cache
        ListTag determines = scriptEntry.getResidingQueue().determinations;
        if (determines == null) {
            determines = new ListTag();
            scriptEntry.getResidingQueue().determinations = determines;
        }
        determines.addObject(outcomeObj);

        if (!passively.asBoolean()) {
            // Stop the queue by clearing the remainder of it.
            scriptEntry.getResidingQueue().clear();
        }
    }
}
