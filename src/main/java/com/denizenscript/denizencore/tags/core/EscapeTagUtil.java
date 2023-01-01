package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class EscapeTagUtil {

    // <--[language]
    // @name Escaping System
    // @group Useful Lists
    // @description
    // Sometimes, you need to avoid having symbols that might be misinterpreted in your data.
    // Denizen provides the tags <@link tag ElementTag.escaped> and <@link tag ElementTag.unescaped> to help deal with that.
    // Escaped replaces symbols with the relevant escape-code, and unescaped replaces the escape-codes with the real symbols.
    //
    // Some older style tags and mechanisms may automatically use this same escaping system.
    //
    // The mapping of symbols to escape codes is as follows:
    //
    // (THESE ARE NOT TAG NAMES)
    //
    // | = &pipe
    // < = &lt
    // > = &gt
    // newline = &nl
    // & = &amp
    // ; = &sc
    // [ = &lb
    // ] = &rb
    // : = &co
    // at sign @ = &at
    // . = &dot
    // \ = &bs
    // ' = &sq
    // " = &quo
    // ! = &exc
    // / = &fs
    // § = &ss
    // # = &ns
    // = = &eq
    // { = &lc
    // } = &rc
    //
    // Also, you can input a non-breaking space via &sp
    //
    // Note that these are NOT tag names. They are exclusively used by the escaping system.
    //
    // -->

    public static AsciiMatcher needsEscapingMatcher = new AsciiMatcher("&|<>\n;[]:@.\\'\"!/§#={}");

    /**
     * A quick function to escape strings.
     * This is just to prevent tag reading errors.
     *
     * @param input the unescaped data.
     * @return the escaped data.
     */
    public static String escape(String input) {
        if (input == null) {
            return null;
        }
        if (!needsEscapingMatcher.containsAnyMatch(input)) {
            return input;
        }
        input = CoreUtilities.replace(input, "&", "&amp");
        input = CoreUtilities.replace(input, "|", "&pipe");
        input = CoreUtilities.replace(input, "<", "&lt");
        input = CoreUtilities.replace(input, ">", "&gt");
        input = CoreUtilities.replace(input, "\n", "&nl");
        input = CoreUtilities.replace(input, ";", "&sc");
        input = CoreUtilities.replace(input, "[", "&lb");
        input = CoreUtilities.replace(input, "]", "&rb");
        input = CoreUtilities.replace(input, ":", "&co");
        input = CoreUtilities.replace(input, "@", "&at");
        input = CoreUtilities.replace(input, ".", "&dot");
        input = CoreUtilities.replace(input, "\\", "&bs");
        input = CoreUtilities.replace(input, "'", "&sq");
        input = CoreUtilities.replace(input, "\"", "&quo");
        input = CoreUtilities.replace(input, "!", "&exc");
        input = CoreUtilities.replace(input, "/", "&fs");
        input = CoreUtilities.replace(input, "§", "&ss");
        input = CoreUtilities.replace(input, "#", "&ns");
        input = CoreUtilities.replace(input, "=", "&eq");
        input = CoreUtilities.replace(input, "{", "&lc");
        input = CoreUtilities.replace(input, "}", "&rc");
        return input;
    }

    /**
     * A quick function to reverse a string escaping.
     * This is just to prevent tag reading errors.
     *
     * @param input the escaped data.
     * @return the unescaped data.
     */
    public static String unEscape(String input) {
        if (input == null) {
            return null;
        }
        if (input.indexOf('&') == -1) {
            return input;
        }
        input = CoreUtilities.replace(input, "&pipe", "|");
        input = CoreUtilities.replace(input, "&lt", "<");
        input = CoreUtilities.replace(input, "&gt", ">");
        input = CoreUtilities.replace(input, "&nl", "\n");
        input = CoreUtilities.replace(input, "&sc", ";");
        input = CoreUtilities.replace(input, "&lb", "[");
        input = CoreUtilities.replace(input, "&rb", "]");
        input = CoreUtilities.replace(input, "&co", ":");
        input = CoreUtilities.replace(input, "&at", "@");
        input = CoreUtilities.replace(input, "&dot", ".");
        input = CoreUtilities.replace(input, "&bs", "\\");
        input = CoreUtilities.replace(input, "&sq", "'");
        input = CoreUtilities.replace(input, "&quo", "\"");
        input = CoreUtilities.replace(input, "&exc", "!");
        input = CoreUtilities.replace(input, "&fs", "/");
        input = CoreUtilities.replace(input, "&ss", "§");
        input = CoreUtilities.replace(input, "&ns", "#");
        input = CoreUtilities.replace(input, "&eq", "=");
        input = CoreUtilities.replace(input, "&lc", "{");
        input = CoreUtilities.replace(input, "&rc", "}");
        input = CoreUtilities.replace(input, "&sp", CoreUtilities.NBSP);
        input = CoreUtilities.replace(input, "&amp", "&");
        return input;
    }
}
