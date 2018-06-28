package net.aufdemrand.denizencore.scripts.commands;

import net.aufdemrand.denizencore.exceptions.ScriptEntryCreationException;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptBuilder;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public abstract class BracedCommand extends AbstractCommand {

    public static class BracedData {
        public String key;
        public List<String> args;
        public List<ScriptEntry> value;
        public int aStart, aEnd;
        public boolean needPatch;

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
     * @param scriptEntry The ScriptEntry to get the braced commands from.
     * @return The list of ScriptEntries to be executed in the command.
     */
    public static List<BracedData> getBracedCommands(ScriptEntry scriptEntry) {

        if (scriptEntry == null) {
            return null;
        }

        boolean hyperdebug = dB.verbose;

        // And a place to store all the final braces...
        List<BracedData> bracedSections = new ArrayList<BracedData>();

        List<BracedData> entryBracedSet = scriptEntry.getBracedSet();
        if (entryBracedSet != null) {
            List<BracedData> res = new ArrayList<BracedData>(entryBracedSet);
            try {
                for (int i = 0; i < res.size(); i++) {
                    BracedData newbd = new BracedData();
                    BracedData bd = res.get(i);
                    res.set(i, newbd);
                    newbd.key = bd.key;
                    newbd.value = new ArrayList<ScriptEntry>(bd.value.size());
                    for (ScriptEntry sEntry : bd.value) {
                        ScriptEntry newEntry = sEntry.clone();
                        newEntry.entryData.transferDataFrom(scriptEntry.entryData);
                        newbd.value.add(newEntry);
                    }
                    if (bd.needPatch) {
                        newbd.args = new ArrayList<String>(bd.args.size());
                        for (int x = bd.aStart; x <= bd.aEnd; x++) {
                            newbd.args.add(scriptEntry.args.get(x));
                        }
                        break;
                    }
                    else {
                        newbd.args = bd.args;
                    }
                }
            }
            catch (Exception e) {
                dB.echoError(scriptEntry.getResidingQueue(), e);
            }
            return res;
        }

        if (scriptEntry.getInsideList() != null) {
            List<Object> contents = scriptEntry.getInsideList();
            List<ScriptEntry> entries = ScriptBuilder.buildScriptEntries(contents,
                    scriptEntry.getScript() == null ? null : scriptEntry.getScript().getContainer(), scriptEntry.entryData);
            BracedData bd = new BracedData();
            bd.key = "base";
            bd.args = new ArrayList<String>();
            bd.value = entries;
            bracedSections.add(bd);
            scriptEntry.setBracedSet(bracedSections);
            return getBracedCommands(scriptEntry);
        }

        // We need a place to store the commands being built at...
        TreeMap<Integer, ArrayList<String>> commandList = new TreeMap<Integer, ArrayList<String>>();

        int bracesEntered = 0;
        boolean newCommand = true;
        boolean waitingForDash = false;

        // Inject the scriptEntry into the front of the queue, otherwise it doesn't exist
        //scriptEntry.getResidingQueue().injectEntry(scriptEntry, 0);
        // Send info to debug
        if (hyperdebug) {
            dB.echoDebug(scriptEntry, "Starting getBracedCommands...");
        }

        // If the specified amount of possible entries is less than the queue size, print that instead
        //if (hyperdebug) dB.echoDebug(scriptEntry, "...with queue size: " + scriptEntry.getResidingQueue().getQueueSize());
        if (hyperdebug) {
            dB.echoDebug(scriptEntry, "...with first command name: " + scriptEntry.getCommandName());
        }
        if (hyperdebug) {
            dB.echoDebug(scriptEntry, "...with first command arguments: " + scriptEntry.getArguments());
        }

        if (hyperdebug) {
            dB.echoDebug(scriptEntry, "Entry found: " + scriptEntry.getCommandName());
        }

        // Loop through the arguments of each entry
        List<String> argList = scriptEntry.getArguments();

        // Set the variable to use for naming braced command lists; the first should be the command name
        String bracesName = scriptEntry.getCommandName().toUpperCase();
        ArrayList<String> bracesArgs = new ArrayList<String>();
        bracesArgs.add(bracesName);

        int startArg = 0;
        for (int i = 0; i < argList.size(); i++) {
            String arg = argList.get(i);
            if (arg.equals("{")) {
                startArg = i;
                break;
            }
        }

        int tStart = -1;
        int tEnd = -1;
        boolean tPatchMe = false;

        for (int i = startArg; i < argList.size(); i++) {
            String arg = argList.get(i);
            if (hyperdebug) {
                dB.echoDebug(scriptEntry, "Arg found: " + arg);
            }

            // Listen for opened braces
            if (arg.equals("{")) {
                bracesEntered++;
                if (bracesEntered == 1 && bracedSections.size() != 0) {
                    tEnd = i - 1;
                }
                else {
                    tEnd = -1;
                }
                newCommand = false;
                waitingForDash = bracesEntered == 1;
                if (hyperdebug) {
                    dB.echoDebug(scriptEntry, "Opened brace; " + String.valueOf(bracesEntered) + " now");
                }
                if (bracesEntered > 1) {
                    commandList.get(commandList.lastKey()).add(arg);
                }
            }

            // Listen for closed braces
            else if (arg.equals("}")) {
                bracesEntered--;
                newCommand = false;
                if (hyperdebug) {
                    dB.echoDebug(scriptEntry, "Closed brace; " + String.valueOf(bracesEntered) + " now");
                }
                if (bracesEntered > 0) {
                    commandList.get(commandList.lastKey()).add(arg);
                }
                else {
                    BracedData bd = new BracedData();
                    bd.key = bracesName;
                    if (bracedSections.contains(bd)) {
                        dB.echoError(scriptEntry.getResidingQueue(), "You may not have braced commands with the same arguments.");
                        break;
                    }
                    ArrayList<ScriptEntry> bracesSection = new ArrayList<ScriptEntry>();
                    for (ArrayList<String> command : commandList.values()) {
                        try {
                            if (command.isEmpty()) {
                                if (hyperdebug) {
                                    dB.echoError(scriptEntry.getResidingQueue(), "Empty command?");
                                }
                                continue;
                            }
                            String cmd = command.get(0);
                            if (hyperdebug) {
                                dB.echoDebug(scriptEntry, "Calculating " + cmd);
                            }
                            command.remove(0);
                            String[] args = new String[command.size()];
                            args = command.toArray(args);
                            bracesSection.add(new ScriptEntry(cmd,
                                    args,
                                    scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null));
                            bracesSection.get(bracesSection.size() - 1).entryData.transferDataFrom(scriptEntry.entryData);
                            if (hyperdebug) {
                                dB.echoDebug(scriptEntry, "Command added: " + cmd + ", with " + String.valueOf(args.length) + " arguments");
                            }
                        }
                        catch (ScriptEntryCreationException e) {
                            dB.echoError(scriptEntry.getResidingQueue(), e.getMessage());
                        }
                    }
                    if (hyperdebug) {
                        dB.echoDebug(scriptEntry, "Adding section " + bracesName);
                    }
                    bd.args = bracesArgs;
                    bd.aStart = tStart;
                    bd.aEnd = tEnd;
                    bd.needPatch = tStart != -1 && tEnd != -1 && tPatchMe;
                    bd.value = bracesSection;
                    bracedSections.add(bd);
                    bracesName = "";
                    bracesArgs = new ArrayList<String>();
                    commandList = new TreeMap<Integer, ArrayList<String>>();
                    tEnd = -1;
                    tStart = i + 1;
                }
            }

            // Finish building a command
            else if (newCommand && bracesEntered == 1) {
                commandList.put(commandList.size(), new ArrayList<String>());
                commandList.get(commandList.lastKey()).add(arg);
                newCommand = false;
                if (hyperdebug) {
                    dB.echoDebug(scriptEntry, "Treating as new command");
                }
            }

            // Start building a command
            else if (arg.equals("-") && bracesEntered == 1) {
                newCommand = true;
                waitingForDash = false;
                if (hyperdebug) {
                    dB.echoDebug(scriptEntry, "Assuming following is a new command");
                }
            }

            // Add to the name of the braced command list
            else if (bracesEntered == 0) {
                bracesName += arg + " ";
                bracesArgs.add(arg);
                if (arg.indexOf('%') != -1) {
                    tPatchMe = true;
                }
            }

            // Continue building the current command
            else {
                if (waitingForDash) {
                    dB.echoError(scriptEntry.getResidingQueue(), "Malformed braced section! Missing a - symbol!");
                    break;
                }
                newCommand = false;
                commandList.get(commandList.lastKey()).add(arg);
                if (hyperdebug) {
                    dB.echoDebug(scriptEntry, "Adding to the command");
                }
            }
        }

        scriptEntry.setBracedSet(bracedSections);
        return getBracedCommands(scriptEntry);

    }
}
