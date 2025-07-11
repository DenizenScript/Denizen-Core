package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.*;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a special class to contain all deprecation warnings, ordered by date they were added.
 * This should help in keeping track of what's been deprecated (but still is present).
 */
public class Deprecations {

    /**
     * Tracker of deprecation messages that were triggered recently (since last count).
     */
    public static ConcurrentHashMap<String, Boolean> firedRecently = new ConcurrentHashMap<>(); // Note: can be called async

    // ==================== REMOVE THESE ====================
    // Every warning inside this section should be removed from the codebase.

    // Added on 2019/02/06
    // Safe to remove now.
    public static Warning contextShorthand = new StrongWarning("contextShorthand", "Short-named tags are hard to read. Please use 'context' instead of 'c' as a root tag.");

    // Added on 2019/10/09, but was changed earlier.
    // 2022-year-end commonality: #11
    // Safe to remove now.
    public static Warning oldTernTag = new StrongWarning("oldTernTag", "'tern[a]:b||c' tag style is deprecated. Please use 'tern[a].pass[b].fail[c]' tag style instead.");

    // Added on 2019/10/08, made current on 2020/02/12
    // 2022-year-end commonality: #19
    // Safe to remove now.
    public static Warning ifCommandSingleLine = new StrongWarning("ifCommandSingleLine", "Single line if commands are deprecated. Please update them to modern format.");
    // 2022-year-end commonality: #22
    // Safe to remove now.
    public static Warning oldBraceSyntax = new StrongWarning("oldBraceSyntax", "The { braced } command format is deprecated. Please use the ':' colon syntax (refer to documentation).");

    // Added 2020/03/14
    // 2022-year-end commonality: #12
    // 2023-year-end commonality: #18
    // Safe to remove now.
    public static Warning oldStyleRandomCommand = new StrongWarning("oldStyleRandomCommand", "Using the 'random' command with an argument number is deprecated: use the modern colon syntax instead (refer to documentation).");

    // Added 2020/06/13, made strong 2022/12/31.
    // Safe to remove now.
    public static Warning scriptConstantTag = new StrongWarning("scriptConstantTag", "The script.constant system has been deprecated in favor of just using data_key.");

    // Added 2021/02/17, made normal 2022/12/31, made strong 2024/01/02.
    // Safe to remove now.
    public static Warning listEscapeContents = new StrongWarning("listEscapeContents", "The tags ListTag.escape_contents and unescape_contents are deprecated: use parse[escaped] and parse[unescaped], or just don't escape in the first place as most list escapes are no longer needed.");

    // ==================== STRONG deprecations ====================
    // These show up every time, and warn any online ops. These are made clear they need to be fixed ASAP.

    // Added 2021/04/16, made strong 2022/03/19.
    // Valid candidate for functionality remove with a 'past deprecations' backup warning - has been deprecated for a while, and is a trivial update to apply
    // 2022-year-end commonality: #34
    // 2023-year-end commonality: #21
    // 2024-year-end commonality: #14
    public static Warning ymlFileExtension = new StrongWarning("ymlFileExtension", "Denizen scripts use the '.dsc' file extension, not '.yml'. Please follow the Denizen beginner's guide https://guide.denizenscript.com/ - if you need help, ask in the official Denizen Discord @ https://discord.gg/Q6pZGSR");

    // Added 2020/05/29, made slow 2022/12/31, made normal 2024/01/02, made strong 2025/01/15.
    // Safe to remove now.
    public static Warning listOldMapTags = new StrongWarning("listOldMapTags", "Old list.map_* tags are deprecated: use the modern MapTag options instead.");

    // Added 2020/06/13, made slow 2022/12/31, made normal 2024/01/02, made strong 2025/01/15.
    // Safe to remove now.
    public static Warning yamlDataContainer = new StrongWarning("yamlDataContainer", "'yaml data' containers are now just called 'data' containers.");

    // Added 2021/10/17, made slow 2022/12/31, made normal 2024/01/02, made strong 2025/01/15.
    // Safe to remove now.
    public static Warning queueExists = new StrongWarning("queueExists", "'queue.exists[...]' tag is deprecated in favor of queue[...].exists");
    public static Warning queueStats = new StrongWarning("queueStats", "'queue.stats' tag is deprecated in favor of 'util.event_stats', and 'queue.list' is deprecated in favor of 'util.queues'");

    // Added 2020/05/24, made slow 2022/12/31, made normal 2024/01/02, made strong 2025/01/15.
    // Safe to remove now.
    public static final String pointlessSubtagPrefix = "Most pointless sub-tags are deprecated in favor of explicit unique tags. ";
    public static Warning flagIsExpiredTag = new StrongWarning("flagIsExpiredTag", pointlessSubtagPrefix + "'flag[...].is_expired' is deprecated: use 'has_flag[...]' instead.");
    public static Warning flagExpirationTag = new StrongWarning("flagExpirationTag", pointlessSubtagPrefix + "'flag[...].expiration' is deprecated: use 'flag_expiration[...]' instead.");

    // Added 2020/12/14, but deprecated unofficially earlier, made slow 2022/12/31, made normal 2024/01/02, made strong 2025/01/15.
    // 2023-year-end commonality: #25
    // Safe to remove now.
    public static Warning queueClear = new StrongWarning("queueClear", "Usage of 'queue clear' or 'queue stop' to stop the current queue is deprecated: use the 'stop' command.");

    // Added 2021/10/28, made slow 2022/12/31, made normal 2024/01/02, made strong 2025/01/15.
    // Safe to remove now.
    public static Warning dynamicPrefix = new StrongWarning("dynamicPrefix", "Dynamically prefixed arguments (for 'prefix:value' arguments, like '<[sometag]>:<[somevalue]>') were never officially permitted and are now deprecated. You must specify a prefix explicitly if one is needed.");

    // ==================== Normal deprecations ====================
    // These show up every time, and should get the server owner's attention quickly if they check their logs.

    // Added 2019/10/03, made slow 2021/11/14, made normal 2025/01/15.
    // 2022-year-end commonality: #20
    // Safe to remove now.
    public static Warning inAreaSwitchFormat = new Warning("inAreaSwitchFormat", "The old 'in <area>' in-line event format is deprecated, use the switch format for 'in:<area>'.");

    // Added 2020/05/23, made slow 2022/12/31, made normal 2025/01/15.
    // 2022-year-end commonality: #21
    // 2023-year-end commonality: #15
    // 2024-year-end commonality: #23
    public static Warning timeTagRewrite = new Warning("timeTagRewrite", "Using old Duration-Time - TimeTag is now separate from DurationTag, and some tags have changed as a result.");

    // ==================== SLOW deprecations ====================
    // These aren't spammed, but will show up repeatedly until fixed. Server owners will probably notice them.

    // Added 2022/08/23, bump forward rapidly after commands are fully implemented to the new system to enable spread of autoExecute.
    // 2022-year-end commonality: #24
    // 2023-year-end commonality: #6
    // 2024-year-end commonality: #4
    public static Warning outOfOrderArgs = new SlowWarning("outOfOrderArgs", "Command has out-of-order linear arguments. This can only be interpreted by the legacy parser, and will not be understood by the modern parser. Please make sure your command's linear arguments match the documented order.");

    // Added 2021/04/14, made very-slow 2022/12/31, made slow 2025/01/15.
    // 2022-year-end commonality: #4
    // 2023-year-end commonality: #9
    // 2024-year-end commonality: #1
    public static Warning locallyArgument = new SlowWarning("locallyArgument", "The 'locally' argument in run/inject is deprecated: just specify the script name, or <script>.");

    // Added 2021/12/08, made very-slow 2022/12/31, made slow 2025/01/15.
    // 2022-year-end commonality: #30
    // Safe to remove now.
    public static Warning oldUtilRandomTags = new SlowWarning("oldUtilRandomTags", "Several 'util.random.xxx' are deprecated in favor of 'util.random_xxx' (the same tag, but '_' instead of a '.', for format standardization reasons)");

    // Added 2022/04/11, made very-slow 2024/01/02, made slow 2025/01/15.
    // Safe to remove now.
    public static Warning prebinaryTags = new SlowWarning("prebinaryTags", "Tags and tools related to binary processing that predate the BinaryTag feature are deprecated in favor of using BinaryTag. This includes 'ElementTag.base64_encode/decode', 'hex_encode/decode', ... refer to meta-docs for specifics");

    // Added 2025/07/11
    public static Warning optionalPrefixedArgs = new SlowWarning("optionalPrefixedArgs", "Optional prefixed arguments are deprecated in favor of just using proper prefixes (refer to documentation).");

    // ==================== VERY SLOW deprecations ====================
    // These are only shown minimally, so server owners are aware of them but not bugged by them. Only servers with active scripters (using 'ex reload') will see them often.

    // Added 2022/12/05, relevant years earlier, made very-slow 2024/01/02.
    // 2023-year-end commonality: #12
    // 2024-year-end commonality: #20
    public static Warning intTagVariants = new VerySlowWarning("intTagVariants", "Several of the _int math tag variants (excluding 'div_int') don't actually have any benefit over the non-_int forms and haven't for many years now.");

    // Added 2021/02/04, made very-slow 2025/01/15.
    // 2022-year-end commonality: #9
    // 2023-year-end commonality: #20
    // 2024-year-end commonality: #11
    public static Warning splitNewDataAction = new VerySlowWarning("splitNewDataAction", "The 'split to new list' data action ('key:!|:value') is deprecated: this no longer has a purpose, as you can instead just set to the list.");

    // Added 2022/03/30, deprecate officially by 2024, made very-slow 2025/01/15.
    // 2022-year-end commonality: #32
    // 2023-year-end commonality: #7
    // 2024-year-end commonality: #6
    public static Warning oldNonSecretTagPassword = new VerySlowWarning("oldNonSecretTagPassword", "Passwords and tokens used to be sent through tags or passwordfiles, it is now recommended instead that you use the 'secrets.secret' file with SecretTag. This includes the 'SQL connect' command.");

    // Added 2022/08/21, deprecate officially by 2025, made very-slow 2025/01/15.
    // 2022-year-end commonality: #1
    // 2023-year-end commonality: #1
    // 2024-year-end commonality: #2
    public static Warning asXTags = new VerySlowWarning("asXTags", "Tags of the form 'as_x' where 'x' is a type, such as 'as_list', are deprecated in favor of the tag ObjectTag.as[<type>], used like 'as[list]'.");

    // ==================== FUTURE deprecations ====================

    // ==================== PAST deprecations of things that are already gone but still have a warning left behind ====================

    // Removed in February 2020.
    public static Warning instantTags = new StrongWarning("instantTags", "Instant tags (those with a caret prefix, like <^tag>) were removed in Denizen 1.1.3, and can no longer be used. Instead, pre-define the player or NPC on the line before.");

    // Added on 2019/08/27, removed 2020/10/24.
    public static Warning yamlFixFormatting = new StrongWarning("yamlFixFormatting", "YAML command 'fix_formatting' argument is deprecated: this should never be used.");

    // Added on 2021/04/16.
    public static Warning dscriptFileExtension = new StrongWarning("dscriptFileExtension", "'.dscript' extension has never been officially supported. Please use '.dsc'.");
}
