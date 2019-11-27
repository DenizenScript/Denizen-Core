package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;

import java.util.HashMap;
import java.util.List;

public class ChooseCommand extends BracedCommand {

    // <--[command]
    // @Name Choose
    // @Syntax choose [<option>] [<cases>]
    // @Required 1
    // @Short Chooses an option from the list of cases.
    // @Group queue

    // @Description
    // Chooses an option from the list of cases.
    // Intended to replace a long chain of simplistic if/else if or complicated script path selection systems.
    // Simply input the selected option, and the system will automatically jump to the most relevant case input.
    // Cases are given as a sub-set of commands inside the current command (see Usage for samples).
    //
    // Optionally, specify "default" in place of a case to give a result when all other cases fail to match.
    //
    // Cases must be static text. They may not contain tags. For multi-tag comparison, consider the IF command.
    // Any one case line can have multiple values in it - each possible value should be its own argument (separated by spaces).
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to choose the only case.
    // - choose 1:
    //   - case 1:
    //     - debug LOG "Success!"
    //
    // @Usage
    // Use to choose the default case.
    // - choose 2:
    //   - case 1:
    //     - debug log "Failure!"
    //   - default:
    //     - debug log "Success!"
    //
    // @Usage
    // Use for dynamically choosing a case.
    // - choose <[entity_type]>:
    //   - case zombie:
    //     - narrate "You slayed an undead zombie!"
    //   - case skeleton:
    //     - narrate "You knocked the bones out of a skeleton!"
    //   - case creeper:
    //     - narrate "You didn't give that creeper a chance to explode!"
    //   - case pig cow chicken:
    //     - narrate "You killed an innocent farm animal!"
    //   - default:
    //     - narrate "You killed a <[entity_type].to_titlecase>!"
    //
    // -->

    @Override
    public void onEnable() {
        setBraced();
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("choice")) {
                scriptEntry.addObject("choice", arg.asElement());
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
        List<BracedData> bdlist = getBracedCommands(scriptEntry, false);
        if (bdlist == null || bdlist.isEmpty()) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Empty sub-commands (internal)!");
            return;
        }
        List<ScriptEntry> bracedCommandsList = bdlist.get(0).value;
        HashMap<String, Integer> lookupTable;
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
                        Debug.echoError("Unknown choose sub-command (missing arguments) '" + se.toString() + "'!");
                    }
                }
                else {
                    Debug.echoError("Unknown choose sub-command '" + cmdName + "'!");
                }
            }
            scriptEntry.internal.specialProcessedData = lookupTable;
        }
        ElementTag choice = scriptEntry.getElement("choice");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), choice.debug());
        }
        String choice_low = CoreUtilities.toLowerCase(choice.asString());
        Integer resultIndex = lookupTable.get(choice_low);
        if (resultIndex == null) {
            resultIndex = lookupTable.get("\0DEFAULT");
            if (resultIndex == null) {
                Debug.echoDebug(scriptEntry, "No result!");
                return;
            }
        }
        ScriptEntry result = bracedCommandsList.get(resultIndex);
        List<BracedData> new_commands = getBracedCommands(result);
        if (new_commands == null || new_commands.isEmpty()) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Empty choose command case sub-commands (internal) for case '" + result.toString() + "'");
            return;
        }
        List<ScriptEntry> new_command_list = new_commands.get(0).value;
        for (ScriptEntry newEntry : new_command_list) {
            newEntry.setInstant(true);
            newEntry.entryData.transferDataFrom(scriptEntry.entryData);
            newEntry.entryData.scriptEntry = newEntry;
        }
        scriptEntry.setInstant(true);
        scriptEntry.getResidingQueue().injectEntries(new_command_list, 0);
    }
}
