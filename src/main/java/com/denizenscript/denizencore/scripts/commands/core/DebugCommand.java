package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;

import java.util.HashSet;

public class DebugCommand extends AbstractCommand implements Holdable {

    public DebugCommand() {
        setName("debug");
        setSyntax("debug [<type>] [<message>] (name:<name>)");
        setRequiredArguments(2, 3);
        isProcedural = true;
        setPrefixesHandled("name");
    }

    // <--[command]
    // @Name Debug
    // @Syntax debug [<type>] [<message>] (name:<name>)
    // @Required 2
    // @Maximum 3
    // @Short Shows a debug message.
    // @Group core
    // @Guide https://guide.denizenscript.com/guides/first-steps/problem-solving.html
    //
    // @Description
    // Use to quickly output debug information to console.
    //
    // Valid types include:
    // DEBUG: standard hideable debug.
    // HEADER: standard hideable debug inside a header line.
    // FOOTER: a footer line.
    // SPACER: a spacer line.
    // LOG: global output, non-hideable.
    // APPROVAL: "Okay!" output, non-hideable.
    // ERROR: "Error!" output, non-hideable.
    // REPORT: normally used to describe the arguments of a command, requires a name, hideable.
    // EXCEPTION: outputs a full java stacktrace.
    // RECORD: Use message 'start' to start recording, 'submit' to submit a recording, or 'cancel' to cancel a recording.
    //
    // TODO: Should [<type>] be required? Perhaps default to 'debug' mode?
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
    // - debug debug "Time is currently <[milliseconds].div[1000].round> seconds!"
    //
    // @Usage
    // Use to record a debug log of a certain script.
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

    public static HashSet<String> DBINFO = Argument.precalcEnum(DebugType.values());

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("type")
                    && arg.limitToOnlyPrefix("type")
                    && arg.matchesEnum(DBINFO)) {
                scriptEntry.addObject("type", arg.asElement());
            }
            else if (!scriptEntry.hasObject("debug")) {
                scriptEntry.addObject("debug", arg.getRawElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("type") || !scriptEntry.hasObject("debug")) {
            throw new InvalidArgumentsException("Must specify a debug type and message!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag debug = scriptEntry.getElement("debug");
        ElementTag type = scriptEntry.getElement("type");
        ElementTag name = scriptEntry.argForPrefixAsElement("name", "name");

        // Intentionally do not DB REPORT - we're making our own debug output!

        DebugType dbType = DebugType.valueOf(type.asString().toUpperCase());
        if (dbType != DebugType.RECORD) {
            scriptEntry.setFinished(true);
        }
        switch (dbType) {
            case DEBUG:
                Debug.echoDebug(scriptEntry, debug.asString());
                break;
            case HEADER:
                Debug.echoDebug(scriptEntry, Debug.DebugElement.Header, debug.asString());
                break;
            case FOOTER:
                Debug.echoDebug(scriptEntry, Debug.DebugElement.Footer, debug.asString());
                break;
            case SPACER:
                Debug.echoDebug(scriptEntry, Debug.DebugElement.Spacer, debug.asString());
                break;
            case LOG:
                Debug.log(debug.asString());
                break;
            case APPROVAL:
                Debug.echoApproval(debug.asString());
                break;
            case ERROR:
                Debug.echoError(scriptEntry.getResidingQueue(), debug.asString());
                break;
            case REPORT:
                if (scriptEntry.dbCallShouldDebug()) {
                    Debug.report(scriptEntry, name.asString(), debug.asString());
                }
                break;
            case EXCEPTION:
                Debug.echoError(scriptEntry.getResidingQueue(), new RuntimeException(debug.asString()));
                break;
            case RECORD:
                String form = CoreUtilities.toLowerCase(debug.asString());
                switch (form) {
                    case "start":
                        Debug.echoDebug(scriptEntry, "Starting debug recording...");
                        DenizenCore.implementation.startRecording();
                        scriptEntry.setFinished(true);
                        break;
                    case "cancel":
                        Debug.echoDebug(scriptEntry, "Stopping debug recording...");
                        DenizenCore.implementation.stopRecording();
                        scriptEntry.setFinished(true);
                        break;
                    case "submit":
                        DenizenCore.implementation.submitRecording(s -> {
                            if (s == null) {
                                Debug.echoDebug(scriptEntry, "Submit failed.");
                            }
                            else if (s.equals("disabled")) {
                                Debug.echoDebug(scriptEntry, "Submit failed: not recording");
                            }
                            else {
                                Debug.echoDebug(scriptEntry, "Submitted to " + s);
                                scriptEntry.addObject("submitted", new ElementTag(s));
                            }
                            scriptEntry.setFinished(true);
                        });
                        break;
                    default:
                        Debug.echoError("Debug 'record' command failed: unknown record form '" + form + "'");
                        scriptEntry.setFinished(true);
                        break;
                }
                break;
        }
    }
}
