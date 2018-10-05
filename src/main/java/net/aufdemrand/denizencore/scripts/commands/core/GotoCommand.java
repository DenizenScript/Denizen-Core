package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.List;

public class GotoCommand extends AbstractCommand {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        // Interpret arguments
        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

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
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        // Fetch required objects
        Element mName = scriptEntry.getElement("m_name");

        // Debug the execution
        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), mName.debug());
        }

        // Jump forth
        boolean hasmark = false;
        for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
            ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
            List<String> args = entry.getOriginalArguments();
            if (entry.getCommandName().equalsIgnoreCase("mark") && args.size() > 0 && args.get(0).equalsIgnoreCase(mName.asString())) {
                hasmark = true;
                break;
            }
        }
        if (hasmark) {
            while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equalsIgnoreCase("mark") && args.size() > 0 && args.get(0).equalsIgnoreCase(mName.asString())) {
                    break;
                }
                scriptEntry.getResidingQueue().removeEntry(0);
            }
        }
        else {
            dB.echoError(scriptEntry.getResidingQueue(), "Cannot go to that location - doesn't seem to exist!");
        }
    }
}
