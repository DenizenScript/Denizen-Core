package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class MarkCommand extends AbstractCommand {

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
    public void execute(ScriptEntry scriptEntry) {

        // Fetch required objects
        Element mName = scriptEntry.getElement("m_name");

        // Debug the execution
        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), mName.debug());
        }
    }
}
