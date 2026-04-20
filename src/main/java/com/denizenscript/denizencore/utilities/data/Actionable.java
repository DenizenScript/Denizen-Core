package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagContext;

public interface Actionable<T extends ObjectTag> {

    default T operationAdd(ObjectTag value, TagContext context) {
        throw new DataActionException("This object does not support adding operations.");
    }

    default T operationSub(ObjectTag value, TagContext context) {
        throw new DataActionException("This object does not support subtracting operations.");
    }

    default T operationMul(ObjectTag value, TagContext context) {
        throw new DataActionException("This object does not support multiplying operations.");
    }

    default T operationDiv(ObjectTag value, TagContext context) {
        throw new DataActionException("This object does not support dividing operations.");
    }
}
