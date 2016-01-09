package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

/**
 * Creates a queue/script-level variable.
 */
public class DefineCommand extends AbstractCommand {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("definition")) {
                if (arg.getValue().equals("!") && arg.hasPrefix()) {
                    scriptEntry.addObject("remove", new Element("true"));
                    scriptEntry.addObject("value", new Element("null"));
                    scriptEntry.addObject("definition", arg.getPrefix().asElement());
                }
                else {
                    scriptEntry.addObject("definition", new Element(arg.getValue().toLowerCase()));
                }
            }

            else if (!scriptEntry.hasObject("value"))
            // Use the raw_value as to not exclude values with :'s in them.
            {
                scriptEntry.addObject("value", new Element(arg.raw_value));
            }

            else {
                arg.reportUnhandled();
            }
        }

        if (!scriptEntry.hasObject("definition") || !scriptEntry.hasObject("value")) {
            throw new InvalidArgumentsException("Must specify a definition and value!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        Element definition = scriptEntry.getElement("definition");
        Element value = scriptEntry.getElement("value");
        Element remove = scriptEntry.getElement("remove");

        dB.report(scriptEntry, getName(), aH.debugObj("queue", scriptEntry.getResidingQueue().id)
                + definition.debug()
                + value.debug()
                + (remove != null ? remove.debug() : ""));

        if (scriptEntry.hasObject("remove")) {
            scriptEntry.getResidingQueue().removeDefinition(definition.asString());
        }
        else {
            scriptEntry.getResidingQueue().addDefinition(definition.asString(), value.asString());
        }
    }
}
