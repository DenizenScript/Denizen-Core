package net.aufdemrand.denizencore.scripts.containers.core;

import net.aufdemrand.denizencore.events.EventManager;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;

public class WorldScriptContainer extends ScriptContainer {

    public WorldScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
        EventManager.world_scripts.put(getName(), this);
    }
}
