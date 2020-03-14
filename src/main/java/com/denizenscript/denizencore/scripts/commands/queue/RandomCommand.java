package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

import java.util.List;

public class RandomCommand extends BracedCommand {

    public RandomCommand() {
        setName("random");
        setSyntax("random [<commands>]");
        setRequiredArguments(0, 1);
    }

    // <--[command]
    // @Name Random
    // @Syntax random [<commands>]
    // @Required 0
    // @Maximum 1
    // @Short Selects a random choice from the following script commands.
    // @Group queue
    //
    // @Description
    // The random command picks one of the following script command and skips all the other script commands that are in its section.
    // Commands like "repeat 1" or "if true" can be used to group together a sublisting of commands to execute together
    // (as a way to get around the 1-command limit).
    //
    // If wanting to choose a random long set of commands,
    // consider instead using <@link command choose> with <@link tag util.random.int.to>
    //
    // @Tags
    // <entry[saveName].possibilities> returns an ElementTag of the possibility count.
    // <entry[saveName].selected> returns an ElementTag of the selected number.
    //
    // @Usage
    // Use to choose randomly from a braced set of commands
    // - random:
    //   - narrate "hi"
    //   - narrate "hello"
    //   - narrate "hey"
    //
    // @Usage
    // Use to perform multiple commands randomly
    // - random:
    //   - repeat 1:
    //     - narrate "Hello"
    //     - narrate "How are you?"
    //   - repeat 1:
    //     - narrate "Hey"
    //     - narrate "It is a nice day."
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        List<BracedData> bdat = getBracedCommands(scriptEntry);

        if (bdat != null && bdat.size() > 0) {
            scriptEntry.addObject("braces", bdat);
        }

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (arg.matches("{")) {
                break;
            }
            else if (!scriptEntry.hasObject("possibilities")
                    && arg.matchesInteger()) {
                Deprecations.oldStyleRandomCommand.warn(scriptEntry);
                scriptEntry.addObject("possibilities", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }

        }

        if (!scriptEntry.hasObject("braces")) {
            if (!scriptEntry.hasObject("possibilities")) {
                throw new InvalidArgumentsException("Missing possibilities!");
            }

            if (scriptEntry.getElement("possibilities").asInt() <= 1) {
                throw new InvalidArgumentsException("Must randomly select more than one item.");
            }

            if (scriptEntry.getResidingQueue().getQueueSize() < scriptEntry.getElement("possibilities").asInt()) {
                throw new InvalidArgumentsException("Invalid Size! Random # must not be larger than the script!");
            }
        }

    }

    private int previous = 0;
    private int previous2 = 0;
    private int previous3 = 0;

    @SuppressWarnings("unchecked")
    @Override
    public void execute(ScriptEntry scriptEntry) {

        int possibilities;
        ScriptQueue queue = scriptEntry.getResidingQueue();
        List<ScriptEntry> bracedCommands = null;

        if (!scriptEntry.hasObject("braces")) {
            possibilities = scriptEntry.getElement("possibilities").asInt();
        }
        else {
            bracedCommands = ((List<BracedData>) scriptEntry.getObject("braces")).get(0).value;
            possibilities = bracedCommands.size();
        }

        int selected = CoreUtilities.getRandom().nextInt(possibilities);
        // Try to not duplicate
        if (selected == previous || selected == previous2 || selected == previous3) {
            selected = CoreUtilities.getRandom().nextInt(possibilities);
        }
        if (selected == previous || selected == previous2 || selected == previous3) {
            selected = CoreUtilities.getRandom().nextInt(possibilities);
        }
        previous3 = previous2;
        previous2 = previous;
        previous = selected;
        scriptEntry.addObject("possibilities", new ElementTag(possibilities));
        scriptEntry.addObject("selected", new ElementTag(selected));

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), ArgumentHelper.debugObj("possibilities", possibilities) + ArgumentHelper.debugObj("choice", selected + 1));
        }

        scriptEntry.setInstant(true);

        if (bracedCommands == null) {

            ScriptEntry keeping = null;

            for (int x = 0; x < possibilities; x++) {

                if (x != selected) {
                    queue.removeEntry(0);
                }
                else {
                    Debug.echoDebug(scriptEntry, "...selected '" + queue.getEntry(0).getCommandName() + ": "
                            + queue.getEntry(0).getArguments() + "'.");
                    keeping = queue.getEntry(0);
                    queue.removeEntry(0);
                }

            }

            queue.injectEntry(keeping, 0);

        }
        else {
            queue.injectEntry(bracedCommands.get(selected), 0);
        }
    }
}
