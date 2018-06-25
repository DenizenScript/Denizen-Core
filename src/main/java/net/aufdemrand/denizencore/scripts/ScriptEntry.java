package net.aufdemrand.denizencore.scripts;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.exceptions.ScriptEntryCreationException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.objects.dScript;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.scripts.commands.BracedCommand;
import net.aufdemrand.denizencore.scripts.commands.Holdable;
import net.aufdemrand.denizencore.scripts.containers.ScriptContainer;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.Debuggable;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.*;


/**
 * ScriptEntry contain information about a single entry from a dScript. It is used
 * by the CommandExecuter, among other parts of Denizen.
 */
public class ScriptEntry implements Cloneable, Debuggable {

    public static class ScriptEntryInternal {

        public String command = null;

        public AbstractCommand actualCommand = null;

        public List<String> pre_tagged_args = null;

        public List<BracedCommand.BracedData> bracedSet = null;

        public List<Argument> args_ref = null;

        public dScript script = null;

        public List<Object> insideList = null;

        public boolean instant = false;

        public boolean waitfor = false;

        public boolean hasTags = false;

        public boolean hasInstantTags = false;

        public boolean hasOldDefs = false;

        public int[] processArgs = null;

        public List<aH.Argument> preprocArgs = null;
    }

    public static class Argument {

        public String prefix = null;

        public List<TagManager.ParseableTagPiece> value = null;

        public aH.Argument aHArg = null;
    }

    public List<Argument> args_cur = null;

    public List<aH.Argument> aHArgs = null;

    public List<String> args = null;

    public List<dObject> processed_arguments = null;

    public ScriptEntryData entryData = null;

    private ScriptQueue queue = null;

    public ScriptEntryInternal internal = null;

    public List<BracedCommand.BracedData> getBracedSet() {
        return internal.bracedSet;
    }

    public void setBracedSet(List<BracedCommand.BracedData> set) {
        internal.bracedSet = set;
    }

    private Map<String, Object> objects = new HashMap<String, Object>();

    public void regenerateArgsCur() {
        args_cur = new ArrayList<Argument>(internal.args_ref);
        for (int i = 0; i < args_cur.size(); i++) {
            Argument arg = args_cur.get(i);
            arg.value = new ArrayList<TagManager.ParseableTagPiece>(arg.value);
            arg.aHArg = aHArgs.get(i);
        }
    }

    public final static aH.Argument NULL_ARGUMENT = new aH.Argument("null_trick", "null_trick");

    public void generateAHArgs() {
        aHArgs = new ArrayList<aH.Argument>(internal.args_ref.size());
        for (int i = 0; i < internal.args_ref.size(); i++) {
            aHArgs.add(NULL_ARGUMENT);
        }
        for (int i : internal.processArgs) {
            Argument arg = internal.args_ref.get(i);
            aHArgs.set(i, arg.aHArg.needsFill ? arg.aHArg.clone() : arg.aHArg);
        }
    }

    @Override
    public ScriptEntry clone() throws CloneNotSupportedException {
        ScriptEntry se = (ScriptEntry) super.clone();
        se.objects = new HashMap<String, Object>(objects);
        se.processed_arguments = processed_arguments == null ? null : new ArrayList<dObject>(processed_arguments);
        se.entryData = entryData.clone();
        return se;
    }

    public List<Object> getInsideList() {
        return internal.insideList;
    }


    /**
     * Get a hot, fresh, script entry, ready for execution! Just supply a valid command,
     * some arguments, and bonus points for a script container (can be null)!
     *
     * @param command   the name of the command this entry will be handed to
     * @param arguments an array of the arguments
     * @param script    optional ScriptContainer reference
     * @throws ScriptEntryCreationException if 'command' is null
     */
    public ScriptEntry(String command, String[] arguments, ScriptContainer script) throws ScriptEntryCreationException {
        this(command, arguments, script, null);
    }

    public ScriptEntry(String command, String[] arguments, ScriptContainer script, List<Object> insides) throws ScriptEntryCreationException {
        if (command == null) {
            throw new ScriptEntryCreationException("dCommand 'name' cannot be null!");
        }
        internal = new ScriptEntryInternal();
        entryData = DenizenCore.getImplementation().getEmptyScriptEntryData();
        internal.command = command.toUpperCase();
        internal.insideList = insides;
        if (script != null) {
            internal.script = script.getAsScriptArg();
        }
        if (command.length() > 0) {
            if (command.charAt(0) == '^') {
                internal.instant = true;
                internal.command = command.substring(1);
            }
            else if (command.charAt(0) == '~') {
                internal.command = command.substring(1);
                if (DenizenCore.getCommandRegistry().get(internal.command) instanceof Holdable) {
                    internal.waitfor = true;
                }
                else {
                    dB.echoError("The command '" + internal.command + "' cannot be waited for!");
                }
            }
            internal.actualCommand = DenizenCore.getCommandRegistry().get(internal.command);
        }
        else {
            internal.actualCommand = null;
        }
        if (arguments != null) {
            args = new ArrayList<String>(arguments.length);
            internal.preprocArgs = new ArrayList<aH.Argument>();
            int nested_depth = 0;
            for (String arg : arguments) {
                if (arg.equals("{")) {
                    nested_depth++;
                    args.add(arg);
                    continue;
                }
                if (arg.equals("}")) {
                    nested_depth--;
                    args.add(arg);
                    continue;
                }
                if (nested_depth > 0) {
                    args.add(arg);
                    continue;
                }
                String parg = arg;
                String after = null;
                if (parg.endsWith("{") && !parg.equals("{")) {
                    after = "{";
                    parg = parg.substring(0, parg.length() - 1);
                    dB.echoError("Command '" + command + "' in script '" + (script == null ? "(None)" : script.getName()) + "' has typo: brace written without space... like 'arg{' when it should be 'arg {'.");
                }
                aH.Argument argObj = new aH.Argument(arg);
                if (argObj.hasPrefix()) {
                    if (argObj.matchesOnePrefix("unparsed")) {
                        args.add(TagManager.escapeOutput(argObj.getValue()));
                    }
                    else if (argObj.matchesOnePrefix("save") || DenizenCore.getImplementation().needsHandleArgPrefix(argObj.prefix)) {
                        internal.preprocArgs.add(argObj);
                    }
                    else {
                        args.add(arg);
                    }
                }
                else {
                    args.add(parg);
                }
                if (after != null) {
                    args.add(after);
                    if (after.equals("{")) {
                        nested_depth++;
                        args.add(arg);
                        continue;
                    }
                }
            }
            internal.pre_tagged_args = new ArrayList<String>(args);
            nested_depth = 0;
            TagContext refContext = DenizenCore.getImplementation().getTagContext(this);
            internal.args_ref = new ArrayList<Argument>(args.size());
            List<Integer> tempProcessArgs = new ArrayList<Integer>();
            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);
                internal.args_ref.add(null);
                if (arg.equals("{")) {
                    nested_depth++;
                    continue;
                }
                if (arg.equals("}")) {
                    nested_depth--;
                    continue;
                }
                if (nested_depth > 0) {
                    continue;
                }
                tempProcessArgs.add(i);
                int colon = arg.indexOf(':');
                int space = arg.indexOf(' ');
                Argument argVal = new Argument();
                internal.args_ref.set(i, argVal);
                if (colon > 0 && (space == -1 || space > colon)) {
                    argVal.prefix = arg.substring(0, colon);
                    arg = arg.substring(colon + 1);
                }
                internal.args_ref.get(i).value = TagManager.genChain(arg, refContext);
                if (arg.indexOf('%') != 0) {
                    internal.hasOldDefs = true;
                }
                boolean isTag = false;
                int indStart = arg.indexOf('<');
                if (indStart >= 0) {
                    int indEnd = arg.indexOf('>');
                    if (indEnd > indStart) {
                        isTag = true;
                        internal.hasTags = true;
                        char c = arg.charAt(indStart + 1);
                        if (c == '!' || c == '^') {
                            internal.hasInstantTags = true;
                        }
                    }
                }
                argVal.aHArg = new aH.Argument(argVal.prefix, arg);
                argVal.aHArg.needsFill = isTag;
            }
            internal.processArgs = new int[tempProcessArgs.size()];
            for (int i = 0; i < tempProcessArgs.size(); i++) {
                internal.processArgs[i] = tempProcessArgs.get(i);
            }
            objectify();
        }
        else {
            args = new ArrayList<String>();
            internal.preprocArgs = new ArrayList<aH.Argument>();
            internal.pre_tagged_args = new ArrayList<String>();
            internal.processArgs = new int[0];
            internal.args_ref = new ArrayList<Argument>();
            processed_arguments = new ArrayList<dObject>();
        }
        if (internal.actualCommand != null) {
            if (internal.actualCommand.getOptions().REQUIRED_ARGS > args.size()) {
                broken = true;
            }
            if (internal.actualCommand instanceof BracedCommand) {
                BracedCommand.getBracedCommands(this);
            }
        }
    }

    /**
     * Adds a context object to the script entry. Just provide a key and an object.
     * Technically any type of object can be stored, however providing dObjects
     * is preferred.
     *
     * @param key    the name of the object
     * @param object the object, preferably a dObject
     */
    public ScriptEntry addObject(String key, Object object) {
        if (object == null) {
            return this;
        }
        if (object instanceof dObject) {
            ((dObject) object).setPrefix(key);
        }
        objects.put(CoreUtilities.toLowerCase(key), object);
        return this;
    }


    /**
     * If the scriptEntry lacks the object corresponding to the
     * key, set it to the first non-null argument
     *
     * @param key The key of the object to check
     * @return The scriptEntry
     */
    public ScriptEntry defaultObject(String key, Object... objects) throws InvalidArgumentsException {
        if (!this.objects.containsKey(CoreUtilities.toLowerCase(key))) {
            for (Object obj : objects) {
                if (obj != null) {
                    this.addObject(key, obj);
                    break;
                }
            }
        }

        // Check if the object has been filled. If not, throw new Invalid Arguments Exception.
        // TODO: Should this be here? Most checks are done separately.
        if (!hasObject(key)) {
            throw new InvalidArgumentsException("Missing '" + key + "' argument!");
        }
        else {
            return this;
        }
    }


    public List<String> getArguments() {
        return args;
    }

    ////////////
    // INSTANCE METHODS
    //////////

    /**
     * Gets the original, pre-tagged arguments, as constructed. This is simply a copy of
     * the original arguments, immune from any changes that may be made (such as tag filling)
     * by the CommandExecuter.
     *
     * @return unmodified arguments from entry creation
     */
    public List<String> getOriginalArguments() {
        return internal.pre_tagged_args;
    }

    public String getCommandName() {
        return internal.command;
    }

    public AbstractCommand getCommand() {
        return internal.actualCommand;
    }

    public boolean broken = false;

    public void setArgument(int ind, String val) {
        args.set(ind, val);
        if (processed_arguments != null) {
            processed_arguments.set(ind, new Element(val));
        }
    }

    public ScriptEntry setArguments(List<String> arguments) {
        args = arguments;
        return this;
    }

    public ScriptEntry setArgumentsObjects(List<dObject> arguments) {
        processed_arguments = arguments;
        args = new ArrayList<String>(arguments.size()); // TODO: Placeholder! Remove old string args entirely!
        for (dObject tmp : arguments) {
            args.add(TagManager.escapeOutput(tmp.toString()));
        }
        return this;
    }

    public void objectify() {
        processed_arguments = new ArrayList<dObject>(args.size());
        for (String arg : args) {
            processed_arguments.add(new Element(arg));
        }
    }

    private ScriptEntry owner = null;

    public void setOwner(ScriptEntry owner) {
        this.owner = owner;
    }

    public ScriptEntry getOwner() {
        return owner;
    }

    private Object data;

    public Object getData() {
        return data;
    }

    public void setData(Object result) {
        this.data = result;
    }

    public void copyFrom(ScriptEntry entry) {
        entryData = entry.entryData.clone();
        setSendingQueue(entry.getResidingQueue());
    }


    //////////////////
    // SCRIPTENTRY CONTEXT
    //////////////

    public Map<String, Object> getObjects() {
        return objects;
    }


    public Object getObject(String key) {
        try {
            return objects.get(key);
        }
        catch (Exception ex) {
            if (dB.verbose) {
                dB.echoError(ex);
            }
            return null;
        }
    }

    public <T extends dObject> T getdObject(String key) {
        try {
            // If an ENUM, return as an Element
            Object gotten = objects.get(key);
            if (gotten instanceof Enum) {
                return (T) new Element(((Enum) gotten).name());
            }
            // Otherwise, just return the stored dObject
            return (T) gotten;
            // If not a dObject, return null
        }
        catch (Exception ex) {
            if (dB.verbose) {
                dB.echoError(ex);
            }
            return null;
        }
    }

    public Element getElement(String key) {
        try {
            return (Element) objects.get(key);
        }
        catch (Exception ex) {
            if (dB.verbose) {
                dB.echoError(ex);
            }
            return null;
        }
    }


    public boolean hasObject(String key) {
        return objects.containsKey(key);
    }

    /////////////
    // CORE LINKED OBJECTS
    ///////

    public dScript getScript() {
        return internal.script;
    }


    public ScriptEntry setScript(String scriptName) {
        internal.script = dScript.valueOf(scriptName);
        return this;
    }


    public ScriptQueue getResidingQueue() {
        return queue;
    }


    public void setSendingQueue(ScriptQueue scriptQueue) {
        queue = scriptQueue;
    }

    //////////////
    // TimedQueue FEATURES
    /////////


    public boolean forceInstant = false;

    public boolean isInstant() {
        return internal.instant || forceInstant;
    }


    public ScriptEntry setInstant(boolean instant) {
        forceInstant = instant;
        return this;
    }

    public boolean isFinished = false;

    public boolean shouldWaitFor() {
        return internal.waitfor && !isFinished;
    }


    public ScriptEntry setFinished(boolean finished) {
        isFinished = finished;
        return this;
    }

    ////////////
    // COMPATIBILITY
    //////////

    // Keep track of objects which were added by mass
    // so that IF can inject them into new entries.
    // This is ugly, but it will keep from breaking
    // previous versions of Denizen.
    // TODO: Get rid of this
    public List<String> tracked_objects = new ArrayList<String>();

    public ScriptEntry trackObject(String key) {
        tracked_objects.add(CoreUtilities.toLowerCase(key));
        return this;
    }

    /////////////
    // DEBUGGABLE
    /////////

    public boolean fallbackDebug = true;

    public Boolean shouldDebugBool = null;

    @Override
    public boolean shouldDebug() {
        if (shouldDebugBool != null) {
            return shouldDebugBool;
        }
        if (internal.script == null || internal.script.getContainer() == null) {
            shouldDebugBool = fallbackDebug;
            return shouldDebugBool;
        }
        shouldDebugBool = internal.script.getContainer().shouldDebug();
        return shouldDebugBool;
    }

    @Override
    public boolean shouldFilter(String criteria) throws Exception {
        return internal.script.getName().equalsIgnoreCase(criteria.replace("s@", ""));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String str : getOriginalArguments()) {
            sb.append(" \"").append(str).append("\"");
        }
        for (aH.Argument arg : internal.preprocArgs) {
            sb.append(" \"").append(arg.toString()).append("\"");
        }
        return internal.command + sb.toString();
    }
}
