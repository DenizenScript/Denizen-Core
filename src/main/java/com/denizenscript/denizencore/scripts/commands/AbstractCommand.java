package com.denizenscript.denizencore.scripts.commands;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.notable.Notable;
import com.denizenscript.denizencore.objects.notable.NoteManager;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.*;

public abstract class AbstractCommand {

    /**
     * Special extra command names that should still not error.
     */
    public static HashSet<String> noErrorCommandNames = new HashSet<>(Arrays.asList("case", "default"));

    public String syntax = "No usage defined! See documentation for more information!";

    public List<String> docFlagArgs = new ArrayList<>();

    public List<String> docPrefixes = new ArrayList<>();

    public void setSyntax(String syntax) {
        this.syntax = syntax;
        int firstSpace = syntax.indexOf(' ');
        if (firstSpace < 0) {
            return;
        }
        String cleaned = syntax.substring(firstSpace).replace("/", " ");
        cleaned = cleaned.replace("[", "").replace("]", "").replace("(", "").replace(")", "").replace("{", "").replace("}", "");
        List<String> args = CoreUtilities.split(cleaned, ' ');
        for (String arg : args) {
            if (arg.isEmpty()) {
                continue;
            }
            int colonIndex = arg.indexOf(':');
            if (colonIndex > 0) {
                String prefix = arg.substring(0, colonIndex);
                if (!prefix.contains("<")) {
                    docPrefixes.add(prefix);
                }
            }
            else if (!arg.contains("<") && !arg.contains("|")) {
                docFlagArgs.add(arg);
            }
        }
        if (Debug.verbose) {
            Debug.log("Command syntax '" + syntax + "' parsed to flat args: ( " + String.join(", ", docFlagArgs) + " ) and prefixes ( " + String.join(", ", docPrefixes) + " ).");
        }
    }

    public static class TabCompletionsBuilder {

        public String arg;

        public ArrayList<String> completions = new ArrayList<>();

        public final void addWithPrefix(String prefix, Set<String> values) {
            if (arg.startsWith(prefix)) {
                for (String val : values) {
                    add(prefix + val);
                }
            }
        }

        public final void addWithPrefix(String prefix, Enum<?>[] values) {
            if (arg.startsWith(prefix)) {
                for (Enum<?> val : values) {
                    add(prefix + val.name());
                }
            }
        }

        public final void add(String text) {
            if (CoreUtilities.toLowerCase(text).startsWith(arg)) {
                completions.add(text);
            }
        }

        public final void add(String a, String... values) {
            add(a);
            for (String val : values) {
                add(val);
            }
        }

        public final void add(Set<String> values) {
            for (String val : values) {
                add(val);
            }
        }

        public final void add(Enum<?>[] values) {
            for (Enum<?> val : values) {
                add(val.name());
            }
        }

        public final void addNotesOfType(Class<? extends Notable> type) {
            for (Notable note : NoteManager.notesByType.get(type)) {
                add(NoteManager.getSavedId(note));
            }
        }

        public final void addScriptsOfType(Class<? extends ScriptContainer> type) {
            for (ScriptContainer script : ScriptRegistry.scriptContainers.values()) {
                if (type.isAssignableFrom(script.getClass())) {
                    add(script.getName());
                }
            }
        }
    }

    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
    }

    private boolean preparseArgs = true;

    public boolean forceHold = false;

    public int minimumArguments = 0;

    public int maximumArguments = Integer.MAX_VALUE;

    public HashSet<String> prefixesHandled = new HashSet<>();

    public HashSet<String> rawValuesHandled = new HashSet<>();

    public HashMap<String, String> prefixRemapper = new HashMap<>();

    public boolean allowedDynamicPrefixes = false;

    public boolean anyPrefixSymbolAllowed = false;

    public void addRemappedPrefixes(String realName, String... alts) {
        prefixesHandled.add(realName);
        prefixesHandled.addAll(Arrays.asList(alts));
        for (String alt : alts) {
            prefixRemapper.put(alt, realName);
        }
    }

    public void setPrefixesHandled(String... prefixes) {
        prefixesHandled.addAll(Arrays.asList(prefixes));
    }

    public void setRawValuesHandled(String... values) {
        rawValuesHandled.addAll(Arrays.asList(values));
    }

    public void setBooleansHandled(String... boolNames) {
        setPrefixesHandled(boolNames);
        setRawValuesHandled(boolNames);
    }

    public static String db(String prefix, boolean value) {
        return "<G>" + prefix + "='<Y>" + value + "<G>'  ";
    }

    public static String db(String prefix, Object value) {
        if (value == null) {
            return "";
        }
        return ArgumentHelper.debugObj(prefix, value);
    }

    /**
     * Whether this command is valid for usage in procedural logic.
     */
    public boolean isProcedural = false;

    public void setRequiredArguments(int min, int max) {
        minimumArguments = min;
        maximumArguments = max == -1 ? Integer.MAX_VALUE : max;
    }

    public void setParseArgs(boolean parse) {
        preparseArgs = parse;
    }

    public boolean shouldPreParse() {
        return preparseArgs;
    }

    public void setName(String commandName) {
        name = commandName.toUpperCase();
    }

    protected String name;

    public String getName() {
        return name;
    }

    public String getUsageHint() {
        return syntax;
    }

    /**
     * Part of the Plugin disable sequence.
     * <p/>
     * Can be '@Override'n by a Command which requires a method when bukkit sends a onDisable() to Denizen. (ie. Server shuts down or restarts)
     */
    public void onDisable() {
    }

    @Deprecated
    public void withOptions(String usageHint, int numberOfRequiredArgs) {
        minimumArguments = numberOfRequiredArgs;
        setSyntax(usageHint);
    }

    public abstract void execute(ScriptEntry scriptEntry);

    /**
     * Called by the CommandExecutor before the execute() method is called. Arguments
     * should be iterated through and checked before continuing to execute(). Note that
     * PLAYER:<player> and NPC:<npc> arguments are parsed automatically by the Executor
     * and should not be handled by this Command otherwise. Their output is stored in the
     * attached {@link ScriptEntry} and can be retrieved with ((BukkitScriptEntryData)scriptEntry.entryData).getPlayer(),
     * scriptEntry.getOfflinePlayer() (if the player specified is not online), and
     * ((BukkitScriptEntryData)scriptEntry.entryData).getNPC(). Remember that any of these have a possibility of being null
     * and should be handled accordingly if required by this Command.
     *
     * @param scriptEntry The {@link ScriptEntry}, which contains run-time context that may
     *                    be utilized by this Command.
     * @throws InvalidArgumentsException Will halt execution of this Command and hint usage to the console to avoid
     *                                   unwanted behavior due to missing information.
     */
    public abstract void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException;

}
