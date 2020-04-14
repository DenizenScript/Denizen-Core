package com.denizenscript.denizencore.scripts.containers.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;
import com.denizenscript.denizencore.utilities.ScriptUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;

import java.util.List;
import java.util.function.Consumer;

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

    /**
     * Runs the task script.
     * @return the new queue.
     */
    public ScriptQueue run() {
        return run(null, null, null);
    }

    /**
     * Runs the task script.
     * @param data the player/npc/other data to attach.
     * @return the new queue.
     */
    public ScriptQueue run(ScriptEntryData data) {
        return run(data, null, null);
    }

    /**
     * Runs the task script.
     * @param data the player/npc/other data to attach.
     * @param context the provider for &lt;context&gt; data.
     * @return the new queue.
     */
    public ScriptQueue run(ScriptEntryData data, ContextSource context) {
        return run(data, context, null);
    }

    /**
     * Runs the task script.
     * @param data the player/npc/other data to attach.
     * @param context the provider for &lt;context&gt; data.
     * @param configure a function to configure the queue (eg add definitions) before starting.
     * @return the new queue.
     */
    public ScriptQueue run(ScriptEntryData data, ContextSource context, Consumer<ScriptQueue> configure) {
        return ScriptUtilities.createAndStartQueue(this, null, data, context, configure, null, null, null, null);
    }
}
