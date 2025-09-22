package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.data.ActionableDataProvider;
import com.denizenscript.denizencore.utilities.data.DataAction;
import com.denizenscript.denizencore.utilities.data.DataActionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FlagCommand extends AbstractCommand {

    public FlagCommand() {
        setName("flag");
        setSyntax("flag [<object>|...] [<name>([<#>])](:<action>)[:<value>] (expire:<time>)");
        setRequiredArguments(1, 3);
        isProcedural = false;
        allowedDynamicPrefixes = true;
    }

    // <--[language]
    // @name Flag System
    // @group Denizen Scripting Language
    // @description
    // The flag system is a core feature of Denizen, that allows for persistent data storage linked to objects.
    //
    // "Persistent" means the data is still around even after a server restart or anything else, and is only removed when you choose for it to be removed.
    // "Linked to objects" means rather than purely global values, flags are associated with a player, or an NPC, or a block, or whatever else.
    //
    // See also the guide page at <@link url https://guide.denizenscript.com/guides/basics/flags.html>.
    //
    // For non-persistent temporary memory, see instead <@link command define>.
    // For more generic memory options, see <@link command yaml> or <@link command sql>.
    //
    // Flags can be sub-mapped with the '.' character, meaning a flag named 'x.y.z' is actually a flag 'x' as a MapTag with key 'y' as a MapTag with key 'z' as the final flag value.
    // In other words, "<server.flag[a.b.c]>" is equivalent to "<server.flag[a].get[b].get[c]>"
    //
    // Server flags can be set by specifying 'server' as the object, essentially a global flag target, that will store data in the file "plugins/Denizen/server_flags.dat"
    //
    // Most unique object types are flaggable - refer to any given object type's language documentation for details.
    //
    // Most flag sets are handled by <@link command flag>, however items are primarily flagged via <@link command inventory> with the 'flag' argument.
    // Any supported object type, including the 'server' base tag, can use the tags
    // <@link tag FlaggableObject.flag>, <@link tag FlaggableObject.has_flag>, <@link tag FlaggableObject.flag_expiration>, <@link tag FlaggableObject.list_flags>.
    //
    // Note that some internal flags exist, and are prefixed with '__' to avoid conflict with normal user flags.
    // This includes:
    // - '__raw' and '__clear' which are part of a fake-flag system used for forcibly setting raw data to a flaggable object,
    // - '__scripts', '__time', etc. which is where some object-type flags are stored inside of server flags,
    // - '__interact_step' which is used for interact script steps, related to <@link command zap>,
    // - '__interact_cooldown' which is used for interact script cooldowns, related to <@link command cooldown>.
    //
    // Flags have an expiration system, which is used by specifying a time at which they should expire (or via a duration which internally calculates the date/time of expiration by adding the duration input to the current date/time).
    // Expirations are then *checked for* in flag tags - meaning, the flag tags will internally compare a stored date/time against the real current date/time,
    // and if the flag's expiration time is in the past, the flag tag will return values equivalent to if the flag doesn't exist.
    // There is no system actively monitoring for flag expirations or applying them. There is no event for expirations occurring, as they don't "occur" per se.
    // In other words, it is correct to say a flag "is expired" or a flag "is not expired",
    // but it is incorrect to say a flag "expires", as it is not an active action (though this wording can be convenient when speaking informally).
    // Expired flags are sometimes 'cleaned up' (meaning, any expired flags get actually removed from internal storage), usually when a flag save file is loaded into the server.
    //
    // As a bonus feature-combo, it is possible to transmit sets of flags exactly in-place and reapply them, this is particular useful for example to synchronize player data across Bungee servers.
    // To do this, you can read raw flag data with the tag <@link tag FlaggableObject.flag_map> and the '__raw' prefix in a flag command. For example:
    // <code>
    // # Gather the original data
    // - define playerdata <player.flag_map[flag1|flag2|taco|potato|waffle|etc]>
    // # Now reapply it elsewhere (eg a different Bungee server)
    // - flag <player> __raw:<[playerdata]>
    // </code>
    // -->

    // <--[command]
    // @Name Flag
    // @Syntax flag [<object>|...] [<name>([<#>])](:<action>)[:<value>] (expire:<time>)
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
    // Flags by default are added permanently (or for the lifetime of the object they're attached to).
    // You can optionally specify a system time the flag will expire at, using either a DurationTag or a TimeTag.
    // If a DurationTag is used, it will be equivalent to: <util.time_now.add[<your_duration_here>]>
    //
    // @Tags
    // <FlaggableObject.flag[<flag_name>]>
    // <FlaggableObject.has_flag[<flag_name>]>
    // <FlaggableObject.flag_expiration[<flag_name>]>
    // <FlaggableObject.list_flags>
    //
    // @Usage
    // Use to create or set a flag on a player.
    // - flag <player> playstyle:aggressive
    //
    // @Usage
    // Use to set a temporary flag for five minutes on a player.
    // - flag <player> just_did_something expire:5m
    //
    // @Usage
    // Use to flag an npc with a given tag value.
    // - flag <npc> location:<npc.location>
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
    // Use to completely remove a flag.
    // - flag server cool_people:!
    //
    // @Usage
    // Use to modify a specific index in a list flag.
    // - flag server myflag[3]:HelloWorld
    // -->

    public static List<Consumer<TabCompletionsBuilder>> flagTabCompleters = new ArrayList<>();

    public static void tabCompleteFlag(TabCompletionsBuilder tab) {
        if (tab.arg.contains(":")) {
            return;
        }
        for (String flagName : DenizenCore.serverFlagMap.listAllFlags()) {
            if (!flagName.startsWith("__")) {
                tab.add(flagName);
            }
        }
        for (Consumer<TabCompletionsBuilder> consumer : flagTabCompleters) {
            consumer.accept(tab);
        }
    }

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tabCompleteFlag(tab);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("targets")
                && !arg.hasPrefix()) {
                scriptEntry.addObject("targets", arg.asType(ListTag.class));
            }
            else if (!scriptEntry.hasObject("expiration")
                    && arg.matchesPrefix("expire", "expires", "expiration", "duration")
                    && arg.matchesArgumentType(TimeTag.class)) {
                scriptEntry.addObject("expiration", arg.asType(TimeTag.class));
            }
            else if (!scriptEntry.hasObject("expiration")
                    && arg.matchesPrefix("expire", "expires", "expiration", "duration")
                    && arg.matchesArgumentType(DurationTag.class)) {
                TimeTag now = TimeTag.now();
                scriptEntry.addObject("expiration", new TimeTag(now.millis() + arg.asType(DurationTag.class).getMillis(), now.instant.getZone()));
            }
            else if (!scriptEntry.hasObject("flag_action")) {
                scriptEntry.addObject("flag_action", DataActionHelper.parse(new FlagActionProvider(), arg, scriptEntry.context));
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
            if (keyName.equals("__raw")) {
                MapTag toSetMap = value.asType(MapTag.class, CoreUtilities.noDebugContext);
                ObjectTag toClear = toSetMap.getObject("__clear");
                if (toClear != null) {
                    for (String key : toClear.asType(ListTag.class, CoreUtilities.noDebugContext)) {
                        if (toSetMap.getObject(key) == null) {
                            tracker.setRootMap(key, null);
                        }
                    }
                }
                for (Map.Entry<StringHolder, ObjectTag> mapData : toSetMap.entrySet()) {
                    if (!mapData.getKey().low.equals("__clear")) {
                        tracker.setRootMap(mapData.getKey().str, (MapTag) mapData.getValue());
                    }
                }
                return;
            }
            if (keyName.startsWith("__")) {
                Debug.echoError("Assigning flag value to reserved '__' namespace (key: '" + keyName + "')");
            }
            tracker.setFlag(keyName, value, expiration);
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ListTag targets = scriptEntry.getObjectTag("targets");
        TimeTag expiration = scriptEntry.getObjectTag("expiration");
        DataAction flagAction = (DataAction) scriptEntry.getObject("flag_action");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), targets, expiration, flagAction);
        }
        ((FlagActionProvider) flagAction.provider).expiration = expiration;
        for (ObjectTag object : targets.objectForms) {
            AbstractFlagTracker tracker;
            if (CoreUtilities.equalsIgnoreCase(object.toString(), "server")) {
                tracker = DenizenCore.serverFlagMap;
            }
            else if (object instanceof FlaggableObject) {
                tracker = ((FlaggableObject) object).getFlagTracker();
                if (tracker == null) {
                    Debug.echoError("The object type '" + object.getDenizenObjectType() + "' is flaggable, however the instance '" + object + "' is not: " + ((FlaggableObject) object).getReasonNotFlaggable());
                    continue;
                }
            }
            else {
                FlaggableObject obj = DenizenCore.implementation.simpleWordToFlaggable(object.toString(), scriptEntry);
                if (obj != null) {
                    tracker = obj.getFlagTracker();
                }
                else {
                    if (object instanceof ElementTag && !object.toString().contains("@")) {
                        Debug.echoError("Cannot flag '" + object + "': that does not appear to be an object! Only objects (like an EntityTag) and special keyword 'server' are flaggable!");
                    }
                    else {
                        Debug.echoError("Cannot flag '" + object + "': that object type is not flaggable!");
                    }
                    continue;
                }
            }
            if (tracker == null) {
                Debug.echoError("Something went wrong, cannot flag '" + object + "'...");
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
