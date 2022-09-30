package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class CoreTextTagBases {

    public CoreTextTagBases() {

        // <--[tag]
        // @attribute <empty>
        // @returns ElementTag
        // @description
        // Returns an empty element.
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "empty", (attribute) -> new ElementTag(""));

        // <--[tag]
        // @attribute <&at>
        // @returns ElementTag
        // @description
        // Returns a at symbol: @
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&at", (attribute) -> new ElementTag("@"));

        // <--[tag]
        // @attribute <&pc>
        // @returns ElementTag
        // @description
        // Returns a percent symbol: %
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&pc", (attribute) -> new ElementTag("%"));

        // <--[tag]
        // @attribute <&nl>
        // @returns ElementTag
        // @description
        // Returns a newline symbol.
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&nl", (attribute) -> new ElementTag("\n"));

        // <--[tag]
        // @attribute <&ss>
        // @returns ElementTag
        // @description
        // Returns an internal coloring symbol: §
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&ss", (attribute) -> new ElementTag("§"));

        // <--[tag]
        // @attribute <&sq>
        // @returns ElementTag
        // @description
        // Returns a single-quote symbol: '
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&sq", (attribute) -> new ElementTag("'"));

        // <--[tag]
        // @attribute <&sp>
        // @returns ElementTag
        // @description
        // Returns a space symbol.
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&sp", (attribute) -> new ElementTag(String.valueOf(' ')));

        // <--[tag]
        // @attribute <&nbsp>
        // @returns ElementTag
        // @description
        // Returns a non-breaking space symbol.
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&nbsp", (attribute) -> new ElementTag(CoreUtilities.NBSP));

        // <--[tag]
        // @attribute <&dq>
        // @returns ElementTag
        // @synonyms &quote
        // @description
        // Returns a double-quote symbol: "
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&dq", (attribute) -> new ElementTag("\""));

        // <--[tag]
        // @attribute <&co>
        // @returns ElementTag
        // @description
        // Returns a colon symbol: :
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&co", (attribute) -> new ElementTag(":"));

        // <--[tag]
        // @attribute <&rb>
        // @returns ElementTag
        // @description
        // Returns a right-bracket symbol: ]
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&rb", (attribute) -> new ElementTag("]"));

        // <--[tag]
        // @attribute <&lb>
        // @returns ElementTag
        // @description
        // Returns a left-bracket symbol: [
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&lb", (attribute) -> new ElementTag("["));

        // <--[tag]
        // @attribute <&rc>
        // @returns ElementTag
        // @description
        // Returns a right-brace symbol: }
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&rc", (attribute) -> new ElementTag("}"));

        // <--[tag]
        // @attribute <&lc>
        // @returns ElementTag
        // @description
        // Returns a left-brace symbol: {
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&lc", (attribute) -> new ElementTag("{"));

        // <--[tag]
        // @attribute <&ns>
        // @returns ElementTag
        // @description
        // Returns a number sign / hash / pound symbol: #
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&ns", (attribute) -> new ElementTag("#"));

        // <--[tag]
        // @attribute <&lt>
        // @returns ElementTag
        // @description
        // Returns a less than symbol: <
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&lt", (attribute) -> new ElementTag("<"));

        // <--[tag]
        // @attribute <&gt>
        // @returns ElementTag
        // @description
        // Returns a greater than symbol: >
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&gt", (attribute) -> new ElementTag(">"));

        // <--[tag]
        // @attribute <&bs>
        // @returns ElementTag
        // @description
        // Returns a backslash symbol: \
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&bs", (attribute) -> new ElementTag("\\"));

        // <--[tag]
        // @attribute <&chr[<character>]>
        // @returns ElementTag
        // @description
        // Returns the Unicode character specified. e.g. <&chr[2665]> returns a heart.
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "&chr", (attribute) -> new ElementTag(String.valueOf((char) Integer.parseInt(attribute.getParam(), 16))));

        // <--[tag]
        // @attribute <n>
        // @returns ElementTag
        // @description
        // Returns a newline symbol.
        // -->
        TagManager.registerStaticTagBaseHandler(ElementTag.class, "n", (attribute) -> new ElementTag("\n"));
    }
}
