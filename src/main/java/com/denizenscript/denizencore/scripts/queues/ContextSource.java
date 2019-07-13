package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.objects.ObjectTag;

/**
 * Provides contexts to a queue.
 */
public interface ContextSource {

    boolean getShouldCache();

    ObjectTag getContext(String name);
}
