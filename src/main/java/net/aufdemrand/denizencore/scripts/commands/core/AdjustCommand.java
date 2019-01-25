package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.CommandExecutionException;
import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.*;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.tags.core.UtilTags;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.HashMap;
import java.util.function.Consumer;


public class AdjustCommand extends AbstractCommand {

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpret(scriptEntry.getArguments())) {
            if (!scriptEntry.hasObject("object")) {
                if (arg.object instanceof dList) {
                    scriptEntry.addObject("object", arg.object);
                }
                else if (arg.object instanceof Element) {
                    // Special parse to avoid prefixing issues
                    scriptEntry.addObject("object", dList.valueOf(arg.raw_value));
                }
                else {
                    scriptEntry.addObject("object", arg.asType(dList.class));
                }
            }
            else if (!scriptEntry.hasObject("mechanism")) {
                if (arg.hasPrefix()) {
                    scriptEntry.addObject("mechanism", new Element(arg.getPrefix().getValue()));
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
        specialAdjustables.put("system", UtilTags::adjustSystem);
    }

    public dObject adjust(dObject object, Element mechanismName, Element value, ScriptEntry entry) {
        Mechanism mechanism = new Mechanism(mechanismName, value, entry.entryData.getTagContext());
        return adjust(object, mechanism, entry);
    }

    public dObject adjust(dObject object, Mechanism mechanism, ScriptEntry entry) {
        String objectString = object.toString();
        String lowerObjectString = CoreUtilities.toLowerCase(objectString);
        Consumer<Mechanism> specialAdjustable = specialAdjustables.get(lowerObjectString);
        if (specialAdjustable != null) {
            specialAdjustable.accept(mechanism);
            return object;
        }
        if (lowerObjectString.startsWith("def:")) {
            String defName = lowerObjectString.substring("def:".length());
            dObject def = entry.getResidingQueue().getDefinitionObject(defName);
            if (def == null) {
                dB.echoError("Invalid definition name '" + defName + "', cannot adjust");
                return object;
            }
            def = adjust(def, mechanism, entry);
            entry.getResidingQueue().addDefinition(defName, def);
            return def;
        }
        if (object instanceof Element) {
            object = ObjectFetcher.pickObjectFor(objectString, entry.entryData.getTagContext());
            if (object instanceof Element) {
                dB.echoError("Unable to determine what object to adjust (missing object notation?), for: " + objectString);
                return object;
            }
        }
        // Make sure this object is Adjustable
        if (!(object instanceof Adjustable)) {
            dB.echoError("'" + objectString + "' is not an adjustable object type.");
            return object;
        }
        ((Adjustable) object).safeAdjust(mechanism);
        return object;
    }


    @Override
    public void execute(ScriptEntry scriptEntry) throws CommandExecutionException {

        Element mechanism = scriptEntry.getElement("mechanism");
        Element value = scriptEntry.getElement("mechanism_value");

        dList objects = scriptEntry.getdObject("object");

        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(),
                    objects.debug()
                            + mechanism.debug()
                            + (value == null ? "" : value.debug()));
        }

        dList result = new dList();

        for (dObject object : objects.objectForms) {
            object = adjust(object, mechanism, value, scriptEntry);
            if (objects.size() == 1) {
                scriptEntry.addObject("result", object);
            }
            result.addObject(object);
        }

        scriptEntry.addObject("result_list", result);

    }
}
