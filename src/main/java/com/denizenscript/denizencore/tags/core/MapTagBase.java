package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagManager;

public class MapTagBase {

    public MapTagBase() {

        // <--[tag]
        // @attribute <map[(<map>)]>
        // @returns MapTag
        // @description
        // Returns a map object constructed from the input value.
        // Give no input to create an empty map.
        // Refer to <@link language MapTag objects>.
        // -->
        TagManager.registerTagHandler("map", (attribute) -> {
            if (!attribute.hasContext(1)) {
                return new MapTag();
            }
            return MapTag.getMapFor(attribute.getContextObject(1), attribute.context);
        });
    }
}
