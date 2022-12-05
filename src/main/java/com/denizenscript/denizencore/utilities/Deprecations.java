package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.*;

import java.util.HashSet;

/**
 * This is a special class to contain all deprecation warnings, ordered by date they were added.
 * This should help in keeping track of what's been deprecated (but still is present).
 */
public class Deprecations {

    /**
     * Tracker of deprecation messages that were triggered recently (since last count).
     */
    public static HashSet<String> firedRecently = new HashSet<>();

    // Added 2020/03/14, moved from future to current 2021/11/12, moved to strong 2022/04/04.
    // Should be rapidly advanced through deprecation levels: removing this allows for a significant amount of legacy code removal.
    public static Warning eventCommand = new StrongWarning("eventCommand", "The event command is deprecated: use 'customevent' instead. The 'event' command represents an outdated idea of how events should function inside.");

    // Added on 2019/02/06
    // Bad candidate for functionality remove - 'c' was used often.
    // Recommend removal 2023 or later.
    public static Warning contextShorthand = new StrongWarning("contextShorthand", "Short-named tags are hard to read. Please use 'context' instead of 'c' as a root tag.");
    // 'e' may be safe for removal
    public static Warning entryShorthand = new StrongWarning("entryShorthand", "Short-named tags are hard to read. Please use 'entry' instead of 'e' as a root tag.");

    // Added on 2019/07/13
    // Bad candidate for functionality removal - used to be commonly used
    public static Warning oldEscapeTags = new StrongWarning("oldEscapeTags", "'escape:' tags are deprecated. Please use '.escaped' element tags instead.");

    // Added on 2019/09/13
    // Bad candidate for functionality removal - used to be commonly used
    public static Warning elementAsIntTag = new StrongWarning("elementAsIntTag", "'element.as_int' tag is deprecated: use '.round', '.round_down', or '.round_up'.");

    // Added on 2019/10/09, but was changed earlier.
    public static Warning oldTernTag = new StrongWarning("oldTernTag", "'tern[a]:b||c' tag style is deprecated. Please use 'tern[a].pass[b].fail[c]' tag style instead.");

    // Added on 2019/10/08, made current on 2020/02/12
    public static Warning ifCommandSingleLine = new StrongWarning("ifCommandSingleLine", "Single line if commands are deprecated. Please update them to modern format.");
    public static Warning oldBraceSyntax = new StrongWarning("oldBraceSyntax", "The { braced } command format is deprecated. Please use the ':' colon syntax (refer to documentation).");

    // Added 2020/03/14
    public static Warning oldStyleRandomCommand = new StrongWarning("oldStyleRandomCommand", "Using the 'random' command with an argument number is deprecated: use the modern colon syntax instead (refer to documentation).");

    // Added 2020/06/13.
    public static Warning scriptConstantTag = new Warning("scriptConstantTag", "The script.constant system has been deprecated in favor of just using data_key.");

    // Added 2021/02/17.
    public static Warning listEscapeContents = new SlowWarning("listEscapeContents", "The tags ListTag.escape_contents and unescape_contents are deprecated: use parse[escaped] and parse[unescaped], or just don't escape in the first place as most list escapes are no longer needed.");

    // Added 2021/04/16, made strong 2022/03/19.
    // Valid candidate for functionality remove with a 'past deprecations' backup warning - has been deprecated for a while, and is a trivial update to apply
    public static Warning ymlFileExtension = new StrongWarning("ymlFileExtension", "Denizen scripts use the '.dsc' file extension, not '.yml'. Please follow the Denizen beginner's guide https://guide.denizenscript.com/ - if you need help, ask in the official Denizen Discord @ https://discord.gg/Q6pZGSR");

    // Added 2019/10/03, made slow 2021/11/14.
    public static Warning inAreaSwitchFormat = new SlowWarning("inAreaSwitchFormat", "The old 'in <area>' in-line event format is deprecated, use the switch format for 'in:<area>'.");

    // ==================== VERY SLOW deprecations ====================
    // These are only shown minimally, so server owners are aware of them but not bugged by them. Only servers with active scripters (using 'ex reload') will see them often.

    // Added 2020/05/23, bump to normal slow warning by 2023.
    public static Warning timeTagRewrite = new VerySlowWarning("timeTagRewrite", "Using old Duration-Time - TimeTag is now separate from DurationTag, and some tags have changed as a result.");

    // Added 2020/05/29, bump to normal slow warning by 2023.
    public static Warning listOldMapTags = new VerySlowWarning("listOldMapTags", "Old list.map_* tags are deprecated: use the modern MapTag options instead.");

    // Added 2020/06/13, bump to normal slow warning by 2023.
    public static Warning yamlDataContainer = new VerySlowWarning("yamlDataContainer", "'yaml data' containers are now just called 'data' containers.");

    // Added 2021/10/17, bump to normal slow warning by 2023.
    public static Warning queueExists = new VerySlowWarning("queueExists", "'queue.exists[...]' tag is deprecated in favor of queue[...].exists");
    public static Warning queueStats = new VerySlowWarning("queueStats", "'queue.stats' tag is deprecated in favor of 'util.event_stats', and 'queue.list' is deprecated in favor of 'util.queues'");

    // Added 2021/10/28, bump to normal slow warning by 2023.
    public static Warning dynamicPrefix = new VerySlowWarning("dynamicPrefix", "Dynamically prefixed arguments (for 'prefix:value' arguments, like '<[sometag]>:<[somevalue]>') were never officially permitted and are now deprecated. You must specify a prefix explicitly if one is needed.");

    // Added 2020/05/24, bump to normal slow warning by 2023.
    private static String pointlessSubtagPrefix = "Most pointless sub-tags are deprecated in favor of explicit unique tags. ";
    public static Warning flagIsExpiredTag = new VerySlowWarning("flagIsExpiredTag", pointlessSubtagPrefix + "'flag[...].is_expired' is deprecated: use 'has_flag[...]' instead.");
    public static Warning flagExpirationTag = new VerySlowWarning("flagExpirationTag", pointlessSubtagPrefix + "'flag[...].expiration' is deprecated: use 'flag_expiration[...]' instead.");

    // Added 2020/12/14, but deprecated unofficially earlier, bump to normal slow warning by 2023.
    public static Warning queueClear = new VerySlowWarning("queueClear", "Usage of 'queue clear' or 'queue stop' to stop the current queue is deprecated: use the 'stop' command.");

    // Added 2022/08/23, bump forward rapidly to enable spread of autoExecute.
    public static Warning outOfOrderArgs = new VerySlowWarning("outOfOrderArgs", "Command has out-of-order linear arguments. This can only be interpreted by the legacy parser, and will not be understood by the modern parser. Please make sure your command's linear arguments match the documented order.");

    // ==================== FUTURE deprecations ====================

    // Added 2021/02/04, deprecate officially by 2024.
    public static Warning splitNewDataAction = new FutureWarning("splitNewDataAction", "The 'split to new list' data action ('key:!|:value') is deprecated: this no longer has a purpose, as you can instead just set to the list.");

    // Added 2021/04/14, deprecate officially by 2023.
    public static Warning locallyArgument = new FutureWarning("locallyArgument", "The 'locally' argument in run/inject is deprecated: just specify the script name, or <script>.");

    // Added 2021/12/08, deprecate officially by 2023.
    public static Warning oldUtilRandomTags = new FutureWarning("oldUtilRandomTags", "Several 'util.random.xxx' are deprecated in favor of 'util.random_xxx' (the same tag, but '_' instead of a '.', for format standardization reasons)");

    // Added 2022/03/30, deprecate officially by 2024.
    public static Warning oldNonSecretTagPassword = new FutureWarning("oldNonSecretTagPassword", "Passwords and tokens used to be sent through tags or passwordfiles, it is now recommended instead that you use the 'secrets.secret' file with SecretTag. This includes the 'SQL connect' command.");

    // Added 2022/04/11, deprecate officially by 2024.
    public static Warning prebinaryTags = new FutureWarning("prebinaryTags", "Tags and tools related to binary processing that predate the BinaryTag feature are deprecated in favor of using BinaryTag. This includes 'ElementTag.base64_encode/decode', 'hex_encode/decode', ... refer to meta-docs for specifics");

    // Added 2022/12/05, relevant years earlier, deprecate officially by 2024.
    public static Warning intTagVariants = new FutureWarning("intTagVariants", "Several of the _int math tag variants (excluding 'div_int') don't actually have any benefit over the non-_int forms and haven't for many years now.");

    // Added 2022/08/21, deprecate officially by 2025.
    public static Warning asXTags = new FutureWarning("asXTags", "Tags of the form 'as_x' where 'x' is a type, such as 'as_list', are deprecated in favor of the tag ObjectTag.as[<type>], used like 'as[list]'.");

    // ==================== PAST deprecations of things that are already gone but still have a warning left behind ====================

    // Removed in February 2020.
    public static Warning instantTags = new StrongWarning("instantTags", "Instant tags (those with a caret prefix, like <^tag>) were removed in Denizen 1.1.3, and can no longer be used. Instead, pre-define the player or NPC on the line before.");

    // Added on 2019/08/27, removed 2020/10/24.
    public static Warning yamlFixFormatting = new StrongWarning("yamlFixFormatting", "YAML command 'fix_formatting' argument is deprecated: this should never be used.");

    // Added on 2021/04/16.
    public static Warning dscriptFileExtension = new StrongWarning("dscriptFileExtension", "'.dscript' extension has never been officially supported. Please use '.dsc'.");
}
