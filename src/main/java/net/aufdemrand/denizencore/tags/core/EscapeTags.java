package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.TagRunnable;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public class EscapeTags {

    public EscapeTags() {
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
    // @ = &at
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
    // These symbols are automatically used by the internal system, if you are
    // writing your own property string and need to escape some symbols, you
    // can just directly type them in, EG: i@stick[display_name=&ltFancy&spStick&gt]
    // -->

    /**
     * A quick function to escape book Strings.
     * This is just to prevent tag reading errors.
     *
     * @param input the unescaped data.
     * @return the escaped data.
     */
    public static String Escape(String input) {
        if (input == null) {
            return null;
        }
        return TagManager.cleanOutputFully(input)
                .replace("&", "&amp").replace("|", "&pipe")
                .replace(">", "&gt").replace("<", "&lt")
                .replace("\n", "&nl").replace(";", "&sc")
                .replace("[", "&lb").replace("]", "&rb")
                .replace(":", "&co").replace("@", "&at")
                .replace(".", "&dot").replace("\\", "&bs")
                .replace("'", "&sq").replace("\"", "&quo")
                .replace("!", "&exc").replace("/", "&fs")
                .replace("§", "&ss").replace("#", "&ns");
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
        return TagManager.cleanOutputFully(input)
                .replace("&pipe", "|").replace("&nl", "\n")
                .replace("&gt", ">").replace("&lt", "<")
                .replace("&sc", ";").replace("&sq", "'")
                .replace("&lb", "[").replace("&rb", "]")
                .replace("&sp", String.valueOf((char) 0x00A0))
                .replace("&co", ":").replace("&at", "@")
                .replace("&dot", ".").replace("&bs", "\\")
                .replace("&quo", "\"").replace("&exc", "!")
                .replace("&fs", "/").replace("&ss", "§")
                .replace("&ns", "#").replace("&amp", "&");
    }

    public void escapeTags(ReplaceableTagEvent event) {
        // <--[tag]
        // @attribute <escape:<text_to_escape>>
        // @returns Element
        // @description
        // Returns the text simply escaped to prevent tagging conflicts.
        // See <@link language Property Escaping>
        // -->
        if (event.matches("escape")) {
            if (!event.hasValue()) {
                dB.echoError("Escape tag '" + event.raw_tag + "' does not have a value!");
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(new Element(Escape(event.getValue())), event.getAttributes().fulfill(1)));
        }
    }

    public void unEscapeTags(ReplaceableTagEvent event) {
        // <--[tag]
        // @attribute <unescape:<escaped_text>>
        // @returns Element
        // @description
        // Returns the text with escaping removed.
        // See <@link language Property Escaping>
        // -->
        if (event.matches("unescape")) {
            if (!event.hasValue()) {
                dB.echoError("Escape tag '" + event.raw_tag + "' does not have a value!");
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(new Element(unEscape(event.getValue())), event.getAttributes().fulfill(1)));
        }
    }
}
