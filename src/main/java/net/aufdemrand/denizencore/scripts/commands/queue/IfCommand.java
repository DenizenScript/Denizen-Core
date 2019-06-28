package net.aufdemrand.denizencore.scripts.commands.queue;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.BracedCommand;
import net.aufdemrand.denizencore.scripts.commands.CommandExecuter;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.ArrayList;
import java.util.List;

public class IfCommand extends BracedCommand {

    // <--[command]
    // @Name If
    // @Syntax if [<value>] (!)(<operator> <value>) (&&/|| ...) [<commands>] (else <commands>)
    // @Required 1
    // @Short Compares values, and runs one script if they match, or a different script if they don't match.
    // @Group queue
    // @Video /denizen/vids/Alternate/Dynamic%20Actions:%20The%20If%20Command
    //
    // @Description
    // TODO: Document Command Details
    //
    // @Tags
    // <el@element.is[<operator>].to[<element>]>
    // <el@element.is[<operator>].than[<element>]>
    //
    // @Usage
    // TODO: Document Command Details
    // -->

    @Override
    public void onEnable() {
        setBraced();
        setParseArgs(false);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        boolean in_subcommand = false;

        boolean in_elsecommand = false;

        List<String> subcommand = new ArrayList<>();

        List<String> elsecommand = new ArrayList<>();

        List<String> comparisons = new ArrayList<>();

        boolean has_brace = scriptEntry.getInsideList() != null;
        if (has_brace) {
            List<BracedData> allData = new ArrayList<>();
            BracedData ifRef = getBracedCommands(scriptEntry).get(0);
            ifRef.key = scriptEntry.toString();
            ifRef.args = new ArrayList<>();
            ifRef.args.add("if");
            ifRef.args.addAll(scriptEntry.getOriginalArguments());
            allData.add(ifRef);
            while (scriptEntry.getResidingQueue().script_entries.size() > 0) {
                ScriptEntry nextEntry = scriptEntry.getResidingQueue().script_entries.get(0);
                if (!(nextEntry.getCommand() instanceof ElseCommand)) {
                    break;
                }
                if (nextEntry.getInsideList() == null) {
                    dB.echoError(scriptEntry.getResidingQueue(), "Upcoming else command is mis-formatted!");
                    break;
                }
                if (nextEntry.internal.hasOldDefs) {
                    nextEntry.generateAHArgs();
                    CommandExecuter.handleDefs(nextEntry, false);
                }
                scriptEntry.getResidingQueue().script_entries.remove(0);
                BracedData elseRef = getBracedCommands(nextEntry).get(0);
                elseRef.key = nextEntry.toString();
                elseRef.args = new ArrayList<>();
                elseRef.args.add("else");
                elseRef.args.addAll(nextEntry.getArguments());
                allData.add(elseRef);
            }
            scriptEntry.addObject("braces", allData);
        }
        else {
            for (String arg : scriptEntry.getArguments()) {
                if (arg.equalsIgnoreCase("{")) {
                    if (dB.verbose) {
                        dB.log("Has_brace = true");
                    }
                    has_brace = true;
                    break;
                }
            }
            if (has_brace) {
                scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
            }
        }

        // Interpret arguments
        for (String arg : scriptEntry.getArguments()) {
            if (arg.equalsIgnoreCase("{")) {
                break;
            }

            if (!has_brace && in_subcommand && arg.equalsIgnoreCase("else")) {
                in_elsecommand = true;
                in_subcommand = false;
            }
            else if (!has_brace && !in_elsecommand && DenizenCore.getCommandRegistry().get(arg.toUpperCase()) != null) {
                in_subcommand = true;
                subcommand.add(arg);
            }
            else if (!has_brace && in_subcommand) {
                subcommand.add(arg);
            }
            else if (!has_brace && in_elsecommand) {
                elsecommand.add(arg);
            }
            else {
                comparisons.add(arg);
            }
        }

        if (!has_brace && in_elsecommand) {
            scriptEntry.addObject("elsecommand", elsecommand);
        }
        if (!has_brace && (in_subcommand || in_elsecommand)) {
            scriptEntry.addObject("subcommand", subcommand);
        }
        scriptEntry.addObject("comparisons", comparisons);
    }


    @Override
    public void execute(ScriptEntry scriptEntry) {

        List<String> subcommand = (List<String>) scriptEntry.getObject("subcommand");
        List<String> elsecommand = (List<String>) scriptEntry.getObject("elsecommand");
        List<String> comparisons = (List<String>) scriptEntry.getObject("comparisons");
        List<BracedData> braces = (List<BracedData>) scriptEntry.getObject("braces");

        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), aH.debugObj("use_braces", braces != null));
        }

        if (dB.verbose) {
            dB.log("comparisons=" + comparisons + ", sc:" + subcommand + ", ec:" + elsecommand);
        }

        boolean first_set = new ArgComparer().compare(comparisons, scriptEntry);
        if (first_set && subcommand != null && subcommand.size() > 0) {
            executeCommandList(subcommand, scriptEntry);
            return;
        }
        if (!first_set && elsecommand != null && elsecommand.size() > 0) {
            executeCommandList(elsecommand, scriptEntry);
            return;
        }
        if (braces != null) {
            if (braces.isEmpty()) {
                dB.echoError(scriptEntry.getResidingQueue(), "Failed to parse IF command: mis-aligned bracing, empty subsections, or other basic formatting error.");
                return;
            }
            if (first_set) {
                if (dB.verbose) {
                    dB.log("Running the first set");
                }
                dB.echoDebug(scriptEntry, "<Y>If command passed, running block.");
                scriptEntry.setInstant(true);
                List<ScriptEntry> bracedCommandsList = braces.get(0).value;
                for (int i = 0; i < bracedCommandsList.size(); i++) {
                    bracedCommandsList.get(i).setInstant(true);
                }
                scriptEntry.getResidingQueue().injectEntries(bracedCommandsList, 0);
                return;
            }
            else {
                for (int z = 1; z < braces.size(); z++) {
                    BracedData braceSet = braces.get(z);
                    if (dB.verbose) {
                        dB.log("Trying: " + braceSet.key);
                    }
                    List<String> key = braceSet.args;
                    boolean should_fire = false;
                    int x = 0;
                    if (key.size() > x && key.get(x).equalsIgnoreCase("else")) {
                        x++;
                    }
                    else {
                        dB.echoError("If command has argument '" + key.get(x) + "' which is unknown.");
                    }
                    if (key.size() > x && key.get(x).equalsIgnoreCase("if")) {
                        x++;
                    }
                    else {
                        should_fire = true;
                    }
                    if (!should_fire) {
                        if (new ArgComparer().compare(key.subList(x, key.size()), scriptEntry)) {
                            should_fire = true;
                        }
                    }
                    if (should_fire) {
                        if (key.size() == 1 && key.get(0).equals("else")) {
                            dB.echoDebug(scriptEntry, "<Y>No part of the if command passed, running ELSE block.");
                        }
                        else {
                            dB.echoDebug(scriptEntry, "<Y>If sub-command " + z + " passed, running block.");
                        }
                        scriptEntry.setInstant(true);
                        List<ScriptEntry> bracedCommandsList = braceSet.value;
                        for (int i = 0; i < bracedCommandsList.size(); i++) {
                            bracedCommandsList.get(i).setInstant(true);
                        }
                        scriptEntry.getResidingQueue().injectEntries(bracedCommandsList, 0);
                        return;
                    }
                }
            }
        }
        dB.echoDebug(scriptEntry, "<Y>No part of the if command passed, no block will run.");
    }

    public void executeCommandList(List<String> subcommand, ScriptEntry scriptEntry) {
        try {
            scriptEntry.setInstant(true);
            String cmd = subcommand.get(0);
            subcommand.remove(0);
            ScriptEntry entry = new ScriptEntry(cmd, subcommand.toArray(new String[subcommand.size()]),
                    scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null);
            entry.entryData = scriptEntry.entryData.clone();
            entry.setInstant(true);
            scriptEntry.getResidingQueue().injectEntry(entry, 0);
        }
        catch (Exception e) {
            dB.echoError(e);
        }
    }

    public static class ArgComparer {

        List argstemp = null;

        ArgInternal[] argstemp_parsed = null;

        ScriptEntry scriptEntry = null;

        Boolean result = null;

        boolean flip = false;

        @Override
        public String toString() {
            return "[ArgComp: " + argstemp + " res " + result + "]";
        }

        public static class ArgInternal {

            boolean negative;

            String value;

            boolean boolify() {
                return negative != value.equals("true");
            }

            @Override
            public String toString() {
                return negative ? "!" + value : value;
            }
        }

        public static boolean boolify(String arg) {
            if (arg.startsWith("!")) {
                return !arg.equalsIgnoreCase("!true");
            }
            return arg.equalsIgnoreCase("true");
        }

        public static boolean procBoolean(Object arg) {
            if (arg instanceof String) {
                return boolify((String) arg);
            }
            else if (arg instanceof ArgInternal) {
                return ((ArgInternal) arg).boolify();
            }
            else if (arg instanceof ArgComparer) {
                return ((ArgComparer) arg).compare();
            }
            else if (arg instanceof Boolean) {
                return ((Boolean) arg);
            }
            return boolify(arg.toString());
        }

        public static String procString(Object arg) {
            if (arg instanceof String) {
                return (String) arg;
            }
            else if (arg instanceof ArgInternal) {
                return arg.toString();
            }
            else if (arg instanceof ArgComparer) {
                return ((ArgComparer) arg).compare() ? "true" : "false";
            }
            else if (arg instanceof Boolean) {
                return ((Boolean) arg) ? "true" : "false";
            }
            return arg.toString();
        }

        public static String procStringNoTag(Object arg) {
            if (arg instanceof String) {
                return (String) arg;
            }
            else if (arg instanceof ArgInternal) {
                return arg.toString();
            }
            else if (arg instanceof ArgComparer) {
                return "<UnTaggedComparison>";
            }
            else if (arg instanceof Boolean) {
                return ((Boolean) arg) ? "true" : "false";
            }
            return arg.toString();
        }

        public boolean tagbool(int arg) {
            if (argstemp_parsed[arg] != null) {
                return argstemp_parsed[arg].boolify();
            }
            Object argObj = argstemp.get(arg);
            if (argObj instanceof String) {
                return tagify((String) argObj).boolify();
            }
            else if (argObj instanceof ArgInternal) {
                return ((ArgInternal) argObj).boolify();
            }
            else if (argObj instanceof ArgComparer) {
                return ((ArgComparer) argObj).compare();
            }
            else if (argObj instanceof Boolean) {
                return ((Boolean) argObj);
            }
            return tagify(argObj.toString()).boolify();
        }

        public ArgInternal tagify(String arg) {
            ArgInternal toRet = new ArgInternal();
            if (arg.startsWith("!")) {
                toRet.negative = true;
                arg = arg.substring(1);
            }
            toRet.value = TagManager.tag(arg, DenizenCore.getImplementation().getTagContextFor(scriptEntry, false));
            return toRet;
        }

        public ArgInternal tagme(int arg) {
            if (argstemp_parsed[arg] != null) {
                return argstemp_parsed[arg];
            }
            ArgInternal got = tagify(procString(argstemp.get(arg)));
            argstemp_parsed[arg] = got;
            return got;
        }

        public ArgComparer construct(List args, ScriptEntry scriptEntry) {
            argstemp = args;
            argstemp_parsed = new ArgInternal[args.size()];
            this.scriptEntry = scriptEntry;
            return this;
        }

        public boolean compare(List args, ScriptEntry scriptEntry) {
            construct(args, scriptEntry);
            return compare();
        }

        public boolean compare() {
            if (result == null) {
                result = compareInternal();
                if (flip) {
                    result = !result;
                }
            }
            return result;
        }

        public boolean compareInternal() {
            List args = argstemp;
            if (dB.verbose) {
                dB.log("Comparing " + args);
            }
            if (args.size() == 0) {
                if (dB.verbose) {
                    dB.log("Args.size == 0, return false");
                }
                return false;
            }
            else if (args.size() == 1) {
                if (dB.verbose) {
                    dB.log("Returning comparison for " + args.get(0));
                }
                return tagbool(0);
            }
            for (int i = 0; i < args.size(); i++) {
                String arg = procStringNoTag(args.get(i));
                if (arg.equals("(") || arg.equals("!(")) {
                    List subargs = new ArrayList();
                    int count = 0;
                    boolean found = false;
                    boolean flip = false;
                    for (int x = i + 1; x < args.size(); x++) {
                        String xarg = procStringNoTag(args.get(x));
                        if (xarg.equals("(")) {
                            count++;
                            subargs.add("(");
                            if (dB.verbose) {
                                dB.log("Open paren");
                            }
                        }
                        else if (xarg.equals(")")) {
                            if (dB.verbose) {
                                dB.log("Close paren");
                            }
                            count--;
                            if (count == -1) {
                                if (dB.verbose) {
                                    dB.log("Crunch");
                                }
                                ArgComparer comp = new ArgComparer().construct(subargs, scriptEntry);
                                comp.flip = arg.startsWith("!");
                                for (int c = 0; c < (x - i) + 1; c++) {
                                    args.remove(i);
                                }
                                args.add(i, comp);
                                found = true;
                                if (dB.verbose) {
                                    dB.log("Shrunk to " + args);
                                }
                                break;
                            }
                            else {
                                subargs.add(")");
                            }
                        }
                        else {
                            subargs.add(args.get(x));
                        }
                    }
                    if (!found) {
                        if (dB.verbose) {
                            dB.log("Returning false: strange(unfound) ()");
                        }
                        return false;
                    }
                }
                else if (arg.equals(")")) {
                    if (dB.verbose) {
                        dB.log("Returning false: strange(stray) ()");
                    }
                    return false;
                }
            }
            if (args.size() == 1) {
                if (dB.verbose) {
                    dB.log("Returning comparison for " + args.get(0));
                }
                return tagbool(0);
            }
            for (int i = 0; i < args.size(); i++) {
                String arg = procStringNoTag(args.get(i));
                if (arg.equalsIgnoreCase("||")) {
                    List beforeargs = new ArrayList(i);
                    for (int x = 0; x < i; x++) {
                        beforeargs.add(args.get(x));
                    }
                    boolean before = new ArgComparer().compare(beforeargs, scriptEntry);
                    if (before) {
                        if (dB.verbose) {
                            dB.log("Returning true because true || irrel");
                        }
                        return true;
                    }
                    List afterargs = new ArrayList(args.size() - (i + 1));
                    for (int x = i + 1; x < args.size(); x++) {
                        afterargs.add(args.get(x));
                    }
                    boolean comp = new ArgComparer().compare(afterargs, scriptEntry);
                    if (dB.verbose) {
                        dB.log("Returning || comparison: " + comp);
                    }
                    return comp;
                }
                else if (arg.equalsIgnoreCase("&&")) {
                    List beforeargs = new ArrayList(i);
                    for (int x = 0; x < i; x++) {
                        beforeargs.add(args.get(x));
                    }
                    boolean before = new ArgComparer().compare(beforeargs, scriptEntry);
                    if (!before) {
                        if (dB.verbose) {
                            dB.log("Returning false because false && irrel");
                        }
                        return false;
                    }
                    List afterargs = new ArrayList(args.size() - (i + 1));
                    for (int x = i + 1; x < args.size(); x++) {
                        afterargs.add(args.get(x));
                    }
                    boolean comp = new ArgComparer().compare(afterargs, scriptEntry);
                    if (dB.verbose) {
                        dB.log("Returning && comparison: " + comp);
                    }
                    return comp;
                }
            }
            if (args.size() == 1) {
                if (dB.verbose) {
                    dB.log("Returning comparison for " + args.get(0));
                }
                return tagbool(0);
            }
            if (args.size() == 2) {
                if (dB.verbose) {
                    dB.log("Returning false because two args only (non-processable)");
                }
                return false;
            }
            String arg = procStringNoTag(args.get(1));
            boolean negative = false;
            if (arg.startsWith("!")) {
                arg = arg.substring(1);
                negative = true;
            }
            if (arg.equals("==") || arg.equals("=")) {
                arg = "EQUALS";
            }
            else if (arg.equals(">=")) {
                arg = "OR_MORE";
            }
            else if (arg.equals("<=")) {
                arg = "OR_LESS";
            }
            else if (arg.equals("<")) {
                arg = "LESS";
            }
            else if (arg.equals(">")) {
                arg = "MORE";
            }
            else if (arg.equals("||")) {
                arg = "OR";
            }
            else if (arg.equals("&&")) {
                arg = "AND";
            }
            Comparable comparable = new Comparable();
            if (negative) {
                comparable.logic = Comparable.Logic.NEGATIVE;
            }
            try {
                comparable.operator = Comparable.Operator.valueOf(arg.toUpperCase());
                comparable.setComparable(tagme(0).toString());
                comparable.setComparedto(tagme(2).toString());
                boolean outcome = comparable.determineOutcome();
                dB.echoDebug(scriptEntry, comparable.toString());
                return outcome;
            }
            catch (IllegalArgumentException ex) {
                dB.echoError(scriptEntry == null ? null : scriptEntry.getResidingQueue(), "If command syntax invalid - possibly wrong number of arguments (check for stray spaces)? IllegalArgumentException: " + ex.getMessage());
                return false;
            }
        }
    }
}
