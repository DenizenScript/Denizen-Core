package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.objects.ObjectTag;

public abstract class ActionableDataProvider {

    /**
     * Return the value object at a key.
     * Result should generally be ElementTag or ListTag.
     */
    public abstract ObjectTag getValueAt(String keyName);

    /**
     * Set the valueu object to a key.
     * Value will be ElementTag or ListTag.
     * null indicates to remove the key.
     */
    public abstract void setValueAt(String keyName, ObjectTag value);
}
