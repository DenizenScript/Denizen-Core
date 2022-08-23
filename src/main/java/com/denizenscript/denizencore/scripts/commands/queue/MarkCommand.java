package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgRaw;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;

public class MarkCommand extends AbstractCommand {

    public MarkCommand() {
        setName("mark");
        setSyntax("mark [<name>]");
        setRequiredArguments(1, 1);
        isProcedural = true;
        autoCompile();
    }

    // <--[command]
    // @Name Mark
    // @Syntax mark [<name>]
    // @Required 1
    // @Maximum 1
    // @Short Marks a location for <@link command goto>.
    // @Group queue
    //
    // @Description
    // Marks a location for the goto command. See <@link command goto> for details.
    //
    // @Tags
    //
    // None
    //
    // @Usage
    // Use to mark a location.
    // - mark potato
    //
    // -->

    public static void autoExecute(@ArgRaw @ArgLinear @ArgName("mark_name") String markName) {
        // Do nothing, this is just a marker.
    }
}
