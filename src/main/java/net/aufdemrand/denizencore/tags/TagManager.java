package net.aufdemrand.denizencore.tags;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.objects.ObjectFetcher;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.tags.core.*;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;

public class TagManager {

    public TagManager() {

    }

    public void registerCoreTags() {
        // Objects
        new ListTags();
        new QueueTags();
        new ScriptTags();

        // Utilities
        new ContextTags();
        new DefinitionTags();
        new EscapeTags();
        new ProcedureScriptTags();
        new UtilTags();

        registerTagEvents(this);
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public static @interface TagEvents {
    }

    private static List<Method> methods = new ArrayList<Method>();
    private static List<Object> method_objects = new ArrayList<Object>();

    public static void registerTagEvents(Object o) {
        for (Method method : o.getClass().getMethods()) {
            if (!method.isAnnotationPresent(TagManager.TagEvents.class)) {
                continue;
            }
            Class[] parameters = method.getParameterTypes();
            if (parameters.length != 1 || parameters[0] != ReplaceableTagEvent.class) {
                dB.echoError("Class " + o.getClass().getCanonicalName() + " has a method "
                        + method.getName() + " that is targeted at the event manager but has invalid parameters.");
                break;
            }
            registerMethod(method, o);
        }
    }

    public static void unregisterTagEvents(Object o) {
        for (int i = 0; i < methods.size(); i++) {
            if (method_objects.get(i) == o) {
                methods.remove(i);
                method_objects.remove(i);
                i--;
            }
        }
    }

    public static void registerMethod(Method method, Object o) {
        methods.add(method);
        method_objects.add(o);
    }

    public static void fireEvent(ReplaceableTagEvent event) {
        for (int i = 0; i < methods.size(); i++) {
            try {
                methods.get(i).invoke(method_objects.get(i), event);
            }
            catch (Exception ex) {
                dB.echoError(ex);
            }
        }
    }

    // INTERNAL MAPPING NOTE:
    // 0x01: <
    // 0x02: >
    // 0x04: Exclusively For Utilities.talkToNPC()
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
                case dList.internal_escape_char:
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
                case dList.internal_escape_char:
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
                    data[i] = dList.internal_escape_char;
                    break;
                default:
                    break;
            }
        }
        return new String(data);
    }

    @TagManager.TagEvents
    public void fetchObject(ReplaceableTagEvent event) {
        if (!event.getName().contains("@")) {
            return;
        }

        String object_type = CoreUtilities.toLowerCase(CoreUtilities.split(event.getName(), '@').get(0));
        Class object_class = ObjectFetcher.getObjectClass(object_type);

        if (object_class == null) {
            dB.echoError("Invalid object type! Could not fetch '" + object_type + "'!");
            event.setReplaced("null");
            return;
        }

        dObject arg;
        try {

            if (!ObjectFetcher.checkMatch(object_class, event.hasNameContext() ? event.getName() + '[' + event.getNameContext() + ']'
                    : event.getName())) {
                dB.echoDebug(event.getScriptEntry(), "Returning null. '" + event.getName()
                        + "' is an invalid " + object_class.getSimpleName() + ".");
                event.setReplaced("null");
                return;
            }

            arg = ObjectFetcher.getObjectFrom(object_class, event.hasNameContext() ? event.getName() + '[' + event.getNameContext() + ']'
                    : event.getName());

            if (arg == null) {
                dB.echoError(((event.hasNameContext() ? event.getName() + '[' + event.getNameContext() + ']'
                        : event.getName()) + " is an invalid dObject!"));
                return;
            }

            Attribute attribute = event.getAttributes();
            event.setReplaced(arg.getAttribute(attribute.fulfill(1)));
        }
        catch (Exception e) {
            dB.echoError("Uh oh! Report this to the Denizen developers! Err: TagManagerObjectReflection");
            dB.echoError(e);
        }
    }

    public static void executeWithTimeLimit(final ReplaceableTagEvent event, int seconds) {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                fireEvent(event);
            }
        });

        executor.shutdown();

        try {
            future.get(seconds, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            dB.echoError("Tag filling was interrupted!");
        }
        catch (ExecutionException e) {
            dB.echoError(e);
        }
        catch (TimeoutException e) {
            future.cancel(true);
            dB.echoError("Tag filling timed out!");
        }

        executor.shutdownNow();
    }

    public static String readSingleTag(String str, TagContext context) {
        ReplaceableTagEvent event = new ReplaceableTagEvent(str, context);
        if (event.isInstant() != context.instant) {
            // Not the right type of tag, escape the brackets so it doesn't get parsed again
            return String.valueOf((char) 0x01) + str.replace('<', (char)0x01).replace('>', (char)0x02) + String.valueOf((char) 0x02);
        }
        else {
            // Call Event
            int tT = DenizenCore.getImplementation().getTagTimeout();
            if (tT <= 0) {
                fireEvent(event);
            }
            else {
                executeWithTimeLimit(event, tT);
            }
            if ((!event.replaced() && event.getAlternative() != null) && event.hasAlternative()) {
                event.setReplaced(event.getAlternative());
            }
            if (context.debug) {
                dB.echoDebug(context.entry, "Filled tag <" + event.toString() + "> with '" +
                        event.getReplaced() + "'.");
            }
            if (!event.replaced()) {
                dB.echoError(context.entry != null ? context.entry.getResidingQueue() : null,
                        "Tag <" + event.toString() + "> is invalid!");
            }
            return escapeOutput(event.getReplaced());
        }
    }

    static HashMap<String, List<ParseableTagPiece>> preCalced = new HashMap<String, List<ParseableTagPiece>>();

    public static class ParseableTagPiece
    {
        public String content;

        public boolean isTag = false;
    }

    public static String parseChain(List<ParseableTagPiece> pieces, TagContext context) {
        if (pieces.size() < 2) {
            if (pieces.size() == 0) {
                return "";
            }
            if (pieces.get(0).isTag) {
                return readSingleTag(pieces.get(0).content, context);
            }
            return pieces.get(0).content;
        }
        StringBuilder helpy = new StringBuilder();
        for (ParseableTagPiece p : pieces) {
            helpy.append(p.isTag ? readSingleTag(p.content, context) : p.content);
        }
        return helpy.toString();
    }

    public static String tag(String arg, TagContext context) {
        if (arg == null) {
            return null;
        }

        List<ParseableTagPiece> pieces = preCalced.get(arg);

        if (pieces != null) {
            return cleanOutput(parseChain(pieces, context));
        }

        String oArg = arg;

        pieces = new ArrayList<ParseableTagPiece>();

        if (arg.indexOf('>') == -1 || arg.length() < 3) {
            ParseableTagPiece txt = new ParseableTagPiece();
            txt.content = arg;
            pieces.add(txt);
            preCalced.put(arg, pieces);
            return cleanOutput(arg);
        }

        int[] positions = locateTag(arg);

        if (positions == null) {
            ParseableTagPiece txt = new ParseableTagPiece();
            txt.content = arg;
            pieces.add(txt);
            preCalced.put(arg, pieces);
            return cleanOutput(arg);
        }

        int failsafe = 0;

        String lastExtra = arg;

        int lastEnder = 0;

        while (positions != null && failsafe < 100) {
            failsafe++;

            String oriarg = arg.substring(positions[0] + 1, positions[1]);

            ParseableTagPiece preText = null;
            if (lastEnder < positions[0]) {
                preText = new ParseableTagPiece();
                preText.content = arg.substring(lastEnder, positions[0]);
                pieces.add(preText);
            }
            ParseableTagPiece midTag = new ParseableTagPiece();
            midTag.content = oriarg;
            midTag.isTag = true;
            pieces.add(midTag);

            if (dB.verbose) {
                dB.log("Tag: " + (preText == null ? "<null>" : preText.content) + " ||| " + midTag.content);
            }

            String replaced = readSingleTag(oriarg, context);
            lastExtra = arg.substring(positions[1] + 1, arg.length());
            arg = arg.substring(0, positions[0]) + replaced + lastExtra;
            lastEnder = positions[0] + replaced.length();
            positions = locateTag(arg);
        }

        ParseableTagPiece postText = new ParseableTagPiece();
        postText.content = lastExtra;
        pieces.add(postText);

        preCalced.put(oArg, pieces);

        if (dB.verbose) {
            dB.log("Tag complete: " + lastExtra);
        }

        return cleanOutput(arg);
    }

    static int[] holder = new int[2];

    private static int[] locateTag(String arg) {
        int first = arg.indexOf('<');
        if (first == -1) {
            return null;
        }
        // Handle "<-" for the flag command
        if (first + 1 < arg.length() && (arg.charAt(first + 1) == '-')) {
            return locateTag(arg.substring(0, first) + (char) 0x01 + arg.substring(first + 1));
        }
        int len = arg.length();
        int bracks = 0;
        int second = -1;
        for (int i = first + 1; i < len; i++) {
            if (arg.charAt(i) == '<') {
                bracks++;
            }
            else if (arg.charAt(i) == '>') {
                bracks--;
                if (bracks == -1) {
                    second = i;
                    break;
                }
            }
        }
        if (first > -1 && second > first) {
            holder[0] = first;
            holder[1] = second;
            return holder;
        }
        else {
            return null;
        }
    }

    public static List<String> fillArguments(List<String> args, TagContext context) {
        List<String> filledArgs = new ArrayList<String>();

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
        List<String> filledArgs = new ArrayList<String>();
        if (args != null) {
            for (String argument : args) {
                filledArgs.add(tag(argument, context));
            }
        }
        return filledArgs;
    }
}
