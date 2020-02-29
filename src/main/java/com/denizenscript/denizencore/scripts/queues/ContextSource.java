package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.objects.ObjectTag;

/**
 * Provides contexts to a queue.
 */
public interface ContextSource {

    ObjectTag getContext(String name);
}
