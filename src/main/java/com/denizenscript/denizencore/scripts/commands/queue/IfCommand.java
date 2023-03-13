package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.commands.Comparable;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;
import com.denizenscript.denizencore.tags.TagManager;

import java.util.ArrayList;
import java.util.List;

public class IfCommand extends BracedCommand {

    public IfCommand() {
        setName("if");
        setSyntax("if [<value>] (!)(<operator> <value>) (&&/|| ...) [<commands>]");
        setRequiredArguments(1, -1);
        setParseArgs(false);
        isProcedural = true;
    }

    // <--[command]
    // @Name If
    // @Syntax if [<value>] (!)(<operator> <value>) (&&/|| ...) [<commands>]
    // @Required 1
    // @Maximum -1
    // @Short Compares values, and runs a subset of commands if they match.
    // @Group queue
    // @Guide https://guide.denizenscript.com/guides/basics/if-command.html
    //
    // @Description
    // Compares values, and runs a subset of commands if they match.
    // Works with the else command, which handles alternatives for when the comparison fails.
    // The if command is equivalent to the English phrasing "if something is true, then do the following".
    //
    // Values are compared using the comparable system. See <@link language operator> for information.
    //
    // Comparisons may be chained together using the symbols '&&' and '||' or their text equivalents 'and' and 'or'.
    // '&&' means "and", '||' means "or".
    // So, for example "if <[a]> && <[b]>:" requires both a AND b to be true.
    // "if <[a]> and <[b]>:" also requires both a AND b to be true.
    //
    // The "or" is inclusive, meaning "if <[a]> || <[b]>:" will pass for any of the following:
    // a = true, b = true
    // a = true, b = false
    // a = false, b = true
    // but will fail when a = false and b = false.
    //
    // Sets of comparisons may be grouped using ( parens ) as separate arguments.
    // So, for example "if ( <[a]> && <[b]> ) || <[c]>", or "if ( <[x]> or <[y]> or <[z]> ) and ( <[a]> or <[b]> or <[c]> )"
    // Grouping is REQUIRED when using both '&&' and '||' in one line. Otherwise, groupings should not be used at all.
    //
    // Boolean inputs and groups both support negating with the '!' symbol as a prefix.
    // This means you can do "if !<[a]>" to say "if a is NOT true".
    // Similarly, you can do "if !( <[a]> || <[b]> )", though be aware that per rules of boolean logic,
    // that example is the exactly same as "if !<[a]> && !<[b]>".
    //
    // You can also use keyword "not" as its own argument to negate a boolean or an operator.
    // For example, "if not <[a]>:" will require a to be false, and "if <[a]> not equals <[b]>:" will require that 'a' does not equal 'b'.
    //
    // When not using a specific comparison operator, true vs false will be determined by Truthiness, see <@link tag ObjectTag.is_truthy> for details.
    // For example, "- if <player||null>:" will pass if a player is linked, valid, and online.
    //
    // @Tags
    // <ObjectTag.is[<operator>].to[<element>]>
    // <ObjectTag.is[<operator>].than[<element>]>
    //
    // @Usage
    // Use to narrate a message only if a player has a flag.
    // - if <player.has_flag[secrets]>:
    //     - narrate "The secret number is 3!"
    //
    // @Usage
    // Use to narrate a different message depending on a player's money level.
    // - if <player.money> > 1000:
    //     - narrate "You're rich!"
    // - else:
    //     - narrate "You're poor!"
    //
    // @Usage
    // Use to stop a script if a player doesn't have all the prerequisites.
    // - if !<player.has_flag[quest_complete]> || !<player.has_permission[new_quests]> || <player.money> < 50:
    //     - narrate "You're not ready!"
    //     - stop
    // - narrate "Okay so your quest is to find the needle item in the haystack build next to town."
    //
    // @Usage
    // Use to perform a complicated requirements test before before changing some event.
    // - if ( poison|magic|melting contains <context.cause> and <context.damage> > 5 ) or <player.has_flag[weak]>:
    //     - determine <context.damage.mul[2]>
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
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
                    Debug.echoError(scriptEntry, "Upcoming else command is mis-formatted!");
                    break;
                }
                scriptEntry.getResidingQueue().script_entries.removeFirst();
                nextEntry.context = scriptEntry.context;
                nextEntry.entryData = scriptEntry.entryData;
                nextEntry.queue = scriptEntry.queue;
                BracedData elseRef = getBracedCommands(nextEntry).get(0);
                elseRef.entry = nextEntry;
                elseRef.key = nextEntry.toString();
                elseRef.args = new ArrayList<>();
                elseRef.args.add("else");
                elseRef.args.addAll(nextEntry.getOriginalArguments());
                allData.add(elseRef);
            }
            scriptEntry.addObject("braces", allData);
        }
        else {
            for (String arg : scriptEntry.getOriginalArguments()) {
                if (arg.equals("{")) {
                    if (CoreConfiguration.debugVerbose) {
                        Debug.log("Has_brace = true");
                    }
                    has_brace = true;
                    break;
                }
            }
            if (has_brace) {
                scriptEntry.addObject("braces", getBracedCommands(scriptEntry));
            }
        }
        boolean in_subcommand = false;
        boolean in_elsecommand = false;
        List<String> subcommand = new ArrayList<>();
        List<String> elsecommand = new ArrayList<>();
        List<String> comparisons = new ArrayList<>();
        for (String arg : scriptEntry.getOriginalArguments()) {
            if (arg.equals("{")) {
                break;
            }
            if (!has_brace && in_subcommand && CoreUtilities.equalsIgnoreCase(arg, "else")) {
                in_elsecommand = true;
                in_subcommand = false;
            }
            else if (!has_brace && !in_elsecommand && DenizenCore.commandRegistry.get(CoreUtilities.toUpperCase(arg)) != null) {
                Deprecations.ifCommandSingleLine.warn(scriptEntry);
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
            Debug.report(scriptEntry, getName(), db("use_braces", braces != null));
        }
        if (CoreConfiguration.debugVerbose) {
            Debug.log("comparisons=" + comparisons + ", sc:" + subcommand + ", ec:" + elsecommand);
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
                Debug.echoError(scriptEntry, "Failed to parse IF command: mis-aligned bracing, empty subsections, or other basic formatting error.");
                return;
            }
            if (first_set) {
                if (CoreConfiguration.debugVerbose) {
                    Debug.log("Running the first set");
                }
                Debug.echoDebug(scriptEntry, "<Y>If command passed, running block.");
                scriptEntry.setInstant(true);
                List<ScriptEntry> bracedCommandsList = braces.get(0).value;
                for (ScriptEntry entry : bracedCommandsList) {
                    entry.setInstant(true);
                }
                scriptEntry.getResidingQueue().injectEntriesAtStart(bracedCommandsList);
                return;
            }
            else {
                for (int z = 1; z < braces.size(); z++) {
                    BracedData braceSet = braces.get(z);
                    if (CoreConfiguration.debugVerbose) {
                        Debug.log("Trying: " + braceSet.key);
                    }
                    List<String> key = braceSet.args;
                    if (key.isEmpty() || !CoreUtilities.equalsIgnoreCase(key.get(0), "else")) {
                        Debug.echoError("If command has argument '" + key.get(0) + "' which is unknown.");
                        continue;
                    }
                    if (key.size() > 1) {
                        if (!CoreUtilities.equalsIgnoreCase(key.get(1), "if")) {
                            Debug.echoError("Else command has argument '" + key.get(1) + "' which is unknown.");
                            continue;
                        }
                        if (!new ArgComparer().compare(key.subList(2, key.size()), braceSet.entry)) {
                            continue;
                        }
                        Debug.echoDebug(scriptEntry, "<Y>If/else-if chain entry #" + (z + 1) + " passed, running block.");
                    }
                    else {
                        Debug.echoDebug(scriptEntry, "<Y>No part of the if command passed, running ELSE block.");
                    }
                    scriptEntry.setInstant(true);
                    List<ScriptEntry> bracedCommandsList = braceSet.value;
                    for (ScriptEntry entry : bracedCommandsList) {
                        entry.setInstant(true);
                    }
                    scriptEntry.getResidingQueue().injectEntriesAtStart(bracedCommandsList);
                    return;
                }
            }
        }
        Debug.echoDebug(scriptEntry, "<Y>No part of the if command passed, no block will run.");
    }

    public void executeCommandList(List<String> subcommand, ScriptEntry scriptEntry) {
        try {
            scriptEntry.setInstant(true);
            String cmd = subcommand.get(0);
            subcommand.remove(0);
            ScriptEntry entry = new ScriptEntry(cmd, subcommand.toArray(new String[0]), scriptEntry.getScript() != null ? scriptEntry.getScript().getContainer() : null);
            entry.entryData = scriptEntry.entryData.clone();
            entry.updateContext();
            entry.setInstant(true);
            scriptEntry.getResidingQueue().injectEntryAtStart(entry);
        }
        catch (Exception e) {
            Debug.echoError(e);
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

        public class ArgInternal {

            boolean negative;

            ObjectTag value;

            boolean boolify() {
                return value.isTruthy() != negative;
            }

            @Override
            public String toString() {
                return negative ? "!" + value : value.toString();
            }
        }

        public static String procString(Object arg) {
            if (arg instanceof String) {
                return (String) arg;
            }
            else if (arg instanceof ScriptEntry.InternalArgument) {
                return ((ScriptEntry.InternalArgument) arg).fullOriginalRawValue;
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
            else if (arg instanceof ScriptEntry.InternalArgument) {
                return ((ScriptEntry.InternalArgument) arg).fullOriginalRawValue;
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

        public boolean tagbool(int arg, boolean canNegate) {
            if (argstemp_parsed[arg] != null) {
                return argstemp_parsed[arg].boolify();
            }
            Object argObj = argstemp.get(arg);
            if (argObj instanceof String) {
                return tagify((String) argObj, canNegate).boolify();
            }
            else if (argObj instanceof ScriptEntry.InternalArgument) {
                // TODO: Special case tag parsing
                return tagify(((ScriptEntry.InternalArgument) argObj).fullOriginalRawValue, canNegate).boolify();
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
            return tagify(argObj.toString(), canNegate).boolify();
        }

        public ArgInternal tagify(String arg, boolean canNegate) {
            ArgInternal toRet = new ArgInternal();
            if (arg.startsWith("!") && canNegate) {
                toRet.negative = true;
                arg = arg.substring(1);
            }
            toRet.value = TagManager.tagObject(arg, DenizenCore.implementation.getTagContext(scriptEntry));
            return toRet;
        }

        public ArgInternal tagme(int arg, boolean canNegate) {
            if (argstemp_parsed[arg] != null) {
                return argstemp_parsed[arg];
            }
            ArgInternal got = tagify(procString(argstemp.get(arg)), canNegate);
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
            if (CoreConfiguration.debugVerbose) {
                Debug.log("Comparing " + args);
            }
            if (args.isEmpty()) {
                if (CoreConfiguration.debugVerbose) {
                    Debug.log("Args.size == 0, return false");
                }
                return false;
            }
            else if (args.size() == 1) {
                if (CoreConfiguration.debugVerbose) {
                    Debug.log("Returning comparison for " + args.get(0));
                }
                return tagbool(0, true);
            }
            for (int i = 0; i < args.size(); i++) {
                String arg = procStringNoTag(args.get(i));
                if (arg.equals("(") || arg.equals("!(")) {
                    List subargs = new ArrayList(args.size());
                    int count = 0;
                    boolean found = false;
                    for (int x = i + 1; x < args.size(); x++) {
                        String xarg = procStringNoTag(args.get(x));
                        if (xarg.equals("(") || xarg.equals("!(")) {
                            count++;
                            subargs.add(xarg);
                            if (CoreConfiguration.debugVerbose) {
                                Debug.log("Open paren");
                            }
                        }
                        else if (xarg.equals(")")) {
                            if (CoreConfiguration.debugVerbose) {
                                Debug.log("Close paren");
                            }
                            count--;
                            if (count == -1) {
                                if (CoreConfiguration.debugVerbose) {
                                    Debug.log("Crunch");
                                }
                                ArgComparer comp = new ArgComparer().construct(subargs, scriptEntry);
                                comp.flip = arg.startsWith("!");
                                for (int c = 0; c < (x - i) + 1; c++) {
                                    args.remove(i);
                                }
                                args.add(i, comp);
                                found = true;
                                if (CoreConfiguration.debugVerbose) {
                                    Debug.log("Shrunk to " + args);
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
                        if (CoreConfiguration.debugVerbose) {
                            Debug.log("Returning false: strange(unfound) ()");
                        }
                        return false;
                    }
                }
                else if (arg.equals(")")) {
                    if (CoreConfiguration.debugVerbose) {
                        Debug.log("Returning false: strange(stray) ()");
                    }
                    return false;
                }
            }
            if (args.size() == 1) {
                if (CoreConfiguration.debugVerbose) {
                    Debug.log("Returning comparison for " + args.get(0));
                }
                return tagbool(0, true);
            }
            for (int i = 0; i < args.size(); i++) {
                String arg = procStringNoTag(args.get(i));
                String argLow = CoreUtilities.toLowerCase(arg);
                if (argLow.equals("||") || argLow.equals("or")) {
                    List beforeargs = new ArrayList(i);
                    for (int x = 0; x < i; x++) {
                        beforeargs.add(args.get(x));
                    }
                    boolean before = new ArgComparer().compare(beforeargs, scriptEntry);
                    if (before) {
                        if (CoreConfiguration.debugVerbose) {
                            Debug.log("Returning true because true || irrel");
                        }
                        return true;
                    }
                    List afterargs = new ArrayList(args.size() - (i + 1));
                    for (int x = i + 1; x < args.size(); x++) {
                        afterargs.add(args.get(x));
                    }
                    boolean comp = new ArgComparer().compare(afterargs, scriptEntry);
                    if (CoreConfiguration.debugVerbose) {
                        Debug.log("Returning || comparison: " + comp);
                    }
                    return comp;
                }
                else if (argLow.equals("&&") || argLow.equals("and")) {
                    List beforeargs = new ArrayList(i);
                    for (int x = 0; x < i; x++) {
                        beforeargs.add(args.get(x));
                    }
                    boolean before = new ArgComparer().compare(beforeargs, scriptEntry);
                    if (!before) {
                        if (CoreConfiguration.debugVerbose) {
                            Debug.log("Returning false because false && irrel");
                        }
                        return false;
                    }
                    List afterargs = new ArrayList(args.size() - (i + 1));
                    for (int x = i + 1; x < args.size(); x++) {
                        afterargs.add(args.get(x));
                    }
                    boolean comp = new ArgComparer().compare(afterargs, scriptEntry);
                    if (CoreConfiguration.debugVerbose) {
                        Debug.log("Returning && comparison: " + comp);
                    }
                    return comp;
                }
            }
            if (args.size() == 1) {
                if (CoreConfiguration.debugVerbose) {
                    Debug.log("Returning comparison for " + args.get(0));
                }
                return tagbool(0, true);
            }
            if (args.size() == 2) {
                if (CoreUtilities.toLowerCase(procStringNoTag(args.get(0))).equals("not")) {
                    if (CoreConfiguration.debugVerbose) {
                        Debug.log("Returning negative comparison for " + args.get(0));
                    }
                    return !tagbool(1, false);
                }
                if (CoreConfiguration.debugVerbose) {
                    Debug.log("Returning false because two args only (non-processable)");
                }
                return false;
            }
            String operatorArg;
            boolean negative = false;
            if (args.size() == 4 && CoreUtilities.toLowerCase(procStringNoTag(args.get(1))).equals("not")) {
                operatorArg = procStringNoTag(args.get(2));
                negative = true;
            }
            else if (args.size() == 3) {
                operatorArg = procStringNoTag(args.get(1));
                if (operatorArg.startsWith("!")) {
                    operatorArg = operatorArg.substring(1);
                    negative = true;
                }
            }
            else {
                Debug.echoError(scriptEntry, "If command syntax invalid - too many arguments? Found " + args.size() + " args: " + args);
                return false;
            }
            try {
                Comparable.Operator operator = Comparable.getOperatorFor(operatorArg);
                if (operator == null) {
                    Debug.echoError(scriptEntry, "If command syntax invalid - invalid operator '" + operatorArg + "'");
                    return false;
                }
                ObjectTag first = tagme(0, false).value;
                ObjectTag second = tagme(args.size() - 1, false).value;
                boolean outcome = Comparable.compare(first, second, operator, negative, scriptEntry.context);
                if (scriptEntry.dbCallShouldDebug()) {
                    Debug.echoDebug(scriptEntry, "Comparing if " + first + (negative ? " not " : " ") + operator.name() + " " + second + " ... " + outcome);
                }
                return outcome;
            }
            catch (Throwable ex) {
                Debug.echoError(scriptEntry, "If command syntax invalid - possibly wrong number of arguments (check for stray spaces)? exception: " + ex.getClass().getName() + ": " + ex.getMessage());
                if (CoreConfiguration.debugVerbose) {
                    Debug.echoError("Was comparing " + operatorArg + " with " + args.get(0) + " and " + args.get(2));
                    Debug.echoError(ex);
                }
                return false;
            }
        }
    }
}
