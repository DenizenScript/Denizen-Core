package net.aufdemrand.denizencore.scripts.commands;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.debugging.dB.DebugElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandExecuter {

    private static final Pattern definition_pattern = Pattern.compile("%(.+?)%");

    public static List<String> parseDefs(List<String> args, ScriptEntry scriptEntry) {
        Matcher m;
        StringBuffer sb;
        List<String> newArgs = new ArrayList<String>(args.size());
        for (String arg : args) {
            if (arg.indexOf('%') != -1) {
                m = definition_pattern.matcher(arg);
                sb = new StringBuffer();
                while (m.find()) {
                    String def = m.group(1);
                    boolean dynamic = false;
                    if (def.startsWith("|")) {
                        def = def.substring(1, def.length() - 1);
                        dynamic = true;
                    }
                    String definition;
                    String defval = scriptEntry.getResidingQueue().getDefinition(def);
                    if (dynamic) {
                        definition = scriptEntry.getResidingQueue().getDefinition(def);
                    }
                    else {
                        definition = TagManager.escapeOutput(scriptEntry.getResidingQueue().getDefinition(def));
                    }
                    if (defval == null) {
                        dB.echoError("Unknown definition %" + m.group(1) + "%.");
                        definition = "null";
                    }
                    dB.echoDebug(scriptEntry, "Filled definition %" + m.group(1) + "% with '" + definition + "'.");
                    m.appendReplacement(sb, Matcher.quoteReplacement(definition));
                }
                m.appendTail(sb);
                arg = sb.toString();
            }
            newArgs.add(arg);
        }
        return newArgs;
    }

    public CommandExecuter() {
    }

    /*
     * Executes a command defined in scriptEntry
     */

    public boolean execute(ScriptEntry scriptEntry) {
        if (DenizenCore.getImplementation().shouldDebug(scriptEntry)) {
            StringBuilder output = new StringBuilder();
            output.append(scriptEntry.getCommandName());
            if (scriptEntry.getOriginalArguments() == null) {
                dB.echoError(scriptEntry.getResidingQueue(), "Original Arguments null for " + scriptEntry.getCommandName());
            }
            else {
                for (String arg : scriptEntry.getOriginalArguments()) {
                    output.append(" \"").append(arg).append("\"");
                }
            }
            DenizenCore.getImplementation().debugQueueExecute(scriptEntry, scriptEntry.getResidingQueue().id, output.toString());
            DenizenCore.getImplementation().debugCommandHeader(scriptEntry);
        }
        AbstractCommand command = scriptEntry.internal.actualCommand;
        if (command == null) {
            command = DenizenCore.getCommandRegistry().get(scriptEntry.internal.command);
            scriptEntry.internal.actualCommand = command;
            if (command == null || command.getOptions().REQUIRED_ARGS > scriptEntry.getArguments().size()) {
                scriptEntry.broken = true;
            }
        }
        if (scriptEntry.broken) {
            dB.echoDebug(scriptEntry, DebugElement.Header, "Executing command: " + scriptEntry.getCommandName());
            if (command == null) {
                dB.echoError(scriptEntry.getResidingQueue(), scriptEntry.getCommandName() + " is an invalid dCommand! Are you sure it loaded?");
            }
            else {
                dB.echoError(scriptEntry.getResidingQueue(), scriptEntry.toString() + " cannot be executed! Is the number of arguments given correct?");
            }
            dB.echoDebug(scriptEntry, DebugElement.Footer);
            return false;
        }
        String saveName = null;
        try {
            scriptEntry.generateAHArgs();
            if (scriptEntry.internal.actualCommand.shouldPreParse() && scriptEntry.internal.hasInstantTags) {
                scriptEntry.regenerateArgsCur();
                scriptEntry.setArgumentsObjects(TagManager.fillArgumentsObjects(scriptEntry.processed_arguments,
                        scriptEntry.args_cur, scriptEntry.aHArgs, true,
                        DenizenCore.getImplementation().getTagContextFor(scriptEntry, true), scriptEntry.internal.processArgs));
            }
            if (scriptEntry.internal.hasOldDefs) {
                for (int argId : scriptEntry.internal.processArgs) {
                    String arg = scriptEntry.args.get(argId);
                    if (arg.indexOf('%') != -1) {
                        Matcher m;
                        StringBuffer sb;
                        m = definition_pattern.matcher(arg);
                        sb = new StringBuffer();
                        while (m.find()) {
                            String def = m.group(1);
                            boolean dynamic = false;
                            if (def.startsWith("|")) {
                                def = def.substring(1, def.length() - 1);
                                dynamic = true;
                            }
                            String definition;
                            String defval = scriptEntry.getResidingQueue().getDefinition(def);
                            if (dynamic) {
                                definition = scriptEntry.getResidingQueue().getDefinition(def);
                            }
                            else {
                                definition = TagManager.escapeOutput(scriptEntry.getResidingQueue().getDefinition(def));
                            }
                            if (defval == null) {
                                dB.echoError(scriptEntry.getResidingQueue(), "Unknown definition %" + m.group(1) + "%.");
                                dB.log("(Attempted: " + scriptEntry.toString() + ")");
                                definition = "null";
                            }
                            dB.echoDebug(scriptEntry, "Filled definition %" + m.group(1) + "% with '" + definition + "'.");
                            m.appendReplacement(sb, Matcher.quoteReplacement(definition));
                        }
                        m.appendTail(sb);
                        scriptEntry.setArgument(argId, sb.toString());
                    }
                }
            }
            for (aH.Argument arg : scriptEntry.internal.preprocArgs) {
                if (DenizenCore.getImplementation().handleCustomArgs(scriptEntry, arg, false)) {
                    // Do nothing
                }
                else if (arg.matchesOnePrefix("save")) {
                    saveName = TagManager.tag(arg.getValue(), DenizenCore.getImplementation().getTagContext(scriptEntry));
                    dB.echoDebug(scriptEntry, "...remembering this script entry as '" + saveName + "'!");
                }
            }
            if (scriptEntry.internal.actualCommand.shouldPreParse()) {
                scriptEntry.setArgumentsObjects(TagManager.fillArgumentsObjects(scriptEntry.processed_arguments,
                        scriptEntry.internal.hasInstantTags ? scriptEntry.args_cur : scriptEntry.internal.args_ref, scriptEntry.aHArgs, false,
                        DenizenCore.getImplementation().getTagContextFor(scriptEntry, false), scriptEntry.internal.processArgs));
                // TODO: Fix this weird interpreter efficiency hack (remove string dependence)
                aH.specialInterpretTrickStrings = scriptEntry.args;
                aH.specialInterpretTrickObjects = scriptEntry.aHArgs;
            }
            command.parseArgs(scriptEntry);
        }
        catch (InvalidArgumentsException e) {
            // Give usage hint if InvalidArgumentsException was called.
            dB.echoError(scriptEntry.getResidingQueue(), "Woah! Invalid arguments were specified!");
            if (e.getMessage() != null && e.getMessage().length() > 0) {
                dB.log("+> MESSAGE follows: " + "'" + e.getMessage() + "'");
            }
            dB.log("Usage: " + command.getUsageHint());
            dB.log("(Attempted: " + scriptEntry.toString() + ")");
            dB.echoDebug(scriptEntry, DebugElement.Footer);
            scriptEntry.setFinished(true);
            return false;
        }
        catch (Exception e) {
            dB.echoError(scriptEntry.getResidingQueue(), "Woah! An exception has been called with this command (while preparing it)!");
            dB.echoError(scriptEntry.getResidingQueue(), e);
            dB.log("(Attempted: " + scriptEntry.toString() + ")");
            dB.echoDebug(scriptEntry, DebugElement.Footer);
            scriptEntry.setFinished(true);
            return false;
        }
        try {
            command.execute(scriptEntry);
            if (saveName != null) {
                scriptEntry.getResidingQueue().holdScriptEntry(saveName, scriptEntry);
            }
            return true;
        }
        catch (Exception e) {
            dB.echoError(scriptEntry.getResidingQueue(), "Woah!! An exception has been called with this command (while executing it)!");
            dB.echoError(scriptEntry.getResidingQueue(), e);
            dB.log("(Attempted: " + scriptEntry.toString() + ")");
            dB.echoDebug(scriptEntry, DebugElement.Footer);
            scriptEntry.setFinished(true);
            return false;
        }
    }
}
