package com.denizenscript.denizencore.flags;

import com.denizenscript.denizencore.objects.ObjectTag;

public interface FlaggableObject extends ObjectTag {

    // <--[ObjectType]
    // @name FlaggableObject
    // @prefix None
    // @base None
    // @format
    // N/A
    //
    // @description
    // "FlaggableObject" is a pseudo-ObjectType that represents any type of object that can hold flags,
    // for use with <@link command flag> or any other flag related tags and mechanisms.
    //
    // Just because an ObjectType implements FlaggableObject, does not mean a specific instance of that object type is flaggable.
    // For example, LocationTag implements FlaggableObject, but a LocationTag-Vector (a location without a world) cannot hold a flag.
    //
    // -->

    AbstractFlagTracker getFlagTracker();

    default AbstractFlagTracker getFlagTrackerForTag() {
        return getFlagTracker();
    }

    void reapplyTracker(AbstractFlagTracker tracker);

    default String getReasonNotFlaggable() {
        return "unknown reason - something went wrong";
    }
}
