package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;

import java.util.List;
import java.util.function.Consumer;

public class ScriptUtilities {

    /**
     * Runs any script.
     * @param container the script to run.
     * @param path the path within the container to run (or null for default).
     * @param data the player/npc/other data to attach (or null for empty).
     * @return the new queue, or null if not possible.
     */
    public static ScriptQueue createAndStartQueue(ScriptContainer container, String path, ScriptEntryData data) {
        return createAndStartQueue(container, path, data, null, null, null, null, null, null);
    }

    /**
     * Runs any script.
     * @param container the script to run.
     * @param path the path within the container to run (or null for default).
     * @param data the player/npc/other data to attach (or null for empty).
     * @param context the provider for &lt;context&gt; data (or null for none).
     * @param configure a function to configure the queue (eg add definitions) before starting (or null for none).
     * @param speed a specific speed to apply (or null for default).
     * @param id the script ID to force (or null for default).
     * @param definitions definitions to add (or null for none).
     * @param debugDefinitions the object to debug added definitions with (or null for no debug).
     * @return the new queue, or null if not possible.
     */
    public static ScriptQueue createAndStartQueue(ScriptContainer container, String path, ScriptEntryData data,
                                                  ContextSource context, Consumer<ScriptQueue> configure,
                                                  DurationTag speed, String id, ListTag definitions, Debuggable debugDefinitions) {
        if (!container.canRunScripts) {
            Debug.echoError("The script container '" + container.getName() + "' is of type '" + container.getContainerType() + "' which cannot run scripts. Consider using a task script instead.");
            return null;
        }
        if (id == null) {
            id = container.getName();
        }
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
        if (entries == null) {
            return null;
        }
        ScriptQueue queue;
        if (speed == null) {
            if (container.contains("SPEED")) {
                speed = DurationTag.valueOf(container.getString("SPEED", "0"), DenizenCore.getImplementation().getTagContext(container));
            }
            if (speed == null) {
                speed = DurationTag.valueOf(DenizenCore.getImplementation().scriptQueueSpeed(), DenizenCore.getImplementation().getTagContext(container));
            }
        }
        if (speed.getTicks() > 0) {
            queue = new TimedQueue(id).setSpeed(speed.getTicks());
        }
        else {
            queue = new InstantQueue(id);
        }
        queue.addEntries(entries);
        queue.contextSource = context;
        if (definitions != null) {
            List<String> definition_names = null;
            if (container.contains("definitions")) {
                String str = container.getString("definitions");
                definition_names = CoreUtilities.split(str, '|');
            }
            int x = 1;
            for (String definition : definitions) {
                String name = definition_names != null && definition_names.size() >= x ? definition_names.get(x - 1).trim() : String.valueOf(x);
                queue.addDefinition(name, definition);
                if (debugDefinitions != null && debugDefinitions.shouldDebug()) {
                    Debug.echoDebug(debugDefinitions, "Adding definition '" + name + "' as " + definition);
                }
                x++;
            }
            queue.addDefinition("raw_context", definitions);
        }
        if (configure != null) {
            configure.accept(queue);
        }
        queue.start();
        return queue;
    }
}
