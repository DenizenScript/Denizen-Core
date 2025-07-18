package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptLoggingContext;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.FormatScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.debugging.DebugSubmitter;

public class DebugCommand extends AbstractCommand implements Holdable {

    public DebugCommand() {
        setName("debug");
        setSyntax("debug (<type>/{debug}) [<message>] (name:<name>) (format:<format>)");
        setRequiredArguments(1, 3);
        isProcedural = true;
        generateDebug = false;
        autoCompile();
    }

    // <--[command]
    // @Name Debug
    // @Syntax debug (<type>/{debug}) [<message>] (name:<name>) (format:<format>)
    // @Required 1
    // @Maximum 3
    // @Short Shows a debug message.
    // @Group core
    // @Guide https://guide.denizenscript.com/guides/first-steps/problem-solving.html
    //
    // @Description
    // Use to quickly output debug information to console.
    //
    // Valid types include:
    // DEBUG: standard hideable debug, supports <@link language Script Logging Formats> (in which case it isn't hideable).
    // HEADER: standard hideable debug inside a header line.
    // FOOTER: a footer line.
    // SPACER: a spacer line.
    // LOG: global output, non-hideable.
    // APPROVAL: "Okay!" output, non-hideable.
    // ERROR: "Error!" output, non-hideable. Supports <@link language Script Logging Formats>.
    // REPORT: normally used to describe the arguments of a command, requires a name, hideable.
    // EXCEPTION: outputs a full java stacktrace.
    // RECORD: Use message 'start' to start recording, 'submit' to submit a recording, or 'cancel' to cancel a recording.
    //
    // @Tags
    // <entry[saveName].submitted> returns the submit link (if any).
    //
    // @Usage
    // Use to show an error.
    // - debug error "Something went wrong!"
    //
    // @Usage
    // Use to add some information to help your own ability to read debug output from you script.
    // - debug "Time is currently <[milliseconds].div[1000].round> seconds!"
    //
    // @Usage
    // Use to record a debug log of a certain script.
    // # NOTICE: Spamming debug recordings to the official Denizen Paste instance will result in you being blocked from the paste service.
    // - debug record start
    // - run myscript
    // - ~debug record submit save:mylog
    // - narrate "Recorded log as <entry[mylog].submitted||<red>FAILED>"
    //
    // -->

    public enum DebugType {
        DEBUG,
        HEADER,
        FOOTER,
        SPACER,
        LOG,
        APPROVAL,
        ERROR,
        REPORT,
        EXCEPTION,
        RECORD
    }

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.add(DebugType.values());
        tab.add("start", "submit", "cancel");
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgRaw @ArgLinear @ArgName("debug") String debug,
                                   @ArgName("type") @ArgDefaultText("debug") DebugType dbType,
                                   @ArgPrefixed @ArgName("name") @ArgDefaultText("name") String name,
                                   @ArgName("format") @ArgPrefixed @ArgDefaultNull ScriptTag formatScript) {
        ScriptLoggingContext loggingContext = null;
        ScriptContainer scriptContainer = scriptEntry.getScriptContainer();
        if (formatScript != null) {
            if (!(formatScript.getContainer() instanceof FormatScriptContainer formatScriptContainer) || formatScriptContainer.getFormatTag() == null) {
                Debug.echoError("Invalid 'format:' script specified: must be a format script container.");
                return;
            }
            loggingContext = formatScriptContainer.getAsLoggingContext();
        }
        else if (scriptContainer != null) {
            loggingContext = scriptContainer.getLoggingContext();
        }
        if (dbType != DebugType.RECORD) {
            scriptEntry.setFinished(true);
        }
        switch (dbType) {
            case DEBUG -> {
                if (loggingContext != null && loggingContext.hasDebugFormat()) {
                    Debug.echoDebug(null, loggingContext.formatDebug(debug, scriptEntry));
                }
                else {
                    Debug.echoDebug(scriptEntry, debug);
                }
            }
            case HEADER -> Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, debug);
            case FOOTER -> Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer, debug);
            case SPACER -> Debug.echoDebug(scriptEntry, Debug.DebugElement.Spacer, debug);
            case LOG -> Debug.log(name != null ? name : (scriptContainer != null ? scriptContainer.getOriginalName() : scriptEntry.getCommandName()), debug);
            case APPROVAL -> Debug.echoApproval(debug);
            case ERROR -> {
                if (loggingContext != null && loggingContext.hasErrorFormat()) {
                    Debug.echoDebug(null, loggingContext.formatError(debug, scriptEntry));
                }
                else {
                    Debug.echoError(scriptEntry, debug);
                }
            }
            case REPORT -> {
                if (scriptEntry.dbCallShouldDebug()) {
                    Debug.report(scriptEntry, name, debug);
                }
            }
            case EXCEPTION -> Debug.echoError(scriptEntry, new RuntimeException(debug));
            case RECORD -> {
                String form = CoreUtilities.toLowerCase(debug);
                switch (form) {
                    case "start":
                        Debug.echoDebug(scriptEntry, "Starting debug recording...");
                        Debug.startRecording();
                        scriptEntry.setFinished(true);
                        break;
                    case "cancel":
                        Debug.echoDebug(scriptEntry, "Stopping debug recording...");
                        Debug.stopRecording();
                        scriptEntry.setFinished(true);
                        break;
                    case "submit":
                        DebugSubmitter.submitCurrentRecording(s -> {
                            if (s == null) {
                                Debug.echoDebug(scriptEntry, "Submit failed.");
                            }
                            else if (s.equals("disabled")) {
                                Debug.echoDebug(scriptEntry, "Submit failed: not recording");
                            }
                            else {
                                Debug.echoDebug(scriptEntry, "Submitted to " + s);
                                scriptEntry.saveObject("submitted", new ElementTag(s));
                            }
                            scriptEntry.setFinished(true);
                        });
                        break;
                    default:
                        Debug.echoError("Debug 'record' command failed: unknown record form '" + form + "'");
                        scriptEntry.setFinished(true);
                        break;
                }
            }
        }
    }
}
