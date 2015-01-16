package net.aufdemrand.denizencore.scripts.containers.core;

import net.aufdemrand.denizencore.events.OldEventManager;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;

public class WorldScriptContainer extends ScriptContainer {

    public WorldScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
        OldEventManager.world_scripts.put(getName(), this);
        ScriptEvent.worldContainers.add(this);
    }
}
