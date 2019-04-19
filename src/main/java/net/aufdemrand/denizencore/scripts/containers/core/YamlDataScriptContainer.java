package net.aufdemrand.denizencore.scripts.containers.core;

import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;

public class YamlDataScriptContainer extends ScriptContainer {

    // <--[language]
    // @name Yaml Data Script Containers
    // @group Script Container System
    // @description
    // Yaml Data script containers are generic script containers for information
    // that will be referenced by other scripts.
    //
    // No part of a 'yaml data' script container is ever run as commands.
    //
    // There are no required keys.
    //
    // Generally, data is read using the <@link tag s@script.yaml_key> tag.
    //
    // <code>
    // Yaml_Data_Script_Name:
    //
    //   type: yaml data
    //
    //   # Your data here
    //
    // </code>
    //
    // -->

    public YamlDataScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
    }
}
