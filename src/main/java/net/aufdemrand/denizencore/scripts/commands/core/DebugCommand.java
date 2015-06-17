package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class DebugCommand extends AbstractCommand {

    public enum DebugType {
        DEBUG,
        HEADER,
        FOOTER,
        SPACER,
        LOG,
        APPROVAL,
        ERROR,
        REPORT
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("type")
                    && arg.matchesEnum(DebugType.values())) {
                scriptEntry.addObject("type", arg.asElement());
            }

            else if (!scriptEntry.hasObject("debug")) {
                scriptEntry.addObject("debug", new Element(arg.raw_value));
            }

            else if (!scriptEntry.hasObject("name")
                    && arg.matchesPrefix("name")) {
                scriptEntry.addObject("name", arg.asElement());
            }

            else arg.reportUnhandled();
        }

        if (!scriptEntry.hasObject("type") || !scriptEntry.hasObject("debug")) {
            throw new InvalidArgumentsException("Must specify a definition and value!");
        }
        scriptEntry.defaultObject("name", new Element("name"));

    }

    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        Element debug = scriptEntry.getElement("debug");
        Element type = scriptEntry.getElement("type");
        Element name = scriptEntry.getElement("name");

        // Intentionally do not DB REPORT - we're making our own debug output!

        switch (DebugType.valueOf(type.asString().toUpperCase())) {
            case DEBUG:
                dB.echoDebug(scriptEntry, debug.asString());
                break;
            case HEADER:
                dB.echoDebug(scriptEntry, dB.DebugElement.Header, debug.asString());
                break;
            case FOOTER:
                dB.echoDebug(scriptEntry, dB.DebugElement.Footer, debug.asString());
                break;
            case SPACER:
                dB.echoDebug(scriptEntry, dB.DebugElement.Spacer, debug.asString());
                break;
            case LOG:
                dB.log(debug.asString());
                break;
            case APPROVAL:
                dB.echoApproval(debug.asString());
                break;
            case ERROR:
                dB.echoError(scriptEntry.getResidingQueue(), debug.asString());
                break;
            case REPORT:
                dB.report(scriptEntry, name.asString(), debug.asString());
                break;
        }
    }
}
