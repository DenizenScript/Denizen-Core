package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.ObjectType;
import com.denizenscript.denizencore.scripts.ScriptBuilder;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.core.FlagCommand;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.InstantQueue;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagManager;

import java.util.*;
import java.util.function.Consumer;

/**
 * Basic utilities for running '/ex' command implementations. Not for general use.
 */
public class ExCommandHelper {

    /**
     * Creates and starts a queue based on a single command; for '/ex' command implementations.
     */
    public static ScriptQueue runString(String id, String command, ScriptEntryData data, Consumer<ScriptQueue> configure) {
        if (data == null) {
            data = DenizenCore.implementation.getEmptyScriptEntryData();
        }
        InstantQueue queue = new InstantQueue(id);
        queue.addEntries(ScriptBuilder.buildScriptEntries(Collections.singletonList(command), null, data));
        if (configure != null) {
            configure.accept(queue);
        }
        queue.start(true);
        return queue;
    }

    /**
     * ScriptQueues stored from {@link #runStringSustained(Object, String, String, ScriptEntryData, Consumer)}.
     */
    public static final Map<Object, TimedQueue> sustainedQueues = new HashMap<>();

    /**
     * Creates and starts a queue based on a single command; for sustained '/exs' command implementations.
     * The queue will be held in {@link #sustainedQueues} using the given {@code Object source} until {@link #removeSustainedQueue(Object)} is called.
     * Calling this method again using the same {@code Object source} will continue the same queue.
     */
    public static ScriptQueue runStringSustained(Object source, String id, String command, ScriptEntryData data, Consumer<ScriptQueue> configure) {
        if (data == null) {
            data = DenizenCore.implementation.getEmptyScriptEntryData();
        }
        TimedQueue queue = sustainedQueues.get(source);
        if (queue == null || queue.isStopped) {
            queue = new TimedQueue(id);
            queue.waitWhenEmpty = true;
            sustainedQueues.put(source, queue);
        }
        queue.addEntries(ScriptBuilder.buildScriptEntries(Collections.singletonList(command), null, data));
        if (configure != null) {
            configure.accept(queue);
        }
        if (!queue.is_started) {
            queue.start(true);
        }
        else {
            queue.onStart();
        }
        return queue;
    }

    /**
     * Removes a sustained queue for the given {@code Object source}, as created by {@link #runStringSustained(Object, String, String, ScriptEntryData, Consumer)}
     * and returns whether the queue existed (and was still capable of continuing).
     */
    public static boolean removeSustainedQueue(Object source) {
        ScriptQueue queue = sustainedQueues.remove(source);
        return queue != null && !queue.isStopped;
    }

    public static List<String> buildTabCompletions(String[] rawArgs, TagContext context) {
        String entry = String.join(" ", rawArgs);
        if (entry.length() > 3 && entry.startsWith("-q ")) {
            entry = entry.substring("-q ".length());
        }
        String[] args = ArgumentHelper.buildArgs(entry, true);
        boolean isNewArg = rawArgs.length == 0 || rawArgs[rawArgs.length - 1].isEmpty();
        boolean isCommandArg = args.length == 0 || (args.length == 1 && !isNewArg) || args[args.length - (isNewArg ? 1 : 2)].equals("-");
        if (isCommandArg) {
            if (isNewArg || args.length == 0) {
                return new ArrayList<>(DenizenCore.commandRegistry.instances.keySet());
            }
            ArrayList<String> output = new ArrayList<>();
            String startOfName = CoreUtilities.toLowerCase(args[args.length - 1]);
            for (String command : DenizenCore.commandRegistry.instances.keySet()) {
                if (command.startsWith(startOfName)) {
                    output.add(command);
                }
            }
            return output;
        }
        String lowArg = CoreUtilities.toLowerCase(rawArgs[rawArgs.length - 1]);
        AbstractCommand.TabCompletionsBuilder completionsBuilder = new AbstractCommand.TabCompletionsBuilder();
        completionsBuilder.arg = lowArg;
        completionsBuilder.context = context;
        if (!isNewArg) {
            String lastArg = rawArgs[rawArgs.length - 1];
            int argStart = 0;
            for (int i = 0; i < lastArg.length(); i++) {
                if (lastArg.charAt(i) == '"' || lastArg.charAt(i) == '\'') {
                    char quote = lastArg.charAt(i++);
                    while (i < lastArg.length() && lastArg.charAt(i) != quote) {
                        i++;
                    }
                }
                else if (lastArg.charAt(i) == ' ') {
                    argStart = i + 1;
                }
            }
            String arg = lastArg.substring(argStart);
            if (CoreUtilities.contains(arg, '<')) {
                int tagBits = 0;
                int relevantTagStart = -1;
                for (int i = arg.length() - 1; i >= 0; i--) {
                    if (arg.charAt(i) == '>') {
                        tagBits++;
                    }
                    else if (arg.charAt(i) == '<') {
                        if (tagBits == 0) {
                            relevantTagStart = i + 1;
                            break;
                        }
                        tagBits--;
                    }
                }
                if (relevantTagStart != -1) {
                    String fullTag = CoreUtilities.toLowerCase(arg.substring(relevantTagStart));
                    int components = 0;
                    int subTags = 0;
                    int squareBrackets = 0;
                    int lastDot = 0;
                    int bracketStart = -1;
                    Collection<Class<? extends ObjectTag>> typesApplicable = null;
                    for (int i = 0; i < fullTag.length(); i++) {
                        char c = fullTag.charAt(i);
                        if (c == '<') {
                            subTags++;
                        }
                        else if (c == '>') {
                            subTags--;
                        }
                        else if (c == '[' && subTags == 0) {
                            squareBrackets++;
                            bracketStart = i;
                        }
                        else if (c == ']' && subTags == 0) {
                            squareBrackets--;
                        }
                        else if (c == '.' && subTags == 0 && squareBrackets == 0) {
                            Class<? extends ObjectTag> type = null;
                            String part = fullTag.substring(lastDot, bracketStart == -1 ? i : bracketStart);
                            if (components == 0) {
                                TagManager.TagBaseData baseType = TagManager.baseTags.get(part);
                                if (baseType != null) {
                                    type = baseType.returnType;
                                }
                            }
                            else if (typesApplicable != null) {
                                for (Class<? extends ObjectTag> possibleType : typesApplicable) {
                                    ObjectType<? extends ObjectTag> typeData = ObjectFetcher.getType(possibleType);
                                    if (typeData != null && typeData.tagProcessor != null) {
                                        ObjectTagProcessor.TagData data = typeData.tagProcessor.registeredObjectTags.get(part);
                                        if (data != null && data.returnType != null) {
                                            type = data.returnType;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (type != null) {
                                typesApplicable = ObjectFetcher.getAllApplicableSubTypesFor(type);
                            }
                            else {
                                typesApplicable = ObjectFetcher.objectsByClass.keySet();
                            }
                            components++;
                            lastDot = i + 1;
                            bracketStart = -1;
                        }
                    }
                    String beforeDot = arg.substring(0, relevantTagStart) + fullTag.substring(0, lastDot);
                    if (components == 0 && !CoreUtilities.contains(fullTag, '[')) {
                        ArrayList<String> output = new ArrayList<>();
                        for (String tagBase : TagManager.baseTags.keySet()) {
                            if (tagBase.startsWith(fullTag)) {
                                output.add(beforeDot + tagBase);
                            }
                        }
                        return output;
                    }
                    String subComponent = fullTag.substring(lastDot);
                    if (lastDot > 0) {
                        int squareBracket = subComponent.indexOf('[');
                        if (squareBracket == -1) {
                            ArrayList<String> output = new ArrayList<>();
                            for (Class<? extends ObjectTag> possibleType : typesApplicable) {
                                ObjectType<? extends ObjectTag> typeData = ObjectFetcher.getType(possibleType);
                                if (typeData != null && typeData.tagProcessor != null) {
                                    for (String tag : typeData.tagProcessor.registeredObjectTags.keySet()) {
                                        if (tag.startsWith(subComponent)) {
                                            output.add(beforeDot + tag);
                                        }
                                    }
                                }
                            }
                            return output;
                        }
                        else {
                            String tagPiece = subComponent.substring(0, squareBracket);
                            if (tagPiece.startsWith("flag") || tagPiece.equals("has_flag")) {
                                completionsBuilder.arg = subComponent.substring(squareBracket + 1);
                                FlagCommand.tabCompleteFlag(completionsBuilder);
                                return completionsBuilder.completions;
                            }
                        }
                    }
                }
            }
        }
        AbstractCommand dcmd = DenizenCore.commandRegistry.get(args[0]);
        for (int i = args.length - 2; i >= 0; i--) {
            if (args[i].equals("-")) {
                dcmd = DenizenCore.commandRegistry.get(args[i + 1]);
            }
        }
        if (dcmd == null) {
            return null;
        }
        for (String flat : dcmd.docFlagArgs) {
            completionsBuilder.add(flat);
        }
        for (String prefix : dcmd.docPrefixes) {
            completionsBuilder.add(prefix + ":");
        }
        dcmd.addCustomTabCompletions(completionsBuilder);
        return completionsBuilder.completions;
    }
}
