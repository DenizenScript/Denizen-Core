package com.denizenscript.denizencore.scripts.queues;

import com.denizenscript.denizencore.objects.ObjectTag;

/**
 * Provides contexts to a queue.
 */
public interface ContextSource {

    public boolean getShouldCache();

    public ObjectTag getContext(String name);
}
