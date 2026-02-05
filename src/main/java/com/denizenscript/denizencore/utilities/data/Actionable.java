package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagContext;

public interface Actionable<T extends ObjectTag> {

    default T additionOperation(ObjectTag value, TagContext context) {
        throw new DataActionException("This object does not support adding operations.");
    }

    default T subtractionOperation(ObjectTag value, TagContext context) {
        throw new DataActionException("This object does not support subtracting operations.");
    }

    default T multiplicationOperation(ObjectTag value, TagContext context) {
        throw new DataActionException("This object does not support multiplying operations.");
    }

    default T divisionOperation(ObjectTag value, TagContext context) {
        throw new DataActionException("This object does not support dividing operations.");
    }
}
