package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.BracedCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagManager;

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

        public List<InternalArgument> args_ref = null;

        public ScriptTag script = null;

        public List<Object> insideList = null;

        public boolean instant = false;

        public boolean waitfor = false;

        public boolean hasTags = false;

        public boolean hasInstantTags = false;

        public boolean hasOldDefs = false;

        public int[] processArgs = null;

        public List<Argument> preprocArgs = null;

        public Object specialProcessedData = null;

        public String originalLine = null;

        public int lineNumber;
    }

    public static class InternalArgument {

        public InternalArgument prefix = null;

        public List<TagManager.ParseableTagPiece> value = null;

        public Argument aHArg = null;

        public InternalArgument duplicate() {
            InternalArgument newArg = new InternalArgument();
            newArg.prefix = prefix == null ? null : prefix.duplicate();
            newArg.value = new ArrayList<>(value);
            newArg.aHArg = aHArg == null ? null : aHArg.clone();
            return newArg;
        }
    }

    public List<Argument> getProcessedArgs() {
        return ArgumentHelper.interpretArguments(aHArgs);
    }

    public List<InternalArgument> args_cur = null;

    public List<Argument> aHArgs = null;

    public List<String> args = null;

    public List<ObjectTag> processed_arguments = null;

    public ScriptEntryData entryData = null;

    private ScriptQueue queue = null;

    public ScriptEntryInternal internal = null;

    public List<BracedCommand.BracedData> getBracedSet() {
        return internal.bracedSet;
    }

    public void setBracedSet(List<BracedCommand.BracedData> set) {
        internal.bracedSet = set;
    }

    private Map<String, Object> objects = new HashMap<>();

    public void regenerateArgsCur() {
        args_cur = new ArrayList<>(internal.args_ref);
        for (int i : internal.processArgs) {
            InternalArgument arg = args_cur.get(i).duplicate();
            args_cur.set(i, arg);
            arg.aHArg = aHArgs.get(i);
        }
    }

    public final static Argument NULL_ARGUMENT = new Argument("null_trick", "null_trick");

    public void generateAHArgs() {
        aHArgs = new ArrayList<>(internal.args_ref.size());
        for (int i = 0; i < internal.args_ref.size(); i++) {
            aHArgs.add(internal.args_ref.get(i) == null ? NULL_ARGUMENT : internal.args_ref.get(i).aHArg);
        }
        for (int i : internal.processArgs) {
            InternalArgument arg = internal.args_ref.get(i);
            arg.aHArg.scriptEntry = this;
            aHArgs.set(i, arg.aHArg.needsFill || arg.aHArg.hasSpecialPrefix || (internal.hasOldDefs && arg.aHArg.raw_value.indexOf('%') != -1) ? arg.aHArg.clone() : arg.aHArg);
        }
    }

    @Override
    public ScriptEntry clone() throws CloneNotSupportedException {
        ScriptEntry se = (ScriptEntry) super.clone();
        se.objects = new HashMap<>();
        se.processed_arguments = processed_arguments == null ? null : new ArrayList<>(processed_arguments);
        se.args = new ArrayList<>(args);
        se.entryData = entryData.clone();
        se.entryData.scriptEntry = se;
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
     */
    public ScriptEntry(String command, String[] arguments, ScriptContainer script) {
        this(command, arguments, script, null);
    }

    public void crunchInto(InternalArgument argVal, String arg, TagContext refContext) {
        if (arg.indexOf('%') != -1) {
            internal.hasOldDefs = true;
            argVal.value = new LinkedList<>();
            TagManager.ParseableTagPiece piece = new TagManager.ParseableTagPiece();
            piece.content = arg;
            argVal.value.add(piece);
        }
        else {
            argVal.value = TagManager.dupChain(TagManager.genChain(arg, refContext));
        }
        boolean isTag = false;
        int indStart = arg.indexOf('<');
        if (indStart >= 0) {
            int indEnd = arg.lastIndexOf('>');
            if (indEnd > indStart) {
                isTag = true;
                internal.hasTags = true;
                while (!internal.hasInstantTags && indStart >= 0 && indStart + 1 < indEnd) {
                    char c = arg.charAt(indStart + 1);
                    if (c == '!' || c == '^') {
                        internal.hasInstantTags = true;
                    }
                    indStart = arg.indexOf('<', indStart + 1);
                }
            }
        }
        argVal.aHArg = new Argument(argVal.prefix == null ? null : argVal.prefix.aHArg.raw_value, arg);
        argVal.aHArg.needsFill = isTag;
        argVal.aHArg.hasSpecialPrefix = argVal.prefix != null;
    }

    public ScriptEntry(String command, String[] arguments, ScriptContainer script, List<Object> insides) {
        if (command == null) {
            throw new RuntimeException("Command name cannot be null!");
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
                    Debug.echoError("The command '" + internal.command + "' cannot be waited for!");
                }
            }
            internal.actualCommand = DenizenCore.getCommandRegistry().get(internal.command);
            if (internal.actualCommand != null && internal.actualCommand.forceHold) {
                internal.waitfor = true;
            }
        }
        else {
            internal.actualCommand = null;
        }
        if (arguments != null) {
            args = new ArrayList<>(arguments.length);
            internal.preprocArgs = new ArrayList<>();
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
                if (parg.endsWith("{")) {
                    after = "{";
                    parg = parg.substring(0, parg.length() - 1);
                    Debug.echoError("Command '" + command + "' in script '" + (script == null ? "(None)" : script.getName()) + "' has typo: brace written without space... like 'arg{' when it should be 'arg {'.");
                }
                Argument argObj = new Argument(arg);
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
            internal.pre_tagged_args = new ArrayList<>(args);
            nested_depth = 0;
            TagContext refContext = DenizenCore.getImplementation().getTagContext(this);
            internal.args_ref = new ArrayList<>(args.size());
            List<Integer> tempProcessArgs = new ArrayList<>();
            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);
                if (arg.equals("{")) {
                    InternalArgument brace = new InternalArgument();
                    brace.aHArg = new Argument("", "{");
                    internal.args_ref.add(brace);
                    nested_depth++;
                    continue;
                }
                if (arg.equals("}")) {
                    InternalArgument brace = new InternalArgument();
                    brace.aHArg = new Argument("", "}");
                    internal.args_ref.add(brace);
                    nested_depth--;
                    continue;
                }
                internal.args_ref.add(null);
                if (nested_depth > 0) {
                    continue;
                }
                tempProcessArgs.add(i);
                InternalArgument argVal = new InternalArgument();
                internal.args_ref.set(i, argVal);
                int colon = TagManager.findColonNotTagNorSpace(arg);
                if (colon > 0) {
                    argVal.prefix = new InternalArgument();
                    crunchInto(argVal.prefix, arg.substring(0, colon), refContext);
                    arg = arg.substring(colon + 1);
                }
                crunchInto(argVal, arg, refContext);
            }
            internal.processArgs = new int[tempProcessArgs.size()];
            for (int i = 0; i < tempProcessArgs.size(); i++) {
                internal.processArgs[i] = tempProcessArgs.get(i);
            }
            objectify();
        }
        else {
            args = new ArrayList<>();
            internal.preprocArgs = new ArrayList<>();
            internal.pre_tagged_args = new ArrayList<>();
            internal.processArgs = new int[0];
            internal.args_ref = new ArrayList<>();
            processed_arguments = new ArrayList<>();
        }
        if (internal.actualCommand != null) {
            if (internal.actualCommand.getOptions().requiredArgs > args.size()) {
                broken = true;
            }
            if (internal.actualCommand instanceof BracedCommand) {
                BracedCommand.getBracedCommands(this);
            }
        }
    }

    /**
     * Adds a context object to the script entry. Just provide a key and an object.
     * Technically any type of object can be stored, however providing ObjectTags
     * is preferred.
     *
     * @param key    the name of the object
     * @param object the object, preferably a ObjectTag
     */
    public ScriptEntry addObject(String key, Object object) {
        if (object == null) {
            return this;
        }
        if (object instanceof ObjectTag) {
            ((ObjectTag) object).setPrefix(key);
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
            processed_arguments.set(ind, new ElementTag(val));
        }
    }

    public ScriptEntry setArguments(List<String> arguments) {
        args = arguments;
        return this;
    }

    public ScriptEntry setArgumentsObjects(List<ObjectTag> arguments) {
        processed_arguments = arguments;
        args = new ArrayList<>(arguments.size()); // TODO: Placeholder! Remove old string args entirely!
        for (ObjectTag tmp : arguments) {
            args.add(TagManager.escapeOutput(tmp.toString()));
        }
        return this;
    }

    public void objectify() {
        processed_arguments = new ArrayList<>(args.size());
        for (String arg : args) {
            processed_arguments.add(new ElementTag(arg));
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
            if (Debug.verbose) {
                Debug.echoError(ex);
            }
            return null;
        }
    }

    public <T extends ObjectTag> T getObjectTag(String key) {
        try {
            // If an ENUM, return as an Element
            Object gotten = objects.get(key);
            if (gotten instanceof Enum) {
                return (T) new ElementTag(((Enum) gotten).name());
            }
            // Otherwise, just return the stored ObjectTag
            return (T) gotten;
            // If not a ObjectTag, return null
        }
        catch (Exception ex) {
            if (Debug.verbose) {
                Debug.echoError(ex);
            }
            return null;
        }
    }

    public ElementTag getElement(String key) {
        try {
            return (ElementTag) objects.get(key);
        }
        catch (Exception ex) {
            if (Debug.verbose) {
                Debug.echoError(ex);
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

    public ScriptTag getScript() {
        return internal.script;
    }

    public ScriptEntry setScript(String scriptName) {
        internal.script = ScriptTag.valueOf(scriptName);
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
    public List<String> tracked_objects = new ArrayList<>();

    public ScriptEntry trackObject(String key) {
        tracked_objects.add(CoreUtilities.toLowerCase(key));
        return this;
    }

    /////////////
    // DEBUGGABLE
    /////////

    public boolean fallbackDebug = true;

    public Boolean shouldDebugBool = null;

    public boolean dbCallShouldDebug() {
        return DenizenCore.getImplementation().shouldDebug(this);
    }

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
            sb.append(" \"").append(str.replace("\"", "<&dq>")).append("\"");
        }
        for (Argument arg : internal.preprocArgs) {
            sb.append(" \"").append(arg.toString().replace("\"", "<&dq>")).append("\"");
        }
        return internal.command + sb.toString();
    }
}
