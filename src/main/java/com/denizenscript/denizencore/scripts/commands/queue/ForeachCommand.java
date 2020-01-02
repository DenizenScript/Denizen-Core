package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;

import java.util.List;

public class ForeachCommand extends BracedCommand {

    // <--[command]
    // @Name Foreach
    // @Syntax foreach [stop/next/<object>|...] (as:<name>) [<commands>]
    // @Required 1
    // @Short Loops through a ListTag, running a set of commands for each item.
    // @Group queue
    // @Guide https://guide.denizenscript.com/guides/basics/loops.html
    //
    // @Description
    // Loops through a ListTag of any type. For each item in the ListTag, the specified commands will be ran for
    // that list entry. To call the value of the entry while in the loop, you can use <[value]>.
    //
    // Optionally, specify "as:<name>" to change the definition name to something other than "value".
    //
    // To end a foreach loop, do - foreach stop
    //
    // To jump immediately to the next entry in the loop, do - foreach next
    //
    // @Tags
    // <[value]> to get the current item in the loop
    // <[loop_index]> to get the current loop iteration number
    //
    // @Usage
    // Use to run commands 'for each entry' in a list of objects/elements.
    // - foreach <[some_entity]>|<[some_npc]>|<[player]>:
    //     - announce "There's something at <[value].location>!"
    //
    // @Usage
    // Use to iterate through entries in any tag that returns a list
    // - foreach <server.list_online_players> as:player:
    //     - narrate "Thanks for coming to our server! Here's a bonus $50.00!" player:<[player]>
    //     - give <[player]> money qty:50
    //
    // -->

    private class ForeachData {
        public int index;
        public ListTag list;
    }

    @Override
    public void onEnable() {
        setBraced();
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        boolean handled = false;

        for (Argument arg : scriptEntry.getProcessedArgs()) {

            if (!handled
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
                    && arg.matches("\0CALLBACK")) {
                scriptEntry.addObject("callback", new ElementTag(true));
                handled = true;
            }
            else if (!scriptEntry.hasObject("as_name")
                    && arg.matchesOnePrefix("as")) {
                scriptEntry.addObject("as_name", arg.asElement());
            }
            else if (!handled) {
                scriptEntry.addObject("list", arg.object instanceof ListTag ? (ListTag) arg.object : ListTag.valueOf(arg.raw_value));
                scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
                handled = true;
            }
            else if (arg.matchesOne("{")) {
                break;
            }
            else {
                arg.reportUnhandled();
            }

        }

        if (!handled) {
            throw new InvalidArgumentsException("Must specify a valid list or 'stop' or 'next'!");
        }

        scriptEntry.defaultObject("as_name", new ElementTag("value"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(ScriptEntry scriptEntry) {

        ElementTag stop = scriptEntry.getElement("stop");
        ElementTag next = scriptEntry.getElement("next");
        ElementTag callback = scriptEntry.getElement("callback");
        ListTag list = (ListTag) scriptEntry.getObject("list");
        ElementTag as_name = scriptEntry.getElement("as_name");

        if (stop != null && stop.asBoolean()) {
            // Report to dB
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), stop.debug());
            }
            boolean hasnext = false;
            for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equalsIgnoreCase("foreach") && args.size() > 0 && args.get(0).equalsIgnoreCase("\0CALLBACK")) {
                    hasnext = true;
                    break;
                }
            }
            if (hasnext) {
                while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                    ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                    List<String> args = entry.getOriginalArguments();
                    if (entry.getCommandName().equalsIgnoreCase("foreach") && args.size() > 0 && args.get(0).equalsIgnoreCase("\0CALLBACK")) {
                        scriptEntry.getResidingQueue().removeEntry(0);
                        break;
                    }
                    scriptEntry.getResidingQueue().removeEntry(0);
                }
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), "Cannot stop foreach: not in one!");
            }
            return;
        }
        else if (next != null && next.asBoolean()) {
            // Report to dB
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), next.debug());
            }
            boolean hasnext = false;
            for (int i = 0; i < scriptEntry.getResidingQueue().getQueueSize(); i++) {
                ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(i);
                List<String> args = entry.getOriginalArguments();
                if (entry.getCommandName().equalsIgnoreCase("foreach") && args.size() > 0 && args.get(0).equalsIgnoreCase("\0CALLBACK")) {
                    hasnext = true;
                    break;
                }
            }
            if (hasnext) {
                while (scriptEntry.getResidingQueue().getQueueSize() > 0) {
                    ScriptEntry entry = scriptEntry.getResidingQueue().getEntry(0);
                    List<String> args = entry.getOriginalArguments();
                    if (entry.getCommandName().equalsIgnoreCase("foreach") && args.size() > 0 && args.get(0).equalsIgnoreCase("\0CALLBACK")) {
                        break;
                    }
                    scriptEntry.getResidingQueue().removeEntry(0);
                }
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), "Cannot stop foreach: not in one!");
            }
            return;
        }
        else if (callback != null && callback.asBoolean()) {
            if (scriptEntry.getOwner() != null && (scriptEntry.getOwner().getCommandName().equalsIgnoreCase("foreach") ||
                    scriptEntry.getOwner().getBracedSet() == null || scriptEntry.getOwner().getBracedSet().size() == 0 ||
                    scriptEntry.getBracedSet().get(0).value.get(scriptEntry.getBracedSet().get(0).value.size() - 1) != scriptEntry)) {
                ForeachData data = (ForeachData) scriptEntry.getOwner().getData();
                data.index++;
                if (data.index <= data.list.size()) {
                    Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "Foreach loop " + data.index);
                    scriptEntry.getResidingQueue().addDefinition("loop_index", String.valueOf(data.index));
                    scriptEntry.getResidingQueue().addDefinition(as_name.asString(), String.valueOf(data.list.get(data.index - 1)));
                    List<ScriptEntry> bracedCommands = BracedCommand.getBracedCommands(scriptEntry.getOwner()).get(0).value;
                    ScriptEntry callbackEntry = new ScriptEntry("FOREACH", new String[]{"\0CALLBACK", "as:" + as_name.asString()},
                            (scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null));
                    callbackEntry.copyFrom(scriptEntry);
                    callbackEntry.setOwner(scriptEntry.getOwner());
                    bracedCommands.add(callbackEntry);
                    for (int i = 0; i < bracedCommands.size(); i++) {
                        bracedCommands.get(i).setInstant(true);
                    }
                    scriptEntry.getResidingQueue().injectEntries(bracedCommands, 0);
                }
                else {
                    Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "Foreach loop complete");
                }
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), "Foreach CALLBACK invalid: not a real callback!");
            }
        }

        else {

            // Get objects
            List<BracedData> bdlist = (List<BracedData>) scriptEntry.getObject("braces");
            if (bdlist == null || bdlist.isEmpty()) {
                Debug.echoError(scriptEntry.getResidingQueue(), "Empty braces (internal)!");
                return;
            }

            List<ScriptEntry> bracedCommandsList = bdlist.get(0).value;

            if (bracedCommandsList == null || bracedCommandsList.isEmpty()) {
                Debug.echoError(scriptEntry.getResidingQueue(), "Empty braces!");
                return;
            }

            // Report to dB
            if (scriptEntry.dbCallShouldDebug()) {
                Debug.report(scriptEntry, getName(), list.debug() + as_name.debug());
            }

            int target = list.size();
            if (target <= 0) {
                Debug.echoDebug(scriptEntry, "Empty list, not looping...");
                return;
            }
            ForeachData datum = new ForeachData();
            datum.list = list;
            datum.index = 1;
            scriptEntry.setData(datum);
            ScriptEntry callbackEntry = new ScriptEntry("FOREACH", new String[]{"\0CALLBACK", "as:" + as_name.asString()},
                    (scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null));
            callbackEntry.copyFrom(scriptEntry);
            callbackEntry.setOwner(scriptEntry);
            bracedCommandsList.add(callbackEntry);
            scriptEntry.getResidingQueue().addDefinition(as_name.asString(), list.get(0));
            scriptEntry.getResidingQueue().addDefinition("loop_index", "1");
            for (int i = 0; i < bracedCommandsList.size(); i++) {
                bracedCommandsList.get(i).setInstant(true);
            }
            scriptEntry.setInstant(true);
            scriptEntry.getResidingQueue().injectEntries(bracedCommandsList, 0);
        }
    }
}
