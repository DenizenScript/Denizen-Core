package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AdjustCommand extends AbstractCommand {

    public AdjustCommand() {
        setName("adjust");
        setSyntax("adjust [<ObjectTag>/def:<name>|...] [<mechanism>](:<value>)");
        setRequiredArguments(2, 2);
        isProcedural = true;
        allowedDynamicPrefixes = true;
    }

    // <--[command]
    // @Name Adjust
    // @Syntax adjust [<ObjectTag>/def:<name>|...] [<mechanism>](:<value>)
    // @Required 2
    // @Maximum 2
    // @Short Adjusts an object's mechanism.
    // @Synonyms Mechanism
    // @Group core
    // @Guide https://guide.denizenscript.com/guides/basics/mechanisms.html
    //
    // @Description
    // Many object tag types contains options and properties that need to be adjusted.
    // Denizen employs a mechanism interface to deal with those adjustments.
    // To easily accomplish this, use this command with a valid object mechanism, and sometimes accompanying value.
    //
    // Specify "def:<name>" as an input to adjust a definition and automatically save the result back to the definition.
    //
    // You can optionally adjust a MapTag of mechanisms to values.
    //
    // @Tags
    // <entry[saveName].result> returns the adjusted object.
    // <entry[saveName].result_list> returns a ListTag of adjusted objects.
    //
    // @Usage
    // Use to set a custom display name on an entity.
    // - adjust <[some_entity]> custom_name:ANGRY!
    //
    // @Usage
    // Use to set the skin of every online player.
    // - adjust <server.online_players> skin:Notch
    //
    // @Usage
    // Use to modify an item held in a definition.
    // - adjust def:stick "display_name:Fancy stick"
    //
    // -->

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.add(PropertyParser.allMechanismsEver);
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("object")) {
                if (arg.object instanceof ListTag) {
                    scriptEntry.addObject("object", arg.object);
                }
                else if (arg.object instanceof ElementTag) {
                    // Special parse to avoid prefixing issues
                    scriptEntry.addObject("object", ListTag.valueOf(arg.getRawValue(), scriptEntry.getContext()));
                }
                else {
                    scriptEntry.addObject("object", arg.asType(ListTag.class));
                }
            }
            else if (!scriptEntry.hasObject("mechanism")
                && !scriptEntry.hasObject("mechanism_map")) {
                if (arg.getRawValue().startsWith("map@")) {
                    scriptEntry.addObject("mechanism_map", arg.asType(MapTag.class));
                }
                else if (arg.hasPrefix()) {
                    scriptEntry.addObject("mechanism", new ElementTag(arg.getPrefix().getValue()));
                    scriptEntry.addObject("mechanism_value", arg.object);
                }
                else {
                    scriptEntry.addObject("mechanism", arg.asElement());
                }
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("object")) {
            throw new InvalidArgumentsException("You must specify an object!");
        }
        if (!scriptEntry.hasObject("mechanism") && !scriptEntry.hasObject("mechanism_map")) {
            throw new InvalidArgumentsException("You must specify a mechanism!");
        }
    }

    public static HashMap<String, Consumer<Mechanism>> specialAdjustables = new HashMap<>();

    public ObjectTag adjust(ObjectTag object, String mechanismName, ObjectTag value, ScriptEntry entry) {
        Mechanism mechanism = new Mechanism(mechanismName, value, entry.entryData.getTagContext());
        return adjust(object, mechanism, entry);
    }

    public ObjectTag adjust(ObjectTag object, Mechanism mechanism, ScriptEntry entry) {
        if (object == null) {
            Debug.echoError("Cannot adjust null object.");
            return null;
        }
        if (object instanceof ElementTag) {
            String objectString = object.toString();
            String lowerObjectString = CoreUtilities.toLowerCase(objectString);
            Consumer<Mechanism> specialAdjustable = specialAdjustables.get(lowerObjectString);
            if (specialAdjustable != null) {
                mechanism.adjusting = null;
                mechanism.isProperty = false;
                if (mechanism.shouldDebug()) {
                    Debug.echoDebug(mechanism.context, "Adjust mechanism '" + mechanism.getName() + "' on special adjustable '" + lowerObjectString + "'...");
                }
                specialAdjustable.accept(mechanism);
                mechanism.autoReport();
                return object;
            }
            if (lowerObjectString.startsWith("def:")) {
                String defName = lowerObjectString.substring("def:".length());
                ObjectTag def = entry.getResidingQueue().getDefinitionObject(defName);
                if (def == null) {
                    Debug.echoError("Invalid definition name '" + defName + "', cannot adjust");
                    return object;
                }
                def = adjust(def, mechanism, entry);
                entry.getResidingQueue().addDefinition(defName, def);
                return def;
            }
            object = ObjectFetcher.pickObjectFor(objectString, entry.context);
            if (object instanceof ElementTag) {
                FlaggableObject altObject = DenizenCore.implementation.simpleWordToFlaggable(objectString, entry);
                if (altObject == null || (altObject instanceof ElementTag)) {
                    Debug.echoError("Unable to determine what object to adjust (missing object notation?), for: " + objectString);
                    return object;
                }
                object = altObject;
            }
        }
        if (object instanceof ListTag subList) {
            return new ListTag(subList.objectForms, obj -> adjust(obj, mechanism, entry));
        }
        if (!object.isUnique()) {
            object = ObjectFetcher.pickObjectFor(object.identify(), mechanism.context); // Create duplicate of object, instead of adjusting original
        }
        if (!(object instanceof Adjustable)) {
            Debug.echoError("'" + object + "' is not an adjustable object type.");
            return object;
        }
        if (entry.getResidingQueue().procedural && object.isUnique()) {
            Debug.echoError("Cannot adjust a unique object within a procedural queue.");
            return null;
        }
        ((Adjustable) object).safeAdjust(mechanism);
        return object;
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {
        ElementTag mechanism = scriptEntry.getElement("mechanism");
        ObjectTag value = scriptEntry.getObjectTag("mechanism_value");
        ListTag objects = scriptEntry.getObjectTag("object");
        MapTag mechanismMap = scriptEntry.getObjectTag("mechanism_map");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), objects, value, mechanism, mechanismMap);
        }
        ListTag result = new ListTag(objects.size());
        for (ObjectTag object : objects.objectForms) {
            if (mechanismMap != null) {
                for (Map.Entry<StringHolder, ObjectTag> entry : mechanismMap.entrySet()) {
                    object = adjust(object, entry.getKey().str, entry.getValue(), scriptEntry);
                }
            }
            else {
                object = adjust(object, mechanism.asString(), value, scriptEntry);
            }
            if (objects.size() == 1) {
                scriptEntry.saveObject("result", object);
            }
            result.addObject(object);
        }
        scriptEntry.saveObject("result_list", result);
    }
}
