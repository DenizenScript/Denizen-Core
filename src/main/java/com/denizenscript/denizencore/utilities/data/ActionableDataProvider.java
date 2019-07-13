package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.objects.dObject;

public abstract class ActionableDataProvider {

    /**
     * Return the value object at a key.
     * Result should generally be Element or dList.
     */
    public abstract dObject getValueAt(String keyName);

    /**
     * Set the valueu object to a key.
     * Value will be Element or dList.
     * null indicates to remove the key.
     */
    public abstract void setValueAt(String keyName, dObject value);
}
