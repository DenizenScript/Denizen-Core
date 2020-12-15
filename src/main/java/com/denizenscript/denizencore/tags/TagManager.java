package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.core.*;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class TagManager {

    public void registerCoreTags() {
        // Objects
        new CustomTagBase();
        new DurationTagBase();
        new ElementTagBase();
        new ListTagBase();
        new MapTagBase();
        new QueueTagBase();
        new ScriptTagBase();
        new TimeTagBase();

        // Utilities
        new ContextTagBase();
        new DefinitionTagBase();
        new EscapeTagBase();
        new ListSingleTagBase();
        new ProcedureScriptTagBase();
        new TernaryTagBase();
        new UtilTagBase();
    }

    public static HashMap<String, TagRunnable.RootForm> rootFormHandlers = new HashMap<>();

    public static HashMap<String, TagRunnable.BaseInterface> baseHandlers = new HashMap<>();

    public static HashSet<String> properTagBases = new HashSet<>();

    public static void registerTagHandler(String name, TagRunnable.BaseInterface run) {
        properTagBases.add(name);
        baseHandlers.put(name, run);
    }

    public static void registerTagHandler(TagRunnable.RootForm run, String... names) {
        properTagBases.add(names[0]);
        if (names.length == 1) {
            run.name = names[0];
            rootFormHandlers.put(run.name, run);
        }
        else {
            for (String name : names) {
                TagRunnable.RootForm rtemp = run.clone();
                rtemp.name = name;
                rootFormHandlers.put(rtemp.name, rtemp);
            }
        }
    }

    public static void fireEvent(ReplaceableTagEvent event) {
        if (Debug.verbose) {
            Debug.log("Tag fire: " + event.raw_tag + ", " + event.getAttributes().attributes[0].rawKey.contains("@") + ", " + event.hasAlternative() + "...");
        }
        TagRunnable.BaseInterface baseHandler = event.mainRef.tagBaseHandler;
        if (baseHandler != null) {
            Attribute attribute = event.getAttributes();
            try {
                ObjectTag result = baseHandler.run(attribute);
                if (result != null) {
                    event.setReplacedObject(result.getObjectAttribute(attribute.fulfill(1)));
                    return;
                }
            }
            catch (Throwable ex) {
                Debug.echoError(ex);
            }
            attribute.echoError("Tag-base '" + attribute.getAttributeWithoutContext(1) + "' returned null.");
            return;
        }
        TagRunnable.RootForm handler = event.mainRef.rootFormHandler;
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
        return readSingleTagObject(context, event).toString();
    }

    public static ObjectTag readSingleTagObject(ParseableTagPiece tag, TagContext context) {
        ReplaceableTagEvent event = new ReplaceableTagEvent(tag.tagData, tag.content, context);
        return readSingleTagObject(context, event);
    }

    public static boolean recentTagError = true;

    public static ObjectTag readSingleTagObject(TagContext context, ReplaceableTagEvent event) {
        // Call Event
        int tT = DenizenCore.getImplementation().getTagTimeout();
        if (Debug.verbose) {
            Debug.log("Tag read: " + event.raw_tag + ", " + tT + "...");
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
            recentTagError = true;
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
            return "(" + isError + ", " + isTag + ", " + (isTag ? tagData.rawTag : "") + ", " + content + "," + objResult + ")";
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

    public static ObjectTag parseChainObject(List<ParseableTagPiece> pieces, TagContext context) {
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
            if (pieces.isEmpty()) {
                return new ElementTag("");
            }
            ParseableTagPiece pzero = pieces.get(0);
            if (pzero.isError) {
                Debug.echoError(context.entry != null ? context.entry.getResidingQueue() : null, pzero.content);
            }
            else if (pzero.isTag) {
                return readSingleTagObject(pzero, context);
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
                helpy.append(readSingleTagObject(p, context).toString());
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
        return tagObject(arg, context).toString();
    }

    public static List<ParseableTagPiece> dupChain(List<ParseableTagPiece> chain) {
        List<ParseableTagPiece> newPieces = new ArrayList<>(chain.size());
        for (TagManager.ParseableTagPiece piece : chain) {
            newPieces.add(piece.duplicate());
        }
        return newPieces;
    }

    public static List<ParseableTagPiece> genChain(String arg, TagContext context) {
        if (arg == null) {
            return null;
        }
        List<ParseableTagPiece> pieces = preCalced.get(arg);
        if (pieces != null) {
            return pieces;
        }
        pieces = new ArrayList<>(1);
        if (arg.indexOf('>') == -1 || arg.length() < 3) {
            ParseableTagPiece txt = new ParseableTagPiece();
            txt.content = arg;
            pieces.add(txt);
            return pieces;
        }
        int[] positions = new int[2];
        positions[0] = -1;
        locateTag(arg, positions, 0);
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
            locateTag(arg, positions, 0);
        }
        if (arg.indexOf('<') != -1 && !arg.contains(":<-")) {
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
        return parseChainObject(genChain(arg, context), context);
    }

    private static void locateTag(String arg, int[] holder, int start) {
        int first = arg.indexOf('<', start);
        holder[0] = first;
        if (first == -1) {
            return;
        }
        int len = arg.length();
        // Handle "<-" for the flag command
        if (first + 1 < len && (arg.charAt(first + 1) == '-')) {
            locateTag(arg, holder, first + 1);
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

    public static void fillArgumentsObjects(List<ScriptEntry.InternalArgument> pieceHelp, List<Argument> aHArgs, TagContext context, int[] targets) {
        if (Debug.verbose) {
            Debug.log("Fill argument objects: " + aHArgs + ", " + targets.length + "...");
        }
        for (int argId : targets) {
            Argument aharg = aHArgs.get(argId);
            ScriptEntry.InternalArgument piece = pieceHelp.get(argId);
            if (piece.prefix != null) {
                if (piece.prefix.aHArg.needsFill) {
                    aharg.prefix = parseChainObject(piece.prefix.value, context).toString();
                    aharg.lower_prefix = CoreUtilities.toLowerCase(aharg.prefix);
                }
                if (aharg.needsFill) {
                    aharg.object = parseChainObject(piece.value, context);
                }
            }
            else {
                ObjectTag created = parseChainObject(piece.value, context);
                aharg.object = created;
                aharg.prefix = null;
                aharg.lower_prefix = null;
            }
        }
    }
}
