package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.utilities.debugging.Debug;

public interface Adjustable extends ObjectTag {

    /**
     * Sets a specific attribute using this object to modify the necessary data.
     *
     * @param mechanism the mechanism to gather change information from
     */
    void adjust(Mechanism mechanism);

    default void safeAdjust(Mechanism mechanism) {
        mechanism.isProperty = false;
        if (mechanism.shouldDebug()) {
            Debug.echoDebug(mechanism.context, "Adjust mechanism '" + mechanism.getName() + "' on object of type '" + getObjectType() + "'...");
        }
        adjust(mechanism);
        mechanism.autoReport();
    }

    /**
     * Applies a property, passing it to 'adjust' or throwing an error, depending on whether
     * the mechanism may be used as a property.
     *
     * @param mechanism the mechanism to gather change information from
     */
    void applyProperty(Mechanism mechanism);

    default void safeApplyProperty(Mechanism mechanism) {
        mechanism.isProperty = true;
        if (mechanism.shouldDebug()) {
            Debug.echoDebug(mechanism.context, "Applying property '" + mechanism.getName() + "' on object of type '" + getObjectType() + "'...");
            if (Debug.verbose) {
                try {
                    throw new Exception("Stack trace of property");
                }
                catch (Exception ex) {
                    Debug.echoError(ex);
                }
            }
        }
        applyProperty(mechanism);
        mechanism.autoReport();
    }
}
