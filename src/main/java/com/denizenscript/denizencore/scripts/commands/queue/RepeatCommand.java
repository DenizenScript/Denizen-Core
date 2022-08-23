package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.EnumHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;

import java.util.List;

public class RepeatCommand extends BracedCommand {

    public RepeatCommand() {
        setName("repeat");
        setSyntax("repeat [stop/next/<amount>] (from:<#>) (as:<name>) [<commands>]");
        setRequiredArguments(1, 3);
        isProcedural = true;
        generateDebug = false;
        autoCompile();
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

    private static class RepeatData {
        public int index;
        public int target;
        public String valueName;
        public ObjectTag originalValue;

        public void reapplyAtEnd(ScriptQueue queue) {
            queue.addDefinition(valueName, originalValue);
        }
    }

    public enum Action { RUN, STOP, NEXT, CALLBACK }

    static {
        EnumHelper<Action> enumHack = EnumHelper.get(Action.class);
        enumHack.valuesMapLower.remove("callback");
        enumHack.valuesMapLower.put("\0callback", Action.CALLBACK);
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgLinear @ArgName("quantity") @ArgDefaultText("-1") int quantity,
                                   @ArgName("action") @ArgDefaultText("run") Action action,
                                   @ArgPrefixed @ArgName("from") @ArgDefaultText("1") int from,
                                   @ArgPrefixed @ArgName("as") @ArgDefaultText("value") String asName) {
        ScriptQueue queue = scriptEntry.getResidingQueue();
        if (action == Action.STOP) {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, "repeat", db("instruction", "stop"));
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
                        queue.removeFirst();
                        break;
                    }
                    queue.removeFirst();
                }
            }
            else {
                Debug.echoError("Cannot stop repeat: not in one!");
            }
            return;
        }
        else if (action == Action.NEXT) {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, "repeat", db("instruction", "next"));
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
                    queue.removeFirst();
                }
            }
            else {
                Debug.echoError("Cannot 'repeat next': not in one!");
            }
            return;
        }
        else if (action == Action.CALLBACK) {
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
                    List<ScriptEntry> bracedCommands = BracedCommand.getBracedCommandsDirect(scriptEntry.getOwner(), scriptEntry);
                    ScriptEntry callbackEntry = scriptEntry.clone();
                    callbackEntry.copyFrom(scriptEntry);
                    callbackEntry.setOwner(scriptEntry.getOwner());
                    bracedCommands.add(callbackEntry);
                    for (ScriptEntry cmd : bracedCommands) {
                        cmd.setInstant(true);
                    }
                    queue.injectEntriesAtStart(bracedCommands);
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
            if (quantity == -1) {
                throw new InvalidArgumentsRuntimeException("Must specify a quantity or 'stop' or 'next'!");
            }
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, "repeat", db("from", from), db("times", quantity), db("as_name", asName));
            }
            if (quantity <= 0) {
                if (scriptEntry.dbCallShouldDebug()) {
                    Debug.echoDebug(scriptEntry, "Zero count, not looping...");
                }
                return;
            }
            RepeatData datum = new RepeatData();
            datum.index = from;
            datum.target = datum.index + quantity - 1;
            datum.valueName = asName;
            scriptEntry.setData(datum);
            ScriptEntry callbackEntry = getCallback(scriptEntry);
            List<ScriptEntry> bracedCommandsList = getBracedCommandsDirect(scriptEntry, scriptEntry);
            if (bracedCommandsList == null || bracedCommandsList.isEmpty()) {
                Debug.echoError(scriptEntry, "Empty subsection - did you forget a ':'?");
                return;
            }
            datum.originalValue = queue.getDefinitionObject(datum.valueName);
            queue.addDefinition(datum.valueName, String.valueOf(datum.index));
            callbackEntry.copyFrom(scriptEntry);
            callbackEntry.setOwner(scriptEntry);
            bracedCommandsList.add(callbackEntry);
            for (ScriptEntry cmd : bracedCommandsList) {
                cmd.setInstant(true);
            }
            scriptEntry.setInstant(true);
            queue.injectEntriesAtStart(bracedCommandsList);
        }
    }

    public static ScriptEntry getCallback(ScriptEntry forEntry) {
        if (forEntry.internal.specialProcessedData == null) {
            forEntry.internal.specialProcessedData = new ScriptEntry("REPEAT", new String[] {"\0CALLBACK"}, forEntry.getScript() != null ? forEntry.getScript().getContainer() : null);
        }
        return ((ScriptEntry) forEntry.internal.specialProcessedData).clone();
    }
}
