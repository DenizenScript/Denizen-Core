package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;

import java.util.List;
import java.util.function.Consumer;

public class ScriptUtilities {

    /**
     * Runs any script.
     * @param container the script to run.
     * @param path the path within the container to run (or null for default).
     * @param data the player/npc/other data to attach (or null for empty).
     * @param context the provider for &lt;context&gt; data (or null for none).
     * @param configure a function to configure the queue (eg add definitions) before starting (or null for none).
     * @return the new queue.
     */
    public static ScriptQueue createAndStartQueue(ScriptContainer container, String path, ScriptEntryData data, ContextSource context, Consumer<ScriptQueue> configure) {
        if (data == null) {
            data = DenizenCore.getImplementation().getEmptyScriptEntryData();
        }
        List<ScriptEntry> entries;
        if (path == null) {
            entries = container.getBaseEntries(data.clone());
        }
        else {
            entries = container.getEntries(data.clone(), path);
        }
        DurationTag speed;
        ScriptQueue queue;
        if (container.contains("SPEED")) {
            speed = DurationTag.valueOf(container.getString("SPEED", "0"));
        }
        else {
            speed = DurationTag.valueOf(DenizenCore.getImplementation().scriptQueueSpeed());
        }
        if (speed.getTicks() > 0) {
            queue = new TimedQueue(container.getName()).setSpeed(speed.getTicks());
        }
        else {
            queue = new InstantQueue(container.getName());
        }
        queue.addEntries(entries);
        queue.contextSource = context;
        if (configure != null) {
            configure.accept(queue);
        }
        queue.start();
        return queue;
    }
}
