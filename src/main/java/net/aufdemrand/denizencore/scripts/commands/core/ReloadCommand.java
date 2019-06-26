package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;

public class ReloadCommand extends AbstractCommand {

    // <--[command]
    // @Name Reload
    // @Syntax reload
    // @Required 0
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
