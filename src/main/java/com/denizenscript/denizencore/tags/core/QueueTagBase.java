package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
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

    public void queueTag(ReplaceableTagEvent event) {
        if (!event.matches("queue")) {
            return;
        }
        Attribute attribute = event.getAttributes();
        // Historical queue.xxx tags:
        if (attribute.startsWith("exists", 2) && attribute.hasContext(2)) {
            Deprecations.queueExists.warn(attribute.context);
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(ScriptQueue.queueExists(attribute.getContext(2))), attribute.fulfill(2)));
            return;
        }
        if (attribute.startsWith("stats", 2)) {
            Deprecations.queueStats.warn(attribute.context);
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(ScriptQueue.getStats()), attribute.fulfill(2)));
            return;
        }
        if (attribute.startsWith("list", 2)) {
            Deprecations.queueStats.warn(attribute.context);
            event.setReplacedObject(CoreUtilities.autoAttrib(new ListTag(ScriptQueue.getQueues(), QueueTag::new), attribute.fulfill(2)));
            return;
        }
        // Modern tag:
        if (attribute.hasParam()) {
            QueueTag queue = attribute.paramAsType(QueueTag.class);
            if (queue == null) {
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(queue, event.getAttributes().fulfill(1)));
            return;
        }
        ScriptQueue queue = event.getScriptEntry().getResidingQueue();
        if (queue == null) {
            return;
        }
        event.setReplacedObject(CoreUtilities.autoAttrib(new QueueTag(event.getScriptEntry().getResidingQueue()), attribute.fulfill(1)));
    }
}
