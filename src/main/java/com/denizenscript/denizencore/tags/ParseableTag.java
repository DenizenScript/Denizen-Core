package com.denizenscript.denizencore.tags;


import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;

import java.util.List;

public class ParseableTag {

    public ElementTag rawElement;

    public List<TagManager.ParseableTagPiece> pieces;

    public TagManager.ParseableTagPiece singleTag;

    public boolean hasTag;

    public final ObjectTag parse(TagContext context) {
        if (rawElement != null) {
            return rawElement;
        }
        else if (singleTag != null) {
            return TagManager.readSingleTagObject(singleTag, context);
        }
        return TagManager.parseChainObject(pieces, context);
    }

    public ParseableTag() {
    }

    public ParseableTag(String text) {
        rawElement = new ElementTag(text, true);
        rawElement.isRawInput = true;
    }
}
