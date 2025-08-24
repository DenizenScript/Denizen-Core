package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptFormattingContext;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.*;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.FormatScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.DebugSubmitter;

public class DebugCommand extends AbstractCommand implements Holdable {

    public static final String DEBUG_FORMAT_TYPE = ScriptFormattingContext.registerFormatType("debug");
    public static final String ERROR_FORMAT_TYPE = ScriptFormattingContext.registerFormatType("error");

    public DebugCommand() {
        setName("debug");
        setSyntax("debug (<type>) [<message>] (name:<name>) (format:<format>)");
        setRequiredArguments(1, 3);
        isProcedural = true;
        generateDebug = false;
        autoCompile();
    }

    // <--[command]
    // @Name Debug
    // @Syntax debug (<type>) [<message>] (name:<name>) (format:<format>)
    // @Required 1
    // @Maximum 3
    // @Short Shows a debug message.
    // @Group core
    // @Guide https://guide.denizenscript.com/guides/first-steps/problem-solving.html
    //
    // @Description
    // Use to quickly output debug information to console.
    //
    // Outputs plain text debug to the console by default, supporting the 'debug' format type (see <@link language Script Formats>).
    //
    // Alternatively, specify one of the following debug types:
    // DEBUG: standard hideable debug.
    // HEADER: standard hideable debug inside a header line.
    // FOOTER: a footer line.
    // SPACER: a spacer line.
    // LOG: global output, non-hideable.
    // APPROVAL: "Okay!" output, non-hideable.
    // ERROR: "Error!" output, non-hideable. Supports the 'error' format type, see <@link language Script Formats>.
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
        OUTPUT,
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
                                   @ArgName("type") @ArgDefaultText("output") DebugType dbType,
                                   @ArgPrefixed @ArgName("name") @ArgDefaultNull String name,
                                   @ArgName("format") @ArgPrefixed @ArgDefaultNull ScriptTag formatScript) {
        ScriptFormattingContext formattingContext = null;
        ScriptContainer scriptContainer = scriptEntry.getScriptContainer();
        if (formatScript != null) {
            if (!(formatScript.getContainer() instanceof FormatScriptContainer formatScriptContainer) || formatScriptContainer.getFormatTag() == null) {
                Debug.echoError("Invalid 'format:' script specified: must be a format script container.");
                return;
            }
            formattingContext = formatScriptContainer.getAsFormattingContext();
        }
        else if (scriptContainer != null) {
            formattingContext = scriptContainer.getFormattingContext();
        }
        if (name == null) {
            name = scriptContainer != null ? scriptContainer.getOriginalName() : "DebugCommand";
        }
        if (dbType != DebugType.RECORD) {
            scriptEntry.setFinished(true);
        }
        switch (dbType) {
            case OUTPUT -> Debug.echoDebug(null, formattingContext == null ? debug : formattingContext.format(DEBUG_FORMAT_TYPE, debug, scriptEntry));
            case DEBUG -> Debug.echoDebug(scriptEntry, debug);
            case HEADER -> Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, debug);
            case FOOTER -> Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer, debug);
            case SPACER -> Debug.echoDebug(scriptEntry, Debug.DebugElement.Spacer, debug);
            case LOG -> Debug.log(name, debug);
            case APPROVAL -> Debug.echoApproval(debug);
            case ERROR -> {
                String formatted = formattingContext != null ? formattingContext.formatOrNull(ERROR_FORMAT_TYPE, debug, scriptEntry) : null;
                if (formatted != null) {
                    Debug.echoDebug(null, formatted);
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
