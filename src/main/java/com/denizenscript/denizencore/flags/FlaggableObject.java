package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;

public interface FlaggableObject extends ObjectTag {

    AbstractFlagTracker getFlagTracker();

    void reapplyTracker(AbstractFlagTracker tracker);
}
