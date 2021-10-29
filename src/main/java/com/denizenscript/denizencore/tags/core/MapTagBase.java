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
        // Refer to <@link ObjectType MapTag>.
        // For example: <map[a=1;b=2;c=3]>
        // -->
        TagManager.registerStaticTagBaseHandler(MapTag.class, "map", (attribute) -> {
            if (!attribute.hasParam()) {
                return new MapTag();
            }
            return MapTag.getMapFor(attribute.getParamObject(), attribute.context);
        });
    }
}
