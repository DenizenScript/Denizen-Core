package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.scripts.queues.core.Delayable;
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
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * ScriptQueues hold/control ScriptEntries while being sent
 * to the CommandExecuter
 */

public abstract class ScriptQueue implements Debuggable, ObjectTag, ObjectTag.ObjectAttributable, DefinitionProvider, Adjustable {
    private static final Map<Class<? extends ScriptQueue>, String> classNameCache = new HashMap<>();

    // <--[language]
    // @name ScriptQueue
    // @group Object System
    // @description
    // A ScriptQueue is a single currently running set of script commands.
    // This is not to be confused with a script path, which is a single set of script commands that can be run.
    // There can be one, multiple, or zero queues running at any time for any given path.
    //
    // For format info, see <@link language q@>
    //
    // -->

    // <--[language]
    // @name q@
    // @group Object Fetcher System
    // @description
    // q@ refers to the 'object identifier' of a ScriptQueue. The 'q@' is notation for Denizen's Object
    // Fetcher. The constructor for a ScriptQueue is the queue ID.
    //
    // For general info, see <@link language ScriptQueue>
    //
    // -->

    protected static long total_queues = 0;


    /**
     * Returns the number of queues created in the current instance
     * as well as the number of currently active queues.
     *
     * @return stats
     */
    public static String _getStats() {
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
                + _queues.size() + ",\n" + stats.toString();
    }


    /**
     * Gets an existing queue. Cast to the correct QueueType to
     * access further methods.
     *
     * @param id the id of the queue
     * @return a ScriptQueue instance, or null
     */
    public static ScriptQueue _getExistingQueue(String id) {
        if (!_queueExists(id)) {
            return null;
        }
        else {
            return _queues.get(id);
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
        return _queues.containsKey(id) ? getNextId(prefix) : id;
    }


    /**
     * Checks the type of an existing queue with the type given.
     *
     * @param queue id of the queue
     * @param type  class of the queue type
     * @return true if they match, false if the queue
     * doesn't exist or does not match
     */
    public static boolean _matchesType(String queue, Class type) {
        return (_queueExists(queue)) && _queues.get(queue).getClass() == type;
    }


    // Contains all currently active queues, keyed by a String id.
    protected static Map<String, ScriptQueue> _queues =
            new ConcurrentHashMap<>(8, 0.9f, 1);


    /**
     * Returns a collection of all active queues.
     *
     * @return a collection of ScriptQueues
     */
    public static Collection<ScriptQueue> _getQueues() {
        return _queues.values();
    }


    /**
     * Checks if a queue exists with the given id.
     *
     * @param id the String ID of the queue to check.
     * @return true if it exists.
     */
    public static boolean _queueExists(String id) {
        return _queues.containsKey(id);
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
        if (_queueExists(id)) {
            generateId(prefix);
            return;
        }
        String colorOne = DenizenCore.getImplementation().getRandomColor();
        String colorTwo = DenizenCore.getImplementation().getRandomColor();
        String colorThree = DenizenCore.getImplementation().getRandomColor();
        debugId = prefix + "_" + colorOne + wordOne + colorTwo + wordTwo + colorThree + wordThree;
    }


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
        newQueue.id = id;
        newQueue.debugId = debugId;
        newQueue.run_async = this.run_async;
        newQueue.debugOutput = this.debugOutput;
        for (ScriptEntry entry : getEntries()) {
            entry.setInstant(true);
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


    protected boolean is_started;

    private Class<? extends ScriptQueue> cachedClass;

    public long startTime = 0;

    private long startTimeMilli = 0;

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

        // Save the instance to the _queues static map
        _queues.put(id, this);

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


    protected boolean is_stopping = false;


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
                if (_queues.get(id) == this) {
                    _queues.remove(id);
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
            if (_queues.get(id) == this) {
                _queues.remove(id);
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


    // dOBJECT
    //

    public static ScriptQueue valueOf(String string) {
        return valueOf(string, null);
    }

    /**
     * Gets a Queue Object from a string form of q@queue_name.
     *
     * @param string the string or dScript argument String
     * @return a ScriptQueue, or null if incorrectly formatted
     */
    @Fetchable("q")
    public static ScriptQueue valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }

        if (string.startsWith("q@") && string.length() > 2) {
            string = string.substring(2);
        }

        if (_queueExists(string)) {
            return _getExistingQueue(string);
        }

        return null;
    }


    public static boolean matches(String string) {
        // Starts with q@? Assume match.
        if (CoreUtilities.toLowerCase(string).startsWith("q@")) {
            return true;
        }
        else {
            return false;
        }
    }

    String prefix = "Queue";


    @Override
    public String getPrefix() {
        return prefix;
    }


    @Override
    public ScriptQueue setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String getObjectType() {
        return "queue";
    }

    @Override
    public String identify() {
        return "q@" + id;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    public static void registerTags() {

        // <--[tag]
        // @attribute <q@queue.id>
        // @returns ElementTag
        // @description
        // Returns the id of the queue.
        // -->
        registerTag("id", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((ScriptQueue) object).id).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.size>
        // @returns ElementTag
        // @description
        // Returns the number of script entries in the queue.
        // -->
        registerTag("size", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                return new ElementTag(((ScriptQueue) object).script_entries.size()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.start_time>
        // @returns DurationTag
        // @description
        // Returns the time this queue started as a duration.
        // -->
        registerTag("start_time", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                return new DurationTag(((ScriptQueue) object).startTimeMilli / 50).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.time_ran>
        // @returns DurationTag
        // @description
        // Returns the time this queue has ran for (the length of time between now and when the queue started) as a duration.
        // -->
        registerTag("time_ran", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                long timeNano = System.nanoTime() - ((ScriptQueue) object).startTime;
                return new DurationTag(timeNano / (1000000 * 1000.0)).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.state>
        // @returns ElementTag
        // @description
        // Returns 'stopping', 'running', 'paused', or 'unknown'.
        // -->
        registerTag("state", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                String state;
                if ((object instanceof Delayable) && ((Delayable) object).isPaused()) {
                    state = "paused";
                }
                else if (((ScriptQueue) object).is_started) {
                    state = "running";
                }
                else if (((ScriptQueue) object).is_stopping) {
                    state = "stopping";
                }
                else {
                    state = "unknown";
                }
                return new ElementTag(state).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.script>
        // @returns ScriptTag
        // @description
        // Returns the script that started this queue.
        // -->
        registerTag("script", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                if (((ScriptQueue) object).script == null) {
                    return null;
                }
                return ((ScriptQueue) object).script.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.commands>
        // @returns ListTag
        // @description
        // Returns a list of commands waiting in the queue.
        // -->
        registerTag("commands", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                ListTag commands = new ListTag();
                for (ScriptEntry entry : ((ScriptQueue) object).script_entries) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(entry.getCommandName()).append(" ");
                    for (String arg : entry.getOriginalArguments()) {
                        sb.append(arg).append(" ");
                    }
                    commands.add(sb.substring(0, sb.length() - 1));
                }
                return commands.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.definitions>
        // @returns ListTag
        // @description
        // Returns the names of all definitions that were passed to the current queue.
        // -->
        registerTag("definitions", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                return new ListTag(((ScriptQueue) object).getAllDefinitions().keySet()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.definition[<definition>]>
        // @returns ObjectTag
        // @description
        // Returns the value of the specified definition.
        // Returns null if the queue lacks the definition.
        // -->
        registerTag("definition", new TagRunnable.ObjectForm() {
            @Override
            public ObjectTag run(Attribute attribute, ObjectTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag q@queue.definition[...] must have a value.");
                    return null;
                }
                return CoreUtilities.autoAttrib(((ScriptQueue) object).getDefinitionObject(attribute.getContext(1)), attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.determination>
        // @returns ListTag
        // @description
        // Returns the values that have been determined via <@link command Determine>
        // for this queue, or null if there is none.
        // -->
        registerTag("determination", new TagRunnable() {
            @Override
            public String run(Attribute attribute, ObjectTag object) {
                if (((ScriptQueue) object).determinations == null) {
                    return null;
                }
                else {
                    return ((ScriptQueue) object).determinations.getAttribute(attribute.fulfill(1));
                }
            }
        });

    }

    public static HashMap<String, TagRunnable> registeredTags = new HashMap<>();

    public static void registerTag(String name, TagRunnable runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registeredTags.put(name, runnable);
    }

    public static HashMap<String, TagRunnable.ObjectForm> registeredObjectTags = new HashMap<>();

    public static void registerTag(String name, TagRunnable.ObjectForm runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registeredObjectTags.put(name, runnable);
    }

    @Override
    public <T extends ObjectTag> T asObjectType(Class<T> type, TagContext context) {
        return null;
    }

    @Override
    public String getAttribute(Attribute attribute) {
        return CoreUtilities.stringifyNullPass(getObjectAttribute(attribute));
    }

    @Override
    public Class<? extends ObjectTag> getdObjectClass() {
        return ScriptQueue.class;
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        if (attribute == null) {
            return null;
        }

        if (attribute.isComplete()) {
            return this;
        }

        // TODO: Scrap getAttribute, make this functionality a core system
        String attrLow = CoreUtilities.toLowerCase(attribute.getAttributeWithoutContext(1));
        TagRunnable.ObjectForm otr = registeredObjectTags.get(attrLow);
        if (otr != null) {
            if (!otr.name.equals(attrLow)) {
                Debug.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + otr.name + "': '" + attrLow + "'.");
            }
            return otr.run(attribute, this);
        }

        TagRunnable tr = registeredTags.get(attrLow);
        if (tr != null) {
            if (!tr.name.equals(attrLow)) {
                Debug.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + tr.name + "': '" + attrLow + "'.");
            }
            return new ElementTag(tr.run(attribute, this));
        }

        ObjectTag returned = CoreUtilities.autoPropertyTagObject(this, attribute);
        if (returned != null) {
            return returned;
        }

        return new ElementTag(identify()).getObjectAttribute(attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("ScriptQueues can not hold properties.");
    }

    @Override
    public void adjust(Mechanism mechanism) {
        CoreUtilities.autoPropertyMechanism(this, mechanism);
    }
}
