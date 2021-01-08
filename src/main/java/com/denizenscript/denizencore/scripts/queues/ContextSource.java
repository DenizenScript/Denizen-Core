package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.objects.ObjectTag;

import java.util.Map;

/**
 * Provides contexts to a queue.
 */
public interface ContextSource {

    ObjectTag getContext(String name);

    class SimpleMap implements ContextSource {

        public Map<String, ObjectTag> contexts;

        @Override
        public ObjectTag getContext(String name) {
            return contexts.get(name);
        }
    }
}
