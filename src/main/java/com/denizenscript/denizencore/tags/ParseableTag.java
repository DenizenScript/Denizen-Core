package com.denizenscript.denizencore.tags;


import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

import java.util.List;

/**
 * Simple core representation of a section of user input that may contain tags, with methods to parse the tags inside.
 */
public class ParseableTag {

    public ObjectTag rawObject;

    public List<TagManager.ParseableTagPiece> pieces;

    public TagManager.ParseableTagPiece singleTag;

    public boolean hasTag;

    /**
     * Get the object represented by this tag.
     * If the user input was plaintext (ie not a tag, or text mixed with a tag), with return an ElementTag.
     */
    public final ObjectTag parse(TagContext context) {
        if (rawObject != null) {
            return rawObject;
        }
        else if (singleTag != null) {
            return TagManager.readSingleTagObject(singleTag, context);
        }
        return TagManager.parseChainObject(pieces, context);
    }

    public ParseableTag() {
    }

    public ParseableTag(String text) {
        ElementTag rawElement = new ElementTag(text, true);
        rawElement.isRawInput = true;
        rawObject = rawElement;
    }

    @Override
    public String toString() {
        if (rawObject != null) {
            return rawObject.toString();
        }
        else {
            return "(ParseableTag: non-static value)";
        }
    }
}
