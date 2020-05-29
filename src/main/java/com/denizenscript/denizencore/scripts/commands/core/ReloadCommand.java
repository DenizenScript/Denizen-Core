package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;

public class ReloadCommand extends AbstractCommand {

    public ReloadCommand() {
        setName("reload");
        setSyntax("reload");
        setRequiredArguments(0, 0);
        isProcedural = false;
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

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        // No arguments

        // TODO: Allow 'reload saves', etc?
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        DenizenCore.reloadScripts();
    }
}
