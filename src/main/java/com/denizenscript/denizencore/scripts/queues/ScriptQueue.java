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
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.Schedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.scripts.ScriptEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ScriptQueues hold/control ScriptEntries while being sent
 * to the CommandExecuter
 */

public abstract class ScriptQueue implements Debuggable, DefinitionProvider {
    private static final Map<Class<? extends ScriptQueue>, String> classNameCache = new HashMap<>();

    protected static long total_queues = 0;

    /**
     * Returns the number of queues created in the current instance
     * as well as the number of currently active queues.
     *
     * @return stats
     */
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

    /**
     * Gets an existing queue. Cast to the correct QueueType to
     * access further methods.
     *
     * @param id the id of the queue
     * @return a ScriptQueue instance, or null
     */
    public static ScriptQueue getExistingQueue(String id) {
        if (!queueExists(id)) {
            return null;
        }
        else {
            return allQueues.get(id);
        }
    }

    /*
    private static String randomEntry(String[] strings) {
        return strings[CoreUtilities.getRandom().nextInt(strings.length)];
    }*/

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

    // Contains all currently active queues, keyed by a String id.
    protected static Map<String, ScriptQueue> allQueues =
            new ConcurrentHashMap<>(8, 0.9f, 1);

    /**
     * Returns a collection of all active queues.
     *
     * @return a collection of ScriptQueues
     */
    public static Collection<ScriptQueue> getQueues() {
        return allQueues.values();
    }

    /**
     * Checks if a queue exists with the given id.
     *
     * @param id the String ID of the queue to check.
     * @return true if it exists.
     */
    public static boolean queueExists(String id) {
        return allQueues.containsKey(id);
    }

    /////////////////////
    // Public instance fields
    /////////////////////

    public String id;

    public String debugId;

    public boolean was_cleared = false;

    public boolean run_async = false;

    /**
     * Optional secondary debug output method.
     */
    public Consumer<String> debugOutput = null;

    /////////////////////
    // Private instance fields and constructors
    /////////////////////

    public final List<ScriptEntry> script_entries = new ArrayList<>();

    private ScriptEntry lastEntryExecuted = null;

    /**
     If this number is larger than
     DenizenCore.serverTimeMillis, the queue will
     delay execution of the next ScriptEntry
     */
    private long delay_time = 0;

    private final HashMap<String, ObjectTag> definitions = new HashMap<>();

    public ListTag determinations = null;

    private final HashMap<String, ScriptEntry> held_entries = new HashMap<>();

    public ScriptTag script;

    /**
     * Creates a ScriptQueue instance. Users of
     * the API should instead use the static members
     * of classes that extend ScriptQueue.
     *
     * @param id the name of the ScriptQueue
     */
    protected ScriptQueue(String id) {
        // Remember the 'id'
        this.id = id;
        generateId(id);
        // Increment the stats
        total_queues++;
    }

    protected ScriptQueue(String id, boolean async) {
        this(id);
        this.run_async = async;
    }

    /////////////////////
    // Public instance setters and getters
    /////////////////////

    /**
     * Gets a boolean indicating whether the queue
     * was cleared.
     *
     * @return whether the queue has been cleared.
     */
    public boolean getWasCleared() {
        return was_cleared;
    }

    /**
     * Gets a held script entry. Held script entries might
     * contains some script entry context that might need
     * to be fetched!
     */
    public ScriptEntry getHeldScriptEntry(String id) {
        return held_entries.get(CoreUtilities.toLowerCase(id));
    }

    /**
     * Provides a way to hold a script entry for retrieval later in the
     * script. Keyed by an id, which is turned to lowercase making
     * it case insensitive.
     *
     * @param id    intended name of the entry
     * @param entry the ScriptEntry instance
     * @return the ScriptQueue, just in case you need to do more with it
     */

    public ScriptQueue holdScriptEntry(String id, ScriptEntry entry) {
        // to lowercase to avoid case sensitivity.
        held_entries.put(CoreUtilities.toLowerCase(id), entry);

        return this;
    }

    /**
     * Gets a context from the queue. Script writers can
     * use the <c.context_name> or <context.context_name> tags
     * to fetch this data.
     *
     * @param id The name of the definitions
     * @return The value of the definitions, or null
     */
    public ObjectTag getContext(String id) {
        id = CoreUtilities.toLowerCase(id);
        if (contextSource == null) {
            return null;
        }
        ObjectTag obj = cachedContext.get(id);
        if (obj != null) {
            return obj;
        }
        obj = contextSource.getContext(id);
        if (obj != null && contextSource.getShouldCache()) {
            cachedContext.put(id, obj);
        }
        return obj;
    }

    public ContextSource contextSource = null;

    public HashMap<String, ObjectTag> cachedContext;

    public void setContextSource(ContextSource source) {
        contextSource = source;
        cachedContext = new HashMap<>();
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

    /**
     * Checks for a piece of definitions.
     *
     * @param definition The name of the definitions
     * @return true if the definition exists.
     */
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

    /**
     * Removes an existing definitions from the queue. This
     * can be done with dScript as well by using the
     * 'define' command, with :! as the value using the definition
     * name as a prefix.
     *
     * @param definition the name of the definitions
     */
    @Override
    public void removeDefinition(String definition) {
        definitions.remove(CoreUtilities.toLowerCase(definition));
    }

    /**
     * Returns a Map of all the current definitions
     * stored in the queue, keyed by 'definition id'
     *
     * @return all current definitions, empty if none.
     */
    @Override
    public Map<String, ObjectTag> getAllDefinitions() {
        return definitions;
    }

    /**
     * The last entry that was executed. Note: any
     * replaceable tags/etc. are already replaced
     * in this ScriptEntry.
     *
     * @return the last entry executed
     */
    public ScriptEntry getLastEntryExecuted() {
        return lastEntryExecuted;
    }

    /**
     * Clears the script queue.
     * <p/>
     * Use the 'queue clear' command in dScript to
     * access this method.
     */
    public void clear() {
        was_cleared = true;
        script_entries.clear();
    }

    /**
     * Will delay the start of the queue until Java's
     * System.currentTimeMillis() is less than the
     * delayTime.
     *
     * @param delayTime the time to start the queue, in
     *                  System.currentTimeMillis() format.
     */
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
        String wordOne = QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size));
        String wordTwo = QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size));
        String wordThree = QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size));
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
        stop();
        TimedQueue newQueue = new TimedQueue(id, 0);
        replacementQueue = newQueue;
        newQueue.id = id;
        newQueue.debugId = debugId;
        newQueue.run_async = this.run_async;
        newQueue.debugOutput = this.debugOutput;
        for (ScriptEntry entry : getEntries()) {
            entry.setInstant(true);
            entry.setSendingQueue(newQueue);
        }
        newQueue.addEntries(getEntries());
        for (Map.Entry<String, ObjectTag> def : getAllDefinitions().entrySet()) {
            newQueue.addDefinition(def.getKey(), def.getValue());
        }
        newQueue.setContextSource(contextSource);
        newQueue.cachedContext = cachedContext;
        for (Map.Entry<String, ScriptEntry> entry : held_entries.entrySet()) {
            newQueue.holdScriptEntry(entry.getKey(), entry.getValue());
        }
        newQueue.setLastEntryExecuted(getLastEntryExecuted());
        clear();
        if (delay != null) {
            newQueue.delayFor(delay);
        }
        newQueue.script = script;
        newQueue.callBack(r);
        newQueue.start();
        return newQueue;
    }

    /**
     * Called when the script queue is started.
     */
    protected abstract void onStart();

    public boolean is_started;

    private Class<? extends ScriptQueue> cachedClass;

    public long startTime = 0;

    public long startTimeMilli = 0;

    public String getName() {
        Class<? extends ScriptQueue> clazz = this.cachedClass == null ? this.cachedClass = getClass() : this.cachedClass;
        String name = classNameCache.get(clazz);
        if (name == null) {
            classNameCache.put(clazz, name = clazz.getSimpleName());
        }
        return name;
    }

    public void runMeNow() {
        startTime = System.nanoTime();
        startTimeMilli = System.currentTimeMillis();
        onStart(); /* Start the engine */
    }

    public void queueDebug(String message) {
        Debug.echoDebug(this, "<O>" + message.replace("<QUEUE>", debugId + "<O>"));
    }

    /**
     * Starts the script queue.
     */
    public void start() {
        if (is_started) {
            return;
        }

        if (script_entries.isEmpty()) {
            // Nothing to execute
            return;
        }

        // Save the instance to the allQueues static map
        allQueues.put(id, this);

        // Set as started, and check for a valid delay_time.
        is_started = true;
        long delay = delay_time - DenizenCore.serverTimeMillis;
        boolean is_delayed = delay > 0;

        // Record what script generated the first entry in the queue
        script = script_entries.get(0).getScript();

        // Debug info
        String name = getName();
        if (is_delayed) {
            queueDebug("Delaying " + name + " '<QUEUE>'" + " for '"
                    + new DurationTag(((double) delay) / 1000f).identify() + "'...");
        }
        else {
            queueDebug("Starting " + name + " '<QUEUE>'" + DenizenCore.getImplementation().queueHeaderInfo(script_entries.get(0)) + "...");
        }

        // If it's delayed, schedule it for later
        if (is_delayed) {
            Schedulable schedulable = new OneTimeSchedulable(new Runnable() {
                @Override
                public void run() {
                    runMeNow();
                }
            }, ((float) delay) / 1000);
            if (run_async) {
                schedulable = new AsyncSchedulable(schedulable);
            }
            DenizenCore.schedule(schedulable);

        }
        else {
            // If it's not, start the engine now!
            if (!run_async) {
                runMeNow();
            }
            else {
                AsyncSchedulable.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        runMeNow();
                    }
                });
            }
        }
    }

    /**
     * Immediately runs a list of entries within the script queue.
     * Primarily used as a simple method of instant command injection.
     *
     * @param entries the entries to be run.
     */
    public String runNow(List<ScriptEntry> entries, String type) {
        //Note which entry comes next in the existing queue
        ScriptEntry nextup = getQueueSize() > 0 ? getEntry(0) : null;
        // Inject the entries at the start
        injectEntries(entries, 0);
        // Loop through until the queue is emptied or the entry noted above is reached
        while (getQueueSize() > 0 && getEntry(0) != nextup && !was_cleared) {
            if (breakMe != null) {
                removeEntry(0);
            }
            else {
                getEntry(0).setInstant(true);
                // Don't let the system try to 'hold' this entry.
                getEntry(0).setFinished(true);
                // Execute the ScriptEntry properly through the Script Engine.
                DenizenCore.getScriptEngine().revolveOnceForce(this);
            }
        }
        if (breakMe != null && breakMe.startsWith(type)) {
            String origBreakMe = breakMe;
            breakMe = null;
            return origBreakMe;
        }
        return null;
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

    private String breakMe = null;

    public void breakLoop(String toBreak) {
        breakMe = toBreak;
    }

    public String isLoopBroken() {
        return breakMe;
    }

    /**
     * Stops the script_queue and breaks it down.
     */
    protected abstract void onStop();

    public boolean is_stopping = false;

    public void stop() {

        // If this is the first time this has been called, check the
        // ScriptContainer event 'on queue completes' which may have
        // a few more script entries to run.
        if (!is_stopping) {
            is_stopping = true;

            // Get the entries
            List<ScriptEntry> entries =
                    (lastEntryExecuted != null && lastEntryExecuted.getScript() != null ?
                            lastEntryExecuted.getScript().getContainer()
                                    .getEntries(lastEntryExecuted.entryData.clone(), "on queue completes") : null);
            // Add the 'finishing' entries back into the queue (if not empty)
            if (entries != null && !entries.isEmpty()) {
                script_entries.addAll(entries);
                queueDebug("Finishing up queue '<QUEUE>'...");
            }
            else /* if empty, just stop the queue like normal */ {
                if (allQueues.get(id) == this) {
                    allQueues.remove(id);
                }
                queueDebug("Completing queue '<QUEUE>' in " + ((System.nanoTime() - startTime) / 1000000) + "ms.");
                if (callback != null) {
                    callback.run();
                }
                is_started = false;
                onStop();
            }
        }

        // Else, just complete the queue.
        // 1) Remove the id from active queue list
        // 2) Cancel the corresponding task_id
        else {
            if (allQueues.get(id) == this) {
                allQueues.remove(id);
                queueDebug("Re-completing queue '<QUEUE>' in " + ((System.nanoTime() - startTime) / 1000000) + "ms.");
                if (callback != null) {
                    callback.run();
                }
                is_started = false;
                onStop();
            }
        }
    }

    ////////////////////
    // Internal methods and fields
    ////////////////////

    /**
     * Sets the last entry executed by the ScriptEngine.
     *
     * @param entry the ScriptEntry last executed.
     */
    public void setLastEntryExecuted(ScriptEntry entry) {
        lastEntryExecuted = entry;
    }

    protected abstract boolean shouldRevolve();

    protected void revolve() {
        // If entries queued up are empty, deconstruct the queue.
        if (script_entries.isEmpty()) {
            stop();
            return;
        }

        if (!shouldRevolve()) {
            return;
        }

        // Criteria met for a successful 'revolution' of this queue,
        // so send the next script entry to the ScriptEngine.
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

    public boolean hasInjectedItems = false;

    public ScriptQueue injectEntries(List<ScriptEntry> entries, int position) {
        if (position > script_entries.size() || position < 0) {
            position = 1;
        }
        if (script_entries.size() == 0) {
            position = 0;
        }
        script_entries.addAll(position, entries);
        hasInjectedItems = true;
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
        if (script_entries.size() == 0) {
            position = 0;
        }
        script_entries.add(position, entry);
        hasInjectedItems = true;
        return this;
    }

    public int getQueueSize() {
        return script_entries.size();
    }

    // DEBUGGABLE
    //

    @Override
    public boolean shouldDebug() {
        return (lastEntryExecuted != null ? lastEntryExecuted.shouldDebug()
                : script_entries.get(0).shouldDebug());
    }

    @Override
    public boolean shouldFilter(String criteria) throws Exception {
        return (lastEntryExecuted != null ? lastEntryExecuted.getScript().getName().equalsIgnoreCase(criteria.replace("s@", ""))
                : script_entries.get(0).getScript().getName().equalsIgnoreCase(criteria.replace("s@", "")));
    }

    @Override
    public String toString() {
        return id;
    }
}
