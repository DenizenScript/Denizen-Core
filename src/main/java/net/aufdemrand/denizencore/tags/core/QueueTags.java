package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.TagRunnable;
import net.aufdemrand.denizencore.objects.dList;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;

public class QueueTags {

    public QueueTags() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                queueTag(event);
            }
        }, "queue", "q");
    }


    //////////
    //  ReplaceableTagEvent handler
    ////////

    public void queueTag(ReplaceableTagEvent event) {

        if (!event.matches("queue", "q")) {
            return;
        }

        // Handle <queue[id]. ...> tags

        if (event.hasNameContext()) {
            if (!ScriptQueue._queueExists(event.getNameContext())) {
                return;
            }
            else {
                event.setReplacedObject(CoreUtilities.autoAttrib(ScriptQueue._getExistingQueue(event.getNameContext())
                        ,event.getAttributes().fulfill(1)));
            }
            return;
        }

        Attribute attribute = event.getAttributes().fulfill(1);


        // Otherwise, try to use queue in a static manner.

        // <--[tag]
        // @attribute <queue.exists[<queue_id>]>
        // @returns Element(Boolean)
        // @description
        // Returns whether the specified queue exists.
        // -->
        if (attribute.startsWith("exists")
                && attribute.hasContext(1)) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new Element(ScriptQueue._queueExists(attribute.getContext(1)))
                    ,attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <queue.stats>
        // @returns Element
        // @description
        // Returns stats for all queues during this server session
        // -->
        if (attribute.startsWith("stats")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new Element(ScriptQueue._getStats())
                    ,attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <queue.list>
        // @returns dList(Queue)
        // @description
        // Returns a list of all currently running queues on the server.
        // -->
        if (attribute.startsWith("list")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new dList(ScriptQueue._getQueues())
                    ,attribute.fulfill(1)));
            return;
        }


        // Else,
        // Use current queue

        event.setReplacedObject(CoreUtilities.autoAttrib(event.getScriptEntry().getResidingQueue()
                ,attribute));
    }
}


