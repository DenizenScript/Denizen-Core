package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.utilities.AsciiMatcher;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.TagManager;

public class EscapeTagBase {

    public EscapeTagBase() {
        // Intentionally no docs due to deprecation
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                escapeTags(event);
            }
        }, "escape");
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                unEscapeTags(event);
            }
        }, "unescape");
    }

    // <--[language]
    // @name Property Escaping
    // @group Useful Lists
    // @description
    // Some item properties (and corresponding mechanisms) need to escape their
    // text output/input to prevent players using them to cheat the system
    // (EG, if a player set the display name of an item to:
    //      'name;enchantments=damage_all,3', they would get a free enchantment!)
    // These are the escape codes used to prevent that:
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
    //
    // Also, you can input a non-breaking space via &sp
    //
    // Note that these are NOT tag names. They are exclusively used by the escaping system.
    //
    // These symbols are automatically used by the internal system, if you are
    // writing your own property string and need to escape some symbols, you
    // can just directly type them in, EG: stick[display_name=&ltFancy&spStick&gt]
    //
    // You can use these escape codes in a tag via <@link tag ElementTag.escaped> and <@link tag ElementTag.unescaped>.
    //
    // -->

    public static AsciiMatcher needsEscapingMatcher = new AsciiMatcher("&|<>\n;[]:@.\\'\"!/§#");

    /**
     * A quick function to escape book Strings.
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
        return input;
    }

    /**
     * A quick function to reverse a book string escaping.
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
        input = CoreUtilities.replace(input, "&sp", CoreUtilities.NBSP);
        input = CoreUtilities.replace(input, "&amp", "&");
        return input;
    }

    public void escapeTags(ReplaceableTagEvent event) {
        Deprecations.oldEscapeTags.warn(event.getScriptEntry());
        if (event.matches("escape")) {
            if (!event.hasValue()) {
                Debug.echoError("Escape tag '" + event.raw_tag + "' does not have a value!");
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(escape(event.getValue())), event.getAttributes().fulfill(1)));
        }
    }

    public void unEscapeTags(ReplaceableTagEvent event) {
        Deprecations.oldEscapeTags.warn(event.getScriptEntry());
        if (event.matches("unescape")) {
            if (!event.hasValue()) {
                Debug.echoError("Unescape tag '" + event.raw_tag + "' does not have a value!");
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(unEscape(event.getValue())), event.getAttributes().fulfill(1)));
        }
    }
}
