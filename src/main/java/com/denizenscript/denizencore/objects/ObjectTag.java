package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public interface ObjectTag {

    /*
     * ObjectTags should contain these two static methods, of which valueOf contains a valid
     * annotation for ObjectFetcher
     *
     * public static ObjectTag valueOf(String string);
     *
     * valueOf() should take a string representation of the object, preferably with a valid object
     * notation (x@), and turn it into a new instance of the ObjectTag. Care has to be taken to
     * ensure that it is compatible with the tag system (ie. no periods (.) outside of square brackets),
     * and other parts of Denizen.
     *
     * Since your object may be using the ObjectTag Attributes System, valueOf should take that into
     * account as well.
     *
     *
     * public static boolean matches()
     *
     * matches() should use some logic to determine if a string is in the proper format to possibly
     * return a non-null valueOf() call.
     *
     */


    /**
     * Retrieves the dScript argument prefix. ObjectTags should provide a default
     * prefix if nothing else has been specified.
     *
     * @return the prefix
     */
    String getPrefix();


    /**
     * <p>Gets a standard dB representation of this argument. All ObjectTags should follow
     * suit.</p>
     * <p/>
     * Example: <br/>
     * <tt>
     * Location='x,y,z,world'
     * Location='unique_location(x,y,z,world)'
     * </tt>
     *
     * @return the debug information
     */
    default String debug() {
        return "<G>" + getPrefix() + "='<Y>" + debuggable() + "<G>'  ";
    }

    /**
     * Gets a debuggable format of the object. Like identify, but for console output.
     */
    default String debuggable() {
        return identify();
    }


    /**
     * Determines if this argument object is unique. This typically stipulates
     * that this object has been named, or has some unique identifier that
     * Denizen can use to recall it.
     *
     * @return true if this object is unique, false if it is a 'singleton generic argument/object'
     */
    boolean isUnique();


    /**
     * Returns the string type of the object. This is fairly verbose and crude, but used with
     * a basic dScriptArg attribute.
     *
     * @return a straight-up string description of the type of dScriptArg. ie. ListTag, LocationTag
     */
    String getObjectType();


    /**
     * Gets an ugly, but exact, string representation of this ObjectTag.
     * While not specified in the ObjectTag Interface, this value should be
     * able to be used with a static valueOf(String) method to reconstruct the object.
     *
     * @return a single-line string representation of this argument
     */
    String identify();


    /**
     * Gets an overall string representation of this ObjectTag.
     * This should give the basic jist of the object being identified, but
     * won't include the exactness that identify() uses.
     * <p/>
     * <code>
     * Example: i@gold_sword     vs.    i@gold_sword[display_name=Shiny Sword]
     * ^                       ^
     * +--- identifySimple()   +--- identify()
     * </code>
     * <p/>
     * This may produce the same results as identify(), depending on the complexity
     * of the object being identified.
     *
     * @return a single-line, 'simple' string representation of this argument
     */
    String identifySimple();


    /**
     * Sets the prefix for this argument, otherwise uses the default.
     *
     * @return the ObjectTag
     */
    ObjectTag setPrefix(String prefix);

    default Class<? extends ObjectTag> getObjectTagClass() {
        return getClass();
    }

    /**
     * Gets a specific attribute using this object to fetch the necessary data.
     *
     * @param attribute the name of the attribute
     * @return a string result of the fetched attribute
     */
    default ObjectTag getObjectAttribute(Attribute attribute) {
        String res = getAttribute(attribute);
        return res == null ? null : new ElementTag(res);
    }

    default String getAttribute(Attribute attribute) {
        return CoreUtilities.stringifyNullPass(getObjectAttribute(attribute));
    }

    /**
     * Get the "next object type down" - by default, an ElementTag of identify(), but can be different in some cases (eg a Player's next type down is Entity).
     * Should never be null.
     */
    default ObjectTag getNextObjectTypeDown() {
        return new ElementTag(identify());
    }

    /**
     * Optional special dynamic tag handling
     */
    default ObjectTag specialTagProcessing(Attribute attribute) {
        return null;
    }
}
