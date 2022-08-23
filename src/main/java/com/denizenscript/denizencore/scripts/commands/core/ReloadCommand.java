package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.DenizenCore;

public class ReloadCommand extends AbstractCommand {

    public ReloadCommand() {
        setName("reload");
        setSyntax("reload");
        setRequiredArguments(0, 0);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name Reload
    // @Syntax reload
    // @Required 0
    // @Maximum 0
    // @Short Reloads all Denizen scripts. Primarily for use as an in-game command.
    // @Group core
    //
    // @Description
    // Reloads all Denizen scripts.
    // Primarily for use as an in-game command, like "/ex reload".
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to reload scripts automatically
    // - reload
    //
    // -->

    public static void autoExecute() {
        DenizenCore.reloadScripts();
    }
}
