package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.ListQueue;
import com.denizenscript.denizencore.utilities.QueueWordList;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.Schedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.scripts.ScriptEntry;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class ScriptQueue implements Debuggable, DefinitionProvider {

    protected static long total_queues = 0;

    public static String getStats() {
        String c1 = DenizenCore.implementation.getTextColor(), c2 = DenizenCore.implementation.getEmphasisColor();
        StringBuilder stats = new StringBuilder();
        TreeSet<Map.Entry<Long, String>> statsSet = new TreeSet<>(Comparator.comparingLong(Map.Entry::getKey));
        for (ScriptEvent event : ScriptEvent.events) {
            if (event.eventData.stats_fires > 0) {
                stats.setLength(0);
                stats.append(c1).append("Event '").append(event.getName()).append(c1).append("' ran ").append(c2).append(event.eventData.stats_fires)
                        .append(c1).append(" times (").append(c2).append(event.eventData.stats_scriptFires).append(c1).append(" script fires)")
                        .append(c1).append(", totalling ").append(c2).append((float) event.eventData.stats_nanoTimes / 1000000f)
                        .append(c1).append("ms, averaging ").append(c2).append((float) event.eventData.stats_nanoTimes / 1000000f / (float) event.eventData.stats_fires)
                        .append(c1).append("ms per event or ").append(c2).append(+((float) event.eventData.stats_nanoTimes / 1000000f / (float) event.eventData.stats_scriptFires)).append(c1).append("ms per script.\n");
                statsSet.add(new HashMap.SimpleEntry<>(event.eventData.stats_nanoTimes, stats.toString()));
            }
        }
        return "Total number of queues created: "
                + total_queues
                + ", currently active queues: "
                + allQueues.size() + ",\n" + String.join("", statsSet.stream().map(Map.Entry::getValue).collect(Collectors.joining()));
    }

    public static ListTag getStatsRawData() {
        ListTag result = new ListTag();
        for (ScriptEvent event : ScriptEvent.events) {
            if (event.eventData.stats_fires > 0) {
                MapTag map = new MapTag();
                map.putObject("name", new ElementTag(event.getName()));
                map.putObject("total_fires", new ElementTag(event.eventData.stats_fires));
                map.putObject("script_fires", new ElementTag(event.eventData.stats_scriptFires));
                map.putObject("total_time", new DurationTag(event.eventData.stats_nanoTimes / 1000.0));
                result.addObject(map);
            }
        }
        return result;
    }

    public static ScriptQueue getExistingQueue(String id) {
        if (!queueExists(id)) {
            return null;
        }
        else {
            return allQueues.get(id);
        }
    }

    protected static LinkedHashMap<String, ScriptQueue> allQueues = new LinkedHashMap<>();

    public static Collection<ScriptQueue> getQueues() {
        return allQueues.values();
    }

    public static boolean queueExists(String id) {
        return allQueues.containsKey(id);
    }

    public String id;

    public String debugId;

    /**
     * Whether this queue is locked to procedural commands only.
     */
    public boolean procedural = false;

    /**
     * Optional secondary debug output method.
     */
    public Consumer<String> debugOutput = null;

    public final ListQueue script_entries = new ListQueue(4);

    private ScriptEntry lastEntryExecuted = null;

    /**
     If this number is larger than DenizenCore.serverTimeMillis, the queue will delay execution of the next ScriptEntry.
     */
    private long delay_time = 0;

    public MapTag definitions = new MapTag();

    public ListTag determinations = null;

    private HashMap<String, ScriptEntry> held_entries;

    public ScriptTag script;

    public ContextSource contextSource = null;

    public DeterminationTarget determinationTarget = null;

    public ScriptQueue replacementQueue = null;

    public boolean is_stopping = false;

    public boolean isStopped = false;

    /**
     * If set true, the queue will simply freeze and wait when it's empty.
     * Otherwise (set false), it will fully stop and remove itself when empty.
     */
    public boolean waitWhenEmpty = false;

    public boolean is_started;

    public long startTime = 0;

    public long startTimeMilli = 0;

    private Runnable callback = null;

    protected ScriptQueue(String id) {
        this.id = id;
        generateId(id, 0);
        total_queues++;
    }

    public final ScriptEntry getHeldScriptEntry(String id) {
        if (held_entries == null) {
            return null;
        }
        return held_entries.get(CoreUtilities.toLowerCase(id));
    }

    public final ScriptQueue holdScriptEntry(String id, ScriptEntry entry) {
        if (held_entries == null) {
            held_entries = new HashMap<>();
        }
        held_entries.put(CoreUtilities.toLowerCase(id), entry);
        return this;
    }

    public final void setContextSource(ContextSource source) {
        contextSource = source;
    }

    @Override
    public ObjectTag getDefinitionObject(String definition) {
        if (definition == null) {
            return null;
        }
        if (definition.startsWith("__")) {
            ObjectTag value = DenizenCore.implementation.getSpecialDef(definition, this);
            if (value != null) {
                return value;
            }
        }
        return definitions.getDeepObject(definition);
    }

    @Override
    public void addDefinition(String definition, ObjectTag value) {
        if (definition.startsWith("__")) {
            if (DenizenCore.implementation.setSpecialDef(definition, this, value)) {
                return;
            }
        }
        definitions.putDeepObject(definition, value);
    }

    @Override
    public String getDefinition(String definition) {
        if (definition == null) {
            return null;
        }
        return CoreUtilities.stringifyNullPass(getDefinitionObject(definition));
    }

    @Override
    public boolean hasDefinition(String definition) {
        return getDefinitionObject(definition) != null;
    }

    @Override
    public void addDefinition(String definition, String value) {
        addDefinition(definition, new ElementTag(value));
    }

    @Override
    public void removeDefinition(String definition) {
        addDefinition(definition, (ObjectTag) null);
    }

    @Override
    public MapTag getAllDefinitions() {
        return definitions;
    }

    public final ScriptEntry getLastEntryExecuted() {
        return lastEntryExecuted;
    }

    public final void clear() {
        script_entries.clear();
    }

    public void delayUntil(long delayTime) {
        this.delay_time = delayTime;
    }

    public final void generateId(String prefix, int depth) {
        if (prefix.startsWith("FORCE:")) {
            id = prefix.substring("FORCE:".length());
            debugId = id;
            return;
        }
        // DUUIDs v2.1
        int size = QueueWordList.FinalWordList.size();
        Random random = CoreUtilities.getRandom();
        String wordOne = QueueWordList.FinalWordList.get(random.nextInt(size));
        String wordTwo = QueueWordList.FinalWordList.get(random.nextInt(size));
        String colorOne = DenizenCore.implementation.getRandomColor();
        String colorTwo = DenizenCore.implementation.getRandomColor();
        id = prefix + "_" + wordOne + wordTwo;
        debugId = "<LG>" + prefix + "_" + colorOne + wordOne + colorTwo + wordTwo;
        for (int i = 0; i < depth; i++) {
            String wordThree = QueueWordList.FinalWordList.get(random.nextInt(size));
            String colorThree = DenizenCore.implementation.getRandomColor();
            id += wordThree;
            debugId += colorThree + wordThree;
        }
        if (queueExists(id)) {
            generateId(prefix, depth + 1);
        }
    }

    /**
     * Converts any queue type to a timed queue.
     *
     * @param delay how long to delay initially.
     * @return the newly created queue.
     */
    public final TimedQueue forceToTimed(TimedQueue.DelayTracker delay) {
        Runnable r = callback;
        callback = null;
        TimedQueue newQueue = new TimedQueue("FORCE:" + id, 0);
        replacementQueue = newQueue;
        stop();
        newQueue.id = id;
        newQueue.debugId = debugId;
        newQueue.debugOutput = this.debugOutput;
        for (ScriptEntry entry : getEntries()) {
            entry = entry.clone();
            entry.entryData.scriptEntry = entry;
            entry.setInstant(true);
            entry.setSendingQueue(newQueue);
            entry.updateContext();
            newQueue.script_entries.add(entry);
        }
        newQueue.definitions = definitions.duplicate();
        newQueue.setContextSource(contextSource);
        newQueue.determinationTarget = determinationTarget;
        if (held_entries != null) {
            for (Map.Entry<String, ScriptEntry> entry : held_entries.entrySet()) {
                newQueue.holdScriptEntry(entry.getKey(), entry.getValue());
            }
        }
        newQueue.setLastEntryExecuted(getLastEntryExecuted());
        clear();
        newQueue.delay = delay;
        newQueue.startTime = startTime;
        newQueue.startTimeMilli = startTimeMilli;
        newQueue.script = script;
        newQueue.callBack(r);
        newQueue.start(false);
        return newQueue;
    }

    public abstract void onStart();

    public String getName() {
        return "UnidentifiedQueueType";
    }

    public final void queueDebug(String message) {
        Debug.echoDebug(this, "<O>" + message.replace("<QUEUE>", debugId + "<O>"));
    }

    public final void start() {
        start(true);
    }

    public final void start(boolean doBasicConfig) {
        if (is_started) {
            return;
        }
        if (script_entries.isEmpty()) {
            return;
        }
        allQueues.put(id, this);
        is_started = true;
        long delay = delay_time - DenizenCore.serverTimeMillis;
        boolean is_delayed = delay > 0;
        if (doBasicConfig) {
            script = script_entries.get(0).getScript();
            startTime = System.nanoTime();
            startTimeMilli = System.currentTimeMillis();
        }
        String name = getName();
        if (queueNeedsToDebug()) {
            if (is_delayed) {
                queueDebug("Delaying " + name + " '<QUEUE>'" + " for '" + new DurationTag(((double) delay) / 1000f).identify() + "'...");
            }
            else {
                queueDebug("Starting " + name + " '<QUEUE>'" + DenizenCore.implementation.queueHeaderInfo(script_entries.get(0)) + "...");
            }
        }
        if (is_delayed) {
            Schedulable schedulable = new OneTimeSchedulable(this::onStart, ((float) delay) / 1000);
            DenizenCore.schedule(schedulable);

        }
        else {
            onStart();
        }
    }

    /**
     * Immediately runs a list of entries within the script queue.
     * Primarily used as a simple method of instant command injection.
     *
     * @param entries the entries to be run.
     */
    public final void runNow(List<ScriptEntry> entries) {
        ScriptEntry nextup = getQueueSize() > 0 ? getEntry(0) : null;
        injectEntriesAtStart(entries);
        while (getQueueSize() > 0 && getEntry(0) != nextup) {
            getEntry(0).setInstant(true);
            getEntry(0).setFinished(true);
            ScriptEngine.revolveOnceForce(this);
        }
        return;
    }

    /**
     * Adds a runnable to call back when the queue is completed.
     *
     * @param r the Runnable to call back
     */
    public final void callBack(Runnable r) {
        callback = r;
    }

    public final void stop() {
        if (is_stopping) {
            return;
        }
        is_stopping = true;
        allQueues.remove(id);
        if (queueNeedsToDebug()) {
            queueDebug("Completing queue '<QUEUE>' in <A>" + ((System.nanoTime() - startTime) / 1000000) + "<O>ms.");
        }
        if (callback != null) {
            callback.run();
        }
        is_started = false;
        isStopped = true;
    }

    public final void setLastEntryExecuted(ScriptEntry entry) {
        lastEntryExecuted = entry;
    }

    public final ScriptEntry getNext() {
        if (!script_entries.isEmpty()) {
            return script_entries.removeFirst();
        }
        else {
            return null;
        }
    }

    public final void addEntries(List<ScriptEntry> entries) {
        script_entries.addAll(entries);
    }

    public final ListQueue getEntries() {
        return script_entries;
    }

    public final void injectEntriesAtStart(List<ScriptEntry> entries) {
        script_entries.addAllToStart(entries);
    }

    public final boolean removeFirst() {
        if (script_entries.isEmpty()) {
            return false;
        }
        script_entries.removeFirst();
        return true;
    }

    public final ScriptEntry getEntry(int position) {
        if (script_entries.size() < position) {
            return null;
        }
        return script_entries.get(position);
    }

    public final void injectEntryAtStart(ScriptEntry entry) {
        script_entries.injectAtStart(entry);
    }

    public final int getQueueSize() {
        return script_entries.size();
    }

    public final boolean queueNeedsToDebug() {
        return DenizenCore.implementation.shouldDebug(this);
    }

    @Override
    public boolean shouldDebug() {
        return (lastEntryExecuted != null ? lastEntryExecuted.shouldDebug() : script_entries.get(0).shouldDebug());
    }

    @Override
    public String toString() {
        return id;
    }
}
