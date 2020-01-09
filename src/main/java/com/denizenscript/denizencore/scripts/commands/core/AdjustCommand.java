package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.tags.core.UtilTagBase;

import java.util.HashMap;
import java.util.function.Consumer;

public class AdjustCommand extends AbstractCommand {

    // <--[command]
    // @Name Adjust
    // @Syntax adjust [<ObjectTag>/def:<name>|...] [<mechanism>](:<value>)
    // @Required 2
    // @Short Adjusts an object's mechanism.
    // @Group core
    // @Guide https://guide.denizenscript.com/guides/basics/mechanisms.html
    //
    // @Description
    // Many object tag types contains options and properties that need to be adjusted. Denizen employs a mechanism
    // interface to deal with those adjustments. To easily accomplish this, use this command with a valid object
    // mechanism, and sometimes accompanying value.
    //
    // Specify "def:<name>" as an input to adjust a definition and automatically save the result back to the definition.
    //
    // To adjust an item in an inventory, use <@link command inventory>, as '- inventory adjust slot:<#> <mechanism>:<value>'.
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
    // - adjust <server.list_online_players> skin:Notch
    //
    // @Usage
    // Use to modify an item held in a definition.
    // - adjust def:stick "display_name:Fancy stick"
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("object")) {
                if (arg.object instanceof ListTag) {
                    scriptEntry.addObject("object", arg.object);
                }
                else if (arg.object instanceof ElementTag) {
                    // Special parse to avoid prefixing issues
                    scriptEntry.addObject("object", ListTag.valueOf(arg.raw_value));
                }
                else {
                    scriptEntry.addObject("object", arg.asType(ListTag.class));
                }
            }
            else if (!scriptEntry.hasObject("mechanism")) {
                if (arg.hasPrefix()) {
                    scriptEntry.addObject("mechanism", new ElementTag(arg.getPrefix().getValue()));
                    scriptEntry.addObject("mechanism_value", arg.asElement());
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

        if (!scriptEntry.hasObject("mechanism")) {
            throw new InvalidArgumentsException("You must specify a mechanism!");
        }
    }

    public static HashMap<String, Consumer<Mechanism>> specialAdjustables = new HashMap<>();

    static {
        specialAdjustables.put("system", UtilTagBase::adjustSystem);
    }

    public ObjectTag adjust(ObjectTag object, ElementTag mechanismName, ElementTag value, ScriptEntry entry) {
        Mechanism mechanism = new Mechanism(mechanismName, value, entry.entryData.getTagContext());
        return adjust(object, mechanism, entry);
    }

    public ObjectTag adjust(ObjectTag object, Mechanism mechanism, ScriptEntry entry) {
        String objectString = object.toString();
        String lowerObjectString = CoreUtilities.toLowerCase(objectString);
        Consumer<Mechanism> specialAdjustable = specialAdjustables.get(lowerObjectString);
        if (specialAdjustable != null) {
            specialAdjustable.accept(mechanism);
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
        if (object instanceof ElementTag) {
            object = ObjectFetcher.pickObjectFor(objectString, entry.entryData.getTagContext());
            if (object instanceof ElementTag) {
                Debug.echoError("Unable to determine what object to adjust (missing object notation?), for: " + objectString);
                return object;
            }
        }
        if (object instanceof ListTag) {
            ListTag subList = (ListTag) object;
            ListTag result = new ListTag();
            for (ObjectTag listObject : subList.objectForms) {
                listObject = adjust(listObject, mechanism, entry);
                result.addObject(listObject);
            }
            return result;
        }
        // Make sure this object is Adjustable
        if (!(object instanceof Adjustable)) {
            Debug.echoError("'" + objectString + "' is not an adjustable object type.");
            return object;
        }
        ((Adjustable) object).safeAdjust(mechanism);
        return object;
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        ElementTag mechanism = scriptEntry.getElement("mechanism");
        ElementTag value = scriptEntry.getElement("mechanism_value");

        ListTag objects = scriptEntry.getObjectTag("object");

        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(),
                    objects.debug()
                            + mechanism.debug()
                            + (value == null ? "" : value.debug()));
        }

        ListTag result = new ListTag();

        for (ObjectTag object : objects.objectForms) {
            object = adjust(object, mechanism, value, scriptEntry);
            if (objects.size() == 1) {
                scriptEntry.addObject("result", object);
            }
            result.addObject(object);
        }

        scriptEntry.addObject("result_list", result);

    }
}
