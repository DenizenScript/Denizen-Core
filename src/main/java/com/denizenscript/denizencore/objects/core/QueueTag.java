package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.flags.MapTagBasedFlagTracker;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.Collection;

public class QueueTag implements ObjectTag, Adjustable, FlaggableObject {

    // <--[ObjectType]
    // @name QueueTag
    // @prefix q
    // @base ElementTag
    // @implements FlaggableObject
    // @ExampleTagBase queue
    // @ExampleValues <queue>
    // @format
    // The identity format for queues is simply the queue ID.
    //
    // @description
    // A QueueTag is a single currently running set of script commands.
    // This is not to be confused with a script path, which is a single set of script commands that can be run.
    // There can be one, multiple, or zero queues running at any time for any given path.
    //
    // This object type is flaggable.
    // Flags on this object type will be reinterpreted as definitions.
    // Flags on queues should just not be used. Use definitions directly.
    //
    // -->

    @Deprecated
    public static QueueTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("q")
    public static QueueTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("q@") && string.length() > 2) {
            string = string.substring(2);
        }
        if (ScriptQueue.queueExists(string)) {
            return new QueueTag(ScriptQueue.getExistingQueue(string));
        }
        return null;
    }

    public static boolean matches(String string) {
        // Starts with q@? Assume match.
        if (CoreUtilities.toLowerCase(string).startsWith("q@")) {
            return true;
        }
        else {
            return false;
        }
    }

    public ScriptQueue queue;

    public QueueTag(ScriptQueue queue) {
        this.queue = queue;
    }

    String prefix = "Queue";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public QueueTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String identify() {
        return "q@" + queue.id;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public String debuggable() {
        return "<LG>q@<Y>" + queue.debugId;
    }

    @Override
    public boolean isTruthy() {
        return !queue.isStopped;
    }

    public class QueueFakeFlagTracker extends MapTagBasedFlagTracker {

        @Override
        public MapTag getRootMap(String key) {
            return (MapTag) getQueue().getDefinitionObject(key);
        }

        @Override
        public void setRootMap(String key, MapTag map) {
            getQueue().addDefinition(key, map);
        }

        @Override
        public Collection<String> listAllFlags() {
            return getQueue().getAllDefinitions().keys();
        }
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return new QueueFakeFlagTracker();
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
        // Nothing to do.
    }

    public static void registerTags() {

        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        // <--[tag]
        // @attribute <QueueTag.id>
        // @returns ElementTag
        // @description
        // Returns the full textual id of the queue.
        // -->
        tagProcessor.registerTag(ElementTag.class, "id", (attribute, object) -> {
            return new ElementTag(object.getQueue().id);
        });

        // <--[tag]
        // @attribute <QueueTag.numeric_id>
        // @returns ElementTag(Number)
        // @description
        // Returns the raw numeric id of the queue. This is an incremental ID one higher than the previous queue's numeric ID.
        // -->
        tagProcessor.registerTag(ElementTag.class, "numeric_id", (attribute, object) -> {
            return new ElementTag(object.getQueue().numericId);
        });

        // <--[tag]
        // @attribute <QueueTag.size>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of script entries in the queue.
        // -->
        tagProcessor.registerTag(ElementTag.class, "size", (attribute, object) -> {
            return new ElementTag(object.getQueue().script_entries.size());
        });

        // <--[tag]
        // @attribute <QueueTag.started_time>
        // @returns TimeTag
        // @description
        // Returns the time this queue started as a duration.
        // -->
        tagProcessor.registerTag(TimeTag.class, "started_time", (attribute, object) -> {
            return new TimeTag(CoreUtilities.monotonicMillisToReal(object.getQueue().startTimeMilli));
        });
        tagProcessor.registerTag(DurationTag.class, "start_time", (attribute, object) -> {
            Deprecations.timeTagRewrite.warn(attribute.context);
            return new DurationTag(CoreUtilities.monotonicMillisToReal(object.getQueue().startTimeMilli) / 50);
        });

        // <--[tag]
        // @attribute <QueueTag.time_ran>
        // @returns DurationTag
        // @description
        // Returns the time this queue has ran for (the length of time between now and when the queue started) as a duration.
        // -->
        tagProcessor.registerTag(DurationTag.class, "time_ran", (attribute, object) -> {
            long timeNano = System.nanoTime() - object.getQueue().startTime;
            return new DurationTag(timeNano / (1000000 * 1000.0));
        });

        // <--[tag]
        // @attribute <QueueTag.is_valid>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the queue has not yet stopped.
        // -->
        tagProcessor.registerTag(ElementTag.class, "is_valid", (attribute, object) -> {
            return new ElementTag(object.getQueue().is_started && !object.getQueue().isStopped);
        });

        // <--[tag]
        // @attribute <QueueTag.state>
        // @returns ElementTag
        // @description
        // Returns 'stopping', 'running', 'paused', or 'unknown'.
        // -->
        tagProcessor.registerTag(ElementTag.class, "state", (attribute, object) -> {
            String state;
            if ((object.getQueue() instanceof TimedQueue) && ((TimedQueue) object.getQueue()).isPaused()) {
                state = "paused";
            }
            else if (object.getQueue().is_started) {
                state = "running";
            }
            else if (object.getQueue().is_stopping) {
                state = "stopping";
            }
            else {
                state = "unknown";
            }
            return new ElementTag(state);
        });

        // <--[tag]
        // @attribute <QueueTag.script>
        // @returns ScriptTag
        // @description
        // Returns the script that started this queue.
        // -->
        tagProcessor.registerTag(ScriptTag.class, "script", (attribute, object) -> {
            if (object.getQueue().script == null) {
                return null;
            }
            return object.getQueue().script;
        });

        // <--[tag]
        // @attribute <QueueTag.commands>
        // @returns ListTag
        // @description
        // Returns a list of commands waiting in the queue.
        // -->
        tagProcessor.registerTag(ListTag.class, "commands", (attribute, object) -> {
            ListTag commands = new ListTag();
            for (ScriptEntry entry : object.getQueue().script_entries) {
                StringBuilder sb = new StringBuilder();
                sb.append(entry.getCommandName()).append(" ");
                for (String arg : entry.getOriginalArguments()) {
                    sb.append(arg).append(" ");
                }
                commands.add(sb.substring(0, sb.length() - 1));
            }
            return commands;
        });

        // <--[tag]
        // @attribute <QueueTag.last_command>
        // @returns ElementTag
        // @description
        // Returns the last command executed in this queue (if any).
        // -->
        tagProcessor.registerTag(ElementTag.class, "last_command", (attribute, object) -> {
            ScriptEntry entry = object.getQueue().getLastEntryExecuted();
            if (entry == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(entry.getCommandName()).append(" ");
            for (String arg : entry.getOriginalArguments()) {
                sb.append(arg).append(" ");
            }
            return new ElementTag(sb.substring(0, sb.length() - 1));
        });

        // <--[tag]
        // @attribute <QueueTag.definitions>
        // @returns ListTag
        // @description
        // Returns the names of all definitions that were added to the current queue.
        // -->
        tagProcessor.registerTag(ListTag.class, "definitions", (attribute, object) -> {
            return object.getQueue().getAllDefinitions().keys();
        });

        // <--[tag]
        // @attribute <QueueTag.definition_map>
        // @returns MapTag
        // @description
        // Returns a map of all definitions on the queue.
        // -->
        tagProcessor.registerTag(MapTag.class, "definition_map", (attribute, object) -> {
            return object.getQueue().getAllDefinitions().duplicate();
        });

        // <--[tag]
        // @attribute <QueueTag.definition[<definition>]>
        // @returns ObjectTag
        // @description
        // Returns the value of the specified definition.
        // Returns null if the queue lacks the definition.
        // -->
        tagProcessor.registerTag(ObjectTag.class, ElementTag.class, "definition", (attribute, object, defName) -> {
            return object.getQueue().getDefinitionObject(defName.asString());
        });

        // <--[tag]
        // @attribute <QueueTag.determination>
        // @returns ListTag
        // @description
        // Returns the values that have been determined via <@link command Determine>
        // for this queue, or null if there is none.
        // -->
        tagProcessor.registerTag(ListTag.class, "determination", (attribute, object) -> {
            if (object.getQueue().determinations == null) {
                return null;
            }
            else {
                return object.getQueue().determinations;
            }
        });

        // <--[tag]
        // @attribute <QueueTag.speed>
        // @returns DurationTag
        // @description
        // Returns the speed of the queue as a Duration. A return of '0' implies it is 'instant'.
        // This is largely considered historical - most queues now default to instant.
        // -->
        tagProcessor.registerTag(DurationTag.class, "speed", (attribute, object) -> {
            if (!(object.getQueue() instanceof TimedQueue)) {
                if (!attribute.hasAlternative()) {
                    Debug.echoError("The tag QueueTag.speed is only valid for Timed queues.");
                }
                return null;
            }
            return ((TimedQueue) object.getQueue()).getSpeed();
        });
    }

    public static ObjectTagProcessor<QueueTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    public ScriptQueue getQueue() {
        ensure();
        return queue;
    }

    public void ensure() {
        while (queue.replacementQueue != null) {
            queue = queue.replacementQueue;
        }
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("QueueTags can not hold properties.");
    }

    @Override
    public void adjust(Mechanism mechanism) {
        ensure();
        CoreUtilities.autoPropertyMechanism(this, mechanism);
    }
}
