package com.denizenscript.denizencore.utilities.text;

import com.denizenscript.denizencore.utilities.CoreUtilities;

/** Helper for case-insensitive strings that remember their original casing. */
public class StringHolder {

    /** Input text with original casing. */
    public final String str;

    /** Input text, pre-lowercased. */
    public final String low;

    public StringHolder(String _str) {
        str = _str;
        low = CoreUtilities.toLowerCase(_str);
    }

    @Override
    public int hashCode() {
        return low.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof String) {
            return CoreUtilities.equalsIgnoreCase(low, (String) obj);
        }
        else if (obj instanceof StringHolder) {
            return low.equals(((StringHolder) obj).low);
        }
        return false;
    }

    @Override
    public String toString() {
        return str;
    }
}
