package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DetermineCommand extends AbstractCommand {

    //
    // Static helpers
    //


    // Default 'DETERMINE_NONE' value.
    public static String DETERMINE_NONE = "none";

    // Map for keeping track of cache
    // Key: ID, Value: outcome
    private static Map<Long, dList> cache = new ConcurrentHashMap<>(8, 0.9f, 1);

    // Start at 0
    public static long uniqueId = 0;


    /**
     * Increment the counter and return it, thus returning
     * a unique id. Determinations are very short lived.
     *
     * @return long ID
     */
    public static long getNewId() {
        // Just in case? Start over if already max_value.
        if (uniqueId == Long.MAX_VALUE) {
            uniqueId = 0;
        }
        // Increment the counter
        return uniqueId++;
    }


    /**
     * Checks the cache for existence of an outcome.
     *
     * @param id the outcome id to check
     * @return if the cache has the outcome
     */
    public static boolean hasOutcome(long id) {
        return cache.containsKey(id) && !cache.get(id).isEmpty();
    }


    /**
     * Gets the outcome, and removes it from the cache.
     *
     * @param id the outcome id to check
     * @return the outcome
     */
    public static dList getOutcome(long id) {
        dList outcome = cache.get(id);
        cache.remove(id);
        return outcome;
    }


    /**
     * Gets the current value of the outcome.
     * Note: The value of the outcome may change.
     *
     * @param id the outcome id to check
     * @return the current value of the outcome
     */
    public static String readOutcome(long id) {
        return cache.get(id).isEmpty() ? DETERMINE_NONE : cache.get(id).get(0);
    }


    //
    // Command Singleton
    //

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        //
        // Parse the arguments
        //

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {

            if (arg.matches("passive", "passively")) {
                scriptEntry.addObject("passively", new Element(true));
            }

            else if (!scriptEntry.hasObject("outcome")) {
                scriptEntry.addObject("outcome", arg.hasPrefix() ? new Element(arg.raw_value) : arg.object);
            }

            else {
                arg.reportUnhandled();
            }
        }

        //
        // Set defaults
        //

        scriptEntry.defaultObject("passively", new Element(false));
        scriptEntry.defaultObject("outcome", new Element(DETERMINE_NONE));
    }


    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        dObject outcomeObj = scriptEntry.getdObject("outcome");

        // Report!
        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), outcomeObj.debug()
                    + scriptEntry.getElement("passively").debug());
        }

        // Fetch the ScriptEntry elements
        Boolean passively = scriptEntry.getElement("passively").asBoolean();

        Long uniqueId = (Long) scriptEntry.getObject("reqid");

        // Useful for debug
        if (uniqueId == null) {
            dB.echoError(scriptEntry.getResidingQueue(), "Cannot use determine in this queue!");
            return;
        }

        // Store the outcome in the cache
        dList strs = cache.get(uniqueId);
        if (strs == null) {
            strs = new dList();
            cache.put(uniqueId, strs);
        }
        strs.addObject(outcomeObj);

        if (!passively) {
            // Stop the queue by clearing the remainder of it.
            scriptEntry.getResidingQueue().clear();
        }
    }
}
