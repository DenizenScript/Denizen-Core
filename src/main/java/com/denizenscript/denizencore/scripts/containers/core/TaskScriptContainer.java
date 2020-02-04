package com.denizenscript.denizencore.scripts.containers.core;

import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.YamlConfiguration;

public class TaskScriptContainer extends ScriptContainer {

    // <--[language]
    // @name Task Script Containers
    // @group Script Container System
    // @description
    // Task script containers are generic script containers for commands that can be run at
    // any time by command.
    //
    // Generally tasks will be ran by <@link command run> or <@link command inject>.
    //
    // The only required key on a task script container is the 'script:' key.
    //
    // <code>
    // Task_Script_Name:
    //
    //   type: task
    //
    //   # When intending to run a task script via the run command with the "def:" argument to pass data through,
    //   # use this "definitions" key to specify the names of the definitions (in the same order as the "def:" argument will use).
    //   definitions: name1|name2|...
    //
    //   script:
    //
    //   - your commands here
    //
    // </code>
    //
    // -->

    public TaskScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
    }
}
