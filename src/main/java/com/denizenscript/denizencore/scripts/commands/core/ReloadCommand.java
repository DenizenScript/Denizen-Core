package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.objects.notable.NoteManager;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

import java.util.function.Consumer;

public class ReloadCommand extends AbstractCommand implements Holdable {

    public ReloadCommand() {
        setName("reload");
        setSyntax("reload ({scripts}/scripts_now/config/saves/notes)");
        setRequiredArguments(0, 1);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name Reload
    // @Syntax reload ({scripts}/scripts_now/config/saves/notes)
    // @Required 0
    // @Maximum 1
    // @Short Reloads all Denizen scripts. Primarily for use as an in-game command.
    // @Group core
    //
    // @Description
    // Reloads all Denizen scripts.
    // Primarily for use as an in-game command, like "/ex reload".
    //
    // By default, reloads scripts in a way that may delay a few ticks to avoid interrupting the server on large reloads.
    //
    // Optionally, specify "scripts_now" to force a locked reload (server freezes until reloaded).
    //
    // You can specify "config", "saves", or "notes" to reload that data instead of scripts.
    //
    // When using 'scripts' (default), the reload command is ~waitable. Refer to <@link language ~waitable>.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to reload scripts automatically
    // - reload
    //
    // -->

    public enum ReloadType { SCRIPTS, SCRIPTS_NOW, CONFIG, SAVES, NOTES }

    public static void autoExecute(ScriptEntry scriptEntry, ScriptQueue queue,
                                   @ArgName("type") @ArgDefaultText("scripts") ReloadType type) {
        switch (type) {
            case SCRIPTS:
                Consumer<String> altDebug = queue.debugOutput;
                if (altDebug != null) {
                    DebugInternals.specialBackupSender = (s) -> DenizenCore.runOnMainThread(() -> altDebug.accept(s));
                }
                DenizenCore.reloadScripts(true, () -> {
                    DebugInternals.specialBackupSender = null;
                    scriptEntry.setFinished(true);
                });
                break;
            case SCRIPTS_NOW:
                scriptEntry.setFinished(true);
                DenizenCore.reloadScripts(false, null);
                break;
            case CONFIG:
                scriptEntry.setFinished(true);
                DenizenCore.implementation.reloadConfig();
                break;
            case SAVES:
                scriptEntry.setFinished(true);
                DenizenCore.implementation.reloadSaves();
                break;
            case NOTES:
                scriptEntry.setFinished(true);
                NoteManager.reload();
                break;
        }
    }
}
