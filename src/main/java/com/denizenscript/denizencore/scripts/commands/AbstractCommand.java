package com.denizenscript.denizencore.scripts.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCommand {

    public String syntax = "No usage defined! See documentation for more information!";

    public List<String> flatArgs = new ArrayList<>();

    public List<String> prefixes = new ArrayList<>();

    public void setSyntax(String syntax) {
        this.syntax = syntax;
        int firstSpace = syntax.indexOf(' ');
        if (firstSpace < 0) {
            return;
        }
        String cleaned = syntax.substring(firstSpace).replace("/", " ");
        cleaned = cleaned.replace("[", "").replace("]", "").replace("(", "").replace(")", "").replace("{", "").replace("}", "");
        List<String> args = CoreUtilities.split(cleaned, ' ');
        for (String arg : args) {
            if (arg.isEmpty()) {
                continue;
            }
            int colonIndex = arg.indexOf(':');
            if (colonIndex > 0) {
                String prefix = arg.substring(0, colonIndex);
                if (!prefix.contains("<")) {
                    prefixes.add(prefix);
                }
            }
            else if (!arg.contains("<") && !arg.contains("|")) {
                flatArgs.add(arg);
            }
        }
        if (Debug.verbose) {
            Debug.log("Command syntax '" + syntax + "' parsed to flat args: ( " + String.join(", ", flatArgs) + " ) and prefixes ( " + String.join(", ", prefixes) + " ).");
        }
    }

    private boolean preparseArgs = true;

    public boolean forceHold = false;

    public int minimumArguments = 0;

    public int maximumArguments = Integer.MAX_VALUE;

    public void setRequiredArguments(int min, int max) {
        minimumArguments = min;
        maximumArguments = max == -1 ? Integer.MAX_VALUE : max;
    }

    public void setParseArgs(boolean parse) {
        preparseArgs = parse;
    }

    public boolean shouldPreParse() {
        return preparseArgs;
    }

    public void setName(String commandName) {
        name = commandName.toUpperCase();
    }

    @Deprecated
    public AbstractCommand as(String commandName) {
        setName(commandName);
        DenizenCore.getCommandRegistry().register(this.name, this);
        onEnable();
        return this;
    }

    protected String name;

    public String getName() {
        return name;
    }

    public String getUsageHint() {
        return syntax;
    }

    /**
     * Part of the Plugin disable sequence.
     * <p/>
     * Can be '@Override'n by a Command which requires a method when bukkit sends a
     * onDisable() to Denizen. (ie. Server shuts down or restarts)
     */
    public void onDisable() {
    }

    @Deprecated
    public void onEnable() {
    }

    @Deprecated
    public void withOptions(String usageHint, int numberOfRequiredArgs) {
        minimumArguments = numberOfRequiredArgs;
        setSyntax(usageHint);
    }

    public abstract void execute(ScriptEntry scriptEntry);

    /**
     * Called by the CommandExecutor before the execute() method is called. Arguments
     * should be iterated through and checked before continuing to execute(). Note that
     * PLAYER:<player> and NPC:<npc> arguments are parsed automatically by the Executor
     * and should not be handled by this Command otherwise. Their output is stored in the
     * attached {@link ScriptEntry} and can be retrieved with ((BukkitScriptEntryData)scriptEntry.entryData).getPlayer(),
     * scriptEntry.getOfflinePlayer() (if the player specified is not online), and
     * ((BukkitScriptEntryData)scriptEntry.entryData).getNPC(). Remember that any of these have a possibility of being null
     * and should be handled accordingly if required by this Command.
     *
     * @param scriptEntry The {@link ScriptEntry}, which contains run-time context that may
     *                    be utilized by this Command.
     * @throws InvalidArgumentsException Will halt execution of this Command and hint usage to the console to avoid
     *                                   unwanted behavior due to missing information.
     */
    public abstract void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException;

}
