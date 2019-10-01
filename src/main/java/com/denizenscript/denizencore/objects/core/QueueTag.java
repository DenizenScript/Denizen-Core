package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.scripts.queues.core.Delayable;
import com.denizenscript.denizencore.scripts.queues.core.TimedQueue;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public class QueueTag implements ObjectTag, Adjustable {

    // <--[language]
    // @name QueueTag
    // @group Object System
    // @description
    // A QueueTag is a single currently running set of script commands.
    // This is not to be confused with a script path, which is a single set of script commands that can be run.
    // There can be one, multiple, or zero queues running at any time for any given path.
    //
    // For format info, see <@link language q@>
    //
    // -->

    // <--[language]
    // @name q@
    // @group Object Fetcher System
    // @description
    // q@ refers to the 'object identifier' of a QueueTag. The 'q@' is notation for Denizen's Object
    // Fetcher. The constructor for a QueueTag is the queue ID.
    //
    // For general info, see <@link language QueueTag>
    //
    // -->

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
    public String getObjectType() {
        return "queue";
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

    public static void registerTags() {

        // <--[tag]
        // @attribute <QueueTag.id>
        // @returns ElementTag
        // @description
        // Returns the id of the queue.
        // -->
        registerTag("id", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                return new ElementTag(object.getQueue().id);
            }
        });

        // <--[tag]
        // @attribute <QueueTag.size>
        // @returns ElementTag
        // @description
        // Returns the number of script entries in the queue.
        // -->
        registerTag("size", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                return new ElementTag(object.getQueue().script_entries.size());
            }
        });

        // <--[tag]
        // @attribute <QueueTag.start_time>
        // @returns DurationTag
        // @description
        // Returns the time this queue started as a duration.
        // -->
        registerTag("start_time", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                return new DurationTag(object.getQueue().startTimeMilli / 50);
            }
        });

        // <--[tag]
        // @attribute <QueueTag.time_ran>
        // @returns DurationTag
        // @description
        // Returns the time this queue has ran for (the length of time between now and when the queue started) as a duration.
        // -->
        registerTag("time_ran", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                long timeNano = System.nanoTime() - object.getQueue().startTime;
                return new DurationTag(timeNano / (1000000 * 1000.0));
            }
        });

        // <--[tag]
        // @attribute <QueueTag.state>
        // @returns ElementTag
        // @description
        // Returns 'stopping', 'running', 'paused', or 'unknown'.
        // -->
        registerTag("state", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                String state;
                if ((object.getQueue() instanceof Delayable) && ((Delayable) object.getQueue()).isPaused()) {
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
            }
        });

        // <--[tag]
        // @attribute <QueueTag.script>
        // @returns ScriptTag
        // @description
        // Returns the script that started this queue.
        // -->
        registerTag("script", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                if (object.getQueue().script == null) {
                    return null;
                }
                return object.getQueue().script;
            }
        });

        // <--[tag]
        // @attribute <QueueTag.commands>
        // @returns ListTag
        // @description
        // Returns a list of commands waiting in the queue.
        // -->
        registerTag("commands", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
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
            }
        });

        // <--[tag]
        // @attribute <QueueTag.definitions>
        // @returns ListTag
        // @description
        // Returns the names of all definitions that were passed to the current queue.
        // -->
        registerTag("definitions", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                return new ListTag(object.getQueue().getAllDefinitions().keySet());
            }
        });

        // <--[tag]
        // @attribute <QueueTag.definition[<definition>]>
        // @returns ObjectTag
        // @description
        // Returns the value of the specified definition.
        // Returns null if the queue lacks the definition.
        // -->
        registerTag("definition", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag QueueTag.definition[...] must have a value.");
                    return null;
                }
                return object.getQueue().getDefinitionObject(attribute.getContext(1));
            }
        });

        // <--[tag]
        // @attribute <QueueTag.determination>
        // @returns ListTag
        // @description
        // Returns the values that have been determined via <@link command Determine>
        // for this queue, or null if there is none.
        // -->
        registerTag("determination", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                if (object.getQueue().determinations == null) {
                    return null;
                }
                else {
                    return object.getQueue().determinations;
                }
            }
        });

        // <--[tag]
        // @attribute <QueueTag.speed>
        // @returns DurationTag
        // @description
        // Returns the speed of the queue as a Duration. A return of '0' implies it is 'instant'.
        // -->
        registerTag("speed", new TagRunnable.ObjectForm<QueueTag>() {
            @Override
            public ObjectTag run(Attribute attribute, QueueTag object) {
                if (!(object.getQueue() instanceof TimedQueue)) {
                    if (!attribute.hasAlternative()) {
                        Debug.echoError("The tag QueueTag.speed is only valid for Timed queues.");
                    }
                    return null;
                }
                return ((TimedQueue) object.getQueue()).getSpeed();
            }
        });
    }

    public static ObjectTagProcessor<QueueTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectForm<QueueTag> runnable) {
        tagProcessor.registerTag(name, runnable);
    }

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
