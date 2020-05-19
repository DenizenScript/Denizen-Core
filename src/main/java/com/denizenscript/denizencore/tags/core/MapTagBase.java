package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class MapTagBase {

    public MapTagBase() {

        // <--[tag]
        // @attribute <map[<map>]>
        // @returns MapTag
        // @description
        // Returns a map object constructed from the input value.
        // -->
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                mapTags(event);
            }
        }, "map");
    }

    public void mapTags(ReplaceableTagEvent event) {
        if (!event.matches("map") || event.replaced()) {
            return;
        }
        MapTag map = null;
        if (event.hasNameContext()) {
            map = MapTag.valueOf(event.getNameContext(), event.getAttributes().context);
        }
        if (map == null) {
            return;
        }
        Attribute attribute = event.getAttributes();
        event.setReplacedObject(CoreUtilities.autoAttrib(map, attribute.fulfill(1)));
    }
}
