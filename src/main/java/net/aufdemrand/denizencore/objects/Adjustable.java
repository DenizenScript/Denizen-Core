package net.aufdemrand.denizencore.objects;

import net.aufdemrand.denizencore.utilities.debugging.dB;

public interface Adjustable extends dObject {

    /**
     * Sets a specific attribute using this object to modify the necessary data.
     *
     * @param mechanism the mechanism to gather change information from
     */
    void adjust(Mechanism mechanism);

    default void safeAdjust(Mechanism mechanism) {
        mechanism.isProperty = false;
        if (mechanism.shouldDebug()) {
            dB.echoDebug(mechanism.context, "Adjust mechanism '" + mechanism.getName() + "' on object of type '" + getObjectType() + "'...");
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
            dB.echoDebug(mechanism.context, "Applying property '" + mechanism.getName() + "' on object of type '" + getObjectType() + "'...");
        }
        applyProperty(mechanism);
        mechanism.autoReport();
    }
}
