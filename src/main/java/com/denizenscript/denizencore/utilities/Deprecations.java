package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.SlowWarning;
import com.denizenscript.denizencore.utilities.debugging.Warning;

/**
 * This is a special class to contain all deprecation warnings, ordered by date they were added.
 * This should help in keeping track of what's been deprecated (but still is present).
 */
public class Deprecations {

    // Prime candidate for removal - been strongly deprecated for a very long time (exact date is unclear, but many years).
    public static Warning ancientDefs = new Warning("Ancient style definitions ('%def%') are deprecated. Please use modern definition syntax: '<[def]>'.");

    // In Bukkit impl, Added on 2018/12/23
    // Bad candidate for functionality removal - a bit handy to use in "/ex", despite being clearly bad in standard scripts.
    public static Warning playerByNameWarning = new SlowWarning("Warning: loading player by name - use the UUID instead (or use tag server.match_player)!");

    // ==================== Tag shorthands ====================
    // ====== All added on 2019/02/06 ======
    // Note: context was most often used, and needs to remain longer than the others.
    public static Warning contextShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'context' instead of 'c' as a root tag.");
    public static Warning entryShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'entry' instead of 'e' as a root tag.");
    public static Warning defShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'def' instead of 'd' as a root tag.");
    public static Warning procShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'proc' instead of 'pr' as a root tag.");
    public static Warning queueShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'queue' instead of 'q' as a root tag.");
    public static Warning scriptShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'script' instead of 's' as a root tag.");
    public static Warning ternShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'tern' instead of 't' as a root tag.");
    public static Warning utilShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'util' instead of 'u' as a root tag.");
    // In Bukkit impl:
    public static Warning locationShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'location' instead of 'l' as a root tag.");
    public static Warning playerShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'player' instead of 'pl' as a root tag.");
    public static Warning serverShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'server' instead of 'svr' as a root tag.");
    // ==================== End tag shorthands ====================

    // In Bukkit impl, Added on 2019/02/06
    public static Warning globalTagName = new Warning("Using 'global' as a base tag is a deprecated alternate name. Please use 'server' instead.");

    // In Bukkit impl, Added on 2019/03/08
    // Prime candidate for functionality removal - has been unsupported for a LONG time.
    public static Warning boundWarning = new SlowWarning("Item script 'bound' functionality has never been reliable and should not be used. Consider replicating the concept with world events.");

    // In Bukkit impl, Added on 2019/07/07
    public static Warning mathTagBase = new Warning("'math:' tags have been non-recommended for years. Please use modern element math tags like 'element.add[...]', etc.");

    // Added on 2019/07/13
    public static Warning oldEscapeTags = new SlowWarning("'escape:' tags are deprecated. Please use '.escaped' element tags instead.");

    // In Bukkit impl, Added on 2019/07/25
    public static Warning oldStylePlayerBreaksItemEvent = new SlowWarning("Event 'player breaks <item>' is old. Use 'player breaks held <item>' instead (this is to prevent conflict with breaks block)");

    // In Bukkit impl, Added on 2019/08/11
    public static Warning oldEconomyTags = new SlowWarning("player.money.currency* tags are deprecated in favor of server.economy.currency* tags.");

    // In Bukkit impl, Added on 2019/08/19
    public static Warning pointlessTextTags = new SlowWarning("Several text tags like '&dot' or '&cm' are pointless (there's no reason you can't just directly write them in). Please replace them with the actual intended text.");

    // ==================== SPECIAL deprecations: Minecraft version ====================

    // To be removed when Minecraft 1.12.2 is no longer supported by the Bukkit impl:
    public static Warning materialIds = new Warning("Material ID and data magic number support is deprecated and WILL be removed in a future release.");
    public static Warning materialIdsSuggestProperties = new Warning("Material ID and data magic number support is deprecated and WILL be removed in a future release. Use relevant properties instead.");
    public static Warning materialIdsSuggestNames = new Warning("Material ID and data magic number support is deprecated and WILL be removed in a future release. Use material names instead.");
    public static Warning skullSkinMaterials = new Warning("As of Minecraft version 1.13 you may only set the skin of a PLAYER_HEAD or PLAYER_WALL_HEAD.");
    public static Warning flowerpotMechanism = new Warning("As of Minecraft version 1.13 potted flowers each have their own material, such as POTTED_CACTUS.");

    // ==================== FUTURE deprecations ====================

    // In Bukkit impl, Relevant as of 2019/07/13, deprecate officially by 2020.
    public static Warning oldParseTag = new SlowWarning("'parse:' tags are deprecated. Please use '.parsed' element tags instead.");
}
