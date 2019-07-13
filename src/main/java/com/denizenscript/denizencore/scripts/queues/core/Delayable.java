package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.objects.core.DurationTag;

public interface Delayable {

    Delayable setPaused(boolean paused);

    boolean isPaused();

    void delayFor(DurationTag duration);

    boolean isDelayed();

    boolean isInstantSpeed();
}
