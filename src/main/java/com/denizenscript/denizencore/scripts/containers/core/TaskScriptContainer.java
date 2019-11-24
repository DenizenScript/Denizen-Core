package com.denizenscript.denizencore.scripts.containers.core;

import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.ScriptBuilder;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    DurationTag speed = null;

    public DurationTag getSpeed() {
        if (speed != null) {
            return speed;
        }
        if (contains("speed")) {
            String tmp = getString("speed", "0t");
            if (CoreUtilities.toLowerCase(tmp).equals("instant")) {
                speed = DurationTag.valueOf("0t");
            }
            else {
                speed = DurationTag.valueOf(tmp);
            }
        }
        else {
            speed = DurationTag.valueOf(DenizenCore.getImplementation().scriptQueueSpeed());
        }
        return speed;
    }

    public TaskScriptContainer setSpeed(DurationTag speed) {
        //  TODO: Remove with RunTask
        this.speed = speed;
        return this;
    }

    public ScriptQueue runTaskScript(ScriptEntryData data, Map<String, String> context) {
        return runTaskScript(getName(), data, context);
    }

    public ScriptQueue runTaskScript(String queueId, ScriptEntryData data, Map<String, String> context) {
        ScriptQueue queue;
        if (getSpeed().getSeconds() == 0) {
            queue = new InstantQueue(queueId);
        }
        else {
            queue = new TimedQueue(queueId).setSpeed(getSpeed().getTicks());
        }

        List<ScriptEntry> listOfEntries = getBaseEntries(data);
        if (context != null) {
            ScriptBuilder.addObjectToEntries(listOfEntries, "context", context);
        }
        queue.addEntries(listOfEntries);
        queue.start();
        return queue;
    }

    public Map<String, Integer> getContextMap() {
        if (contains("context")) {
            Map<String, Integer> context = new HashMap<>();
            int x = 1;
            for (String name : getString("context").split("\\|")) {
                context.put(name.toUpperCase(), x);
                x++;
            }
            return context;
        }
        return Collections.emptyMap();
    }

    public ScriptQueue runTaskScriptWithDelay(String queueId, ScriptEntryData data, Map<String, String> context, DurationTag delay) {
        ScriptQueue queue;
        if (getSpeed().getSeconds() == 0) {
            queue = new InstantQueue(queueId);
        }
        else {
            queue = new TimedQueue(queueId).setSpeed(getSpeed().getTicks());
        }

        List<ScriptEntry> listOfEntries = getBaseEntries(data);
        if (context != null) {
            ScriptBuilder.addObjectToEntries(listOfEntries, "context", context);
        }
        queue.addEntries(listOfEntries);
        queue.delayUntil(DenizenCore.serverTimeMillis + (long) (delay.getSeconds() * 1000));
        queue.start();
        return queue;
    }

    public ScriptQueue injectTaskScript(String queueId, ScriptEntryData data, Map<String, String> context) {
        ScriptQueue queue = ScriptQueue.getExistingQueue(queueId);
        List<ScriptEntry> listOfEntries = getBaseEntries(data);
        if (context != null) {
            ScriptBuilder.addObjectToEntries(listOfEntries, "context", context);
        }
        queue.injectEntries(listOfEntries, 0);
        queue.start();
        return queue;
    }
}
