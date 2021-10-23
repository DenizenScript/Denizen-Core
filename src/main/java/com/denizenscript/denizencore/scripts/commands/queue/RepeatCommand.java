package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;

import java.util.List;

public class RepeatCommand extends BracedCommand {

    public RepeatCommand() {
        setName("repeat");
        setSyntax("repeat [stop/next/<amount>] (from:<#>) (as:<name>) [<commands>]");
        setRequiredArguments(1, 3);
        isProcedural = true;
    }

    // <--[command]
    // @Name Repeat
    // @Syntax repeat [stop/next/<amount>] (from:<#>) (as:<name>) [<commands>]
    // @Required 1
    // @Maximum 3
    // @Short Runs a series of braced commands several times.
    // @Synonyms For
    // @Group queue
    // @Guide https://guide.denizenscript.com/guides/basics/loops.html
    //
    // @Description
    // Loops through a series of braced commands a specified number of times.
    // To get the number of loops so far, you can use <[value]>.
    //
    // Optionally, specify "as:<name>" to change the definition name to something other than "value".
    //
    // Optionally, to specify a starting index, use "from:<#>". Note that the "amount" input is how many loops will happen, not an end index.
    // The default "from" index is "1". Note that the value you give to "from" will be the value of the first loop.
    //
    // To stop a repeat loop, do - repeat stop
    //
    // To jump immediately to the next number in the loop, do - repeat next
    //
    // @Tags
    // <[value]> to get the number of loops so far
    //
    // @Usage
    // Use to loop through a command five times.
    // - repeat 5:
    //     - announce "Announce Number <[value]>"
    //
    // @Usage
    // Use to announce the numbers: 1, 2, 3, 4, 5.
    // - repeat 5 as:number:
    //     - announce "I can count! <[number]>"
    //
    // @Usage
    // Use to announce the numbers: 21, 22, 23, 24, 25.
    // - repeat 5 from:21:
    //     - announce "Announce Number <[value]>"
    // -->

    private class RepeatData {
        public int index;
        public int target;
        public String valueName;
        public ObjectTag originalValue;

        public void reapplyAtEnd(ScriptQueue queue) {
            queue.addDefinition(valueName, originalValue);
        }
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        boolean handled = false;
        for (Argument arg : scriptEntry) {
            if (!handled
                    && arg.matchesInteger() && !arg.hasPrefix()) {
                scriptEntry.addObject("quantity", arg.asElement());
                handled = true;
            }
            else if (!handled
                    && arg.matches("stop") && !arg.hasPrefix()) {
                scriptEntry.addObject("stop", new ElementTag(true));
                handled = true;
            }
            else if (!handled
                    && arg.matches("next") && !arg.hasPrefix()) {
                scriptEntry.addObject("next", new ElementTag(true));
                handled = true;
            }
            else if (!handled
                    && arg.matches("\0callback") && !arg.hasPrefix()) {
                scriptEntry.addObject("callback", new ElementTag(true));
                handled = true;
            }
            else if (!scriptEntry.hasObject("from")
                    && arg.matchesPrefix("from")
                    && arg.matchesInteger()) {
                scriptEntry.addObject("from", arg.asElement());
            }
            else if (!scriptEntry.hasObject("as_name")
                    && arg.matchesPrefix("as")) {
                scriptEntry.addObject("as_name", arg.asElement());
            }
            else if (arg.matches("{")) {
                break;
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!handled) {
            throw new InvalidArgumentsException("Must specify a quantity or 'stop' or 'next'!");
        }
        scriptEntry.defaultObject("as_name", new ElementTag("value"));
        scriptEntry.defaultObject("from", new ElementTag("1"));
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag stop = scriptEntry.getElement("stop");
        ElementTag next = scriptEntry.getElement("next");
        ElementTag callback = scriptEntry.getElement("callback");
        ElementTag quantity = scriptEntry.getElement("quantity");
        ElementTag as_name = scriptEntry.getElement("as_name");
        ElementTag from = scriptEntry.getElement("from");
        ScriptQueue queue = scriptEntry.getResidingQueue();
        if (stop != null && stop.asBoolean()) {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), stop);
            }
            boolean hasnext = false;
            for (int i = 0; i < queue.getQueueSize(); i++) {
                ScriptEntry entry = queue.getEntry(i);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equals("REPEAT") && args.size() == 1 && args.get(0).equals("\0CALLBACK")) {
                    hasnext = true;
                    break;
                }
            }
            if (hasnext) {
                while (queue.getQueueSize() > 0) {
                    ScriptEntry entry = queue.getEntry(0);
                    List<String> args = entry.getOriginalArguments();
                    if (entry.getCommandName().equals("REPEAT") && args.size() == 1 && args.get(0).equals("\0CALLBACK")) {
                        ((RepeatData) entry.getOwner().getData()).reapplyAtEnd(queue);
                        queue.removeEntry(0);
                        break;
                    }
                    queue.removeEntry(0);
                }
            }
            else {
                Debug.echoError("Cannot stop repeat: not in one!");
            }
            return;
        }
        else if (next != null && next.asBoolean()) {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), next);
            }
            boolean hasnext = false;
            for (int i = 0; i < queue.getQueueSize(); i++) {
                ScriptEntry entry = queue.getEntry(i);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equals("REPEAT") && args.size() == 1 && args.get(0).equals("\0CALLBACK")) {
                    hasnext = true;
                    break;
                }
            }
            if (hasnext) {
                while (queue.getQueueSize() > 0) {
                    ScriptEntry entry = queue.getEntry(0);
                    List<String> args = entry.getOriginalArguments();
                    if (entry.getCommandName().equals("REPEAT") && args.size() == 1 && args.get(0).equals("\0CALLBACK")) {
                        break;
                    }
                    queue.removeEntry(0);
                }
            }
            else {
                Debug.echoError("Cannot 'repeat next': not in one!");
            }
            return;
        }
        else if (callback != null && callback.asBoolean()) {
            if (scriptEntry.getOwner() != null && (scriptEntry.getOwner().getCommandName().equals("REPEAT") ||
                    scriptEntry.getOwner().getBracedSet() == null || scriptEntry.getOwner().getBracedSet().isEmpty() ||
                    scriptEntry.getBracedSet().get(0).value.get(scriptEntry.getBracedSet().get(0).value.size() - 1) != scriptEntry)) {
                RepeatData data = (RepeatData) scriptEntry.getOwner().getData();
                data.index++;
                if (data.index <= data.target) {
                    if (scriptEntry.dbCallShouldDebug()) {
                        Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "Repeat loop " + data.index);
                    }
                    queue.addDefinition(data.valueName, String.valueOf(data.index));
                    List<ScriptEntry> bracedCommands = BracedCommand.getBracedCommands(scriptEntry.getOwner()).get(0).value;
                    ScriptEntry callbackEntry = scriptEntry.clone();
                    callbackEntry.copyFrom(scriptEntry);
                    callbackEntry.setOwner(scriptEntry.getOwner());
                    bracedCommands.add(callbackEntry);
                    for (ScriptEntry cmd : bracedCommands) {
                        cmd.setInstant(true);
                        cmd.copyFrom(scriptEntry);
                    }
                    queue.injectEntries(bracedCommands, 0);
                }
                else {
                    data.reapplyAtEnd(queue);
                    if (scriptEntry.dbCallShouldDebug()) {
                        Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "Repeat loop complete");
                    }
                }
            }
            else {
                Debug.echoError("Repeat CALLBACK invalid: not a real callback!");
            }
        }
        else {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), from, quantity, as_name);
            }
            int target = quantity.asInt();
            if (target <= 0) {
                if (scriptEntry.dbCallShouldDebug()) {
                    Debug.echoDebug(scriptEntry, "Zero count, not looping...");
                }
                return;
            }
            RepeatData datum = new RepeatData();
            datum.index = from.asInt();
            datum.target = datum.index + target - 1;
            datum.valueName = as_name.asString();
            scriptEntry.setData(datum);
            ScriptEntry callbackEntry = new ScriptEntry("REPEAT", new String[] {"\0CALLBACK"},
                    (scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null));
            List<BracedCommand.BracedData> data = getBracedCommands(scriptEntry);
            if (data == null || data.isEmpty()) {
                Debug.echoError(queue, "Empty subsection - did you forget a ':'?");
                return;
            }
            List<ScriptEntry> bracedCommandsList = data.get(0).value;
            if (bracedCommandsList == null || bracedCommandsList.isEmpty()) {
                Debug.echoError(queue, "Empty subsection - did you forget to add the sub-commands inside the command?");
                return;
            }
            datum.originalValue = queue.getDefinitionObject(datum.valueName);
            queue.addDefinition(datum.valueName, String.valueOf(datum.index));
            callbackEntry.copyFrom(scriptEntry);
            callbackEntry.setOwner(scriptEntry);
            bracedCommandsList.add(callbackEntry);
            for (ScriptEntry cmd : bracedCommandsList) {
                cmd.setInstant(true);
                cmd.copyFrom(scriptEntry);
            }
            scriptEntry.setInstant(true);
            queue.injectEntries(bracedCommandsList, 0);
        }
    }
}
