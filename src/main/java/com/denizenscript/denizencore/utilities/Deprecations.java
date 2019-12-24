package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.FutureWarning;
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
    public static Warning defShorthand = new Warning("Short-named tags are hard to read. Please use 'def' instead of 'd' as a root tag.");
    public static Warning procShorthand = new Warning("Short-named tags are hard to read. Please use 'proc' instead of 'pr' as a root tag.");
    public static Warning queueShorthand = new Warning("Short-named tags are hard to read. Please use 'queue' instead of 'q' as a root tag.");
    public static Warning scriptShorthand = new Warning("Short-named tags are hard to read. Please use 'script' instead of 's' as a root tag.");
    public static Warning ternShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'tern' instead of 't' as a root tag.");
    public static Warning utilShorthand = new Warning("Short-named tags are hard to read. Please use 'util' instead of 'u' as a root tag.");
    // In Bukkit impl:
    public static Warning locationShorthand = new Warning("Short-named tags are hard to read. Please use 'location' instead of 'l' as a root tag.");
    public static Warning playerShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'player' instead of 'pl' as a root tag.");
    public static Warning serverShorthand = new Warning("Short-named tags are hard to read. Please use 'server' instead of 'svr' as a root tag.");
    public static Warning schematicShorthand = new Warning("Short-named tags are hard to read. Please use 'schematic' instead of 'schem' as a root tag.");
    // ==================== End tag shorthands ====================

    // In Bukkit impl, Added on 2019/02/06
    public static Warning globalTagName = new Warning("Using 'global' as a base tag is a deprecated alternate name. Please use 'server' instead.");

    // In Bukkit impl, Added on 2019/03/08
    // Prime candidate for functionality removal - has been unsupported for a LONG time.
    public static Warning boundWarning = new Warning("Item script 'bound' functionality has never been reliable and should not be used. Consider replicating the concept with world events.");

    // Added on 2019/07/13
    public static Warning oldEscapeTags = new SlowWarning("'escape:' tags are deprecated. Please use '.escaped' element tags instead.");

    // In Bukkit impl, Added on 2019/07/25
    public static Warning oldStylePlayerBreaksItemEvent = new SlowWarning("Event 'player breaks <item>' is old. Use 'player breaks held <item>' instead (this is to prevent conflict with breaks block)");

    // In Bukkit impl, Added on 2019/08/11
    public static Warning oldEconomyTags = new SlowWarning("player.money.currency* tags are deprecated in favor of server.economy.currency* tags.");

    // In Bukkit impl, Added on 2019/08/19
    public static Warning pointlessTextTags = new SlowWarning("Several text tags like '&dot' or '&cm' are pointless (there's no reason you can't just directly write them in). Please replace them with the actual intended text.");

    // Added on 2019/08/27
    // Prime candidate for functionality removal - hasn't been useful for several years.
    public static Warning yamlFixFormatting = new SlowWarning("YAML command 'fix_formatting' argument is deprecated: this should never be used.");

    // Added on 2019/09/13
    // Bad candidate for functionality removal - used to be commonly used
    public static Warning elementAsInTag = new SlowWarning("'element.as_int' tag is deprecated: use '.round', '.round_down', '.round_up', or '.truncate'.");

    // In Bukkit impl, Added on 2019/09/18, but was deprecated earlier.
    public static Warning worldContext = new SlowWarning("'context.world' in events containing a location or chunk context is deprecated: use 'context.location.world' or similar to get the world value.");
    public static Warning entityBreaksHangingEventContext = new SlowWarning("'context.entity' in event 'on player breaks hanging' is deprecated: use 'context.breaker'.");
    public static Warning hangingBreaksEventContext = new SlowWarning("'context.location' in event 'on hanging breaks' is deprecated: use 'context.hanging.location'.");
    public static Warning playerRightClicksEntityContext = new SlowWarning("'context.location' in event 'on player right clicks entity' is deprecated: use 'context.entity.location'.");
    public static Warning blockDispensesItemDetermination = new SlowWarning("Multiplier double determination for 'on block dispenses item' is deprecated: use 'context.velocity.mul[#]'.");
    public static Warning serverRedirectLogging = new SlowWarning("server mechanism redirect_logging is deprecated: use the system mechanism by the same name.");

    // In Bukkit impl, Added on 2019/09/25, but was deprecated earlier.
    public static Warning qtyTags = new SlowWarning("'qty' in a tag is deprecated: use 'quantity'.");
    public static Warning playerStepTag = new SlowWarning("'player.current_step[script]' tag is deprecated: use 'script.step[player]'.");
    public static Warning playerGamemodeTag = new SlowWarning("player.gamemode.id tag is deprecated: IDs are no longer in use. Use the player.gamemode (named) tag.");

    // In Bukkit impl, Added on 2019/09/25.
    // Prime candidate for functionality removal - tags were only recently added, and were always jank.
    public static Warning bookItemRawTags = new SlowWarning("Raw text tags for books were a placeholder. The normal (non-raw) tags now contain all needed data.");

    // In Bukkit impl, Added on 2019/10/13
    // This is just a message, relevant functionality already removed. Remove the script container registration after a few releases.
    public static Warning versionScripts = new SlowWarning("Version script containers are deprecated due to the old script repo no longer being active.");

    // Added on 2019/10/13
    public static Warning scriptReloadEventNoUnderscore = new SlowWarning("In the 'on script reload' event, 'had_error' should be used instead of 'haderror'.");

    // In Bukkit impl, Added on 2019/11/22
    public static Warning serverPluginNamesTag = new SlowWarning("'server.list_plugin_names' is deprecated: use 'server.list_plugins'");

    // In Bukkit impl, Added on 2019/11/25
    public static Warning locationBiomeFormattedTag = new SlowWarning("'location.biome.formatted' is deprecated: use 'location.biome.name' (uses BiomeTag.name)");

    // In Bukkit impl, Added on 2019/11/26
    public static Warning nbtCommand = new SlowWarning("The NBT command is deprecated: adjust the 'nbt' mechanism instead.");

    // In Bukkit impl, Added on 2019/11/30
    public static Warning serverListMaterialNames = new SlowWarning("The tag 'server.list_materials' is deprecated: use '<server.list_material_types.parse[name]>' to get a matching result.");
    public static Warning serverListBiomeNames = new SlowWarning("The tag 'server.list_biomes' is deprecated: use '<server.list_biome_types.parse[name]>' to get a matching result.");

    // In Bukkit impl, Added on 2019/12/24
    public static Warning entityRemainingAir = new SlowWarning("The mechanism 'EntityTag.remaining_air' is deprecated: use 'EntityTag.oxygen' instead (duration input vs. tick input).");

    // ==================== SPECIAL deprecations: Minecraft version ====================

    // In Bukit impl, To be removed when Minecraft 1.12.2 is no longer supported by the Bukkit impl:
    public static Warning materialIds = new Warning("Material ID and data magic number support is deprecated and WILL be removed in a future release.");
    public static Warning materialIdsSuggestProperties = new Warning("Material ID and data magic number support is deprecated and WILL be removed in a future release. Use relevant properties instead.");
    public static Warning materialIdsSuggestNames = new Warning("Material ID and data magic number support is deprecated and WILL be removed in a future release. Use material names instead.");
    public static Warning skullSkinMaterials = new Warning("As of Minecraft version 1.13 you may only set the skin of a PLAYER_HEAD or PLAYER_WALL_HEAD.");
    public static Warning flowerpotMechanism = new Warning("As of Minecraft version 1.13 potted flowers each have their own material, such as POTTED_CACTUS.");

    // ==================== FUTURE deprecations ====================

    // In Bukkit impl, Relevant as of 2019/07/13, deprecate officially by 2020.
    public static Warning oldParseTag = new FutureWarning("'parse:' tags are deprecated. Please use '.parsed' element tags instead.");

    // Added on 2019/10/09, but was changed earlier, deprecate officially by 2020.
    public static Warning oldTernTag = new FutureWarning("'tern[a]:b||c' tag style is deprecated. Please use 'tern[a].pass[b].fail[c]' tag style instead.");

    // In Bukkit impl, Relevant as of 2019/09/09, deprecate officially by 2020.
    public static Warning oldNPCNavigator = new FutureWarning("'npc.navigator.*' tags are deprecated. Just remove the '.navigator' part, they're the same after that.");

    // Relevant as of 2019/09/13, deprecate officially by 2020.
    public static Warning oldMatchesOperator = new FutureWarning("'matches', 'is_empty', and 'contains' operators are deprecated. Use the logically equivalent tags instead.");

    // In Bukkit impl, Relevant as of 2019/09/24, deprecate officially by 2020.
    public static Warning oldRecipeScript = new FutureWarning("Item script single-recipe format is outdated. Use the modern 'recipes' list key (see meta docs).");

    // Added 2019/11/11, deprecate officially by 2020.
    public static Warning oldTagTickSyntax = new FutureWarning("The '^' prefix syntax for 'instant' tags is outdated. Please instead use the 'define' command to track the original player/NPC.");

    // In Bukkit impl, Relevant as of 2019/09/25, deprecate officially by 2021.
    private static String pointlessSubtagPrefix = "Most pointless sub-tags are deprecated in favor of explicit unique tags. ";
    public static Warning npcNicknameTag = new FutureWarning(pointlessSubtagPrefix + "npc.name.nickname is now just npc.nickname.");
    public static Warning npcPreviousLocationTag = new FutureWarning(pointlessSubtagPrefix + "npc.location.previous_location is now just npc.previous_location.");
    public static Warning npcAnchorListTag = new FutureWarning(pointlessSubtagPrefix + "npc.anchor.list is now just npc.list_anchors.");
    public static Warning playerMoneyFormatTag = new FutureWarning(pointlessSubtagPrefix + "player.money.format is now just player.formatted_money.");
    public static Warning playerFoodLevelFormatTag = new FutureWarning(pointlessSubtagPrefix + "player.food_level.format is now just player.formatted_food_level.");
    public static Warning playerItemInHandSlotTag = new FutureWarning(pointlessSubtagPrefix + "player.item_in_hand_slot is now just player.held_item_slot.");
    public static Warning playerBanInfoTags = new FutureWarning(pointlessSubtagPrefix + "player.ban_info.* tags are now just player.ban_*.");
    public static Warning playerNameTags = new FutureWarning(pointlessSubtagPrefix + "player.name.* tags are now just player.*_name.");
    public static Warning playerSidebarTags = new FutureWarning(pointlessSubtagPrefix + "player.sidebar.* tags are now just player.sidebar_*.");
    public static Warning playerAttackCooldownTags = new FutureWarning(pointlessSubtagPrefix + "player.attack_cooldown.* tags are now just player.attack_cooldown_*.");
    public static Warning playerXpTags = new FutureWarning(pointlessSubtagPrefix + "player.xp.* tags are now just player.xp_*.");
    public static Warning entityHealthTags = new FutureWarning(pointlessSubtagPrefix + "entity.health.* tags are now just entity.health_*.");
    public static Warning entityMaxOxygenTag = new FutureWarning(pointlessSubtagPrefix + "entity.oxygen.max is now just entity.max_oxygen.");
    public static Warning itemBookTags = new FutureWarning(pointlessSubtagPrefix + "item.book.* tags are now just item.book_*.");

    // In Bukkit impl, Added 2019/11/11  deprecate officially by 2021.
    public static Warning entityLocationCursorOnTag = new FutureWarning("entity.location.cursor_on tags should be replaced by entity.cursor_on (be careful with the slight differences though).");

    // In Bukkit impl, Added 2019/10/03, deprecate officially by 2021.
    public static Warning inAreaSwitchFormat = new FutureWarning("The old 'in <area>' in-line event format is deprecated, use the switch format for 'in:<area>'.");

    // Added 2019/10/08, deprecate officially by 2021.
    public static Warning ifCommandSingleLine = new FutureWarning("Single line if commands are deprecated. Please update them to modern format.");

    // Added 2019/10/08, deprecate officially by 2022.
    public static Warning oldBraceSyntax = new FutureWarning("The { braced } command format is deprecated. Please use the ':' colon syntax (refer to documentation).");
}
