package net.aufdemrand.denizencore.scripts.commands.core;

import net.aufdemrand.denizencore.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.objects.dObject;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.commands.AbstractCommand;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.data.ActionableDataProvider;
import net.aufdemrand.denizencore.utilities.data.DataAction;
import net.aufdemrand.denizencore.utilities.data.DataActionHelper;
import net.aufdemrand.denizencore.utilities.debugging.dB;

/**
 * Creates a queue/script-level variable.
 */
public class DefineCommand extends AbstractCommand {

    public static class DefinitionActionProvider extends ActionableDataProvider {

        public ScriptQueue queue;

        @Override
        public dObject getValueAt(String keyName) {
            return queue.getDefinitionObject(keyName);
        }

        @Override
        public void setValueAt(String keyName, dObject value) {
            queue.addDefinition(keyName, value);
        }
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {

        for (aH.Argument arg : aH.interpretArguments(scriptEntry.aHArgs)) {

            if (!scriptEntry.hasObject("definition")) {
                if (arg.raw_value.contains(":")) {
                    DefinitionActionProvider provider = new DefinitionActionProvider();
                    provider.queue = scriptEntry.getResidingQueue();
                    scriptEntry.addObject("action", DataActionHelper.parse(provider, arg.raw_value));
                }
                else {
                    scriptEntry.addObject("definition", new Element(CoreUtilities.toLowerCase(arg.getValue())));
                }
            }

            else if (!scriptEntry.hasObject("value")) {
                scriptEntry.addObject("value", arg.object instanceof Element ? new Element(arg.raw_value) : arg.object);
            }

            else {
                arg.reportUnhandled();
            }
        }

        if ((!scriptEntry.hasObject("definition") || !scriptEntry.hasObject("value")) && !scriptEntry.hasObject("action")) {
            throw new InvalidArgumentsException("Must specify a definition and value!");
        }
    }

    @Override
    public void execute(ScriptEntry scriptEntry) {

        Element definition = scriptEntry.getElement("definition");
        dObject value = scriptEntry.getdObject("value");
        Element remove = scriptEntry.getElement("remove");
        Object actionObj = scriptEntry.getObject("action");
        DataAction action = actionObj == null ? null : (DataAction) actionObj;

        if (scriptEntry.dbCallShouldDebug()) {
            dB.report(scriptEntry, getName(), aH.debugObj("queue", scriptEntry.getResidingQueue().id)
                    + (definition == null ? "" : definition.debug())
                    + (value == null ? "" : value.debug())
                    + (action == null ? "" : action.debug())
                    + (remove != null ? remove.debug() : ""));
        }

        if (action != null) {
            action.execute();
            return;
        }
        if (scriptEntry.hasObject("remove")) {
            scriptEntry.getResidingQueue().removeDefinition(definition.asString());
        }
        else {
            scriptEntry.getResidingQueue().addDefinition(definition.asString(), value);
        }
    }
}
