package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.FutureWarning;
import com.denizenscript.denizencore.utilities.debugging.SlowWarning;
import com.denizenscript.denizencore.utilities.debugging.StrongWarning;
import com.denizenscript.denizencore.utilities.debugging.Warning;

/**
 * This is a special class to contain all deprecation warnings, ordered by date they were added.
 * This should help in keeping track of what's been deprecated (but still is present).
 */
public class Deprecations {

    // In Bukkit impl, Added on 2018/12/23
    // Bad candidate for functionality removal - a bit handy to use in "/ex", despite being clearly bad in standard scripts.
    public static Warning playerByNameWarning = new SlowWarning("Warning: loading player by name - use the UUID instead (or use tag server.match_player)!");

    // ==================== Tag shorthands ====================
    // ====== All added on 2019/02/06 ======
    // Note: context was most often used, and needs to remain longer than the others.
    public static Warning contextShorthand = new StrongWarning("Short-named tags are hard to read. Please use 'context' instead of 'c' as a root tag.");
    public static Warning entryShorthand = new StrongWarning("Short-named tags are hard to read. Please use 'entry' instead of 'e' as a root tag.");
    public static Warning ternShorthand = new StrongWarning("Short-named tags are hard to read. Please use 'tern' instead of 't' as a root tag.");
    // In Bukkit impl:
    public static Warning schematicShorthand = new StrongWarning("Short-named tags are hard to read. Please use 'schematic' instead of 'schem' as a root tag.");
    // ==================== End tag shorthands ====================

    // In Bukkit impl, Added on 2019/02/06
    public static Warning globalTagName = new StrongWarning("Using 'global' as a base tag is a deprecated alternate name. Please use 'server' instead.");

    // In Bukkit impl, Added on 2019/03/08
    // Prime candidate for functionality removal - has been unsupported for a LONG time.
    public static Warning boundWarning = new StrongWarning("Item script 'bound' functionality has never been reliable and should not be used. Consider replicating the concept with world events.");

    // Added on 2019/07/13
    public static Warning oldEscapeTags = new StrongWarning("'escape:' tags are deprecated. Please use '.escaped' element tags instead.");

    // In Bukkit impl, Added on 2019/08/11
    public static Warning oldEconomyTags = new StrongWarning("player.money.currency* tags are deprecated in favor of server.economy.currency* tags.");

    // In Bukkit impl, Added on 2019/08/19
    public static Warning pointlessTextTags = new StrongWarning("Several text tags like '&dot' or '&cm' are pointless (there's no reason you can't just directly write them in). Please replace them with the actual intended text.");

    // Added on 2019/08/27
    // Prime candidate for functionality removal - hasn't been useful for several years.
    public static Warning yamlFixFormatting = new StrongWarning("YAML command 'fix_formatting' argument is deprecated: this should never be used.");

    // Added on 2019/09/13
    // Bad candidate for functionality removal - used to be commonly used
    public static Warning elementAsIntTag = new StrongWarning("'element.as_int' tag is deprecated: use '.round', '.round_down', or '.round_up'.");

    // In Bukkit impl, Added on 2019/09/18, but was deprecated earlier.
    public static Warning worldContext = new StrongWarning("'context.world' in events containing a location or chunk context is deprecated: use 'context.location.world' or similar to get the world value.");
    public static Warning entityBreaksHangingEventContext = new StrongWarning("'context.entity' in event 'on player breaks hanging' is deprecated: use 'context.breaker'.");
    public static Warning hangingBreaksEventContext = new StrongWarning("'context.location' in event 'on hanging breaks' is deprecated: use 'context.hanging.location'.");
    public static Warning playerRightClicksEntityContext = new SlowWarning("'context.location' in event 'on player right clicks entity' is deprecated: use 'context.entity.location'.");
    public static Warning blockDispensesItemDetermination = new StrongWarning("Multiplier double determination for 'on block dispenses item' is deprecated: use 'context.velocity.mul[#]'.");
    public static Warning serverRedirectLogging = new StrongWarning("server mechanism redirect_logging is deprecated: use the system mechanism by the same name.");

    // In Bukkit impl, Added on 2019/09/25, but was deprecated earlier.
    public static Warning qtyTags = new StrongWarning("'qty' in a tag is deprecated: use 'quantity'.");
    public static Warning playerStepTag = new StrongWarning("'player.current_step[script]' tag is deprecated: use 'script.step[player]'.");
    public static Warning playerGamemodeTag = new StrongWarning("player.gamemode.id tag is deprecated: IDs are no longer in use. Use the player.gamemode (named) tag.");

    // In Bukkit impl, Added on 2019/09/25
    // Prime candidate for functionality removal - tags were only recently added, and were always jank.
    public static Warning bookItemRawTags = new StrongWarning("Raw text tags for books were a placeholder. The normal (non-raw) tags now contain all needed data.");

    // Added on 2019/10/13
    public static Warning scriptReloadEventNoUnderscore = new StrongWarning("In the 'on script reload' event, 'had_error' should be used instead of 'haderror'.");

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

    // In Bukkit impl, Added on 2019/07/13
    public static Warning oldParseTag = new SlowWarning("'parse:' tags are deprecated. Please use '.parsed' element tags instead.");

    // Added on 2019/10/09, but was changed earlier.
    public static Warning oldTernTag = new SlowWarning("'tern[a]:b||c' tag style is deprecated. Please use 'tern[a].pass[b].fail[c]' tag style instead.");

    // In Bukkit impl, Added on 2019/09/09
    public static Warning oldNPCNavigator = new SlowWarning("'npc.navigator.*' tags are deprecated. Just remove the '.navigator' part, they're the same after that.");

    // Added on 2019/09/13.
    public static Warning oldMatchesOperator = new SlowWarning("'matches', 'is_empty', and 'contains' operators are deprecated. Use the logically equivalent tags instead.");

    // In Bukkit impl, Added on 2019/09/24
    public static Warning oldRecipeScript = new SlowWarning("Item script single-recipe format is outdated. Use the modern 'recipes' list key (see meta docs).");

    // In Bukkit impl, Added on 2020/01/15
    public static Warning worldRandomLoadedChunkTag = new SlowWarning("The 'world.random_loaded_chunk' tag is pointless. Use 'world.loaded_chunks.random' instead.");

    // In Bukkit impl, Added on 2020/01/15
    public static Warning entityCustomIdTag = new SlowWarning("The tag 'EntityTag.custom_id' is deprecated. Use '.script' instead, though it is technically equivalent to <ENTITY.script||<ENTITY.entity_type>>.");

    // In Bukkit impl, Added on 2020/01/15
    public static Warning playerActionBarMech = new SlowWarning("The mechanism 'PlayerTag.action_bar' is deprecated. Use the 'actionbar' command instead.");

    // Added on 2019/10/08, made current on 2020/02/12
    public static Warning ifCommandSingleLine = new SlowWarning("Single line if commands are deprecated. Please update them to modern format.");
    public static Warning oldBraceSyntax = new SlowWarning("The { braced } command format is deprecated. Please use the ':' colon syntax (refer to documentation).");

    // In Bukkit impl, Relevant as of 2019/09/25, made current on 2020/02/12
    private static String pointlessSubtagPrefix = "Most pointless sub-tags are deprecated in favor of explicit unique tags. ";
    public static Warning npcNicknameTag = new SlowWarning(pointlessSubtagPrefix + "npc.name.nickname is now just npc.nickname. Note that this historically appeared in the config.yml file, so check there if you're unsure what's using this tag.");
    public static Warning npcPreviousLocationTag = new SlowWarning(pointlessSubtagPrefix + "npc.location.previous_location is now just npc.previous_location.");
    public static Warning npcAnchorListTag = new SlowWarning(pointlessSubtagPrefix + "npc.anchor.list is now just npc.list_anchors.");
    public static Warning playerMoneyFormatTag = new SlowWarning(pointlessSubtagPrefix + "player.money.format is now just player.formatted_money.");
    public static Warning playerFoodLevelFormatTag = new SlowWarning(pointlessSubtagPrefix + "player.food_level.format is now just player.formatted_food_level.");
    public static Warning playerBanInfoTags = new SlowWarning(pointlessSubtagPrefix + "player.ban_info.* tags are now just player.ban_*.");
    public static Warning playerNameTags = new SlowWarning(pointlessSubtagPrefix + "player.name.* tags are now just player.*_name.");
    public static Warning playerSidebarTags = new SlowWarning(pointlessSubtagPrefix + "player.sidebar.* tags are now just player.sidebar_*.");
    public static Warning playerAttackCooldownTags = new SlowWarning(pointlessSubtagPrefix + "player.attack_cooldown.* tags are now just player.attack_cooldown_*.");
    public static Warning playerXpTags = new SlowWarning(pointlessSubtagPrefix + "player.xp.* tags are now just player.xp_*.");
    public static Warning entityHealthTags = new SlowWarning(pointlessSubtagPrefix + "entity.health.* tags are now just entity.health_*.");
    public static Warning entityMaxOxygenTag = new SlowWarning(pointlessSubtagPrefix + "entity.oxygen.max is now just entity.max_oxygen.");
    public static Warning itemBookTags = new SlowWarning(pointlessSubtagPrefix + "item.book.* tags are now just item.book_*.");

    // In Bukkit impl, Added on 2020/02/17
    // Prime candidate for functionality removal - command hasn't been used or recommended by anyone in years, and has clear faults that would have prevented usage for most users.
    public static Warning scribeCommand = new SlowWarning("The scribe command was created many years ago, in an earlier era of Denizen, and doesn't make sense to use anymore. Consider the 'equip', 'give', or 'drop' commands instead.");

    // In Bukkit impl, Added 2020/03/01
    // Prime candidate for functionality removal - was never recommended.
    public static Warning notableItems = new SlowWarning("Using the note command with items is deprecated: this has never been recommended and is more likely to introduce bugs than ever do anything useful.");

    // In Bukkit impl, Added 2020/03/08
    // Prime candidate for functionality removal - has not been recommended for many years.
    public static Warning anchorWalk = new SlowWarning("Using the anchor 'walkto'/'walknear' options is deprecated: use the walk command with the npc.anchor tag instead, and 'assume' should be replaced by the teleport command.");

    // Added 2020/03/14
    public static Warning oldStyleRandomCommand = new SlowWarning("Using the 'random' command with an argument number is deprecated: use the modern colon syntax instead (refer to documentation).");

    // In Bukkit impl, Added 2020/04/24 but deprecated long ago.
    public static Warning takeCommandInventory = new SlowWarning("'take inventory' is deprecated: use 'inventory clear' instead.");
    public static Warning oldInventoryCommands = new SlowWarning("The 'inventory' command sub-options 'add' and 'remove' are deprecated: use 'give' or 'take' command instead.");

    // In Bukkit impl, Added 2020/04/24.
    public static Warning itemInventoryTag = new SlowWarning("The tag 'item.inventory' is deprecated: use inventory_contents instead.");

    // In Bukkit impl, Added 2020/05/21.
    public static Warning itemSkinFullTag = new SlowWarning(pointlessSubtagPrefix + "item.skin.full is now item.skull_skin.");

    // In Bukkit impl, Added 2020/06/03 but deprecated long ago.
    public static Warning oldBossBarMech = new SlowWarning("The show_boss_bar mechanism is deprecated: use the bossbar command instead.");
    public static Warning oldTimeMech = new SlowWarning("The player.*time mechanisms are deprecated: use the time command instead.");
    public static Warning oldWeatherMech = new SlowWarning("The player.*weather mechanisms are deprecated: use the weather command instead.");
    public static Warning oldKickMech = new SlowWarning("The player.kick mechanism is deprecated: use the kick command instead.");
    public static Warning oldMoneyMech = new SlowWarning("The player.money mechanism is deprecated: use the money command instead.");

    // Added 2020/06/13.
    public static Warning scriptConstantTag = new SlowWarning("The script.constant system has been deprecated in favor of just using data_key.");

    // In Bukkit impl, added 2020/07/04.
    public static Warning cuboidFullTag = new SlowWarning("The tag cuboid.full is deprecated: this should just never be used.");
    public static Warning furnaceTimeTags = new SlowWarning("The furnace_burn_time, cook time, and cook total time tag/mechs have been replaced by _duration instead of _time equivalents (using DurationTag now).");
    public static Warning playerTimePlayedTags = new SlowWarning("The tags player.first_played, last_played, ban_expiration, and ban_created have been replaced by tags of the same name with '_time' added to the end (using TimeTag now).");

    // In Bukkit impl, added 2020/07/19.
    public static Warning airLevelEventDuration = new SlowWarning("The 'entity changes air level' event uses 'air_duration' context now instead of the old tick count number.");
    public static Warning damageEventTypeMap = new SlowWarning("The 'entity damaged' context 'damage_[TYPE]' is deprecated in favor of 'damage_type_map', which is operated as a MapTag.");

    // In Bukkit impl, added 2020/07/28.
    public static Warning headCommand = new SlowWarning("The 'head' command is deprecated: use the 'equip' command with a 'player_head' item using the 'skull_skin' mechanism.");

    // In Bukkit impl, added 2020/08/01.
    public static Warning entityRemoveWhenFar = new SlowWarning("The EntityTag remove_when_far_away property is deprecated in favor of the persistent property (which is the exact inverse).");
    public static Warning entityPlayDeath = new SlowWarning("The EntityTag 'play_death' mechanism is deprecated: use the animate command.");

    // ==================== FUTURE deprecations ====================

    // In Bukkit impl, Relevant as of 2019/09/25, deprecate officially by 2021.
    public static Warning playerItemInHandSlotTag = new FutureWarning(pointlessSubtagPrefix + "player.item_in_hand_slot is now just player.held_item_slot.");

    // In Bukkit impl, Added 2019/11/11, deprecate officially by 2021.
    public static Warning entityLocationCursorOnTag = new FutureWarning("entity.location.cursor_on tags should be replaced by entity.cursor_on (be careful with the slight differences though).");

    // In Bukkit impl, Added 2019/10/03, deprecate officially by 2021.
    public static Warning inAreaSwitchFormat = new FutureWarning("The old 'in <area>' in-line event format is deprecated, use the switch format for 'in:<area>'.");

    // In Bukkit impl, Added 2020/04/16, deprecate officially by 2021.
    public static Warning inventoryScriptName = new FutureWarning("The tag inventory.script_name is deprecated: use 'inventory.script' (optionally append '.name' for exact equivalence).");

    // In Bukkit impl, Added on 2020/05/17, deprecate officially by 2022.
    public static Warning itemFlagsProperty = new FutureWarning("The item.flags property has been renamed to item.hides.");

    // In Bukkit impl, Added 2020/03/05, deprecate officially by 2022.
    public static Warning oldPlayEffectSpecials = new FutureWarning("The playeffect input of forms like 'iconcrack_' have been deprecated in favor of using the special_data input (refer to meta docs).");

    // Added 2020/03/14, deprecate officially by 2022.
    public static Warning eventCommand = new FutureWarning("The event command is deprecated: represents an outdated understanding of how world script events work that is not compatible with modern script events, and so is due for replacement.");

    // Added 2020/05/23, deprecate officially by 2022.
    public static Warning timeTagRewrite = new FutureWarning("Using old Duration-Time - TimeTag is now separate from DurationTag, and some tags have changed as a result.");

    // Added 2020/05/24, deprecate officially by 2022.
    public static Warning flagIsExpiredTag = new FutureWarning(pointlessSubtagPrefix + "'flag[...].is_expired' is deprecated: use 'has_flag[...]' instead.");
    public static Warning flagExpirationTag = new FutureWarning(pointlessSubtagPrefix + "'flag[...].expiration' is deprecated: use 'flag_expiration[...]' instead.");

    // Added 2020/05/29, deprecate officially by 2022.
    public static Warning listOldMapTags = new FutureWarning("Old list.map_* tags are deprecated: use the modern MapTag options instead.");

    // In Bukkit impl, Added 2020/04/19, Relevant for many years now, deprecate officially by 2023.
    public static Warning interactScriptPriority = new FutureWarning("Assignment script 'interact scripts' section should not have numbered priority values, these were removed years ago. Check https://guide.denizenscript.com/guides/troubleshooting/updates-since-videos.html#assignment-script-updates for more info.");

    // Added 2020/06/13, deprecate officially by 2022.
    public static Warning yamlDataContainer = new FutureWarning("'yaml data' containers are now just called 'data' containers.");

    // In Bukkit impl, Added 2020/06/13, deprecate officially by 2022.
    public static Warning listStyleTags = new FutureWarning("'list_' tags are deprecated: just remove the 'list_' prefix.");

    // In Bukkit impl, Added 2020/07/03, deprecate officially by 2022.
    public static Warning attachToMech = new FutureWarning("The entity 'attach_to' mechanism is deprecated: use the new 'attach' command instead!");

    // In Bukkit impl, Added 2020/07/12, deprecate officially by 2022.
    public static Warning entityEquipmentSubtags = new FutureWarning(pointlessSubtagPrefix + " 'entity.equipment.slotname' is deprecated: use 'entity.equipment_map.get[slotname]' instead.");

    // Added 2020/07/23, deprecate officially by 2022.
    public static Warning defExistsTag = new FutureWarning("The def[].exists tag is deprecated: use a fallback and null check, or just set definitions more consistently.");

    // ==================== PAST deprecations of things that are already gone but still have a warning left behind ====================

    // In Bukkit impl, Added on 2019/10/13
    public static Warning versionScripts = new StrongWarning("Version script containers are deprecated due to the old script repo no longer being active.");

    // Removed in February 2020.
    public static Warning ancientDefs = new StrongWarning("Ancient-style definitions (those with percent signs like %def%) were removed in Denizen 1.1.3, and can no longer be used. Instead, use <[def]>.");
    public static Warning instantTags = new StrongWarning("Instant tags (those with a caret prefix, like <^tag>) were removed in Denizen 1.1.3, and can no longer be used. Instead, pre-define the player or NPC on the line before.");
}
