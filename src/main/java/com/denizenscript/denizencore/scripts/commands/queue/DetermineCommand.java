package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public class DetermineCommand extends AbstractCommand {

    public DetermineCommand() {
        setName("determine");
        setSyntax("determine (passively) [<value>]");
        setRequiredArguments(1, 2);
        isProcedural = true;
        setBooleanHandled("passively");
    }

    // <--[command]
    // @Name Determine
    // @Syntax determine (passively) [<value>]
    // @Required 1
    // @Maximum 2
    // @Short Sets the outcome of a script.
    // @Group queue
    // @Synonyms Return
    // @Guide https://guide.denizenscript.com/guides/first-steps/world-script.html
    //
    // @Description
    // Sets the outcome of a script.
    // The most common use case is within script events (for example, to cancel the event).
    // This is also required for all procedure scripts.
    // It may be useful in other cases (such as a task script that returns a result, via the save argument).
    //
    // By default, the determine command will end the queue (similar to <@link command stop>).
    // If you wish to prevent this, specify the "passively" argument.
    //
    // To make multiple determines, simply use the determine command multiple times in a row, with the "passively" argument on each.
    //
    // @Tags
    // <QueueTag.determination>
    //
    // @Usage
    // Use to modify the result of an event.
    // - determine message:<context.message.to_lowercase>
    //
    // @Usage
    // Use to cancel an event, but continue running script commands.
    // - determine passively cancelled
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("outcome")) {
                scriptEntry.addObject("outcome", arg);
            }
            else {
                arg.reportUnhandled();
            }
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        Argument outcomeArg = (Argument) scriptEntry.getObject("outcome");
        if (outcomeArg == null) {
            throw new InvalidArgumentsRuntimeException("Must specify a value to determine.");
        }
        boolean passively = scriptEntry.argAsBoolean("passively");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), db("outcome", outcomeArg), db("passively", passively), new QueueTag(scriptEntry.getResidingQueue()));
        }
        ScriptQueue queue = scriptEntry.getResidingQueue();
        ListTag determines = queue.determinations;
        if (determines == null) {
            determines = new ListTag();
            queue.determinations = determines;
        }
        determines.addObject(outcomeArg.hasPrefix() ? outcomeArg.getRawElement() : outcomeArg.object);
        if (queue.determinationTarget != null) {
            queue.determinationTarget.applyDetermination(outcomeArg.prefix, outcomeArg.object);
        }

        if (!passively) {
            scriptEntry.getResidingQueue().clear();
            scriptEntry.getResidingQueue().stop();
        }
    }
}
