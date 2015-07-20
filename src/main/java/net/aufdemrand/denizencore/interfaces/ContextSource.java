package net.aufdemrand.denizencore.interfaces;

import net.aufdemrand.denizencore.objects.dObject;

/**
 * Provides contexts to a queue.
 */
public interface ContextSource {

    public boolean getShouldCache();

    public dObject getContext(String name);
}
