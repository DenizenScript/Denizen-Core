package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

/**
 * Your command!
 * This class is a template for a Command in Denizen.
 * <p/>
 * If loading externally, implement dExternal and its load() method.
 *
 * @author Jeremy Schroeder, mcmonkey
 */
public class _templateCommand extends AbstractCommand {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        // Interpret arguments
        for (aH.Argument arg : aH.interpretArguments(scriptEntry.aHArgs)) {

            if (!scriptEntry.hasObject("required_integer")
                    && arg.matchesPrimitive(aH.PrimitiveType.Integer)) {
                scriptEntry.addObject("required_integer", arg.asElement());
            }

            // else if (...)

            else {
                arg.reportUnhandled();
            }
        }

        // Check for required information
        if (!scriptEntry.hasObject("required_object")) {
            throw new InvalidArgumentsException("Must have required object!");
        }

    }


    @Override
    public void execute(ScriptEntry scriptEntry) {

        // Fetch required objects
        Element required_integer = scriptEntry.getElement("required_integer");
        // dObject object = scriptEntry.getdObject("required_object");

        // Debug the execution
        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), required_integer.debug());
        }

        // Do the execution

        // INSERT
        // YOUR
        // CODE
        // HERE :)
    }
}
