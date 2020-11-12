package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;

import java.util.List;

public class RepeatCommand extends BracedCommand {

    public RepeatCommand() {
        setName("repeat");
        setSyntax("repeat [stop/next/<amount>] (as:<name>) [<commands>]");
        setRequiredArguments(1, 2);
        isProcedural = true;
    }

    // <--[command]
    // @Name Repeat
    // @Syntax repeat [stop/next/<amount>] (as:<name>) [<commands>]
    // @Required 1
    // @Maximum 2
    // @Short Runs a series of braced commands several times.
    // @Group queue
    // @Guide https://guide.denizenscript.com/guides/basics/loops.html
    //
    // @Description
    // Loops through a series of braced commands a specified number of times.
    // To get the number of loops so far, you can use <[value]>.
    //
    // Optionally, specify "as:<name>" to change the definition name to something other than "value".
    //
    // To stop a repeat loop, do - repeat stop
    //
    // To jump immediately to the next number in the loop, do - repeat next
    //
    // @Tags
    // <[value]> to get the number of loops so far
    //
    // @Usage
    // Use to loop through a command several times
    // - repeat 5:
    //     - announce "Announce Number <[value]>"
    // -->

    private class RepeatData {
        public int index;
        public int target;
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        boolean handled = false;

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (!handled
                    && arg.matchesInteger()) {
                scriptEntry.addObject("quantity", arg.asElement());
                scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
                handled = true;
            }
            else if (!handled
                    && arg.matches("stop")) {
                scriptEntry.addObject("stop", new ElementTag(true));
                handled = true;
            }
            else if (!handled
                    && arg.matches("next")) {
                scriptEntry.addObject("next", new ElementTag(true));
                handled = true;
            }
            else if (!handled
                    && arg.matches("\0callback")) {
                scriptEntry.addObject("callback", new ElementTag(true));
                handled = true;
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

    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag stop = scriptEntry.getElement("stop");
        ElementTag next = scriptEntry.getElement("next");
        ElementTag callback = scriptEntry.getElement("callback");
        ElementTag quantity = scriptEntry.getElement("quantity");
        ElementTag as_name = scriptEntry.getElement("as_name");
        if (stop != null && stop.asBoolean()) {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), stop.debug());
            }
            boolean hasnext = false;
            for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equals("REPEAT") && args.size() > 0 && args.get(0).equals("\0CALLBACK")) {
                    hasnext = true;
                    break;
                }
            }
            if (hasnext) {
                while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                    ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                    List<String> args = entry.getOriginalArguments();
                    if (entry.getCommandName().equals("REPEAT") && args.size() > 0 && args.get(0).equals("\0CALLBACK")) {
                        scriptEntry.getResidingQueue().removeEntry(0);
                        break;
                    }
                    scriptEntry.getResidingQueue().removeEntry(0);
                }
            }
            else {
                Debug.echoError("Cannot stop repeat: not in one!");
            }
            return;
        }
        else if (next != null && next.asBoolean()) {
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), next.debug());
            }
            boolean hasnext = false;
            for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equals("REPEAT") && args.size() > 0 && args.get(0).equals("\0CALLBACK")) {
                    hasnext = true;
                    break;
                }
            }
            if (hasnext) {
                while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                    ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                    List<String> args = entry.getOriginalArguments();
                    if (entry.getCommandName().equals("REPEAT") && args.size() > 0 && args.get(0).equals("\0CALLBACK")) {
                        break;
                    }
                    scriptEntry.getResidingQueue().removeEntry(0);
                }
            }
            else {
                Debug.echoError("Cannot stop repeat: not in one!");
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
                    scriptEntry.getResidingQueue().addDefinition(as_name.asString(), String.valueOf(data.index));
                    List<ScriptEntry> bracedCommands = BracedCommand.getBracedCommands(scriptEntry.getOwner()).get(0).value;
                    ScriptEntry callbackEntry = scriptEntry.clone();
                    callbackEntry.copyFrom(scriptEntry);
                    callbackEntry.setOwner(scriptEntry.getOwner());
                    bracedCommands.add(callbackEntry);
                    for (int i = 0; i < bracedCommands.size(); i++) {
                        bracedCommands.get(i).setInstant(true);
                    }
                    scriptEntry.getResidingQueue().injectEntries(bracedCommands, 0);
                }
                else {
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
            List<BracedCommand.BracedData> data = ((List<BracedCommand.BracedData>) scriptEntry.getObject("braces"));
            if (data == null || data.isEmpty()) {
                Debug.echoError(scriptEntry.getResidingQueue(), "Empty braces (internal)!");
                Debug.echoError(scriptEntry.getResidingQueue(), "Empty braces!");
                return;
            }
            List<ScriptEntry> bracedCommandsList = data.get(0).value;
            if (bracedCommandsList == null || bracedCommandsList.isEmpty()) {
                Debug.echoError("Empty braces!");
                return;
            }
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), quantity.debug() + as_name.debug());
            }
            int target = quantity.asInt();
            if (target <= 0) {
                if (scriptEntry.dbCallShouldDebug()) {
                    Debug.echoDebug(scriptEntry, "Zero count, not looping...");
                }
                return;
            }
            RepeatData datum = new RepeatData();
            datum.target = target;
            datum.index = 1;
            scriptEntry.setData(datum);
            ScriptEntry callbackEntry = new ScriptEntry("REPEAT", new String[] {"\0CALLBACK", "as:" + as_name.asString()},
                    (scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null));
            callbackEntry.copyFrom(scriptEntry);
            callbackEntry.setOwner(scriptEntry);
            bracedCommandsList.add(callbackEntry);
            scriptEntry.getResidingQueue().addDefinition(as_name.asString(), "1");
            for (int i = 0; i < bracedCommandsList.size(); i++) {
                bracedCommandsList.get(i).setInstant(true);
            }
            scriptEntry.setInstant(true);
            scriptEntry.getResidingQueue().injectEntries(bracedCommandsList, 0);
        }
    }
}
