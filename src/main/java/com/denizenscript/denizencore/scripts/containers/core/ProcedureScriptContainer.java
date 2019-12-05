package com.denizenscript.denizencore.scripts.containers.core;

import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.YamlConfiguration;

public class ProcedureScriptContainer extends ScriptContainer {

    // <--[language]
    // @name Procedure Script Containers
    // @group Script Container System
    // @description
    // Procedure script containers are used to define a script that can be ran through a tag.
    //
    // Generally called via <@link tag proc> or <@link tag proc.context>.
    //
    // The only required key is 'script:'.
    //
    // Note that procedure scripts must NEVER change external state.
    // That is, a procedure script cannot change anything at all, ONLY determine a value.
    // Setting a flag, loading a YAML document, placing a block, etc. are all examples of external changes that are NOT allowed.
    //
    // This restriction comes from two main reasons:
    // - Tags run in arbitrary conditions. They may be read asynchronously or in other weird circumstances that can result
    // in applied changes crashing your server or other unexpected side effects.
    // - Tags can run for a variety of reasons.
    // If you were to make a proc script 'spawn_entity' that actually spawns an entity into the world,
    // you would likely end up with a *lot* of unintentional entity spawns.
    // Some tags will be read multiple times when theoretically ran once,
    // in some circumstances a tag read might even be based on user input! (Particularly if you ever make use of the '.parsed' tag,
    // or the list.parse/filter/sort_by_number tags).
    // Imagine if for example, a tag can be read when users input a specific custom command,
    // and a clever user finds out they can type "/testcommand 32 <proc[spawn_entity].context[creeper]>"
    // to spawn a creeper ... that would be a major problem!
    // In general, maximum caution is the best for situations like this... simply *never* make a procedure
    // that executes external changes.
    //
    // <code>
    // Proc_Script_Name:
    //
    //   type: procedure
    //
    //   # Optionally specify definition names to use with the 'context' input of the proc tag.
    //   definitions: def|names|here
    //
    //   script:
    //
    //   # Put any logic, then determine the result.
    //   - determine 5
    //
    // </code>
    //
    // -->

    public ProcedureScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
    }
}
