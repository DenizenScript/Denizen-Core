package net.aufdemrand.denizencore.scripts.commands.queue;

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

    // <--[command]
    // @Name Define
    // @Syntax define [<id>](:<action>)[:<value>]
    // @Required 1
    // @Short Creates a temporary variable inside a script queue.
    // @Group queue
    //
    // @Description
    // Definitions are queue-level (or script-level) 'variables' that can be used throughout a script, once
    // defined, by using the <[<id>]> tag. Definitions are only valid on the current queue and are
    // not transferred to any new queues constructed within the script, such as a 'run' command, without explicitly
    // specifying to do so.
    //
    // Definitions are lighter and faster than creating a temporary flag, but unlike flags, are only a single entry,
    // that is, you can't add or remove from the definition, but you can re-create it if you wish to specify a new
    // value. Definitions are also automatically removed when the queue is completed, so there is no worry for
    // leaving unused data hanging around.
    //
    // Refer to <@link language data actions>
    //
    // @Tags
    // <[<id>]> to get the value assigned to an ID
    //
    // @Usage
    // Use to make complex tags look less complex, and scripts more readable
    // - narrate 'You invoke your power of notice...'
    // - define range:<player.flag[range_level].mul[3]>
    // - define blocks:<player.flag[noticeable_blocks]>
    // - narrate '[NOTICE] You have noticed <player.location.find.blocks[<[blocks]>].within[<[range]>].size> blocks in the area that may be of interest.'
    //
    // @Usage
    // Use to keep the value of a replaceable tag that you might use many times within a single script. Definitions
    // can be faster and cleaner than reusing a replaceable tag over and over
    // - define arg1:<context.args.get[1]>
    // - if <[arg1]> == hello:
    //   - narrate 'Hello!'
    // - else if <[arg1]> == goodbye:
    //   - narrate 'Goodbye!'
    //
    // @Usage
    // Use to remove a definition
    // - define myDef:!
    //
    // -->

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
