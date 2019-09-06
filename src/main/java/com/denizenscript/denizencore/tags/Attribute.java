package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Attribute {

    public static class AttributeComponent {

        public final String rawKey;

        public final String key;

        public final String context;

        public AttributeComponent(String inp) {
            if (inp.endsWith("]") && inp.contains("[")) {
                int ind = inp.indexOf('[');
                rawKey = inp.substring(0, ind);
                context = inp.substring(ind + 1, inp.length() - 1);
            }
            else {
                rawKey = inp;
                context = null;
            }
            key = CoreUtilities.toLowerCase(rawKey);
        }

        @Override
        public String toString() {
            if (context != null) {
                return key + "[" + context + "]";
            }
            return key;
        }
    }

    private static HashMap<String, AttributeComponent[]> attribsLookup = new HashMap<>();

    private static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    private static AttributeComponent[] separate_attributes(String attributes) {

        AttributeComponent[] matchesRes = attribsLookup.get(attributes);

        if (matchesRes != null) {
            return matchesRes;
        }

        ArrayList<AttributeComponent> matches = new ArrayList<>();

        int x1 = 0, x2 = -1;
        int braced = 0;

        char[] attrInp = attributes.toCharArray();

        for (int x = 0; x < attrInp.length; x++) {

            Character chr = attrInp[x];

            if (chr == '[') {
                braced++;
            }

            else if (x == attrInp.length - 1) {
                x2 = x + 1;
            }

            else if (chr == ']') {
                if (braced > 0) {
                    braced--;
                }
            }

            else if (chr == '.'
                    && !(x > 0 && isNumber(attrInp[x + 1]) && isNumber(attrInp[x - 1]))
                    && braced == 0) {
                x2 = x;
            }

            if (x2 > -1) {
                matches.add(new AttributeComponent(attributes.substring(x1, x2)));
                x2 = -1;
                x1 = x + 1;
            }

        }

        if (Debug.verbose) {
            Debug.log("attribute splitter: '" + attributes + "' becomes: " + matches);
        }

        matchesRes = new AttributeComponent[matches.size()];
        matchesRes = matches.toArray(matchesRes);

        attribsLookup.put(attributes, matchesRes);

        return matchesRes;
    }

    public AttributeComponent[] attributes;
    public ObjectTag[] contexts;

    ScriptEntry scriptEntry;
    public TagContext context;

    private String raw_tag;
    private String raw_tag_low;
    private int rawtaglen = -1;
    String origin;

    public String getRawTag() {
        rebuild_raw_tag();
        return raw_tag;
    }

    public String getRawTagLow() {
        rebuild_raw_tag();
        return raw_tag_low;
    }

    public List<String> seemingSuccesses = new ArrayList<>(2);

    public boolean hasContextFailed = false;

    public void resetErrorTrack() {
        seemingSuccesses.clear();
        hasContextFailed = false;
    }

    public ScriptEntry getScriptEntry() {
        return scriptEntry;
    }

    public String getOrigin() {
        return origin;
    }

    public Attribute(Attribute ref, ScriptEntry scriptEntry, TagContext context) {
        origin = ref.origin;
        this.scriptEntry = scriptEntry;
        this.context = context;
        attributes = ref.attributes;
        contexts = new ObjectTag[attributes.length];
        hadAlternative = ref.hadAlternative;
    }

    public Attribute(String attributes, ScriptEntry scriptEntry, TagContext context) {
        origin = attributes;
        this.scriptEntry = scriptEntry;
        this.context = context;
        this.attributes = separate_attributes(attributes);
        contexts = new ObjectTag[this.attributes.length];
    }

    public boolean matches(String string) {
        if (fulfilled >= attributes.length) {
            return false;
        }
        return attributes[fulfilled].key.equals(string);
    }

    public boolean startsWith(String string) {
        if (fulfilled >= attributes.length) {
            return false;
        }
        if (string.indexOf('.') >= 0) {
            if (Debug.verbose) {
                Debug.log("Trying tag startsWith " + string + " on tag " + raw_tag);
            }
            List<String> tmp = CoreUtilities.split(string, '.');
            if (tmp.size() + fulfilled > attributes.length) {
                return false;
            }
            for (int i = 0; i < tmp.size(); i++) {
                if (!attributes[fulfilled + i].key.equals(tmp.get(i))) {
                    return false;
                }
            }
            seemingSuccesses.add(string);
            return true;
        }
        if (attributes[fulfilled].key.equals(string)) {
            seemingSuccesses.add(string);
            return true;
        }
        return false;
    }

    public boolean startsWith(String string, int attribute) {
        return CoreUtilities.toLowerCase(getAttribute(attribute)).startsWith(string);
    }

    int fulfilled = 0;

    public boolean isComplete() {
        return fulfilled >= attributes.length;
    }

    public Attribute fulfill(int attributes) {
        resetErrorTrack();
        fulfilled += attributes;
        return this;
    }

    private void rebuild_raw_tag() {
        if (attributes.length == 0) {
            raw_tag = "";
            raw_tag_low = "";
            return;
        }
        if (fulfilled == rawtaglen) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (AttributeComponent attribute : attributes) {
            sb.append(attribute.toString()).append(".");
        }
        raw_tag = sb.toString();
        if (raw_tag.length() > 1) {
            raw_tag = raw_tag.substring(0, raw_tag.length() - 1);
        }
        raw_tag_low = CoreUtilities.toLowerCase(raw_tag);
        rawtaglen = fulfilled;
    }

    public boolean hasContext(int attribute) {
        attribute += fulfilled - 1;
        if (attribute < 0 || attribute >= attributes.length) {
            return false;
        }
        if (attributes[attribute].context != null) {
            return true;
        }
        hasContextFailed = true;
        return false;
    }

    public ObjectTag getContextObject(int attribute) {
        attribute += fulfilled - 1;
        if (attribute < 0 || attribute >= attributes.length) {
            return null;
        }
        ObjectTag tagged = contexts[attribute];
        if (tagged != null) {
            return tagged;
        }
        String inp = attributes[attribute].context;
        if (inp == null) {
            return null;
        }
        tagged = TagManager.tagObject(inp, context);
        contexts[attribute] = tagged;
        return tagged;
    }

    public String getContext(int attribute) {
        return CoreUtilities.stringifyNullPass(getContextObject(attribute));
    }

    private boolean hadAlternative = false;

    public boolean hasAlternative() {
        return hadAlternative;
    }

    public void setHadAlternative(boolean hadAlternative) {
        this.hadAlternative = hadAlternative;
    }

    public int getIntContext(int attribute) {
        try {
            if (hasContext(attribute)) {
                return Integer.valueOf(getContext(attribute));
            }
        }
        catch (Exception e) {
        }

        return 0;
    }

    public double getDoubleContext(int attribute) {
        try {
            if (hasContext(attribute)) {
                return Double.valueOf(getContext(attribute));
            }
        }
        catch (Exception e) {
        }
        return 0;
    }

    public String getAttribute(int num) {
        num += fulfilled - 1;
        if (num < 0 || num >= attributes.length) {
            return "";
        }
        return attributes[num].toString();
    }

    public String getAttributeWithoutContext(int num) {
        num += fulfilled - 1;
        if (num < 0 || num >= attributes.length) {
            return "";
        }
        return attributes[num].key;
    }

    public String unfilledString() {
        StringBuilder sb = new StringBuilder();
        for (int i = fulfilled; i < attributes.length; i++) {
            if (contexts[i] != null) {
                sb.append(attributes[i].key).append("[").append(contexts[i]).append("].");
            }
            else {
                sb.append(attributes[i].toString()).append(".");
            }
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attributes.length; i++) {
            if (contexts[i] != null) {
                sb.append(attributes[i].key).append("[").append(contexts[i]).append("].");
            }
            else {
                sb.append(attributes[i].toString()).append(".");
            }
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }
}
