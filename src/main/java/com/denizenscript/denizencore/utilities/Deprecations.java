package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.utilities.debugging.*;

/**
 * This is a special class to contain all deprecation warnings, ordered by date they were added.
 * This should help in keeping track of what's been deprecated (but still is present).
 */
public class Deprecations {

    // In Bukkit impl, Added on 2018/12/23
    // Bad candidate for functionality removal - a bit handy to use in "/ex", despite being clearly bad in standard scripts.
    public static Warning playerByNameWarning = new Warning("Warning: loading player by name - use the UUID instead (or use tag server.match_player)!");

    // ==================== Tag shorthands ====================
    // ====== All added on 2019/02/06 ======
    // Note: context was most often used, and needs to remain longer than the others.
    public static Warning contextShorthand = new StrongWarning("Short-named tags are hard to read. Please use 'context' instead of 'c' as a root tag.");
    public static Warning entryShorthand = new StrongWarning("Short-named tags are hard to read. Please use 'entry' instead of 'e' as a root tag.");
    // ==================== End tag shorthands ====================

    // In Bukkit impl, Added on 2019/02/06
    public static Warning globalTagName = new StrongWarning("Using 'global' as a base tag is a deprecated alternate name. Please use 'server' instead.");

    // Added on 2019/07/13
    public static Warning oldEscapeTags = new StrongWarning("'escape:' tags are deprecated. Please use '.escaped' element tags instead.");

    // In Bukkit impl, Added on 2019/08/11
    public static Warning oldEconomyTags = new StrongWarning("player.money.currency* tags are deprecated in favor of server.economy.currency* tags.");

    // In Bukkit impl, Added on 2019/08/19
    public static Warning pointlessTextTags = new StrongWarning("Several text tags like '&dot' or '&cm' are pointless (there's no reason you can't just directly write them in). Please replace them with the actual intended text.");

    // Added on 2019/09/13
    // Bad candidate for functionality removal - used to be commonly used
    public static Warning elementAsIntTag = new StrongWarning("'element.as_int' tag is deprecated: use '.round', '.round_down', or '.round_up'.");

    // In Bukkit impl, Added on 2019/09/18, but was deprecated earlier.
    public static Warning worldContext = new StrongWarning("'context.world' in events containing a location or chunk context is deprecated: use 'context.location.world' or similar to get the world value.");
    public static Warning entityBreaksHangingEventContext = new StrongWarning("'context.entity' in event 'on player breaks hanging' is deprecated: use 'context.breaker'.");
    public static Warning hangingBreaksEventContext = new StrongWarning("'context.location' in event 'on hanging breaks' is deprecated: use 'context.hanging.location'.");
    public static Warning playerRightClicksEntityContext = new StrongWarning("'context.location' in event 'on player right clicks entity' is deprecated: use 'context.entity.location'.");
    public static Warning blockDispensesItemDetermination = new StrongWarning("Multiplier double determination for 'on block dispenses item' is deprecated: use 'context.velocity.mul[#]'.");
    public static Warning serverRedirectLogging = new StrongWarning("server mechanism redirect_logging is deprecated: use the system mechanism by the same name.");

    // In Bukkit impl, Added on 2019/09/25, but was deprecated earlier.
    public static Warning qtyTags = new StrongWarning("'qty' in a tag or command is deprecated: use 'quantity'.");

    // In Bukkit impl, Added on 2019/09/25
    // Prime candidate for functionality removal - tags were only recently added, and were always jank.
    public static Warning bookItemRawTags = new StrongWarning("Raw text tags for books were a placeholder. The normal (non-raw) tags now contain all needed data.");

    // In Bukkit impl, Added on 2019/11/22
    public static Warning serverPluginNamesTag = new StrongWarning("'server.list_plugin_names' is deprecated: use 'server.list_plugins'");

    // In Bukkit impl, Added on 2019/11/25
    public static Warning locationBiomeFormattedTag = new StrongWarning("'location.biome.formatted' is deprecated: use 'location.biome.name' (uses BiomeTag.name)");

    // In Bukkit impl, Added on 2019/11/26
    public static Warning nbtCommand = new StrongWarning("The NBT command is deprecated: use item flags instead.");

    // In Bukkit impl, Added on 2019/11/30
    public static Warning serverListMaterialNames = new StrongWarning("The tag 'server.list_materials' is deprecated: use '<server.list_material_types.parse[name]>' to get a matching result.");
    public static Warning serverListBiomeNames = new StrongWarning("The tag 'server.list_biomes' is deprecated: use '<server.list_biome_types.parse[name]>' to get a matching result.");

    // In Bukkit impl, Added on 2019/12/24
    public static Warning entityRemainingAir = new StrongWarning("The mechanism 'EntityTag.remaining_air' is deprecated: use 'EntityTag.oxygen' instead (duration input vs. tick input).");

    // In Bukkit impl, Added on 2019/07/13
    public static Warning oldParseTag = new StrongWarning("'parse:' tags are deprecated. Please use '.parsed' element tags instead.");

    // Added on 2019/10/09, but was changed earlier.
    public static Warning oldTernTag = new StrongWarning("'tern[a]:b||c' tag style is deprecated. Please use 'tern[a].pass[b].fail[c]' tag style instead.");

    // In Bukkit impl, Added on 2019/09/09
    public static Warning oldNPCNavigator = new StrongWarning("'npc.navigator.*' tags are deprecated. Just remove the '.navigator' part, they're the same after that.");

    // In Bukkit impl, Added on 2019/09/24
    public static Warning oldRecipeScript = new Warning("Item script single-recipe format is outdated. Use the modern 'recipes' list key (see meta docs).");

    // In Bukkit impl, Added on 2020/01/15
    public static Warning worldRandomLoadedChunkTag = new StrongWarning("The 'world.random_loaded_chunk' tag is pointless. Use 'world.loaded_chunks.random' instead.");

    // In Bukkit impl, Added on 2020/01/15
    public static Warning entityCustomIdTag = new StrongWarning("The tag 'EntityTag.custom_id' is deprecated. Use '.script' instead, though it is technically equivalent to <ENTITY.script||<ENTITY.entity_type>>.");

    // In Bukkit impl, Added on 2020/01/15
    public static Warning playerActionBarMech = new StrongWarning("The mechanism 'PlayerTag.action_bar' is deprecated. Use the 'actionbar' command instead.");

    // Added on 2019/10/08, made current on 2020/02/12
    public static Warning ifCommandSingleLine = new StrongWarning("Single line if commands are deprecated. Please update them to modern format.");
    public static Warning oldBraceSyntax = new StrongWarning("The { braced } command format is deprecated. Please use the ':' colon syntax (refer to documentation).");

    // In Bukkit impl, Relevant as of 2019/09/25, made current on 2020/02/12
    private static String pointlessSubtagPrefix = "Most pointless sub-tags are deprecated in favor of explicit unique tags. ";
    public static Warning npcNicknameTag = new Warning(pointlessSubtagPrefix + "npc.name.nickname is now just npc.nickname. Note that this historically appeared in the config.yml file, so check there if you're unsure what's using this tag.");
    public static Warning npcPreviousLocationTag = new Warning(pointlessSubtagPrefix + "npc.location.previous_location is now just npc.previous_location.");
    public static Warning npcAnchorListTag = new Warning(pointlessSubtagPrefix + "npc.anchor.list is now just npc.list_anchors.");
    public static Warning playerMoneyFormatTag = new Warning(pointlessSubtagPrefix + "player.money.format is now just player.formatted_money.");
    public static Warning playerFoodLevelFormatTag = new Warning(pointlessSubtagPrefix + "player.food_level.format is now just player.formatted_food_level.");
    public static Warning playerBanInfoTags = new Warning(pointlessSubtagPrefix + "player.ban_info.* tags are now just player.ban_*.");
    public static Warning playerNameTags = new Warning(pointlessSubtagPrefix + "player.name.* tags are now just player.*_name.");
    public static Warning playerSidebarTags = new Warning(pointlessSubtagPrefix + "player.sidebar.* tags are now just player.sidebar_*.");
    public static Warning playerAttackCooldownTags = new Warning(pointlessSubtagPrefix + "player.attack_cooldown.* tags are now just player.attack_cooldown_*.");
    public static Warning playerXpTags = new Warning(pointlessSubtagPrefix + "player.xp.* tags are now just player.xp_*.");
    public static Warning entityHealthTags = new Warning(pointlessSubtagPrefix + "entity.health.* tags are now just entity.health_*.");
    public static Warning entityMaxOxygenTag = new Warning(pointlessSubtagPrefix + "entity.oxygen.max is now just entity.max_oxygen.");
    public static Warning itemBookTags = new Warning(pointlessSubtagPrefix + "item.book.* tags are now just item.book_*.");
    public static Warning playerItemInHandSlotTag = new Warning(pointlessSubtagPrefix + "player.item_in_hand_slot is now just player.held_item_slot.");

    // In Bukkit impl, Added on 2020/02/17
    // Prime candidate for functionality removal - command hasn't been used or recommended by anyone in years, and has clear faults that would have prevented usage for most users.
    public static Warning scribeCommand = new StrongWarning("The scribe command was created many years ago, in an earlier era of Denizen, and doesn't make sense to use anymore. Consider the 'equip', 'give', or 'drop' commands instead.");

    // Added 2020/03/14
    public static Warning oldStyleRandomCommand = new StrongWarning("Using the 'random' command with an argument number is deprecated: use the modern colon syntax instead (refer to documentation).");

    // In Bukkit impl, Added 2020/04/24 but deprecated long ago.
    public static Warning takeCommandInventory = new StrongWarning("'take inventory' is deprecated: use 'inventory clear' instead.");
    public static Warning oldInventoryCommands = new StrongWarning("The 'inventory' command sub-options 'add' and 'remove' are deprecated: use 'give' or 'take' command instead.");

    // In Bukkit impl, Added 2020/04/24.
    public static Warning itemInventoryTag = new Warning("The tag 'item.inventory' is deprecated: use inventory_contents instead.");

    // In Bukkit impl, Added 2020/05/21.
    public static Warning itemSkinFullTag = new Warning(pointlessSubtagPrefix + "item.skin.full is now item.skull_skin.");

    // In Bukkit impl, Added 2020/06/03 but deprecated long ago.
    public static Warning oldBossBarMech = new Warning("The show_boss_bar mechanism is deprecated: use the bossbar command instead.");
    public static Warning oldTimeMech = new Warning("The player.*time mechanisms are deprecated: use the time command instead.");
    public static Warning oldWeatherMech = new Warning("The player.*weather mechanisms are deprecated: use the weather command instead.");
    public static Warning oldKickMech = new Warning("The player.kick mechanism is deprecated: use the kick command instead.");
    public static Warning oldMoneyMech = new Warning("The player.money mechanism is deprecated: use the money command instead.");

    // Added 2020/06/13.
    public static Warning scriptConstantTag = new Warning("The script.constant system has been deprecated in favor of just using data_key.");

    // Added 2020/03/14, moved from future to current 2021/11/12.
    // Should be rapidly advanced through deprecation levels: removing this allows for a significant amount of legacy code removal.
    public static Warning eventCommand = new Warning("The event command is deprecated: use 'customevent' instead. The 'event' command represents an outdated idea of how events should function inside.");

    // In Bukkit impl, added 2020/07/04.
    public static Warning cuboidFullTag = new Warning("The tag cuboid.full is deprecated: this should just never be used.");
    public static Warning furnaceTimeTags = new Warning("The furnace_burn_time, cook time, and cook total time tag/mechs have been replaced by _duration instead of _time equivalents (using DurationTag now).");
    public static Warning playerTimePlayedTags = new Warning("The tags player.first_played, last_played, ban_expiration, and ban_created have been replaced by tags of the same name with '_time' added to the end (using TimeTag now).");

    // In Bukkit impl, added 2020/07/19.
    public static Warning airLevelEventDuration = new Warning("The 'entity changes air level' event uses 'air_duration' context now instead of the old tick count number.");
    public static Warning damageEventTypeMap = new Warning("The 'entity damaged' context 'damage_[TYPE]' is deprecated in favor of 'damage_type_map', which is operated as a MapTag.");

    // In Bukkit impl, added 2020/07/28.
    public static Warning headCommand = new Warning("The 'head' command is deprecated: use the 'equip' command with a 'player_head' item using the 'skull_skin' mechanism.");

    // In Bukkit impl, added 2020/08/01.
    public static Warning entityRemoveWhenFar = new Warning("The EntityTag remove_when_far_away property is deprecated in favor of the persistent property (which is the exact inverse).");
    public static Warning entityPlayDeath = new Warning("The EntityTag 'play_death' mechanism is deprecated: use the animate command.");

    // In Bukkit impl, added 2020/08/19.
    public static Warning npcSpawnMechanism = new Warning("The NPCTag 'spawn' mechanism is deprecated: use the spawn command.");

    // In Bukkit impl, Added 2020/05/17, made current on 2020/10/24.
    public static Warning itemFlagsProperty = new StrongWarning("The item.flags property has been renamed to item.hides, to avoid confusion with the new flaggable itemtags system.");

    // In Bukkit impl, Added 2020/11/22.
    public static Warning biomeSpawnableTag = new Warning(pointlessSubtagPrefix + "The tag BiomeTag.spawnable_entities.(type) is deprecated: the type is now an input context instead.");

    // In Bukkit impl, Added 2020/11/30.
    public static Warning npcDespawnMech = new Warning("The NPCTag despawn mechanism is deprecated: use the despawn command.");

    // Added 2021/02/17.
    public static Warning listEscapeContents = new SlowWarning("The tags ListTag.escape_contents and unescape_contents are deprecated: use parse[escaped] and parse[unescaped], or just don't escape in the first place as most list escapes are no longer needed.");

    // Added 2021/02/25.
    public static Warning zapPrefix = new SlowWarning("The 'zap' command should be used with the scriptname and step as two separate arguments, not just one.");

    // Added 2021/04/16.
    public static Warning ymlFileExtension = new Warning("Denizen scripts use the '.dsc' file extension, not '.yml'. Please follow the Denizen beginner's guide https://guide.denizenscript.com/");

    // In Bukkit impl, Added 2020/03/05, made current on 2021/04/16.
    public static Warning oldPlayEffectSpecials = new SlowWarning("The playeffect input of forms like 'iconcrack_' have been deprecated in favor of using the special_data input (refer to meta docs).");

    // In Bukkit impl, Added 2020/04/16.
    public static Warning entityStandingOn = new SlowWarning(pointlessSubtagPrefix + "entity.location.standing_on is now just entity.standing_on.");

    // In Bukkit impl, Added 2021/05/02.
    public static Warning hurtSourceOne = new SlowWarning("The 'hurt' command's 'source_once' argument is deprecated due to being now irrelevant thanks to the new NMS backing for the hurt command.");

    // In Bukkit impl, Added 2021/05/05.
    public static Warning materialLit = new SlowWarning("The MaterialTag property 'lit' is deprecated in favor of 'switched'.");
    public static Warning materialCampfire = new SlowWarning("The MaterialTag property 'campfire' are deprecated in favor of 'type'.");
    public static Warning materialDrags = new SlowWarning("The MaterialTag property 'drags' are deprecated in favor of 'mode'.");

    // In Bukkit impl, Added 2021/06/15, but was irrelevant years earlier.
    public static Warning itemMessage = new SlowWarning("The PlayerTag mechanism 'item_message' is deprecated in favor of using the actionbar.");

    // In Bukkit impl, Added 2021/09/08, but was irrelevant years earlier.
    public static Warning isValidTag = new SlowWarning("The 'server.x_is_valid' style tags are deprecated: use '.exists', '.is_spawned.if_null[false]', etc.");

    // In Bukkit impl, Added 2021/11/14.
    public static Warning blockSpreads = new SlowWarning("There are two '<block> spreads' events - use 'block spreads type:<block>' or 'liquid spreads type:<block>'");

    // In Bukkit impl, Added 2019/10/03, bumped to slow 2021/11/14.
    public static Warning inAreaSwitchFormat = new SlowWarning("The old 'in <area>' in-line event format is deprecated, use the switch format for 'in:<area>'.");

    // In Bukkit impl, Added 2021/11/15.
    public static Warning horseJumpsFormat = new SlowWarning("The '<color> horse jumps' event is deprecated: don't put the color in the event line. (Deprecated for technical design reasons).");

    // In Bukkit impl, Added 2019/11/11.
    public static Warning entityLocationCursorOnTag = new SlowWarning("entity.location.cursor_on tags should be replaced by entity.cursor_on (be careful with the slight differences though).");

    // ==================== VERY SLOW deprecations ====================
    // These are only shown minimally, so server owners are aware of them but not bugged by them. Only servers with active scripters (using 'ex reload') will see them often.

    // Added 2020/05/23, bump to normal slow warning by 2023.
    public static Warning timeTagRewrite = new VerySlowWarning("Using old Duration-Time - TimeTag is now separate from DurationTag, and some tags have changed as a result.");

    // Added 2020/05/29, bump to normal slow warning by 2023.
    public static Warning listOldMapTags = new VerySlowWarning("Old list.map_* tags are deprecated: use the modern MapTag options instead.");

    // In Bukkit impl, Added 2020/04/19, Relevant for many years now, bump to normal slow warning by 2023.
    public static Warning interactScriptPriority = new VerySlowWarning("Assignment script 'interact scripts' section should not have numbered priority values, these were removed years ago. Check https://guide.denizenscript.com/guides/troubleshooting/updates-since-videos.html#assignment-script-updates for more info.");

    // Added 2020/06/13, bump to normal slow warning by 2023.
    public static Warning yamlDataContainer = new VerySlowWarning("'yaml data' containers are now just called 'data' containers.");

    // Added 2021/10/17, bump to normal slow warning by 2023.
    public static Warning queueExists = new VerySlowWarning("'queue.exists[...]' tag is deprecated in favor of queue[...].exists");
    public static Warning queueStats = new VerySlowWarning("'queue.stats' tag is deprecated in favor of 'util.event_stats', and 'queue.list' is deprecated in favor of 'util.queues'");

    // In Bukkit impl, Added 2021/10/24, bump to normal slow warning by 2023.
    public static Warning entityArmorPose = new VerySlowWarning("The old EntityTag.armor_pose and armor_pose_list tags are dperecated in favor of armor_pose_map.");

    // Added 2021/10/28, bump to normal slow warning by 2023.
    public static Warning dynamicPrefix = new VerySlowWarning("Dynamically prefixed arguments (for 'prefix:value' arguments, like '<[sometag]>:<[somevalue]>') were never officially permitted and are now deprecated. You must specify a prefix explicitly if one is needed.");

    // In Bukkit impl, Added 2021/04/13, bump to normal slow warning by 2023.
    public static Warning materialHasDataPackTag = new VerySlowWarning("The tag 'MaterialTag.has_vanilla_data_tag[...]' is deprecated in favor of MaterialTag.vanilla_tags.contains[<name>]");
    public static Warning materialPropertyTags = new VerySlowWarning("Old MaterialTag.is_x property tags are deprecated in favor of PropertyHolderObject.supports[property-name]");

    // Added 2020/05/24, bump to normal slow warning by 2023.
    public static Warning flagIsExpiredTag = new VerySlowWarning(pointlessSubtagPrefix + "'flag[...].is_expired' is deprecated: use 'has_flag[...]' instead.");
    public static Warning flagExpirationTag = new VerySlowWarning(pointlessSubtagPrefix + "'flag[...].expiration' is deprecated: use 'flag_expiration[...]' instead.");

    // In Bukkit impl, Added 2020/06/13, bump to normal slow warning by 2023.
    public static Warning listStyleTags = new VerySlowWarning("'list_' tags are deprecated: just remove the 'list_' prefix.");

    // In Bukkit impl, Added 2020/07/03, bump to normal slow warning by 2023.
    public static Warning attachToMech = new VerySlowWarning("The entity 'attach_to' mechanism is deprecated: use the new 'attach' command instead!");

    // In Bukkit impl, Added 2020/07/12, bump to normal slow warning by 2023.
    public static Warning entityEquipmentSubtags = new VerySlowWarning(pointlessSubtagPrefix + " 'entity.equipment.slotname' is deprecated: use 'entity.equipment_map.get[slotname]' instead.");

    // In Bukkit impl, Added 2020/12/14, but deprecated unofficially earlier, bump to normal slow warning by 2023.
    public static Warning queueClear = new VerySlowWarning("Usage of 'queue clear' or 'queue stop' to stop the current queue is deprecated: use the 'stop' command.");

    // In Bukkit impl, Added 2020/12/25, bump to normal slow warning by 2023.
    public static Warning itemEnchantmentTags = new VerySlowWarning(pointlessSubtagPrefix + "The ItemTag.enchantments.* tags are deprecated: use enchantment_map and relevant MapTag subtags.");

    // ==================== FUTURE deprecations ====================

    // In Bukkit impl, Added 2020/10/18, deprecate officially by 2023.
    // Bad candidate for functionality removal due to frequency of use and likelihood of pre-existing data in save files.
    public static Warning itemDisplayNameMechanism = new FutureWarning("The item 'display_name' mechanism is now just the 'display' mechanism.");

    // In Bukkit impl, Added 2020/12/05, deprecate officially by 2022.
    // Bad candidate for functionality removal due to frequency of use and likelihood of pre-existing data remaining in world data.
    public static Warning itemNbt = new FutureWarning("The item 'nbt' property is deprecated: use ItemTag flags instead!");

    // In Bukkit impl, Added 2021/02/03, deprecate officially by 2023.
    public static Warning hasScriptTags = new FutureWarning("The ItemTag.scriptname and EntityTag.scriptname and ItemTag.has_script and NPCTag.has_script tags are deprecated: use '.script.name' or a null check on .script.");

    // In Bukkit impl, Added 2021/10/18, deprecate officially by 2023.
    public static Warning entityMechanismsFormat = new FutureWarning("Entity script containers previously allowed mechanisms in the script's root, however they should now be under a 'mechanisms' key.");

    // Added 2021/02/04, deprecate officially by 2024.
    public static Warning splitNewDataAction = new FutureWarning("The 'split to new list' data action ('key:!|:value') is deprecated: this no longer has a purpose, as you can instead just set to the list.");

    // Added 2021/02/05, deprecate officially by 2023.
    public static Warning itemProjectile = new FutureWarning("The item_projectile custom entity type is deprecated: modern minecraft lets you set the item of any projectile, like 'snowball[item=stick]'");

    // Added 2021/03/02, deprecate officially by 2023.
    public static Warning itemScriptColor = new FutureWarning("The item script 'color' key is deprecated: use the 'color' mechanism under the 'mechanisms' key instead.");

    // In Bukkit impl, Added 2021/03/29, deprecate officially by 2023.
    public static Warning legacyAttributeProperties = new FutureWarning("The 'attribute' properties are deprecated in favor of the 'attribute_modifiers' properties which more fully implement the attribute system.");

    // Added 2021/04/14, deprecate officially by 2023.
    public static Warning locallyArgument = new FutureWarning("The 'locally' argument in run/inject is deprecated: just specify the script name, or <script>.");

    // In Bukkit impl, Added 2021/07/26, deprecate officially by 2023.
    public static Warning itemEnchantmentsLegacy = new FutureWarning("The tag 'ItemTag.enchantments' is deprecated: use enchantments_map, or enchantment_types.");
    public static Warning echantmentTagUpdate = new FutureWarning("Several legacy enchantment-related tags are deprecated in favor of using EnchantmentTag.");

    // In Bukkit impl, Added 2021/03/27, deprecate officially by 2024.
    public static Warning locationFindEntities = new FutureWarning("The tag 'LocationTag.find.entities.within' and 'blocks' tags are replaced by the 'find_entities' and 'find_blocks' versions. They are mostly compatible, but now have advanced matcher options.");
    public static Warning inventoryNonMatcherTags = new FutureWarning("The 'InventoryTag' tags 'contains', 'quantity', 'find', 'exclude' with raw items are deprecated and replaced by 'contains_item', 'quantity_item', 'find_item', 'exclude_item' that use advanced matcher logic.");
    public static Warning takeRawItems = new FutureWarning("The 'take' command's ability to remove raw items without any command prefix, and the 'material' and 'scriptname' options are deprecated: use the 'item:<matcher>' option.");

    // In Bukkit impl, Added 2021/08/30, deprecate officially by 2023.
    public static Warning giveTakeMoney = new FutureWarning("The 'take' and 'give' commands option for 'money' are deprecated in favor of using the 'money' command.");

    // In Bukkit impl, Added 2021/08/30, deprecate officially by 2024.
    public static Warning playerResourcePackMech = new FutureWarning("The 'resource_pack' mechanism is deprecated in favor of using the 'resourcepack' command.");

    // In Bukkit impl, Added 2021/11/07, deprecate officially by 2024.
    public static Warning assignmentRemove = new FutureWarning("'assignment remove' without a script is deprecated: use 'clear' to clear all scripts, or 'remove' to remove one at a time.");
    public static Warning npcScriptSingle = new FutureWarning("'npc.script' is deprecated in favor of 'npc.scripts' (plural).");

    // In multiple places, Added 2021/11/20, deprecate officially by 2023.
    public static Warning pseudoTagBases = new FutureWarning("Pseudo-tags like '<text>', '<name>', '<amount>', and '<permission>' are deprecated in favor of definitions: just replace <text> with <[text]> or similar.");

    // ==================== PAST deprecations of things that are already gone but still have a warning left behind ====================

    // In Bukkit impl, Added on 2019/10/13
    public static Warning versionScripts = new StrongWarning("Version script containers are deprecated due to the old script repo no longer being active.");

    // Removed in February 2020.
    public static Warning ancientDefs = new StrongWarning("Ancient-style definitions (those with percent signs like %def%) were removed in Denizen 1.1.3, and can no longer be used. Instead, use <[def]>.");
    public static Warning instantTags = new StrongWarning("Instant tags (those with a caret prefix, like <^tag>) were removed in Denizen 1.1.3, and can no longer be used. Instead, pre-define the player or NPC on the line before.");

    // In Bukkit impl, Added on 2019/03/08, removed 2020/10/24.
    public static Warning boundWarning = new StrongWarning("Item script 'bound' functionality has never been reliable and should not be used. Consider replicating the concept with world events.");

    // Added on 2019/08/27, removed 2020/10/24.
    public static Warning yamlFixFormatting = new StrongWarning("YAML command 'fix_formatting' argument is deprecated: this should never be used.");

    // Added on 2021/04/16.
    public static Warning dscriptFileExtension = new StrongWarning("'.dscript' extension has never been officially supported. Please use '.dsc'.");
}
