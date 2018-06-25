package net.aufdemrand.denizencore.objects;

import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public abstract class TagRunnable implements Cloneable {

    public static abstract class ObjectForm implements Cloneable {

        @Override
        public ObjectForm clone() {
            try {
                return (ObjectForm) super.clone();
            }
            catch (Exception ex) {
                dB.echoError(ex);
                return null;
            }
        }

        public String name = null;

        public abstract dObject run(Attribute attribute, dObject object);
    }

    public static abstract class RootForm implements Cloneable {

        @Override
        public RootForm clone() {
            try {
                return (RootForm) super.clone();
            }
            catch (Exception ex) {
                dB.echoError(ex);
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
            dB.echoError(ex);
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
    public abstract String run(Attribute attribute, dObject object);
}
