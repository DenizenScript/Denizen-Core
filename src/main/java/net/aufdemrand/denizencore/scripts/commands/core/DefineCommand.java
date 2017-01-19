package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

/**
 * Creates a queue/script-level variable.
 */
public class DefineCommand extends AbstractCommand {

    @Override
    public void onEnable() {
        setParseArgs(false);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        TagContext context = DenizenCore.getImplementation().getTagContext(scriptEntry);

        for (aH.Argument arg : aH.interpret(scriptEntry.getOriginalArguments())) {

            if (!scriptEntry.hasObject("definition")) {
                if (arg.getValue().equals("!") && arg.hasPrefix()) {
                    scriptEntry.addObject("remove", new Element("true"));
                    scriptEntry.addObject("value", new Element("null"));
                    scriptEntry.addObject("definition", new Element(TagManager.tag(arg.getPrefix().getValue(), context)));
                }
                else {
                    scriptEntry.addObject("definition", new Element(CoreUtilities.toLowerCase(TagManager.tag(arg.getValue(), context))));
                }
            }

            else if (!scriptEntry.hasObject("value")) {
                // Use the raw_value as to not exclude values with :'s in them.
                scriptEntry.addObject("value", new Element(arg.raw_value));
            }

            else if (!scriptEntry.hasObject("no_parse")
                    && arg.matches("no_parse")) {
                scriptEntry.addObject("no_parse", new Element(true));
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
        Element noParse = scriptEntry.getElement("no_parse");

        String parsedValue = value.asString();

        if (noParse == null) {
            parsedValue = TagManager.tag(parsedValue, DenizenCore.getImplementation().getTagContext(scriptEntry));
        }

        dB.report(scriptEntry, getName(), aH.debugObj("queue", scriptEntry.getResidingQueue().id)
                + definition.debug()
                + aH.debugObj("value", parsedValue)
                + (remove != null ? remove.debug() : "")
                + (noParse != null ? noParse.debug() : ""));

        if (scriptEntry.hasObject("remove")) {
            scriptEntry.getResidingQueue().removeDefinition(definition.asString());
        }
        else {
            scriptEntry.getResidingQueue().addDefinition(definition.asString(), parsedValue);
        }
    }
}
