package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.List;

public class GotoCommand extends AbstractCommand {

    public GotoCommand() {
        setName("goto");
        setSyntax("goto [<name>]");
        setRequiredArguments(1, 1);
        isProcedural = true;
    }

    // <--[command]
    // @Name Goto
    // @Syntax goto [<name>]
    // @Required 1
    // @Maximum 1
    // @Short Jump forward to a location marked by <@link command mark>.
    // @Group queue
    //
    // @Description
    // Jumps forward to a marked location in the script.
    // For example:
    // <code>
    // - goto potato
    // - narrate "This will never show"
    // - mark potato
    // </code>
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to jump forward to a location.
    // - goto potato
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (!scriptEntry.hasObject("m_name")) {
                scriptEntry.addObject("m_name", arg.asElement());
            }

            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("m_name")) {
            throw new InvalidArgumentsException("Must have a mark name!");
        }

    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        ElementTag mName = scriptEntry.getElement("m_name");

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), mName.debug());
        }

        String markName = mName.asString();

        // Jump forth
        boolean hasmark = false;
        for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
            ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
            List<String> args = entry.getOriginalArguments();
            if (CoreUtilities.equalsIgnoreCase(entry.getCommandName(), "mark") && args.size() > 0 && CoreUtilities.equalsIgnoreCase(args.get(0), markName)) {
                hasmark = true;
                break;
            }
        }
        if (hasmark) {
            while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                List<String> args = entry.getOriginalArguments();
                if (CoreUtilities.equalsIgnoreCase(entry.getCommandName(), "mark") && args.size() > 0 && CoreUtilities.equalsIgnoreCase(args.get(0), markName)) {
                    break;
                }
                scriptEntry.getResidingQueue().removeEntry(0);
            }
        }
        else {
            Debug.echoError(scriptEntry.getResidingQueue(), "Cannot go to that location - doesn't seem to exist!");
        }
    }
}
