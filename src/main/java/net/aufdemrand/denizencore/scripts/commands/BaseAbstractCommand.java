package net.aufdemrand.denizencore.scripts.commands;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.interfaces.RegistrationableInstance;

public abstract class BaseAbstractCommand implements RegistrationableInstance {
    /**
     * Contains required options for a Command in a single class for the
     * ability to add optional options in the future.
     *
     * See {@link #withOptions} for information on using CommandOptions with this command.
     */
    public class CommandOptions {
        public String USAGE_HINT;
        public int REQUIRED_ARGS;

        public CommandOptions(String usageHint, int numberOfRequiredArgs) {
            this.USAGE_HINT = usageHint;
            this.REQUIRED_ARGS = numberOfRequiredArgs;
        }
    }

    private boolean braced = false;

    public void setBraced() {
        braced = true;
    }

    public boolean isBraced() {
        return braced;
    }

    @Override
    public BaseAbstractCommand activate() {
        return this;
    }

    @Override
    public BaseAbstractCommand as(String commandName) {
        // Register command with Registry with a Name
        name = commandName.toUpperCase();
        DenizenCore.getCommandRegistry().register(this.name, this);
        onEnable();
        return this;
    }

    protected String name;

    public CommandOptions commandOptions;

    @Override
    public String getName() {
        return name;
    }

    /**
     * Returns the {@link CommandOptions} specified at startup.
     *
     * @return commandOptions
     *
     */
    public CommandOptions getOptions() {
        return commandOptions;
    }

    /**
     * Returns USAGE_HINT specified in the {@link CommandOptions}, if specified.
     *
     * @return USAGE_HINT if specified, otherwise "No usage defined! See documentation for more information!"
     *
     */
    public String getUsageHint() {
        return !commandOptions.USAGE_HINT.equals("") ? commandOptions.USAGE_HINT : "No usage defined! See documentation for more information!";
    }

    /**
     * Part of the Plugin disable sequence.
     *
     * Can be '@Override'n by a Command which requires a method when bukkit sends a
     * onDisable() to Denizen. (ie. Server shuts down or restarts)
     *
     */
    public void onDisable() {

    }

    /**
     * Part of the Plugin enable sequence. This is called when the command is
     * instanced by the CommandRegistry, which is generally on a server startup.
     *
     * Can be '@Override'n by a Command which requires a method when starting, such
     * as registering as a Bukkit Listener.
     *
     */
    public void onEnable() {

    }

    /**
     * Creates a new {@link CommandOptions} for this command.
     *
     * @param usageHint
     *         A String representation of the suggested usage format of this command.
     *         Typically []'s represent required arguments and ()'s represent optional arguments.
     *         Example from SWITCH command: [LOCATION:x,y,z,world] (STATE:ON|OFF|TOGGLE) (DURATION:#)
     * @param numberOfRequiredArgs
     *         The minimum number of required arguments needed to ensure proper functionality. The
     *         Executer will not parseArgs() for this command if this number is not met.
     * @return
     *         The newly created CommandOptions object for the possibility of setting other
     *         criteria, though currently none exists.
     *
     */
    public CommandOptions withOptions(String usageHint, int numberOfRequiredArgs) {
        this.commandOptions = new CommandOptions(usageHint, numberOfRequiredArgs);
        return commandOptions;
    }
}
