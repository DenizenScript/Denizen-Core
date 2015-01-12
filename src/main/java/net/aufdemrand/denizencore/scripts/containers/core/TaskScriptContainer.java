package net.aufdemrand.denizencore.scripts.containers.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.objects.Duration;

import net.aufdemrand.denizencore.scripts.queues.core.InstantQueue;
import net.aufdemrand.denizencore.scripts.queues.core.TimedQueue;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskScriptContainer extends ScriptContainer {

    public TaskScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
    }

    public Duration getSpeed() {
        Duration speed;
        if (contains("SPEED")) {
            if (getString("SPEED", "0").toUpperCase().equals("INSTANT"))
                speed = Duration.valueOf("0t");
            else
                speed = Duration.valueOf(getString("SPEED", "0t"));

        } else
        // TODO: Settings = read dImplementation
            speed = new Duration(Duration.valueOf(DenizenCore.getImplementation().scriptQueueSpeed()).getSeconds());

        return speed;
    }

    public TaskScriptContainer setSpeed(Duration speed) {
        //  TODO: Remove with RunTask
        set("SPEED", speed.getSeconds());
        return this;
    }

    // TODO: use a context object
    public ScriptQueue runTaskScript(ScriptEntryData data, Map<String, String> context) {
        return runTaskScript(ScriptQueue.getNextId(getName()), data, context);
    }

    public ScriptQueue runTaskScript(String queueId, ScriptEntryData data, Map<String, String> context) {
        ScriptQueue queue;
        if (getSpeed().getSeconds() == 0)
            queue = InstantQueue.getQueue(queueId);
        else queue = TimedQueue.getQueue(queueId).setSpeed(getSpeed().getTicks());

        List<ScriptEntry> listOfEntries = getBaseEntries(data);
        if (context != null)
            ScriptBuilder.addObjectToEntries(listOfEntries, "context", context);
        queue.addEntries(listOfEntries);
        queue.start();
        return queue;
    }

    public Map<String, Integer> getContextMap() {
        if (contains("CONTEXT")) {
            Map<String, Integer> context = new HashMap<String, Integer>();
            int x = 1;
            for (String name : getString("CONTEXT").split("\\|")) {
                context.put(name.toUpperCase(), x);
                x++;
            }
            return context;
        }
        return Collections.emptyMap();
    }

    public ScriptQueue runTaskScriptWithDelay(String queueId, ScriptEntryData data, Map<String, String> context, Duration delay) {
        ScriptQueue queue;
        if (getSpeed().getSeconds() == 0)
            queue = InstantQueue.getQueue(queueId);
        else queue = TimedQueue.getQueue(queueId).setSpeed(getSpeed().getTicks());

        List<ScriptEntry> listOfEntries = getBaseEntries(data);
        if (context != null)
            ScriptBuilder.addObjectToEntries(listOfEntries, "context", context);
        queue.addEntries(listOfEntries);
        queue.delayUntil(System.currentTimeMillis() + (long) (delay.getSeconds() * 1000));
        queue.start();
        return queue;
    }

    public ScriptQueue injectTaskScript(String queueId, ScriptEntryData data, Map<String, String> context) {
        ScriptQueue queue = ScriptQueue._getExistingQueue(queueId);
        List<ScriptEntry> listOfEntries = getBaseEntries(data);
        if (context != null)
            ScriptBuilder.addObjectToEntries(listOfEntries, "context", context);
        queue.injectEntries(listOfEntries, 0);
        queue.start();
        return queue;
    }
}
