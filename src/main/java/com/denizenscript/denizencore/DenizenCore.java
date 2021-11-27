package com.denizenscript.denizencore;

import com.denizenscript.denizencore.events.OldEventManager;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.events.core.*;
import com.denizenscript.denizencore.flags.SavableMapFlagTracker;
import com.denizenscript.denizencore.objects.notable.NoteManager;
import com.denizenscript.denizencore.scripts.ScriptHelper;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.commands.CommandRegistry;
import com.denizenscript.denizencore.scripts.commands.queue.RunLaterCommand;
import com.denizenscript.denizencore.scripts.queues.ScriptEngine;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.debugging.LogInterceptor;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.Schedulable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * The entry point of the core Denizen engine.
 */
public class DenizenCore {

    /**
     * (Automatically populated) current core version.
     */
    public final static String VERSION;

    /**
     * All commands known to the system are registered here.
     */
    public static CommandRegistry commandRegistry;

    /**
     * Core script processing engine.
     */
    public static ScriptEngine scriptEngine;

    /**
     * Time (System.currentTimeMillis) that the engine first loaded.
     */
    public final static long startTime = System.currentTimeMillis();

    /**
     * Server flags, for the 'flag' command and 'server.flag[...]' tags.
     */
    public static SavableMapFlagTracker serverFlagMap;

    /**
     * Last time (System.currentTimeMillis) that scripts wre reloaded.
     */
    public static long lastReloadTime;

    /**
     * How many times scripts have been reloaded.
     */
    public static int reloads = 0;

    /**
     * Helper to intercept System.out for the redirect_logging mechanism.
     */
    public static LogInterceptor logInterceptor = new LogInterceptor();

    /**
     * Known main thread reference, for async scheduler usage.
     */
    public static Thread MAIN_THREAD;

    /**
     * Current system time (System.currentTimeMillis), updated per-tick.
     * Used to avoid multiple checks in the same tick having different time values.
     */
    public static long currentTimeMillis = System.currentTimeMillis();

    /**
     * Duration of time, in milliseconds, since the server started.
     */
    public static long serverTimeMillis = 1;

    /**
     * All current scheduled tasks.
     */
    public static final ArrayList<Schedulable> scheduled = new ArrayList<>();

    /**
     * All current delayed queues.
     */
    public static final ArrayList<TimedQueue> timedQueues = new ArrayList<>();

    /**
     * Implementation helper class, must be implemented for Denizen to function.
     */
    public static DenizenImplementation implementation;

    static {
        String version = "UNKNOWN";
        try {
            InputStream is = DenizenCore.class.getClassLoader().getResourceAsStream("denizencore.properties");
            if (is == null) {
                throw new FileNotFoundException("denizencore.properties not found in jar file");
            }
            else {
                Properties properties = new Properties();
                properties.load(is);
                version = properties.getProperty("version") + " (Build " + properties.getProperty("build") + ")";
                is.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        VERSION = version;
    }

    /**
     * Must be called first: prepares the engine!
     *
     * @param implementation your Denizen implementation.
     */
    public static void init(DenizenImplementation implementation) {
        currentTimeMillis = System.currentTimeMillis();
        DenizenCore.implementation = implementation;
        MAIN_THREAD = Thread.currentThread();
        Debug.log("Initializing Denizen Core v" + VERSION +
                ", implementation for " + implementation.getImplementationName()
                + " version " + implementation.getImplementationVersion());
        scriptEngine = new ScriptEngine();
        ScriptEvent.registerCoreEvents();
    }

    /**
     * Call to reload anything that was saved, especially after init.
     */
    public static void reloadSaves() {
        serverFlagMap = SavableMapFlagTracker.loadFlagFile(new File(implementation.getDataFolder(), "server_flags").getPath());
    }

    /**
     * Call to save anything that needs to be saved, especially before shutdown.
     */
    public static void saveAll() {
        NoteManager.save();
        serverFlagMap.saveToFile(new File(implementation.getDataFolder(), "server_flags").getPath());
    }

    /**
     * Must be called last: performs final shutdowns / saves / etc.
     */
    public static void shutdown() {
        ShutdownScriptEvent.instance.fire();
        saveAll();
        logInterceptor.standardOutput();
        commandRegistry.disableCoreMembers();
    }

    /**
     * Call postLoadScripts after.
     */
    public static void preloadScripts() {
        try {
            reloads++;
            PreScriptReloadScriptEvent.instance.fire();
            ScriptEvent.worldContainers.clear();
            implementation.preScriptReload();
            ScriptHelper.resetError();
            ScriptHelper.reloadScripts();
        }
        catch (Exception ex) {
            implementation.debugMessage("Error loading scripts:");
            implementation.debugException(ex);
        }
    }

    /**
     * Called last in the init sequence, loads all scripts and starts the Denizen engine.
     * Call preloadScripts first.
     */
    public static void postLoadScripts() {
        try {
            TagManager.preCalced.clear();
            Attribute.attribsLookup.clear();
            ReplaceableTagEvent.refs.clear();
            ScriptRegistry.postLoadScripts();
            OldEventManager.scanWorldEvents();
            ScriptEvent.reload();
            implementation.onScriptReload();
            lastReloadTime = System.currentTimeMillis();
        }
        catch (Exception ex) {
            implementation.debugMessage("Error loading scripts:");
            implementation.debugException(ex);
        }
    }

    /**
     * Call when a script reload is required (EG, requested by user command.)
     */
    public static void reloadScripts() {
        preloadScripts();
        postLoadScripts();
        ReloadScriptsScriptEvent.instance.hadError = ScriptHelper.hadError();
        ReloadScriptsScriptEvent.instance.fire();
        Debug.log("Scripts reloaded.");
    }

    /**
     * Schedule an item to be run automatically after a given period of time, optionally repeating.
     */
    public static void schedule(Schedulable sched) {
        synchronized (scheduled) {
            scheduled.add(sched);
        }
    }

    /**
     * Ran by 'tick' once per second.
     */
    static void oncePerSecond() {
        SystemTimeScriptEvent.instance.checkTime();
        DeltaTimeScriptEvent.instance.checkTime();
    }

    /**
     * Counter for 'oncePerSecond'.
     */
    static int tMS = 0;

    /**
     * Call every 'tick' in the engine. (1/20th of a second on a standard engine.)
     *
     * @param ms_elapsed how many MS have actually elapsed. (50 on a standard engine).
     */
    public static void tick(int ms_elapsed) {
        serverTimeMillis += ms_elapsed;
        currentTimeMillis = System.currentTimeMillis();
        TickScriptEvent.instance.ticks++;
        if (TickScriptEvent.instance.enabled) {
            TickScriptEvent.instance.fire();
        }
        RunLaterCommand.tickFutureRuns();
        tMS += ms_elapsed;
        while (tMS > 1000) {
            tMS -= 1000;
            oncePerSecond();
        }
        synchronized (scheduled) {
            for (int i = 0; i < scheduled.size(); i++) {
                if (!scheduled.get(i).tick((float) ms_elapsed / 1000)) {
                    scheduled.remove(i--);
                }
            }
        }
        for (int i = 0; i < timedQueues.size(); i++) {
            TimedQueue queue = timedQueues.get(i);
            queue.tryRevolveOnce();
            if (queue.isStopped) {
                timedQueues.remove(i--);
            }
        }
    }
}
