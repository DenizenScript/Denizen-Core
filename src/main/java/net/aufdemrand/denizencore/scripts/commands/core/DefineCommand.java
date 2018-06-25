package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

/**
 * Creates a queue/script-level variable.
 */
public class DefineCommand extends AbstractCommand {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.args)) {

            if (!scriptEntry.hasObject("definition")) {
                if (arg.getValue().equals("!") && arg.hasPrefix()) {
                    scriptEntry.addObject("remove", new Element("true"));
                    scriptEntry.addObject("value", new Element("null"));
                    scriptEntry.addObject("definition", arg.getPrefix().asElement());
                }
                else {
                    scriptEntry.addObject("definition", new Element(CoreUtilities.toLowerCase(arg.getValue())));
                }
            }

            else if (!scriptEntry.hasObject("value")) {
                scriptEntry.addObject("value", arg.object instanceof Element ? new Element(arg.raw_value) : arg.object);
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
        dObject value = scriptEntry.getdObject("value");
        Element remove = scriptEntry.getElement("remove");

        if (DenizenCore.getImplementation().shouldDebug(scriptEntry)) {
            dB.report(scriptEntry, getName(), aH.debugObj("queue", scriptEntry.getResidingQueue().id)
                    + definition.debug()
                    + value.debug()
                    + (remove != null ? remove.debug() : ""));
        }

        if (scriptEntry.hasObject("remove")) {
            scriptEntry.getResidingQueue().removeDefinition(definition.asString());
        }
        else {
            scriptEntry.getResidingQueue().addDefinition(definition.asString(), value);
        }
    }
}
