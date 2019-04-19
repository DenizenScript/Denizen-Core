package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;

public class ReloadCommand extends AbstractCommand {

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
