package net.aufdemrand.denizencore.scripts.queues;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.interfaces.ContextSource;
import net.aufdemrand.denizencore.objects.*;
import net.aufdemrand.denizencore.objects.properties.Property;
import net.aufdemrand.denizencore.objects.properties.PropertyParser;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.core.DetermineCommand;
import net.aufdemrand.denizencore.scripts.queues.core.Delayable;
import net.aufdemrand.denizencore.scripts.queues.core.TimedQueue;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.DefinitionProvider;
import net.aufdemrand.denizencore.utilities.QueueWordList;
import net.aufdemrand.denizencore.utilities.debugging.Debuggable;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.scheduling.AsyncSchedulable;
import net.aufdemrand.denizencore.utilities.scheduling.OneTimeSchedulable;
import net.aufdemrand.denizencore.utilities.scheduling.Schedulable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScriptQueues hold/control ScriptEntries while being sent
 * to the CommandExecuter
 */

public abstract class ScriptQueue implements Debuggable, dObject, dObject.ObjectAttributable, DefinitionProvider {
    private static final Map<Class<? extends ScriptQueue>, String> classNameCache = new HashMap<Class<? extends ScriptQueue>, String>();

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
            stats.append("Event '" + event.getName() + "' ran "
                    + event.fires + " times (" + event.scriptFires + " script fires)"
                    + ", totalling " + ((float) event.nanoTimes / 1000000f) + "ms, averaging "
                    + ((float) event.nanoTimes / 1000000f / (float) event.fires) + "ms per event or " +
                    +((float) event.nanoTimes / 1000000f / (float) event.scriptFires) + "ms per script.\n");
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

    /**
     * Gets a random id for use in creating a 'nameless' queue.
     *
     * @param prefix the name of the script running the new queue.
     * @return String value of a random id
     */
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
            new ConcurrentHashMap<String, ScriptQueue>(8, 0.9f, 1);


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


    // Name of the queue -- this identifies
    // the ScriptQueue when using _getQueue()
    public String id;

    // Whether the queue was cleared
    public boolean was_cleared = false;

    // Whether the queue should run asynchronously
    public boolean run_async = false;


    /////////////////////
    // Private instance fields and constructors
    /////////////////////


    // List of ScriptEntries in the queue
    public final List<ScriptEntry> script_entries = new ArrayList<ScriptEntry>();


    // The last script entry that was executed
    // in this queue.
    private ScriptEntry lastEntryExecuted = null;


    // If this number is larger than Java's
    // getCurrentTimeMillis(), the queue will
    // delay execution of the next ScriptEntry
    private long delay_time = 0;


    private final HashMap<String, dObject> definitions = new HashMap<String, dObject>();


    private final HashMap<String, ScriptEntry> held_entries = new HashMap<String, ScriptEntry>();


    public dScript script;

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
    public dObject getContext(String id) {
        id = CoreUtilities.toLowerCase(id);
        if (contextSource == null) {
            return null;
        }
        dObject obj = cachedContext.get(id);
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

    public HashMap<String, dObject> cachedContext;

    public void setContextSource(ContextSource source) {
        contextSource = source;
        cachedContext = new HashMap<String, dObject>();
    }


    private long reqId = -1L;

    /**
     * Sets the instant-queue ID for usage by the determine command.
     *
     * @param ID the ID to use.
     * @return the queue for re-use.
     */
    public ScriptQueue setReqId(long ID) {
        reqId = ID;
        return this;
    }

    @Override
    public dObject getDefinitionObject(String definition) {
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


    public void addDefinition(String definition, dObject value) {
        definitions.put(CoreUtilities.toLowerCase(definition), value);
    }


    @Override
    public void addDefinition(String definition, String value) {
        definitions.put(CoreUtilities.toLowerCase(definition), new Element(value));
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
    public Map<String, dObject> getAllDefinitions() {
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


    /**
     * Converts any queue type to a timed queue.
     *
     * @param delay how long to delay initially.
     * @return the newly created queue.
     */
    public TimedQueue forceToTimed(Duration delay) {
        Runnable r = callback;
        callback = null;
        stop();
        TimedQueue newQueue = new TimedQueue(id, 0);
        newQueue.run_async = this.run_async;
        for (ScriptEntry entry : getEntries()) {
            entry.setInstant(true);
        }
        newQueue.addEntries(getEntries());
        for (Map.Entry<String, dObject> def : getAllDefinitions().entrySet()) {
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

    /**
     * Starts the script queue.
     */
    public void start() {
        if (is_started) {
            return;
        }

        // Save the instance to the _queues static map
        _queues.put(id, this);

        // Set as started, and check for a valid delay_time.
        is_started = true;
        long delay = delay_time - System.currentTimeMillis();
        boolean is_delayed = delay > 0;

        // Record what script generated the first entry in the queue
        if (script_entries.size() > 0) {
            script = script_entries.get(0).getScript();
        }

        // Debug info
        String name = getName();
        if (is_delayed) {
            dB.echoDebug(this, "Delaying " + name + " '" + id + "'" + " for '"
                    + new Duration(((double) delay) / 1000f).identify() + "'...");
        }
        else {
            dB.echoDebug(this, "Starting " + name + " '" + id + "'...");
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
                dB.echoDebug(this, "Finishing up queue '" + id + "'...");
            }
            else /* if empty, just stop the queue like normal */ {
                if (_queues.get(id) == this) {
                    _queues.remove(id);
                }
                dB.echoDebug(this, "Completing queue '" + id + "' in " + ((System.nanoTime() - startTime) / 1000000) + "ms.");
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
                dB.echoDebug(this, "Re-completing queue '" + id + "' in " + ((System.nanoTime() - startTime) / 1000000) + "ms.");
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
    public boolean shouldDebug() throws Exception {
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
    public String debug() {
        return "<G>" + prefix + "='<Y>" + identify() + "<G>'  ";
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
        // @returns Element
        // @description
        // Returns the id of the queue.
        // -->
        registerTag("id", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((ScriptQueue) object).id).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.size>
        // @returns Element
        // @description
        // Returns the number of script entries in the queue.
        // -->
        registerTag("size", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Element(((ScriptQueue) object).script_entries.size()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.start_time>
        // @returns Duration
        // @description
        // Returns the time this queue started as a duration.
        // -->
        registerTag("start_time", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new Duration(((ScriptQueue) object).startTimeMilli / 50).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.state>
        // @returns Element
        // @description
        // Returns 'stopping', 'running', 'paused', or 'unknown'.
        // -->
        registerTag("state", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
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
                return new Element(state).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.script>
        // @returns dScript
        // @description
        // Returns the script that started this queue.
        // -->
        registerTag("script", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                if (((ScriptQueue) object).script == null) {
                    return null;
                }
                return ((ScriptQueue) object).script.getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.commands>
        // @returns dList
        // @description
        // Returns a list of commands waiting in the queue.
        // -->
        registerTag("commands", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                dList commands = new dList();
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
        // @returns dList
        // @description
        // Returns the names of all definitions that were passed to the current queue.
        // -->
        registerTag("definitions", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                return new dList(((ScriptQueue) object).getAllDefinitions().keySet()).getAttribute(attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.definition[<definition>]>
        // @returns dObject
        // @description
        // Returns the value of the specified definition.
        // Returns null if the queue lacks the definition.
        // -->
        registerTag("definition", new TagRunnable.ObjectForm() {
            @Override
            public dObject run(Attribute attribute, dObject object) {
                if (!attribute.hasContext(1)) {
                    dB.echoError("The tag q@queue.definition[...] must have a value.");
                    return null;
                }
                return CoreUtilities.autoAttrib(((ScriptQueue) object).getDefinitionObject(attribute.getContext(1)), attribute.fulfill(1));
            }
        });

        // <--[tag]
        // @attribute <q@queue.determination>
        // @returns dObject
        // @description
        // Returns the value that has been determined via <@link command Determine>
        // for this queue, or null if there is none.
        // The object will be returned as the most-valid type based on the input.
        // -->
        registerTag("determination", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                Long rID = ((ScriptQueue) object).reqId;
                if (rID < 0 || !DetermineCommand.hasOutcome(rID)) {
                    return null;
                }
                else {
                    return ObjectFetcher.pickObjectFor(DetermineCommand.readOutcome(rID)).getAttribute(attribute.fulfill(1));
                }
            }
        });

        // <--[tag]
        // @attribute <q@queue.determinable>
        // @returns Element(Boolean)
        // @description
        // Returns true if this queue currently supports using the Determine command.
        // -->
        registerTag("determinable", new TagRunnable() {
            @Override
            public String run(Attribute attribute, dObject object) {
                ScriptEntry entry = ((ScriptQueue) object).getEntries().get(0);
                if (entry != null) {
                    return new Element(entry.getObject("reqid") != null).getAttribute(attribute.fulfill(1));
                }
                else {
                    return new Element(false).getAttribute(attribute.fulfill(1));
                }
            }
        });

    }

    public static HashMap<String, TagRunnable> registeredTags = new HashMap<String, TagRunnable>();

    public static void registerTag(String name, TagRunnable runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registeredTags.put(name, runnable);
    }

    public static HashMap<String, TagRunnable.ObjectForm> registeredObjectTags = new HashMap<String, TagRunnable.ObjectForm>();

    public static void registerTag(String name, TagRunnable.ObjectForm runnable) {
        if (runnable.name == null) {
            runnable.name = name;
        }
        registeredObjectTags.put(name, runnable);
    }

    @Override
    public <T extends dObject> T asObjectType(Class<T> type, TagContext context) {
        return null;
    }

    @Override
    public String getAttribute(Attribute attribute) {
        return CoreUtilities.stringifyNullPass(getObjectAttribute(attribute));
    }

    @Override
    public dObject getObjectAttribute(Attribute attribute) {
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
                dB.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + otr.name + "': '" + attrLow + "'.");
            }
            return otr.run(attribute, this);
        }

        TagRunnable tr = registeredTags.get(attrLow);
        if (tr != null) {
            if (!tr.name.equals(attrLow)) {
                dB.echoError(attribute.getScriptEntry() != null ? attribute.getScriptEntry().getResidingQueue() : null,
                        "Using deprecated form of tag '" + tr.name + "': '" + attrLow + "'.");
            }
            return new Element(tr.run(attribute, this));
        }

        // Iterate through this object's properties' attributes
        for (Property property : PropertyParser.getProperties(this, attrLow)) {
            dObject returned = CoreUtilities.autoAttrib(property, attribute);
            if (returned != null) {
                return returned;
            }
        }

        return new Element(identify()).getObjectAttribute(attribute);
    }
}
