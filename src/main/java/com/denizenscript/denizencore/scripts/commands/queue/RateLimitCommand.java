package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashMap;

public class RateLimitCommand extends AbstractCommand {

    // <--[command]
    // @Name RateLimit
    // @Syntax ratelimit [<object>] [<duration>]
    // @Required 2
    // @Short Limits the rate that queues may process a script at.
    // @Group queue
    //
    // @Description
    // Limits the rate that queues may process a script at.
    // If another queue tries to run the same script faster than the duration, that second queue will be stopped.
    //
    // Note that the rate limiting is tracked based on two unique factors: the object input, and the specific script line.
    // That is to say: if you have a 'ratelimit <player> 10s', and then a few lines down a 'ratelimit <player> 10s',
    // those are two separate rate limiters.
    // Additionally, if you have a 'ratelimit <player> 10s' and two different players run it, they each have a separate rate limit applied.
    //
    // @Tags
    // None.
    //
    // @Usage
    // Use to show a message to a player no faster than once every ten seconds.
    // - ratelimit <player> 10s
    // - narrate "Wow!"
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (arg.matchesArgumentType(DurationTag.class)
                && !scriptEntry.hasObject("duration")) {
                scriptEntry.addObject("duration", arg.asType(DurationTag.class));
            }
            else if (!scriptEntry.hasObject("object")) {
                scriptEntry.addObject("object", new ElementTag(arg.raw_value));
            }
            else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {


        DurationTag duration = scriptEntry.getObjectTag("duration");
        ElementTag object = scriptEntry.getElement("object");

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), duration.debug() + object.debug());
        }

        if (scriptEntry.internal.specialProcessedData == null) {
            scriptEntry.internal.specialProcessedData = new HashMap<>();
        }
        HashMap<String, Long> map = (HashMap<String, Long>) scriptEntry.internal.specialProcessedData;
        String key = CoreUtilities.toLowerCase(object.asString());
        Long endTime = map.get(key);
        long curTime = DenizenCore.serverTimeMillis;
        if (endTime != null && curTime < endTime) {
            Debug.echoDebug(scriptEntry, "Rate limit applied with " + (endTime - curTime) + "ms left.");
            scriptEntry.getResidingQueue().clear();
            scriptEntry.getResidingQueue().stop();
            return;
        }
        map.put(key, curTime + duration.getMillis());
    }
}
