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
import com.denizenscript.denizencore.utilities.Deprecations;

public class QueueTagBase {

    public QueueTagBase() {

        // <--[tag]
        // @attribute <queue[(<queue>)]>
        // @returns QueueTag
        // @description
        // Returns a queue object constructed from the input value.
        // Refer to <@link ObjectType QueueTag>.
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

        Attribute attribute = event.getAttributes();

        if (attribute.hasContext(1)) {
            QueueTag queue = attribute.contextAsType(1, QueueTag.class);
            if (queue == null) {
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(queue, event.getAttributes().fulfill(1)));
            return;
        }

        attribute = attribute.fulfill(1);

        // Otherwise, try to use queue in a static manner.

        if (attribute.startsWith("exists")
                && attribute.hasContext(1)) {
            Deprecations.queueExists.warn(attribute.context);
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(ScriptQueue.queueExists(attribute.getContext(1))),
                    attribute.fulfill(1)));
            return;
        }

        if (attribute.startsWith("stats")) {
            Deprecations.queueStats.warn(attribute.context);
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(ScriptQueue.getStats()),
                    attribute.fulfill(1)));
            return;
        }

        if (attribute.startsWith("list")) {
            Deprecations.queueStats.warn(attribute.context);
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

