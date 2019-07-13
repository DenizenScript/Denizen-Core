package com.denizenscript.denizencore.scripts.containers.core;

import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.events.OldEventManager;
import com.denizenscript.denizencore.events.ScriptEvent;

public class WorldScriptContainer extends ScriptContainer {

    // <--[language]
    // @name World Script Containers
    // @group Script Container System
    // @description
    // World script containers are generic script containers for commands that are automatically
    // ran when some given event happens in the server.
    //
    // The only required key is 'events:', within which you can list any events to handle.
    //
    // <code>
    // World_Script_Name:
    //
    //   type: world
    //
    //   events:
    //
    //     # Any event label can be placed here
    //     # This includes generic labels like 'on entity death:',
    //     # Specified labels  like 'on player death:',
    //     # And detailed labels like 'on player death ignorecancelled:true priority:5:'
    //     some event label:
    //     # Write any logic that should fire when the event runs.
    //     # Optionally 'determine' any results to the event.
    //     - some commands
    //
    //     # List additional events here
    //
    // </code>
    //
    // -->

    public WorldScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
        OldEventManager.world_scripts.put(getName(), this);
        ScriptEvent.worldContainers.add(this);
    }
}
