package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.tags.TagManager;

public class QueueTagBase {

    public QueueTagBase() {

        // <--[tag]
        // @attribute <queue[(<queue>)]>
        // @returns QueueTag
        // @description
        // Returns a queue object constructed from the input value.
        // Refer to <@link language QueueTag objects>.
        // If no input is given, returns the current queue.
        // -->
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                queueTag(event);
            }
        }, "queue");
    }

    //////////
    //  ReplaceableTagEvent handler
    ////////

    public void queueTag(ReplaceableTagEvent event) {

        if (!event.matches("queue")) {
            return;
        }

        // Handle <queue[id]. ...> tags

        if (event.hasNameContext()) {
            if (!ScriptQueue.queueExists(event.getNameContext())) {
                return;
            }
            else {
                event.setReplacedObject(CoreUtilities.autoAttrib(new QueueTag(ScriptQueue.getExistingQueue(event.getNameContext())),
                        event.getAttributes().fulfill(1)));
            }
            return;
        }

        Attribute attribute = event.getAttributes().fulfill(1);

        // Otherwise, try to use queue in a static manner.

        // <--[tag]
        // @attribute <queue.exists[<queue_id>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether the specified queue exists.
        // -->
        if (attribute.startsWith("exists")
                && attribute.hasContext(1)) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(ScriptQueue.queueExists(attribute.getContext(1))),
                    attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <queue.stats>
        // @returns ElementTag
        // @description
        // Returns stats for all queues during this server session
        // -->
        if (attribute.startsWith("stats")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(ScriptQueue.getStats()),
                    attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <queue.list>
        // @returns ListTag(QueueTag)
        // @description
        // Returns a list of all currently running queues on the server.
        // -->
        if (attribute.startsWith("list")) {
            ListTag list = new ListTag();
            for (ScriptQueue queue : ScriptQueue.getQueues()) {
                list.addObject(new QueueTag(queue));
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(list,
                    attribute.fulfill(1)));
            return;
        }

        // Else,
        // Use current queue

        event.setReplacedObject(CoreUtilities.autoAttrib(new QueueTag(event.getScriptEntry().getResidingQueue()),
                attribute));
    }
}

