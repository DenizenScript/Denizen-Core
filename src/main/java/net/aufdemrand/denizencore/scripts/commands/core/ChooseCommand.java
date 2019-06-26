package net.aufdemrand.denizencore.scripts.commands.core;

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

    // <--[command]
    // @Name Choose
    // @Syntax choose [<option>] [<cases>]
    // @Required 1
    // @Short Chooses an option from the list of cases.
    // @Group core

    // @Description
    // Chooses an option from the list of cases.
    // Intended to replace a long chain of simplistic if/else if or complicated script path selection systems.
    // Simply input the selected option, and the system will automatically jump to the most relevant case input.
    // Cases are given as a sub-set of commands inside the current command (see Usage for samples).
    //
    // Optionally, specify "default" in place of a case to give a result when all other cases fail to match.
    //
    // Cases must be static text. They may not contain tags. For multi-tag comparison, consider the IF command.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to choose the only case.
    // - choose "1":
    //   - case "1":
    //     - debug LOG "Success!"
    //
    // @Usage
    // Use to choose the default case.
    // - choose "2":
    //   - case "1":
    //     - debug log "Failure!"
    //   - default:
    //     - debug log "Success!"
    //
    // @Usage
    // Use for dynamically choosing a case.
    // - choose "<def[entity_type]>":
    //   - case "zombie":
    //     - narrate "You slayed an undead zombie!"
    //   - case "skeleton":
    //     - narrate "You knocked the bones out of a skeleton!"
    //   - case "creeper":
    //     - narrate "You didn't give that creeper a chance to explode!"
    //   - case "pig" "cow" "chicken":
    //     - narrate "You killed an innocent farm animal!"
    //   - default:
    //     - narrate "You killed a <def[entity_type].to_titlecase>!"
    //
    // -->

    @Override
    public void onEnable() {
        setBraced();
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpretArguments(scriptEntry.aHArgs)) {

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
    public void execute(ScriptEntry scriptEntry) {

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

        for (ScriptEntry newEntry : new_command_list) {
            newEntry.setInstant(true);
            newEntry.addObject("reqid", scriptEntry.getObject("reqid"));
            newEntry.entryData.transferDataFrom(scriptEntry.entryData);
            newEntry.entryData.scriptEntry = newEntry;
        }

        scriptEntry.setInstant(true);

        scriptEntry.getResidingQueue().injectEntries(new_command_list, 0);
    }
}
