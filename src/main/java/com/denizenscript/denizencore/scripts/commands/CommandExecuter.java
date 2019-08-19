package com.denizenscript.denizencore.scripts.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandExecuter {

    private static final Pattern definition_pattern = Pattern.compile("%(.+?)%");

    public CommandExecuter() {
    }

    public static String parseDefsRaw(ScriptEntry scriptEntry, String arg) {
        if (!hasDef(arg)) {
            return arg;
        }
        if (scriptEntry.getResidingQueue() == null) {
            return arg;
        }
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
                Debug.echoError(scriptEntry.getResidingQueue(), "Unknown definition %" + m.group(1) + "%.");
                Debug.log("(Attempted: " + scriptEntry.toString() + ")");
                definition = "null";
            }
            Deprecations.ancientDefs.warn(scriptEntry.getResidingQueue());
            Debug.echoDebug(scriptEntry, "Filled definition %" + m.group(1) + "% with '" + definition + "'.");
            m.appendReplacement(sb, Matcher.quoteReplacement(definition));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static boolean hasDef(String arg) {
        return arg.indexOf('%') != -1;
    }

    public static boolean handleDefs(ScriptEntry scriptEntry, boolean genned) {
        if (scriptEntry.internal.hasOldDefs) {
            if (!genned) {
                scriptEntry.regenerateArgsCur();
                genned = true;
            }
            for (int argId : scriptEntry.internal.processArgs) {
                String arg = scriptEntry.args.get(argId);
                if (hasDef(arg)) {
                    String parsed = parseDefsRaw(scriptEntry, arg);
                    scriptEntry.setArgument(argId, parsed);
                    Argument aharg = new Argument(parsed);
                    Argument oldaharg = scriptEntry.aHArgs.get(argId);
                    aharg.needsFill = oldaharg.needsFill || oldaharg.hasSpecialPrefix;
                    aharg.hasSpecialPrefix = false;
                    scriptEntry.aHArgs.set(argId, aharg);
                    ScriptEntry.InternalArgument argse = scriptEntry.args_cur.get(argId);
                    argse.value = TagManager.dupChain(TagManager.genChain(parsed, scriptEntry));
                    argse.prefix = null;
                }
            }
        }
        return genned;
    }

    public static ScriptQueue currentQueue;

    public boolean execute(ScriptEntry scriptEntry) {
        if (scriptEntry.dbCallShouldDebug()) {
            StringBuilder output = new StringBuilder();
            output.append("<G>(line ").append(scriptEntry.internal.lineNumber).append(")<W> ");
            output.append(scriptEntry.getCommandName());
            if (scriptEntry.getOriginalArguments() == null) {
                Debug.echoError(scriptEntry.getResidingQueue(), "Original Arguments null for " + scriptEntry.getCommandName());
            }
            else {
                for (String arg : scriptEntry.getOriginalArguments()) {
                    output.append(" \"").append(arg).append("\"");
                }
            }
            DenizenCore.getImplementation().debugQueueExecute(scriptEntry, scriptEntry.getResidingQueue().debugId, output.toString());
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
            Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, "Executing command: " + scriptEntry.getCommandName());
            if (command == null) {
                Debug.echoError(scriptEntry.getResidingQueue(), scriptEntry.getCommandName() + " is an invalid command! Are you sure it loaded?");
            }
            else {
                Debug.echoError(scriptEntry.getResidingQueue(), scriptEntry.toString() + " cannot be executed! Is the number of arguments given correct?");
            }
            Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer);
            return false;
        }
        currentQueue = scriptEntry.getResidingQueue();
        String saveName = null;
        try {
            scriptEntry.generateAHArgs();
            boolean genned = false;
            if (scriptEntry.internal.actualCommand.shouldPreParse() && scriptEntry.internal.hasInstantTags) {
                scriptEntry.regenerateArgsCur();
                genned = true;
                TagManager.fillArgumentsObjects(scriptEntry.processed_arguments, scriptEntry.args,
                        scriptEntry.args_cur, scriptEntry.aHArgs, true,
                        DenizenCore.getImplementation().getTagContextFor(scriptEntry, true), scriptEntry.internal.processArgs);
            }
            genned = handleDefs(scriptEntry, genned);
            for (Argument arg : scriptEntry.internal.preprocArgs) {
                if (DenizenCore.getImplementation().handleCustomArgs(scriptEntry, arg, false)) {
                    // Do nothing
                }
                else if (arg.matchesOnePrefix("save")) {
                    saveName = TagManager.tag(arg.getValue(), DenizenCore.getImplementation().getTagContext(scriptEntry));
                    Debug.echoDebug(scriptEntry, "...remembering this script entry as '" + saveName + "'!");
                }
            }
            if (scriptEntry.internal.actualCommand.shouldPreParse()) {
                TagManager.fillArgumentsObjects(scriptEntry.processed_arguments, scriptEntry.args,
                        genned ? scriptEntry.args_cur : scriptEntry.internal.args_ref, scriptEntry.aHArgs, false,
                        DenizenCore.getImplementation().getTagContextFor(scriptEntry, false), scriptEntry.internal.processArgs);
                // TODO: Fix this weird interpreter efficiency hack (remove string dependence)
                ArgumentHelper.specialInterpretTrickStrings = scriptEntry.args;
                ArgumentHelper.specialInterpretTrickObjects = scriptEntry.aHArgs;
            }
            command.parseArgs(scriptEntry);
        }
        catch (InvalidArgumentsException e) {
            // Give usage hint if InvalidArgumentsException was called.
            Debug.echoError(scriptEntry.getResidingQueue(), "Woah! Invalid arguments were specified!");
            if (e.getMessage() != null && e.getMessage().length() > 0) {
                Debug.log("+> MESSAGE follows: " + "'" + e.getMessage() + "'");
            }
            Debug.log("Usage: " + command.getUsageHint());
            Debug.log("(Attempted: " + scriptEntry.toString() + ")");
            Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer);
            scriptEntry.setFinished(true);
            currentQueue = null;
            return false;
        }
        catch (Exception e) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Woah! An exception has been called with this command (while preparing it)!");
            Debug.echoError(scriptEntry.getResidingQueue(), e);
            Debug.log("(Attempted: " + scriptEntry.toString() + ")");
            Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer);
            scriptEntry.setFinished(true);
            currentQueue = null;
            return false;
        }
        try {
            command.execute(scriptEntry);
            if (saveName != null) {
                scriptEntry.getResidingQueue().holdScriptEntry(saveName, scriptEntry);
            }
            currentQueue = null;
            return true;
        }
        catch (Exception e) {
            Debug.echoError(scriptEntry.getResidingQueue(), "Woah!! An exception has been called with this command (while executing it)!");
            Debug.echoError(scriptEntry.getResidingQueue(), e);
            Debug.log("(Attempted: " + scriptEntry.toString() + ")");
            Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer);
            scriptEntry.setFinished(true);
            currentQueue = null;
            return false;
        }
    }
}
