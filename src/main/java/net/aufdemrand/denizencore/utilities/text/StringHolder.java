package net.aufdemrand.denizencore.utilities.text;

import net.aufdemrand.denizencore.utilities.CoreUtilities;

public class StringHolder {

    public final String str;

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
            return low.equals(CoreUtilities.toLowerCase((String)obj));
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
