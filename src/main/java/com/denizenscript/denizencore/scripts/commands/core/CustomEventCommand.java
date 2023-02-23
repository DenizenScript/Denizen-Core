package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.events.core.CustomScriptEvent;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;

public class CustomEventCommand extends AbstractCommand {

    public CustomEventCommand() {
        setName("customevent");
        setSyntax("customevent [id:<id>] (context:<map>)");
        setRequiredArguments(1, 2);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name CustomEvent
    // @Syntax customevent [id:<id>] (context:<map>)
    // @Required 1
    // @Maximum 2
    // @Short Fires a custom world script event.
    // @Group core
    //
    // @Description
    // Fires a custom world script event.
    //
    // Input is an ID (the name of your custom event, choose a constant name to use), and an optional MapTag of context data.
    //
    // Linked data (player, npc, ...) is automatically sent across to the event.
    //
    // Use with <@link event custom event>
    //
    // @Tags
    // <entry[saveName].any_ran> returns a boolean indicating whether any events ran as a result of this command.
    // <entry[saveName].was_cancelled> returns a boolean indicating whether the event was cancelled.
    // <entry[saveName].determination_list> returns a ListTag of determinations to this event. Will be an empty list if 'determine output:' is never used.
    //
    // @Usage
    // Use to call a custom event with path "on custom event id:things_happened:"
    // - customevent id:things_happened
    //
    // @Usage
    // Use to call a custom event with path "on custom event id:things_happened:" and supply a context map of basic data.
    // - customevent id:things_happened context:[a=1;b=2;c=3]
    //
    // @Usage
    // Use to call a custom event with a path such as "on custom event id:things_happened data:item:stone:" and supply a context map of more interesting data.
    // - definemap context:
    //     waffle: hello world
    //     item: <player.item_in_hand>
    // - customevent id:things_happened context:<[context]>
    //
    // @Usage
    // Use to call a custom event and allow cancelling or replacing a value.
    // - definemap context:
    //     message: hello world
    // - customevent id:custom_message context:<[context]> save:event
    // - if <entry[event].was_cancelled>:
    //     - stop
    // - define message <entry[event].determination_list.first.if_null[<[context.message]>]>
    // - narrate "Final message is: <[message]>"
    //
    // -->

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("id") ElementTag id,
                                   @ArgDefaultNull @ArgPrefixed @ArgName("context") MapTag context) {
        CustomScriptEvent ranEvent = CustomScriptEvent.runCustomEvent(scriptEntry.entryData, id.asLowerString(), context);
        scriptEntry.saveObject("any_ran", new ElementTag(ranEvent != null && ranEvent.anyMatched));
        scriptEntry.saveObject("was_cancelled", new ElementTag(ranEvent != null && ranEvent.cancelled));
        scriptEntry.saveObject("determination_list", ranEvent == null ? new ListTag() : new ListTag(ranEvent.determinations));
    }
}
