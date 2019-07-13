package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.utilities.debugging.dB;
import com.denizenscript.denizencore.objects.Element;
import com.denizenscript.denizencore.objects.aH;
import com.denizenscript.denizencore.objects.dList;
import com.denizenscript.denizencore.objects.dObject;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class DetermineCommand extends AbstractCommand {

    // <--[command]
    // @Name Determine
    // @Syntax determine (passively) [<value>]
    // @Required 1
    // @Short Sets the outcome of a world event.
    // @Group queue
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

        for (aH.Argument arg : aH.interpretArguments(scriptEntry.aHArgs)) {

            if (arg.matches("passive", "passively")) {
                scriptEntry.addObject("passively", new Element(true));
            }

            else if (!scriptEntry.hasObject("outcome")) {
                scriptEntry.addObject("outcome", arg.hasPrefix() ? new Element(arg.raw_value) : arg.object);
            }

            else {
                arg.reportUnhandled();
            }
        }

        //
        // Set defaults
        //

        scriptEntry.defaultObject("passively", new Element(false));
        scriptEntry.defaultObject("outcome", new Element(DETERMINE_NONE));
    }


    @Override
    public void execute(ScriptEntry scriptEntry) {

        dObject outcomeObj = scriptEntry.getdObject("outcome");
        Element passively = scriptEntry.getElement("passively");

        // Report!
        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), outcomeObj.debug() + passively.debug());
        }

        // Store the outcome in the cache
        dList strs = scriptEntry.getResidingQueue().determinations;
        if (strs == null) {
            strs = new dList();
            scriptEntry.getResidingQueue().determinations = strs;
        }
        strs.addObject(outcomeObj);

        if (!passively.asBoolean()) {
            // Stop the queue by clearing the remainder of it.
            scriptEntry.getResidingQueue().clear();
        }
    }
}
