package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.BracedCommand;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

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
                scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
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

        if (!scriptEntry.hasObject("braces")) {
            throw new InvalidArgumentsException("Must have sub-commands!");
        }
    }


    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        Element choice = scriptEntry.getElement("choice");

        List<BracedData> bdlist = (List<BracedData>) scriptEntry.getObject("braces");

        if (bdlist == null || bdlist.isEmpty()) {
            dB.echoError(scriptEntry.getResidingQueue(), "Empty sub-commands (internal)!");
            return;
        }

        List<ScriptEntry> bracedCommandsList = bdlist.get(0).value;

        dB.report(scriptEntry, getName(), choice.debug());

        String choice_low = CoreUtilities.toLowerCase(choice.asString());

        ScriptEntry result = null;

        for (ScriptEntry se : bracedCommandsList) {
            String cmdName = CoreUtilities.toLowerCase(se.getCommandName());
            if (cmdName.equals("default")) {
                result = se;
                break;
            }
            else if (cmdName.equals("case")) {
                if (se.getArguments().size() == 1) {
                    String arg = TagManager.tag(se.getArguments().get(0), DenizenCore.getImplementation().getTagContextFor(scriptEntry, false));
                    if (CoreUtilities.toLowerCase(arg).equals(choice_low)) {
                        result = se;
                        break;
                    }
                }
                else {
                    dB.echoError("Unknown choice sub-command '" + se.toString() + "'!");
                }
            }
            else {
                dB.echoError("Unknown choice sub-command '" + cmdName + "'!");
            }
        }

        if (result == null) {
            dB.echoDebug(scriptEntry, "No result!");
            return;
        }

        List<BracedData> new_commands = getBracedCommands(result);

        if (new_commands == null || new_commands.isEmpty()) {
            dB.echoError(scriptEntry.getResidingQueue(), "Empty case sub-commands (internal)!");
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
