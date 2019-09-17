package com.denizenscript.denizencore;

import com.denizenscript.denizencore.events.OldEventManager;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.events.core.DeltaTimeScriptEvent;
import com.denizenscript.denizencore.events.core.ReloadScriptsScriptEvent;
import com.denizenscript.denizencore.events.core.SystemTimeScriptEvent;
import com.denizenscript.denizencore.events.core.TickScriptEvent;
import com.denizenscript.denizencore.scripts.ScriptHelper;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.commands.CommandRegistry;
import com.denizenscript.denizencore.scripts.queues.ScriptEngine;
import com.denizenscript.denizencore.utilities.debugging.LogInterceptor;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.Schedulable;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The entry point of the core Denizen engine.
 */
public class DenizenCore {

    public final static String VERSION;

    static CommandRegistry commandRegistry;
    static ScriptEngine scriptEngine;

    public static CommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    public static void setCommandRegistry(CommandRegistry registry) {
        commandRegistry = registry;
    }

    public static ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public static LogInterceptor logInterceptor = new LogInterceptor();

    public static Thread MAIN_THREAD;

    public static long currentTimeMillis = System.currentTimeMillis();

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

    static DenizenImplementation implementation;

    public static DenizenImplementation getImplementation() {
        return implementation;
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
     * Call postLoadScripts first.
     */
    public static void preloadScripts() {
        ScriptEvent.worldContainers.clear();
        implementation.preScriptReload();
        ScriptHelper.resetError();
        ScriptHelper.reloadScripts();
    }

    /**
     * Called last in the init sequence, loads all scripts and starts the Denizen engine.
     * Call preloadScripts first.
     */
    public static void postLoadScripts() {
        try {
            ScriptRegistry.postLoadScripts();
            OldEventManager.scanWorldEvents();
            ScriptEvent.reload();
            implementation.onScriptReload();
            ReloadScriptsScriptEvent.instance.reset();
            ReloadScriptsScriptEvent.instance.all = true;
            ReloadScriptsScriptEvent.instance.hadError = ScriptHelper.hadError();
            ReloadScriptsScriptEvent.instance.data = DenizenCore.getImplementation().getEmptyScriptEntryData();
            ReloadScriptsScriptEvent.instance.fire();

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
    }

    public static final List<Schedulable> scheduled = new ArrayList<>();

    /**
     * Schedule an item to be run automatically after a given period of time, optionally repeating.
     */
    public static void schedule(Schedulable sched) {
        synchronized (scheduled) {
            scheduled.add(sched);
        }
    }

    static void oncePerSecond() {
        SystemTimeScriptEvent.instance.checkTime();
        DeltaTimeScriptEvent.instance.checkTime();
    }

    static int tMS = 0;

    public static long serverTimeMillis = 1;

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
    }
}
