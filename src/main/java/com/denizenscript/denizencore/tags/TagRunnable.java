package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public abstract class TagRunnable implements Cloneable {

    public static abstract class ObjectForm<T extends ObjectTag> implements Cloneable {

        @Override
        public ObjectForm<T> clone() {
            try {
                return (ObjectForm<T>) super.clone();
            }
            catch (Exception ex) {
                Debug.echoError(ex);
                return null;
            }
        }

        public String name = null;

        public abstract ObjectTag run(Attribute attribute, T object);
    }

    public static abstract class RootForm implements Cloneable {

        @Override
        public RootForm clone() {
            try {
                return (RootForm) super.clone();
            }
            catch (Exception ex) {
                Debug.echoError(ex);
                return null;
            }
        }

        public String name = null;

        public abstract void run(ReplaceableTagEvent event);
    }

    @Override
    public TagRunnable clone() {
        try {
            return (TagRunnable) super.clone();
        }
        catch (Exception ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public String name = null;

    /**
     * Calculates the tag.
     *
     * @param attribute the tag input.
     * @param object    the object being calculated against.
     * @return null if this tag is invalid or a string of the return value if it is valid.
     */
    public abstract String run(Attribute attribute, ObjectTag object);
}
