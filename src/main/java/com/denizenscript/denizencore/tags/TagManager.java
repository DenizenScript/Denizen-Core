package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.core.*;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class TagManager {

    public TagManager() {

    }

    public void registerCoreTags() {
        // Objects
        new DurationTagBase();
        new ElementTagBase();
        new ListTagBase();
        new QueueTagBase();
        new ScriptTagBase();

        // Utilities
        new ContextTagBase();
        new DefinitionTagBase();
        new EscapeTagBase();
        new ProcedureScriptTagBase();
        new TernaryTagBase();
        new UtilTagBase();
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TagEvents {
    }

    public static HashMap<String, TagRunnable.RootForm> handlers = new HashMap<>();

    @FunctionalInterface
    public interface OldTagRunner {
        void run(ReplaceableTagEvent event);
    }

    public static void registerTagHandler(TagRunnable.RootForm run, String... names) {
        if (names.length == 1) {
            run.name = names[0];
            handlers.put(run.name, run);
        }
        else {
            for (String name : names) {
                TagRunnable.RootForm rtemp = run.clone();
                rtemp.name = name;
                handlers.put(rtemp.name, rtemp);
            }
        }
    }

    public static void fireEvent(ReplaceableTagEvent event) {
        if (Debug.verbose) {
            Debug.log("Tag fire: " + event.raw_tag + ", " + event.isInstant() + ", " + event.getAttributes().attributes[0].rawKey.contains("@") + ", " + event.hasAlternative() + "...");
        }
        if (event.getAttributes().attributes[0].rawKey.contains("@")) {
            fetchObject(event);
            return;
        }
        TagRunnable.RootForm handler = handlers.get(event.getName());
        if (handler != null) {
            try {
                if (Debug.verbose) {
                    Debug.log("Tag handle: " + event.raw_tag + " " + handler.name + "...");
                }
                handler.run(event);
                if (event.replaced()) {
                    if (Debug.verbose) {
                        Debug.log("Tag handle success: " + event.getReplaced());
                    }
                    return;
                }
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
        }
        else {
            if (!event.hasAlternative()) {
                Debug.echoError("No tag-base handler for '" + event.getName() + "'.");
            }
        }
        if (Debug.verbose) {
            Debug.log("Tag unhandled!");
        }
    }

    // INTERNAL MAPPING NOTE:
    // 0x00: Null, reserved for special handlers
    // 0x01: <
    // 0x02: >
    // 0x04: Reserved for impl
    // 0x05: |
    // 0x2011: ;

    /**
     * Cleans escaped symbols generated within Tag Manager so that
     * they can be parsed now.
     *
     * @param input the potentially escaped input string.
     * @return the cleaned output string.
     */
    public static String cleanOutput(String input) {
        if (input == null) {
            return null;
        }
        char[] data = input.toCharArray();
        for (int i = 0; i < data.length; i++) {
            switch (data[i]) {
                case 0x01:
                    data[i] = '<';
                    break;
                case 0x02:
                    data[i] = '>';
                    break;
                case 0x07:
                    data[i] = '[';
                    break;
                case 0x09:
                    data[i] = ']';
                    break;
                case ListTag.internal_escape_char:
                    data[i] = '|';
                    break;
                default:
                    break;
            }
        }
        return new String(data);
    }

    /**
     * Cleans any potential internal escape characters (secret characters
     * used to hold the place of symbols that might get parsed weirdly
     * like > or | ) back into their proper form. Use this function
     * when outputting information that is going to be read by a
     * person.
     *
     * @param input the potentially escaped input string.
     * @return the cleaned output string.
     */
    public static String cleanOutputFully(String input) {
        if (input == null) {
            return null;
        }
        char[] data = input.toCharArray();
        for (int i = 0; i < data.length; i++) {
            switch (data[i]) {
                case 0x01:
                    data[i] = '<';
                    break;
                case 0x02:
                    data[i] = '>';
                    break;
                case 0x2011:
                    data[i] = ';';
                    break;
                case 0x07:
                    data[i] = '[';
                    break;
                case 0x09:
                    data[i] = ']';
                    break;
                case ListTag.internal_escape_char:
                    data[i] = '|';
                    break;
                case 0x00A0:
                    data[i] = ' ';
                    break;
                default:
                    break;
            }
        }
        return new String(data);
    }

    public static String escapeOutput(String input) {
        if (input == null) {
            return null;
        }
        char[] data = input.toCharArray();
        for (int i = 0; i < data.length; i++) {
            switch (data[i]) {
                case '<':
                    data[i] = 0x01;
                    break;
                case '>':
                    data[i] = 0x02;
                    break;
                case '[':
                    data[i] = 0x07;
                    break;
                case ']':
                    data[i] = 0x09;
                    break;
                case '|':
                    data[i] = ListTag.internal_escape_char;
                    break;
                default:
                    break;
            }
        }
        return new String(data);
    }

    public static void fetchObject(ReplaceableTagEvent event) {
        String object_type = CoreUtilities.toLowerCase(CoreUtilities.split(event.getAttributes().attributes[0].rawKey, '@').get(0));
        Class object_class = ObjectFetcher.getObjectClass(object_type);

        if (object_class == null) {
            if (!event.hasAlternative()) {
                Debug.echoError("Invalid object type! Could not fetch '" + object_type + "'!");
                event.setReplaced("null");
            }
            return;
        }

        ObjectTag arg;
        try {

            String tagObjectFull = event.hasNameContext() ? event.getAttributes().attributes[0].rawKey + '[' + event.getNameContext() + ']'
                    : event.getAttributes().attributes[0].rawKey;
            if (!ObjectFetcher.checkMatch(object_class, tagObjectFull)) {
                if (!event.hasAlternative()) {
                    Debug.echoDebug(event.getScriptEntry(), "Returning null. '" + event.getAttributes().attributes[0].rawKey
                            + "' is an invalid " + object_class.getSimpleName() + ".");
                    event.setReplaced("null");
                }
                return;
            }

            arg = ObjectFetcher.getObjectFrom(object_class, tagObjectFull, DenizenCore.getImplementation().getTagContext(event.getScriptEntry()));

            if (arg == null) {
                if (!event.hasAlternative()) {
                    Debug.echoError(((event.hasNameContext() ? event.getAttributes().attributes[0].rawKey + '[' + event.getNameContext() + ']'
                            : event.getAttributes().attributes[0].rawKey) + " is an invalid ObjectTag!"));
                    event.setReplaced("null");
                }
                return;
            }

            Attribute attribute = event.getAttributes();
            event.setReplacedObject(CoreUtilities.autoAttrib(arg, attribute.fulfill(1)));
        }
        catch (Exception e) {
            Debug.echoError("Uh oh! Report this to the Denizen developers! Err: TagManagerObjectReflection");
            Debug.echoError(e);
            if (!event.hasAlternative()) {
                event.setReplaced("null");
            }
        }
    }

    public static boolean isInTag = false;

    public static void executeWithTimeLimit(final ReplaceableTagEvent event, int seconds) {

        ExecutorService executor = Executors.newFixedThreadPool(4);

        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    DenizenCore.getImplementation().preTagExecute();
                    if (isInTag) {
                        fireEvent(event);
                    }
                    else {
                        isInTag = true;
                        fireEvent(event);
                        isInTag = false;
                    }
                }
                finally {
                    DenizenCore.getImplementation().postTagExecute();
                }
            }
        });

        executor.shutdown();

        try {
            future.get(seconds, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Debug.echoError("Tag filling was interrupted!");
        }
        catch (ExecutionException e) {
            Debug.echoError(e);
        }
        catch (TimeoutException e) {
            future.cancel(true);
            Debug.echoError("Tag filling timed out!");
        }

        executor.shutdownNow();
    }

    public static String readSingleTag(String str, TagContext context) {
        ReplaceableTagEvent event = new ReplaceableTagEvent(str, context);
        if (event.isInstant() != context.instant) {
            return String.valueOf((char) 0x01) + str.replace('<', (char) 0x01).replace('>', (char) 0x02) + String.valueOf((char) 0x02);
        }
        return escapeOutput(readSingleTagObject(context, event).toString());
    }

    public static ObjectTag readSingleTagObject(ParseableTagPiece tag, TagContext context) {
        if (tag.tagData.isInstant != context.instant) {
            return new ElementTag("<" + tag.content + ">");
        }
        ReplaceableTagEvent event = new ReplaceableTagEvent(tag.tagData, tag.content, context);
        return readSingleTagObject(context, event);
    }

    public static ObjectTag readSingleTagObject(TagContext context, ReplaceableTagEvent event) {
        // Call Event
        int tT = DenizenCore.getImplementation().getTagTimeout();
        if (Debug.verbose) {
            Debug.log("Tag read: " + event.raw_tag + ", " + event.isInstant() + ", " + tT + "...");
        }
        if (tT <= 0 || isInTag || (!DenizenCore.getImplementation().shouldDebug(context) && !DenizenCore.getImplementation().tagTimeoutWhenSilent())) {
            fireEvent(event);
        }
        else {
            executeWithTimeLimit(event, tT);
        }
        if (!event.replaced() && event.hasAlternative()) {
            event.setReplacedObject(event.getAlternative());
        }
        if (context.debug && event.replaced()) {
            DenizenCore.getImplementation().debugTagFill(context, event.toString(), event.getReplacedObj().debuggable());
        }
        if (!event.replaced()) {
            ScriptQueue queue = context.entry != null ? context.entry.getResidingQueue() : null;
            String tagStr = "<" + event.toString() + ">";
            Debug.echoError(queue, "Tag " + tagStr + " is invalid!");
            if (OBJECTTAG_CONFUSION_PATTERN.matcher(tagStr).matches()) {
                Debug.echoError(queue, "'ObjectTag' notation is for documentation purposes, and not to be used literally."
                    + " An actual object must be inserted instead. If confused, join our Discord at https://discord.gg/Q6pZGSR to ask for help!");
            }
            return new ElementTag(event.raw_tag);
        }
        return event.getReplacedObj();
    }

    public static Pattern OBJECTTAG_CONFUSION_PATTERN = Pattern.compile("<\\w+tag[\\[.>].*", Pattern.CASE_INSENSITIVE);

    static HashMap<String, List<ParseableTagPiece>> preCalced = new HashMap<>();

    public static class ParseableTagPiece {

        public String content;

        public ObjectTag objResult = null;

        public boolean isTag = false;

        public boolean isError = false;

        public ReplaceableTagEvent.ReferenceData tagData = null;

        @Override
        public String toString() {
            return "(" + isError + ", " + isTag + ", " + (isTag ? tagData.isInstant + ", " + tagData.rawTag : "") + ", " + content + "," + objResult + ")";
        }

        public ParseableTagPiece duplicate() {
            ParseableTagPiece newPiece = new ParseableTagPiece();
            newPiece.content = content;
            newPiece.objResult = objResult;
            newPiece.isTag = isTag;
            newPiece.isError = isError;
            newPiece.tagData = tagData;
            return newPiece;
        }
    }

    public static ObjectTag parseChainObject(List<ParseableTagPiece> pieces, TagContext context, boolean repush) {
        if (Debug.verbose) {
            Debug.log("Tag parse chain: " + pieces + "...");
            try {
                throw new RuntimeException("Stack");
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
        }
        if (pieces.size() < 2) {
            if (pieces.size() == 0) {
                return new ElementTag("");
            }
            ParseableTagPiece pzero = pieces.get(0);
            if (pzero.isError) {
                Debug.echoError(context.entry != null ? context.entry.getResidingQueue() : null, pzero.content);
            }
            else if (pzero.isTag) {
                ObjectTag objt = readSingleTagObject(pzero, context);
                if (repush && (!pzero.isTag || pzero.tagData.isInstant == context.instant)) {
                    ParseableTagPiece piece = new ParseableTagPiece();
                    piece.objResult = objt;
                    pieces.set(0, piece);
                }
                return objt;
            }
            else if (pzero.objResult != null) {
                return pzero.objResult;
            }
            return new ElementTag(pieces.get(0).content);
        }
        StringBuilder helpy = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            ParseableTagPiece p = pieces.get(i);
            if (p.isError) {
                Debug.echoError(context.entry != null ? context.entry.getResidingQueue() : null, p.content);
            }
            else if (p.isTag) {
                ObjectTag objt = readSingleTagObject(p, context);
                if (repush && (!p.isTag || p.tagData.isInstant == context.instant)) {
                    ParseableTagPiece piece = new ParseableTagPiece();
                    piece.objResult = objt;
                    pieces.set(i, piece);
                }
                helpy.append(objt.toString());
            }
            else if (p.objResult != null) {
                helpy.append(p.objResult.toString());
            }
            else {
                helpy.append(p.content);
            }
        }
        return new ElementTag(helpy.toString());
    }

    public static String tag(String arg, TagContext context) {
        return cleanOutput(tagObject(arg, context).toString());
    }

    public static List<ParseableTagPiece> dupChain(List<ParseableTagPiece> chain) {
        List<ParseableTagPiece> newPieces = new ArrayList<>(chain.size());
        for (TagManager.ParseableTagPiece piece : chain) {
            newPieces.add(piece.duplicate());
        }
        return newPieces;
    }

    public static List<ParseableTagPiece> genChain(String arg, ScriptEntry entry) {
        return genChain(arg, DenizenCore.getImplementation().getTagContext(entry));
    }

    public static List<ParseableTagPiece> genChain(String arg, TagContext context) {
        if (arg == null) {
            return null;
        }
        arg = cleanOutput(arg);
        List<ParseableTagPiece> pieces = preCalced.get(arg);
        if (pieces != null) {
            return pieces;
        }
        pieces = new ArrayList<>();
        if (arg.indexOf('>') == -1 || arg.length() < 3) {
            ParseableTagPiece txt = new ParseableTagPiece();
            txt.content = arg;
            pieces.add(txt);
            return pieces;
        }
        int[] positions = new int[2];
        positions[0] = -1;
        locateTag(arg, positions);
        if (positions[0] == -1) {
            ParseableTagPiece txt = new ParseableTagPiece();
            txt.content = arg;
            pieces.add(txt);
            return pieces;
        }
        String orig = arg;
        while (positions[0] != -1) {
            ParseableTagPiece preText = null;
            if (positions[0] > 0) {
                preText = new ParseableTagPiece();
                preText.content = arg.substring(0, positions[0]);
                pieces.add(preText);
            }
            String tagToProc = arg.substring(positions[0] + 1, positions[1]);
            ParseableTagPiece midTag = new ParseableTagPiece();
            midTag.content = tagToProc;
            midTag.isTag = true;
            midTag.tagData = new ReplaceableTagEvent(tagToProc, context).mainRef;
            pieces.add(midTag);
            if (Debug.verbose) {
                Debug.log("Tag: " + (preText == null ? "<null>" : preText.content) + " ||| " + midTag.content);
            }
            arg = arg.substring(positions[1] + 1);
            locateTag(arg, positions);
        }
        if (arg.indexOf('<') != -1) {
            ParseableTagPiece errorNote = new ParseableTagPiece();
            errorNote.isError = true;
            errorNote.content = "Potential issue: inconsistent tag marks in command! (issue snippet: " + arg + "; from: " + orig + ")";
            pieces.add(errorNote);
        }
        if (arg.length() > 0) {
            ParseableTagPiece postText = new ParseableTagPiece();
            postText.content = arg;
            pieces.add(postText);
        }
        if (Debug.verbose) {
            Debug.log("Tag chainify complete: " + arg);
        }
        return pieces;
    }

    public static ObjectTag tagObject(String arg, TagContext context) {
        return parseChainObject(genChain(arg, context), context, false);
    }

    public static int findColonNotTagNorSpace(String arg) {
        if (arg.indexOf(':') == -1) {
            return -1;
        }
        char[] arr = arg.toCharArray();
        int bracks = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '<') {
                bracks++;
            }
            else if (arr[i] == '>') {
                bracks--;
            }
            else if (arr[i] == ':' && bracks == 0) {
                return i;
            }
            else if (arr[i] == ' ' && bracks == 0) {
                return -1;
            }
        }
        return -1;
    }

    private static void locateTag(String arg, int[] holder) {
        int first = arg.indexOf('<');
        holder[0] = first;
        if (first == -1) {
            return;
        }
        int len = arg.length();
        // Handle "<-" for the flag command
        if (first + 1 < len && (arg.charAt(first + 1) == '-')) {
            locateTag(arg.substring(0, first) + (char) 0x01 + arg.substring(first + 1), holder);
            return;
        }
        int bracks = 1;
        for (int i = first + 1; i < len; i++) {
            if (arg.charAt(i) == '<') {
                bracks++;
            }
            else if (arg.charAt(i) == '>') {
                bracks--;
                if (bracks == 0) {
                    holder[1] = i;
                    return;
                }
            }
        }
        holder[0] = -1;
    }

    public static List<ObjectTag> fillArgumentsObjects(List<String> args, TagContext context) {
        if (Debug.verbose) {
            Debug.log("Fill argument objects (old): " + args + ", " + context.instant + "...");
        }
        List<ObjectTag> filledArgs = new ArrayList<>();

        int nested_level = 0;
        if (args != null) {
            for (String argument : args) {
                // Check nested level to avoid filling tags prematurely.
                if (argument.equals("{")) {
                    nested_level++;
                }
                if (argument.equals("}")) {
                    nested_level--;
                }
                // If this argument isn't nested, fill the tag.
                if (nested_level < 1) {
                    filledArgs.add(tagObject(argument, context));
                }
                else {
                    filledArgs.add(new ElementTag(argument));
                }
            }
        }
        return filledArgs;
    }

    public static void fillArgumentsObjects(List<ObjectTag> args, List<String> strArgs, List<ScriptEntry.InternalArgument> pieceHelp, List<Argument> aHArgs, boolean repush, TagContext context, int[] targets) {
        if (Debug.verbose) {
            Debug.log("Fill argument objects: " + args + ", " + context.instant + ", " + targets.length + "...");
        }
        for (int argId : targets) {
            Argument aharg = aHArgs.get(argId);
            if (aharg.needsFill || aharg.hasSpecialPrefix) {
                ScriptEntry.InternalArgument piece = pieceHelp.get(argId);
                if (piece.prefix != null) {
                    if (piece.prefix.aHArg.needsFill) {
                        aharg.prefix = parseChainObject(piece.prefix.value, context, repush).toString();
                        aharg.lower_prefix = CoreUtilities.toLowerCase(aharg.prefix);
                    }
                    if (aharg.needsFill) {
                        aharg.object = parseChainObject(piece.value, context, repush);
                    }
                    String fullx = aharg.prefix + ":" + aharg.object.toString();
                    args.set(argId, new ElementTag(fullx));
                    strArgs.set(argId, fullx);
                }
                else {
                    ObjectTag created = parseChainObject(piece.value, context, repush);
                    args.set(argId, created);
                    strArgs.set(argId, created.toString());
                    aharg.object = created;
                    aharg.prefix = null;
                    aharg.lower_prefix = null;
                }
            }
        }
    }

    public static List<String> fillArguments(List<String> args, TagContext context) {
        List<String> filledArgs = new ArrayList<>();

        int nested_level = 0;
        if (args != null) {
            for (String argument : args) {
                // Check nested level to avoid filling tags prematurely.
                if (argument.equals("{")) {
                    nested_level++;
                }
                if (argument.equals("}")) {
                    nested_level--;
                }
                // If this argument isn't nested, fill the tag.
                if (nested_level < 1) {
                    filledArgs.add(tag(argument, context));
                }
                else {
                    filledArgs.add(argument);
                }
            }
        }
        return filledArgs;
    }

    public static List<String> fillArguments(String[] args, TagContext context) {
        List<String> filledArgs = new ArrayList<>();
        if (args != null) {
            for (String argument : args) {
                filledArgs.add(tag(argument, context));
            }
        }
        return filledArgs;
    }
}
