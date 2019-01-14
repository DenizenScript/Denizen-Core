package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.BracedCommand;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.HashMap;
import java.util.List;

public class ChooseCommand extends BracedCommand {

    @Override
    public void onEnable() {
        setBraced();
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (!scriptEntry.hasObject("choice")) {
                scriptEntry.addObject("choice", arg.asElement());
                scriptEntry.addObject("braces", getBracedCommands(scriptEntry, false));
                break;
            }

            else {
                arg.reportUnhandled();
                break;
            }
        }

        if (!scriptEntry.hasObject("choice")) {
            throw new InvalidArgumentsException("Must have a choice!"); // Should never happen
        }
    }


    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        List<BracedData> bdlist = (List<BracedData>) scriptEntry.getObject("braces");
        if (bdlist == null || bdlist.isEmpty()) {
            dB.echoError(scriptEntry.getResidingQueue(), "Empty sub-commands (internal)!");
            return;
        }
        List<ScriptEntry> bracedCommandsList = bdlist.get(0).value;

        HashMap<String, Integer> lookupTable = null;
        if (scriptEntry.internal.specialProcessedData instanceof HashMap) {
            lookupTable = (HashMap<String, Integer>) scriptEntry.internal.specialProcessedData;
        }
        else {
            lookupTable = new HashMap<>();
            for (int i = 0; i < bracedCommandsList.size(); i++) {
                ScriptEntry se = bracedCommandsList.get(i);
                String cmdName = CoreUtilities.toLowerCase(se.getCommandName());
                if (cmdName.equals("default")) {
                    lookupTable.put("\0DEFAULT", i);
                    break;
                }
                else if (cmdName.equals("case")) {
                    if (se.getArguments().size() > 0) {
                        for (String arg : se.getArguments()) {
                            lookupTable.put(CoreUtilities.toLowerCase(arg), i);
                        }
                    }
                    else {
                        dB.echoError("Unknown choose sub-command (missing arguments) '" + se.toString() + "'!");
                    }
                }
                else {
                    dB.echoError("Unknown choose sub-command '" + cmdName + "'!");
                }
            }
            scriptEntry.internal.specialProcessedData = lookupTable;
        }

        Element choice = scriptEntry.getElement("choice");

        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), choice.debug());
        }

        String choice_low = CoreUtilities.toLowerCase(choice.asString());

        Integer resultIndex = lookupTable.get(choice_low);

        if (resultIndex == null) {
            resultIndex = lookupTable.get("\0DEFAULT");
            if (resultIndex == null) {
                dB.echoDebug(scriptEntry, "No result!");
                return;
            }
        }

        ScriptEntry result = bracedCommandsList.get(resultIndex);

        List<BracedData> new_commands = getBracedCommands(result);

        if (new_commands == null || new_commands.isEmpty()) {
            dB.echoError(scriptEntry.getResidingQueue(), "Empty choose command case sub-commands (internal) for case '" + result.toString() + "'");
            return;
        }

        List<ScriptEntry> new_command_list = new_commands.get(0).value;

        for (int i = 0; i < new_command_list.size(); i++) {
            new_command_list.get(i).setInstant(true);
            new_command_list.get(i).addObject("reqid", scriptEntry.getObject("reqid"));
        }

        scriptEntry.setInstant(true);

        scriptEntry.getResidingQueue().injectEntries(new_command_list, 0);
    }
}
