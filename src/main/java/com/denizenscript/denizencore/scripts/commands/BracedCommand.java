package com.denizenscript.denizencore.scripts.commands;

import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.ScriptBuilder;
import com.denizenscript.denizencore.scripts.ScriptEntry;

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
        return getBracedCommands(scriptEntry, true);
    }

    public static List<BracedData> getBracedCommands(ScriptEntry scriptEntry, boolean duplicate) {
        if (scriptEntry == null) {
            return null;
        }

        boolean hyperdebug = Debug.verbose;

        // And a place to store all the final braces...
        List<BracedData> bracedSections = new ArrayList<>();

        List<BracedData> entryBracedSet = scriptEntry.getBracedSet();
        if (entryBracedSet != null) {
            if (!duplicate) {
                return entryBracedSet;
            }
            List<BracedData> res = new ArrayList<>(entryBracedSet);
            try {
                for (int i = 0; i < res.size(); i++) {
                    BracedData newbd = new BracedData();
                    BracedData bd = res.get(i);
                    res.set(i, newbd);
                    newbd.key = bd.key;
                    newbd.value = new ArrayList<>(bd.value.size());
                    newbd.needPatch = bd.needPatch;
                    for (ScriptEntry sEntry : bd.value) {
                        ScriptEntry newEntry = sEntry.clone();
                        newEntry.entryData.transferDataFrom(scriptEntry.entryData);
                        newEntry.entryData.scriptEntry = newEntry;
                        newbd.value.add(newEntry);
                    }
                    if (Debug.verbose) {
                        Debug.echoDebug(scriptEntry, "Wrangling braced command args[" + bd.needPatch + "]: " + bd.key);
                    }
                    if (bd.needPatch) {
                        newbd.args = new ArrayList<>(bd.args.size());
                        for (int x = bd.aStart; x <= bd.aEnd; x++) {
                            newbd.args.add(CommandExecuter.parseDefsRaw(scriptEntry, scriptEntry.args.get(x)));
                        }
                    }
                    else {
                        newbd.args = bd.args;
                    }
                }
            }
            catch (Exception e) {
                Debug.echoError(scriptEntry.getResidingQueue(), e);
            }
            return res;
        }

        if (scriptEntry.getInsideList() != null) {
            List<Object> contents = scriptEntry.getInsideList();
            List<ScriptEntry> entries = ScriptBuilder.buildScriptEntries(contents,
                    scriptEntry.getScript() == null ? null : scriptEntry.getScript().getContainer(), scriptEntry.entryData);
            BracedData bd = new BracedData();
            bd.key = "base";
            bd.args = new ArrayList<>();
            bd.value = entries;
            bracedSections.add(bd);
            scriptEntry.setBracedSet(bracedSections);
            return getBracedCommands(scriptEntry);
        }

        // We need a place to store the commands being built at...
        TreeMap<Integer, ArrayList<String>> commandList = new TreeMap<>();

        int bracesEntered = 0;
        boolean newCommand = true;
        boolean waitingForDash = false;

        // Inject the scriptEntry into the front of the queue, otherwise it doesn't exist
        //scriptEntry.getResidingQueue().injectEntry(scriptEntry, 0);
        // Send info to debug
        if (hyperdebug) {
            Debug.echoDebug(scriptEntry, "Starting getBracedCommands...");
        }

        // If the specified amount of possible entries is less than the queue size, print that instead
        //if (hyperdebug) dB.echoDebug(scriptEntry, "...with queue size: " + scriptEntry.getResidingQueue().getQueueSize());
        if (hyperdebug) {
            Debug.echoDebug(scriptEntry, "...with first command name: " + scriptEntry.getCommandName());
        }
        if (hyperdebug) {
            Debug.echoDebug(scriptEntry, "...with first command arguments: " + scriptEntry.getArguments());
        }

        if (hyperdebug) {
            Debug.echoDebug(scriptEntry, "Entry found: " + scriptEntry.getCommandName());
        }

        // Loop through the arguments of each entry
        List<String> argList = scriptEntry.getArguments();

        // Set the variable to use for naming braced command lists; the first should be the command name
        String bracesName = scriptEntry.getCommandName().toUpperCase();
        ArrayList<String> bracesArgs = new ArrayList<>();
        bracesArgs.add(bracesName);

        int startArg = -1;
        for (int i = 0; i < argList.size(); i++) {
            String arg = argList.get(i);
            if (arg.equals("{")) {
                startArg = i;
                break;
            }
        }

        if (startArg == -1) {
            return null;
        }

        Deprecations.oldBraceSyntax.warn(scriptEntry);

        int tStart = -1;
        int tEnd = -1;
        boolean tPatchMe = false;

        for (int i = startArg; i < argList.size(); i++) {
            String arg = argList.get(i);
            if (hyperdebug) {
                Debug.echoDebug(scriptEntry, "Arg found: " + arg);
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
                    Debug.echoDebug(scriptEntry, "Opened brace; " + String.valueOf(bracesEntered) + " now");
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
                    Debug.echoDebug(scriptEntry, "Closed brace; " + String.valueOf(bracesEntered) + " now");
                }
                if (bracesEntered > 0) {
                    commandList.get(commandList.lastKey()).add(arg);
                }
                else {
                    BracedData bd = new BracedData();
                    bd.key = bracesName;
                    if (bracedSections.contains(bd)) {
                        Debug.echoError(scriptEntry.getResidingQueue(), "You may not have braced commands with the same arguments.");
                        break;
                    }
                    ArrayList<ScriptEntry> bracesSection = new ArrayList<>();
                    for (ArrayList<String> command : commandList.values()) {
                        if (command.isEmpty()) {
                            if (hyperdebug) {
                                Debug.echoError(scriptEntry.getResidingQueue(), "Empty command?");
                            }
                            continue;
                        }
                        String cmd = command.get(0);
                        if (hyperdebug) {
                            Debug.echoDebug(scriptEntry, "Calculating " + cmd);
                        }
                        command.remove(0);
                        int lineNum = 1;
                        if (cmd.length() > 2 && cmd.charAt(0) == ScriptBuilder.LINE_PREFIX_CHAR && cmd.charAt(cmd.length() - 1) == ScriptBuilder.LINE_PREFIX_CHAR) {
                            lineNum = Integer.valueOf(cmd.substring(1, cmd.length() - 1));
                            cmd = command.get(0);
                            command.remove(0);
                        }
                        String[] args = new String[command.size()];
                        args = command.toArray(args);
                        ScriptEntry newEntry = new ScriptEntry(cmd, args, scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null);
                        newEntry.internal.lineNumber = lineNum;
                        newEntry.internal.originalLine = newEntry.toString();
                        bracesSection.add(newEntry);
                        bracesSection.get(bracesSection.size() - 1).entryData.transferDataFrom(scriptEntry.entryData);
                        if (hyperdebug) {
                            Debug.echoDebug(scriptEntry, "Command added: " + cmd + ", with " + String.valueOf(args.length) + " arguments");
                        }
                    }
                    if (hyperdebug) {
                        Debug.echoDebug(scriptEntry, "Adding section " + bracesName + " with " + tStart + " to " + tEnd);
                    }
                    bd.args = bracesArgs;
                    bd.aStart = tStart;
                    bd.aEnd = tEnd;
                    bd.needPatch = tStart != -1 && tEnd != -1 && tPatchMe;
                    bd.value = bracesSection;
                    bracedSections.add(bd);
                    bracesName = "";
                    bracesArgs = new ArrayList<>();
                    commandList = new TreeMap<>();
                    tEnd = -1;
                    tStart = i + 1;
                    tPatchMe = false;
                }
            }

            // Finish building a command
            else if (newCommand && bracesEntered == 1) {
                commandList.put(commandList.size(), new ArrayList<>());
                commandList.get(commandList.lastKey()).add(arg);
                newCommand = false;
                if (hyperdebug) {
                    Debug.echoDebug(scriptEntry, "Treating as new command");
                }
            }

            // Start building a command
            else if (arg.equals("-") && bracesEntered == 1) {
                newCommand = true;
                waitingForDash = false;
                if (hyperdebug) {
                    Debug.echoDebug(scriptEntry, "Assuming following is a new command");
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
                    Debug.echoError(scriptEntry.getResidingQueue(), "Malformed braced section! Missing a - symbol!");
                    break;
                }
                newCommand = false;
                commandList.get(commandList.lastKey()).add(arg);
                if (hyperdebug) {
                    Debug.echoDebug(scriptEntry, "Adding to the command");
                }
            }
        }

        scriptEntry.setBracedSet(bracedSections);
        return getBracedCommands(scriptEntry);

    }
}
