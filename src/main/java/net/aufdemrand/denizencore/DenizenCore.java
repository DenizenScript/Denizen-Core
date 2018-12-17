package net.aufdemrand.denizencore;

import net.aufdemrand.denizencore.events.OldEventManager;
import net.aufdemrand.denizencore.events.ScriptEvent;
import net.aufdemrand.denizencore.events.core.SystemTimeScriptEvent;
import net.aufdemrand.denizencore.scripts.ScriptHelper;
import net.aufdemrand.denizencore.scripts.commands.CommandRegistry;
import net.aufdemrand.denizencore.scripts.queues.ScriptEngine;
import net.aufdemrand.denizencore.utilities.debugging.LogInterceptor;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.scheduling.Schedulable;

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
        MAIN_THREAD = implementation.getMainThread();
        dB.log("Initializing Denizen Core v" + VERSION +
                ", implementation for " + implementation.getImplementationName()
                + " version " + implementation.getImplementationVersion());
        scriptEngine = new ScriptEngine();
        ScriptEvent.registerCoreEvents();
    }

    /**
     * Called after all objects are added and registered, to calculate
     * everything that needs calculating.
     */
    public static void initSecondary() {

    }

    /**
     * Called last in the init sequence, loads all scripts and starts the Denizen engine.
     */
    public static void loadScripts() {
        try {
            ScriptEvent.worldContainers.clear();
            implementation.preScriptReload();
            ScriptHelper.resetError();
            ScriptHelper.reloadScripts();
            OldEventManager.scanWorldEvents();
            ScriptEvent.reload();
            implementation.onScriptReload();

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
        loadScripts();
    }

    public static final List<Schedulable> scheduled = new ArrayList<Schedulable>();

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
