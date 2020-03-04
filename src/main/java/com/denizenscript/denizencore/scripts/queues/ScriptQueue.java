package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
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

public abstract class ScriptQueue implements Debuggable, DefinitionProvider {

    protected static long total_queues = 0;

    public static String getStats() {
        StringBuilder stats = new StringBuilder();
        for (ScriptEvent event : ScriptEvent.events) {
            if (event.stats.fires > 0) {
                stats.append("Event '" + event.getName() + "' ran "
                        + event.stats.fires + " times (" + event.stats.scriptFires + " script fires)"
                        + ", totalling " + ((float) event.stats.nanoTimes / 1000000f) + "ms, averaging "
                        + ((float) event.stats.nanoTimes / 1000000f / (float) event.stats.fires) + "ms per event or " +
                        +((float) event.stats.nanoTimes / 1000000f / (float) event.stats.scriptFires) + "ms per script.\n");
            }
        }
        return "Total number of queues created: "
                + total_queues
                + ", currently active queues: "
                + allQueues.size() + ",\n" + stats.toString();
    }

    public static ScriptQueue getExistingQueue(String id) {
        if (!queueExists(id)) {
            return null;
        }
        else {
            return allQueues.get(id);
        }
    }

    public static String getNextId(String prefix) {
        // DUUIDs v2.1
        int size = QueueWordList.FinalWordList.size();
        String id = prefix + "_"
                + QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size))
                + QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size))
                + QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size));
        // DUUIDs v3.1
        /*
        String id = prefix.replace(' ', '_')
                + "_"
                + randomEntry(QueueWordList.Pronouns)
                + randomEntry(QueueWordList.Verbs)
                + randomEntry(QueueWordList.Modifiers)
                + randomEntry(QueueWordList.Adjectives)
                + randomEntry(QueueWordList.Nouns);*/
        return allQueues.containsKey(id) ? getNextId(prefix) : id;
    }

    protected static LinkedHashMap<String, ScriptQueue> allQueues = new LinkedHashMap<>();

    public static Collection<ScriptQueue> getQueues() {
        return allQueues.values();
    }

    public static boolean queueExists(String id) {
        return allQueues.containsKey(id);
    }

    /////////////////////
    // Public instance fields
    /////////////////////

    public String id;

    public String debugId;

    public boolean was_cleared = false;

    /**
     * Optional secondary debug output method.
     */
    public Consumer<String> debugOutput = null;

    /////////////////////
    // Private instance fields and constructors
    /////////////////////

    public final List<ScriptEntry> script_entries = new ArrayList<>(4);

    private ScriptEntry lastEntryExecuted = null;

    /**
     If this number is larger than DenizenCore.serverTimeMillis, the queue will delay execution of the next ScriptEntry.
     */
    private long delay_time = 0;

    private final HashMap<String, ObjectTag> definitions = new HashMap<>();

    public ListTag determinations = null;

    private final HashMap<String, ScriptEntry> held_entries = new HashMap<>();

    public ScriptTag script;

    protected ScriptQueue(String id) {
        this.id = id;
        generateId(id);
        total_queues++;
    }

    /////////////////////
    // Public instance setters and getters
    /////////////////////

    public ScriptEntry getHeldScriptEntry(String id) {
        return held_entries.get(CoreUtilities.toLowerCase(id));
    }

    public ScriptQueue holdScriptEntry(String id, ScriptEntry entry) {
        held_entries.put(CoreUtilities.toLowerCase(id), entry);
        return this;
    }

    public ObjectTag getContext(String id) {
        id = CoreUtilities.toLowerCase(id);
        if (contextSource == null) {
            return null;
        }
        return contextSource.getContext(id);
    }

    public ContextSource contextSource = null;

    public DeterminationTarget determinationTarget = null;

    public void setContextSource(ContextSource source) {
        contextSource = source;
    }

    @Override
    public ObjectTag getDefinitionObject(String definition) {
        if (definition == null) {
            return null;
        }
        return definitions.get(CoreUtilities.toLowerCase(definition));
    }

    @Override
    public String getDefinition(String definition) {
        if (definition == null) {
            return null;
        }
        return CoreUtilities.stringifyNullPass(definitions.get(CoreUtilities.toLowerCase(definition)));
    }

    @Override
    public boolean hasDefinition(String definition) {
        return definitions.containsKey(CoreUtilities.toLowerCase(definition));
    }

    public void addDefinition(String definition, ObjectTag value) {
        definitions.put(CoreUtilities.toLowerCase(definition), value);
    }

    @Override
    public void addDefinition(String definition, String value) {
        definitions.put(CoreUtilities.toLowerCase(definition), new ElementTag(value));
    }

    @Override
    public void removeDefinition(String definition) {
        definitions.remove(CoreUtilities.toLowerCase(definition));
    }

    @Override
    public Map<String, ObjectTag> getAllDefinitions() {
        return definitions;
    }

    public ScriptEntry getLastEntryExecuted() {
        return lastEntryExecuted;
    }

    public void clear() {
        was_cleared = true;
        script_entries.clear();
    }

    public void delayUntil(long delayTime) {
        this.delay_time = delayTime;
    }

    ///////////////////
    // Public 'functional' methods
    //////////////////

    public void generateId(String prefix) {
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
        String wordThree = QueueWordList.FinalWordList.get(random.nextInt(size));
        id = prefix + "_" + wordOne + wordTwo + wordThree;
        if (queueExists(id)) {
            generateId(prefix);
            return;
        }
        String colorOne = DenizenCore.getImplementation().getRandomColor();
        String colorTwo = DenizenCore.getImplementation().getRandomColor();
        String colorThree = DenizenCore.getImplementation().getRandomColor();
        debugId = prefix + "_" + colorOne + wordOne + colorTwo + wordTwo + colorThree + wordThree;
    }

    public ScriptQueue replacementQueue = null;

    /**
     * Converts any queue type to a timed queue.
     *
     * @param delay how long to delay initially.
     * @return the newly created queue.
     */
    public TimedQueue forceToTimed(DurationTag delay) {
        Runnable r = callback;
        callback = null;
        TimedQueue newQueue = new TimedQueue("FORCE:" + id, 0);
        replacementQueue = newQueue;
        stop();
        newQueue.id = id;
        newQueue.debugId = debugId;
        newQueue.debugOutput = this.debugOutput;
        for (ScriptEntry entry : getEntries()) {
            entry.setInstant(true);
            entry.setSendingQueue(newQueue);
            entry.updateContext();
        }
        newQueue.addEntries(getEntries());
        for (Map.Entry<String, ObjectTag> def : getAllDefinitions().entrySet()) {
            newQueue.addDefinition(def.getKey(), def.getValue());
        }
        newQueue.setContextSource(contextSource);
        newQueue.determinationTarget = determinationTarget;
        for (Map.Entry<String, ScriptEntry> entry : held_entries.entrySet()) {
            newQueue.holdScriptEntry(entry.getKey(), entry.getValue());
        }
        newQueue.setLastEntryExecuted(getLastEntryExecuted());
        clear();
        if (delay != null) {
            newQueue.delayFor(delay);
        }
        newQueue.startTime = startTime;
        newQueue.startTimeMilli = startTimeMilli;
        newQueue.script = script;
        newQueue.callBack(r);
        newQueue.start(false);
        return newQueue;
    }

    protected abstract void onStart();

    public boolean is_started;

    public long startTime = 0;

    public long startTimeMilli = 0;

    public String getName() {
        return "UnidentifiedQueueType";
    }

    public void runMeNow() {
        onStart();
    }

    public void queueDebug(String message) {
        Debug.echoDebug(this, "<O>" + message.replace("<QUEUE>", debugId + "<O>"));
    }

    public void start() {
        start(true);
    }

    public void start(boolean doBasicConfig) {
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
                queueDebug("Starting " + name + " '<QUEUE>'" + DenizenCore.getImplementation().queueHeaderInfo(script_entries.get(0)) + "...");
            }
        }
        if (is_delayed) {
            Schedulable schedulable = new OneTimeSchedulable(new Runnable() {
                @Override
                public void run() {
                    runMeNow();
                }
            }, ((float) delay) / 1000);
            DenizenCore.schedule(schedulable);

        }
        else {
            runMeNow();
        }
    }

    /**
     * Immediately runs a list of entries within the script queue.
     * Primarily used as a simple method of instant command injection.
     *
     * @param entries the entries to be run.
     */
    public void runNow(List<ScriptEntry> entries) {
        ScriptEntry nextup = getQueueSize() > 0 ? getEntry(0) : null;
        injectEntries(entries, 0);
        while (getQueueSize() > 0 && getEntry(0) != nextup && !was_cleared) {
            getEntry(0).setInstant(true);
            getEntry(0).setFinished(true);
            DenizenCore.getScriptEngine().revolveOnceForce(this);
        }
        return;
    }

    private Runnable callback = null;

    /**
     * Adds a runnable to call back when the queue is completed.
     *
     * @param r the Runnable to call back
     */
    public void callBack(Runnable r) {
        callback = r;
    }

    protected abstract void onStop();

    public boolean is_stopping = false;

    public boolean isStopped = false;

    public void stop() {
        if (is_stopping) {
            return;
        }
        is_stopping = true;
        allQueues.remove(id);
        if (queueNeedsToDebug()) {
            queueDebug("Completing queue '<QUEUE>' in " + ((System.nanoTime() - startTime) / 1000000) + "ms.");
        }
        if (callback != null) {
            callback.run();
        }
        is_started = false;
        onStop();
        isStopped = true;
    }

    ////////////////////
    // Internal methods and fields
    ////////////////////

    public void setLastEntryExecuted(ScriptEntry entry) {
        lastEntryExecuted = entry;
    }

    protected abstract boolean shouldRevolve();

    protected void revolve() {
        if (script_entries.isEmpty()) {
            stop();
            return;
        }
        if (!shouldRevolve()) {
            return;
        }
        DenizenCore.getScriptEngine().revolve(this);
        if (script_entries.isEmpty()) {
            stop();
        }
    }

    public ScriptEntry getNext() {
        if (!script_entries.isEmpty()) {
            return script_entries.remove(0);
        }
        else {
            return null;
        }
    }

    public ScriptQueue addEntries(List<ScriptEntry> entries) {
        script_entries.addAll(entries);
        return this;
    }

    public List<ScriptEntry> getEntries() {
        return script_entries;
    }

    public ScriptQueue injectEntries(List<ScriptEntry> entries, int position) {
        if (position > script_entries.size() || position < 0) {
            position = 1;
        }
        if (script_entries.isEmpty()) {
            position = 0;
        }
        script_entries.addAll(position, entries);
        return this;
    }

    public boolean removeEntry(int position) {
        if (script_entries.size() < position) {
            return false;
        }
        script_entries.remove(position);
        return true;
    }

    public ScriptEntry getEntry(int position) {
        if (script_entries.size() < position) {
            return null;
        }
        return script_entries.get(position);
    }

    public ScriptQueue injectEntry(ScriptEntry entry, int position) {
        if (position > script_entries.size() || position < 0) {
            position = 1;
        }
        if (script_entries.isEmpty()) {
            position = 0;
        }
        script_entries.add(position, entry);
        return this;
    }

    public int getQueueSize() {
        return script_entries.size();
    }

    public boolean queueNeedsToDebug() {
        return DenizenCore.getImplementation().shouldDebug(this);
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
