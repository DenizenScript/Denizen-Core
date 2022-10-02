package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptBuilder;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Basic utilities for running '/ex' command implementations. Not for general use.
 */
public class ExCommandHelper {

    /**
     * Creates and starts a queue based on a single command; for '/ex' command implementations.
     */
    public static ScriptQueue runString(String id, String command, ScriptEntryData data, Consumer<ScriptQueue> configure) {
        if (data == null) {
            data = DenizenCore.implementation.getEmptyScriptEntryData();
        }
        InstantQueue queue = new InstantQueue(id);
        queue.addEntries(ScriptBuilder.buildScriptEntries(Collections.singletonList(command), null, data));
        if (configure != null) {
            configure.accept(queue);
        }
        queue.start(true);
        return queue;
    }

    /**
     * ScriptQueues stored from {@link #runStringSustained(Object, String, String, ScriptEntryData, Consumer)}.
     */
    public static final Map<Object, TimedQueue> sustainedQueues = new HashMap<>();

    /**
     * Creates and starts a queue based on a single command; for sustained '/exs' command implementations.
     * The queue will be held in {@link #sustainedQueues} using the given {@code Object source} until {@link #removeSustainedQueue(Object)} is called.
     * Calling this method again using the same {@code Object source} will continue the same queue.
     */
    public static ScriptQueue runStringSustained(Object source, String id, String command, ScriptEntryData data, Consumer<ScriptQueue> configure) {
        if (data == null) {
            data = DenizenCore.implementation.getEmptyScriptEntryData();
        }
        TimedQueue queue = sustainedQueues.get(source);
        if (queue == null || queue.isStopped) {
            queue = new TimedQueue(id);
            queue.waitWhenEmpty = true;
            sustainedQueues.put(source, queue);
        }
        queue.addEntries(ScriptBuilder.buildScriptEntries(Collections.singletonList(command), null, data));
        if (configure != null) {
            configure.accept(queue);
        }
        if (!queue.is_started) {
            queue.start(true);
        }
        else {
            queue.onStart();
        }
        return queue;
    }

    /**
     * Removes a sustained queue for the given {@code Object source}, as created by {@link #runStringSustained(Object, String, String, ScriptEntryData, Consumer)}
     * and returns whether the queue existed (and was still capable of continuing).
     */
    public static boolean removeSustainedQueue(Object source) {
        ScriptQueue queue = sustainedQueues.remove(source);
        return queue != null && !queue.isStopped;
    }
}
