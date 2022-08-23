package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgRaw;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

import java.util.List;

public class GotoCommand extends AbstractCommand {

    public GotoCommand() {
        setName("goto");
        setSyntax("goto [<name>]");
        setRequiredArguments(1, 1);
        isProcedural = true;
        autoCompile();
    }

    // <--[command]
    // @Name Goto
    // @Syntax goto [<name>]
    // @Required 1
    // @Maximum 1
    // @Short Jump forward to a location marked by <@link command mark>.
    // @Group queue
    //
    // @Description
    // Jumps forward to a marked location in the script.
    // For example:
    // <code>
    // - goto potato
    // - narrate "This will never show"
    // - mark potato
    // </code>
    //
    // Most scripters should never use this. This is only for certain special cases.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to jump forward to a location.
    // - goto potato
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgRaw @ArgLinear @ArgName("mark_name") String markName) {
        boolean hasmark = false;
        for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
            ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
            List<String> args = entry.getOriginalArguments();
            if (CoreUtilities.equalsIgnoreCase(entry.getCommandName(), "mark") && args.size() > 0 && CoreUtilities.equalsIgnoreCase(args.get(0), markName)) {
                hasmark = true;
                break;
            }
        }
        if (hasmark) {
            while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                List<String> args = entry.getOriginalArguments();
                if (CoreUtilities.equalsIgnoreCase(entry.getCommandName(), "mark") && args.size() > 0 && CoreUtilities.equalsIgnoreCase(args.get(0), markName)) {
                    break;
                }
                scriptEntry.getResidingQueue().removeFirst();
            }
        }
        else {
            Debug.echoError(scriptEntry, "Cannot go to that location - doesn't seem to exist!");
        }
    }
}
