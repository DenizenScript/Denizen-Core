package net.aufdemrand.denizencore.objects;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjectFetcher {

    // Keep track of each Class keyed by its 'object identifier' --> i@, e@, etc.
    private static Map<String, Class> objects = new HashMap<String, Class>();

    // Keep track of the static 'matches' and 'valueOf' methods for each dObject
    static Map<Class, Method> matches = new WeakHashMap<Class, Method>();
    static Map<Class, Method> valueof = new WeakHashMap<Class, Method>();

    public static void _initialize() throws IOException, ClassNotFoundException {

        if (fetchable_objects.isEmpty())
            return;

        Map<String, Class> adding = new HashMap<String, Class>();
        for (Class dClass : fetchable_objects) {
            try {
                Method method = dClass.getMethod("valueOf", String.class, TagContext.class);
                if (method.isAnnotationPresent(Fetchable.class)) {
                    String[] identifiers = method.getAnnotation(Fetchable.class).value().split(",");
                    for (String identifier : identifiers) {
                        adding.put(identifier.trim().toLowerCase(), dClass);
                        dB.log("Registered: " + dClass.getSimpleName() + " as " + identifier);
                    }
                }
            }
            catch (Throwable e) {
                dB.echoError("Failed to initialize an object type(" + dClass.getSimpleName() + "): ");
                dB.echoError(e);
            }
        }

        objects.putAll(adding);
        dB.echoApproval("Added objects to the ObjectFetcher " + adding.keySet().toString());
        fetchable_objects.clear();
    }

    public static void _registerCoreObjects() throws NoSuchMethodException, ClassNotFoundException, IOException {

        // Initialize the ObjectFetcher
        registerWithObjectFetcher(CustomObject.class); // custom@
        registerWithObjectFetcher(dList.class);        // li@/fl@
        dList.registerTags(); // TODO: Automate this once all classes have tag registries
        registerWithObjectFetcher(dScript.class);      // s@
        dScript.registerTags(); // TODO: Automate this once all classes have tag registries
        registerWithObjectFetcher(Element.class);      // el@
        Element.registerTags(); // TODO: Automate this once all classes have tag registries
        registerWithObjectFetcher(Duration.class);     // d@
        Duration.registerTags(); // TODO: Automate this once all classes have tag registries
        registerWithObjectFetcher(ScriptQueue.class);  // q@
        _initialize();

    }

    private static ArrayList<Class> fetchable_objects = new ArrayList<Class>();

    public static void registerWithObjectFetcher(Class dObject) {
        try {
            fetchable_objects.add(dObject);
            matches.put(dObject, dObject.getMethod("matches", String.class));
            valueof.put(dObject, dObject.getMethod("valueOf", String.class, TagContext.class));
        }
        catch (Throwable e) {
            dB.echoError("Failed to register an object type (" + dObject.getSimpleName() + "): ");
            dB.echoError(e);
        }
    }

    public static boolean canFetch(String id) {
        return objects.containsKey(CoreUtilities.toLowerCase(id));
    }

    public static Class getObjectClass(String id) {
        if (canFetch(id)) {
            return objects.get(CoreUtilities.toLowerCase(id));
        }
        else {
            return null;
        }
    }

    final static Pattern PROPERTIES_PATTERN = Pattern.compile("([^\\[]+)\\[(.+=.+)\\]", Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.MULTILINE);

    public final static Pattern DESCRIBED_PATTERN =
            Pattern.compile("[^\\[]+\\[.+=.+\\]", Pattern.DOTALL | Pattern.MULTILINE);

    public static boolean checkMatch(Class<? extends dObject> dClass, String value) {
        if (value == null || dClass == null)
            return false;
        Matcher m = PROPERTIES_PATTERN.matcher(value);
        try {
            return (Boolean) matches.get(dClass).invoke(null, m.matches() ? m.group(1) : value);
        }
        catch (Exception e) {
            dB.echoError(e);
        }

        return false;

    }

    @Deprecated
    public static <T extends dObject> T getObjectFrom(Class<T> dClass, String value) {
        return getObjectFrom(dClass, value, DenizenCore.getImplementation().getTagContext(null));
    }

    public static List<String> separateProperties(String input) {
        if (input.indexOf('[') == -1 || input.lastIndexOf(']') != input.length() - 1)
            return null;
        ArrayList<String> output = new ArrayList<String>();
        int start = 0;
        boolean needObject = true;
        int brackets = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '[' && needObject) {
                needObject = false;
                output.add(input.substring(start, i));
                start = i + 1;
            }
            else if (input.charAt(i) == '[') {
                brackets++;
            }
            else if (input.charAt(i) == ']' && brackets > 0) {
                brackets--;
            }
            else if ((input.charAt(i) == ';' || input.charAt(i) == ']') && brackets == 0) {
                output.add((input.substring(start, i)));
                start = i + 1;
            }
        }
        return output;
    }

    public static <T extends dObject> T getObjectFrom(Class<T> dClass, String value, TagContext context) {
        try {
            List<String> matches = separateProperties(value);
            boolean matched = matches != null && Adjustable.class.isAssignableFrom(dClass);
            T gotten = (T) valueof.get(dClass).invoke(null, matched ? matches.get(0) : value, context);
            if (gotten != null && matched) {
                for (int i = 1; i < matches.size(); i++) {
                    List<String> data = CoreUtilities.split(matches.get(i), '=', 2);
                    if (data.size() != 2) {
                        dB.echoError("Invalid property string '" + matches.get(i) + "'!");
                        continue;
                    }
                    ((Adjustable) gotten).applyProperty(new Mechanism(new Element(data.get(0)),
                            new Element((data.get(1)).replace((char) 0x2011, ';'))));
                }
            }
            return gotten;
        }
        catch (Exception e) {
            dB.echoError(e);
        }

        return null;
    }

    /**
     * This function will return the most-valid dObject for the input string.
     * If the input lacks @ notation or is not a valid object, an Element will be returned.
     *
     * @param value the input string.
     * @return the most-valid dObject available.
     */
    public static dObject pickObjectFor(String value) {
        return pickObjectFor(value, DenizenCore.getImplementation().getEmptyScriptEntryData().getTagContext());
    }

    public static dObject pickObjectFor(String value, TagContext context) {
        // While many inputs are valid as various object types
        // (EG, 'bob' could be a player or NPC's name)
        // Only use specific objects for input with @ notation
        if (value.contains("@")) {
            String type = value.split("@", 2)[0];
            // Of course, ensure the @ notation is valid first
            if (canFetch(type)) {
                Class toFetch = getObjectClass(type);
                dObject fetched = getObjectFrom(toFetch, value, context);
                // Only return if a valid object is born... otherwise, use an element.
                if (fetched != null) {
                    return fetched;
                }
            }
        }
        // If all else fails, just use a simple Element!
        return new Element(value);
    }
}
