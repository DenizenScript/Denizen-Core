package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.TimeTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.data.ActionableDataProvider;
import com.denizenscript.denizencore.utilities.data.DataAction;
import com.denizenscript.denizencore.utilities.data.DataActionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public class FlagCommand extends AbstractCommand {

    public FlagCommand() {
        setName("flag");
        setSyntax("flag [<object>|...] [<name>([<#>])](:<action>)[:<value>] (duration:<value>)");
        setRequiredArguments(1, 3);
        isProcedural = false;
    }

    // <--[command]
    // @Name Flag
    // @Syntax flag [<object>|...] [<name>([<#>])](:<action>)[:<value>] (duration:<value>)
    // @Required 2
    // @Maximum 3
    // @Short Sets or modifies a flag on any flaggable object.
    // @Group core
    // @Guide https://guide.denizenscript.com/guides/basics/flags.html
    //
    // @Description
    // The flag command sets or modifies custom data values stored on any flaggable object (the server, a player/NPC/entity, a block location, ...).
    // See also <@link language flag system>.
    //
    // This command supports data actions, see <@link language data actions>.
    //
    // @Tags
    // <FlaggableObject.flag[<flag_name>]>
    // <FlaggableObject.has_flag[<flag_name>]>
    // <FlaggableObject.flag_expiration[<flag_name>]>
    // <FlaggableObject.list_flags>
    // <server.online_players_flagged[<flag_name>]>
    // <server.players_flagged[<flag_name>]>
    // <server.spawned_npcs_flagged[<flag_name>]>
    // <server.npcs_flagged[<flag_name>]>
    //
    // @Usage
    // Use to create or set a flag on a player.
    // - flag player playstyle:aggressive
    //
    // @Usage
    // Use to flag an npc with a given tag value.
    // - flag npc location:<npc.location>
    //
    // @Usage
    // Use to apply mathematical changes to a flag's value on a unique object.
    // - flag <context.damager> damage_dealt:+:<context.damage>
    //
    // @Usage
    // Use to add an item to a server flag as a new value without removing existing values.
    // - flag server cool_people:->:<[player]>
    //
    // @Usage
    // Use to add multiple items as individual new values to a server flag that is already a list.
    // - flag server cool_people:|:<[player]>|<[someplayer]>
    //
    // @Usage
    // Use to remove an entry from a server flag.
    // - flag server cool_people:<-:<[someplayer]>
    //
    // @Usage
    // Use to clear a flag and fill it with a new list of values.
    // - flag server cool_people:!|:<[player]>|<[someplayer]>|<[aplayer]>
    //
    // @Usage
    // Use to completely remove a flag.
    // - flag server cool_people:!
    //
    // @Usage
    // Use to modify a specific index in a list flag.
    // - flag server myflag[3]:HelloWorld
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("targets")
                && !arg.hasPrefix()) {
                scriptEntry.addObject("targets", arg.asType(ListTag.class));
            }
            else if (!scriptEntry.hasObject("duration")
                    && arg.matchesPrefix("duration")
                    && arg.matchesArgumentType(DurationTag.class)) {
                scriptEntry.addObject("duration", arg.asType(DurationTag.class));
            }
            else if (!scriptEntry.hasObject("flag_action")) {
                scriptEntry.addObject("flag_action", DataActionHelper.parse(new FlagActionProvider(), arg.getRawValue()));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("targets")) {
            throw new InvalidArgumentsException("Must specify flag target(s)!");
        }
        if (!scriptEntry.hasObject("flag_action")) {
            throw new InvalidArgumentsException("Must specify a flag to set!");
        }
    }

    public static class FlagActionProvider extends ActionableDataProvider {

        public AbstractFlagTracker tracker;

        public TimeTag expiration;

        @Override
        public ObjectTag getValueAt(String keyName) {
            return tracker.getFlagValue(keyName);
        }

        @Override
        public void setValueAt(String keyName, ObjectTag value) {
            tracker.setFlag(keyName, value, expiration);
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ListTag targets = scriptEntry.getObjectTag("targets");
        DurationTag duration = scriptEntry.getObjectTag("duration");
        DataAction flagAction = (DataAction) scriptEntry.getObject("flag_action");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), targets.debug()
                    + (duration == null ? "" : duration.debug())
                    + flagAction.debug());
        }
        if (duration != null) {
            ((FlagActionProvider) flagAction.provider).expiration = new TimeTag(TimeTag.now().millis() + duration.getMillis());
        }
        for (ObjectTag object : targets.objectForms) {
            AbstractFlagTracker tracker;
            if (CoreUtilities.equalsIgnoreCase(object.toString(), "server")) {
                tracker = DenizenCore.getImplementation().getServerFlags();
            }
            else if (object instanceof FlaggableObject) {
                tracker = ((FlaggableObject) object).getFlagTracker();
            }
            else {
                Debug.echoError("Cannot flag '" + object + "': that object type is not flaggable!");
                continue;
            }
            ((FlagActionProvider) flagAction.provider).tracker = tracker;
            flagAction.execute(scriptEntry.getContext());
            if (object instanceof FlaggableObject) {
                ((FlaggableObject) object).reapplyTracker(tracker);
            }
        }
    }
}
