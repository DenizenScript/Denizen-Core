package net.aufdemrand.denizencore.scripts.commands;

import java.util.*;

import net.aufdemrand.denizencore.exceptions.ScriptEntryCreationException;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public abstract class BracedCommand extends AbstractCommand {

    public static class BracedData {
        public String key;
        public List<String> args;
        public List<ScriptEntry> value;

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof BracedData) {
                return key.equals(((BracedData) o).key);
            }
            return false;
        }
    }

    /**
     * Gets the commands inside the braces of this ScriptEntry.
     *
     * @param scriptEntry
     *          The ScriptEntry to get the braced commands from.
     *
     * @return
     *          The list of ScriptEntries to be executed in the command.
     */
    public static List<BracedData> getBracedCommands(ScriptEntry scriptEntry) {

        boolean hyperdebug = dB.verbose;

        // And a place to store all the final braces...
        List<BracedData> bracedSections = new ArrayList<BracedData>();

        List<BracedData> entryBracedSet = scriptEntry.getBracedSet();
        if (entryBracedSet != null) {
            try {
                for (BracedData entry: entryBracedSet) {
                    ArrayList<ScriptEntry> array = new ArrayList<ScriptEntry>(entry.value.size());
                    for (ScriptEntry sEntry: entry.value) {
                        ScriptEntry newEntry = sEntry.clone();
                        // TODO: should this be cloning entryData?
                        newEntry.entryData.transferDataFrom(scriptEntry.entryData);
                        array.add(newEntry);
                    }
                    entry.value = array;
                    bracedSections.add(entry);
                }
                return bracedSections;
            }
            catch (CloneNotSupportedException e) {
                dB.echoError(scriptEntry.getResidingQueue(), e);
            }
        }

        // We need a place to store the commands being built at...
        TreeMap<Integer, ArrayList<String>> commandList = new TreeMap<Integer, ArrayList<String>>();

        int bracesEntered = 0;
        boolean newCommand = true;
        boolean waitingForDash = false;

        // Inject the scriptEntry into the front of the queue, otherwise it doesn't exist
        //scriptEntry.getResidingQueue().injectEntry(scriptEntry, 0);
        // Send info to debug
        if (hyperdebug) dB.echoDebug(scriptEntry, "Starting getBracedCommands...");

        // If the specified amount of possible entries is less than the queue size, print that instead
        //if (hyperdebug) dB.echoDebug(scriptEntry, "...with queue size: " + scriptEntry.getResidingQueue().getQueueSize());
        if (hyperdebug) dB.echoDebug(scriptEntry, "...with first command name: " + scriptEntry.getCommandName());
        if (hyperdebug) dB.echoDebug(scriptEntry, "...with first command arguments: " + scriptEntry.getArguments());

        ScriptEntry entry = scriptEntry;//scriptEntry.getResidingQueue().getEntry(0);
        if (hyperdebug) dB.echoDebug(scriptEntry, "Entry found: " + entry.getCommandName());

        // Loop through the arguments of each entry
        List<aH.Argument> argList = aH.interpret(entry.getArguments());

        // Set the variable to use for naming braced command lists; the first should be the command name
        String bracesName = scriptEntry.getCommandName().toUpperCase();
        ArrayList<String> bracesArgs = new ArrayList<String>();
        bracesArgs.add(bracesName);

        int startArg = 0;
        for (int i = 0; i < argList.size(); i++) {
            aH.Argument arg = argList.get(i);
            if (arg.asElement().asString().equals("{")) {
                startArg = i;
                break;
            }
        }

        for (int i = startArg; i < argList.size(); i++) {
            aH.Argument arg = argList.get(i);
            if (hyperdebug) dB.echoDebug(scriptEntry, "Arg found: " + arg.raw_value);

            // Listen for opened braces
            if (arg.matches("{")) {
                bracesEntered++;
                newCommand = false;
                waitingForDash = bracesEntered == 1;
                if (hyperdebug) dB.echoDebug(scriptEntry, "Opened brace; " + String.valueOf(bracesEntered) + " now");
                if (bracesEntered > 1) {
                    commandList.get(commandList.lastKey()).add(arg.raw_value);
                }
            }

            // Listen for closed braces
            else if (arg.matches("}")) {
                bracesEntered--;
                newCommand = false;
                if (hyperdebug) dB.echoDebug(scriptEntry, "Closed brace; " + String.valueOf(bracesEntered) + " now");
                if (bracesEntered > 0) {
                    commandList.get(commandList.lastKey()).add(arg.raw_value);
                }
                else {
                    BracedData temp = new BracedData();
                    temp.key = bracesName;
                    if (bracedSections.contains(temp)) {
                        dB.echoError(scriptEntry.getResidingQueue(), "You may not have braced commands with the same arguments.");
                        break;
                    }
                    ArrayList<ScriptEntry> bracesSection = new ArrayList<ScriptEntry>();
                    for (ArrayList<String> command : commandList.values()) {
                        try {
                            if (command.isEmpty()) {
                                if (hyperdebug) dB.echoError(scriptEntry.getResidingQueue(), "Empty command?");
                                continue;
                            }
                            String cmd = command.get(0);
                            if (hyperdebug) dB.echoDebug(scriptEntry, "Calculating " + cmd);
                            command.remove(0);
                            String[] args = new String[command.size()];
                            args = command.toArray(args);
                            bracesSection.add(new ScriptEntry(cmd,
                                    args,
                                    scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null));
                            bracesSection.get(bracesSection.size() - 1).entryData.transferDataFrom(scriptEntry.entryData);
                            if (hyperdebug) dB.echoDebug(scriptEntry, "Command added: " + cmd + ", with " + String.valueOf(args.length) + " arguments");
                        } catch (ScriptEntryCreationException e) {
                            if (hyperdebug) dB.echoError(scriptEntry.getResidingQueue(), e.getMessage());
                        }
                    }
                    if (hyperdebug) dB.echoDebug(scriptEntry, "Adding section " + bracesName);
                    BracedData bd = new BracedData();
                    bd.key = bracesName;
                    bd.args = bracesArgs;
                    bd.value = bracesSection;
                    bracedSections.add(bd);
                    bracesName = "";
                    bracesArgs = new ArrayList<String>();
                }
            }

            // Finish building a command
            else if (newCommand && bracesEntered == 1) {
                commandList.put(commandList.size(), new ArrayList<String>());
                commandList.get(commandList.lastKey()).add(arg.raw_value);
                newCommand = false;
                if (hyperdebug) dB.echoDebug(scriptEntry, "Treating as new command");
            }

            // Start building a command
            else if (arg.matches("-") && bracesEntered == 1) {
                newCommand = true;
                waitingForDash = false;
                if (hyperdebug) dB.echoDebug(scriptEntry, "Assuming following is a new command");
            }

            // Add to the name of the braced command list
            else if (bracesEntered == 0) {
                bracesName += arg.raw_value + " ";
                bracesArgs.add(arg.raw_value);
            }

            // Continue building the current command
            else {
                if (waitingForDash) {
                    dB.echoError(scriptEntry.getResidingQueue(), "Malformed braced section! Missing a - symbol!");
                    break;
                }
                newCommand = false;
                commandList.get(commandList.lastKey()).add(arg.raw_value);
                if (hyperdebug) dB.echoDebug(scriptEntry, "Adding to the command");
            }
        }

        scriptEntry.setBracedSet(bracedSections);
        return bracedSections;

    }
}
