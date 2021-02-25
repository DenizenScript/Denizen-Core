package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;

public interface FlaggableObject extends ObjectTag {

    AbstractFlagTracker getFlagTracker();

    default AbstractFlagTracker getFlagTrackerForTag() {
        return getFlagTracker();
    }

    void reapplyTracker(AbstractFlagTracker tracker);

    default String getReasonNotFlaggable() {
        return "unknown reason - something went wrong";
    }
}
