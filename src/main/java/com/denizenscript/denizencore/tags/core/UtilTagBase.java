package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.events.core.TickScriptEvent;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.objects.notable.NoteManager;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.commands.CommandRegistry;
import com.denizenscript.denizencore.scripts.commands.core.AdjustCommand;
import com.denizenscript.denizencore.scripts.commands.core.MongoCommand;
import com.denizenscript.denizencore.scripts.commands.core.SQLCommand;
import com.denizenscript.denizencore.scripts.commands.queue.RunLaterCommand;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.PseudoObjectTagBase;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class UtilTagBase extends PseudoObjectTagBase<UtilTagBase> {

    // <--[ObjectType]
    // @name system
    // @prefix None
    // @base None
    // @ExampleAdjustObject system
    // @format
    // N/A
    //
    // @description
    // "system" is an internal pseudo-ObjectType that is used as a mechanism adjust target for some core mechanisms.
    //
    // -->

    public static UtilTagBase instance;

    public UtilTagBase() {
        instance = this;
        TagManager.registerStaticTagBaseHandler(UtilTagBase.class, "util", (t) -> instance);
        AdjustCommand.specialAdjustables.put("system", mechanism -> tagProcessor.processMechanism(instance, mechanism));
    }

    @Override
    public void register() {
        tagProcessor.registerTag(ElementTag.class, "random", (attribute, object) -> {
            attribute.fulfill(1);

            // <--[tag]
            // @attribute <util.random.int[<#>].to[<#>]>
            // @returns ElementTag(Number)
            // @description
            // Returns a random integer number between the 2 specified integer numbers, inclusive.
            // @Example
            // # Will narrate '1', '2', or '3'
            // - narrate <util.random.int[1].to[3]>
            // -->
            if (attribute.startsWith("int")) {
                String stc = attribute.getParam();
                attribute.fulfill(1);
                if (attribute.startsWith("to")) {
                    if (ArgumentHelper.matchesInteger(stc) && ArgumentHelper.matchesInteger(attribute.getParam())) {
                        int min = Integer.parseInt(stc);
                        int max = attribute.getIntParam();
                        // in case the first number is larger than the second, reverse them
                        if (min > max) {
                            int store = min;
                            min = max;
                            max = store;
                        }
                        return new ElementTag(CoreUtilities.getRandom().nextInt(max - min + 1) + min);
                    }
                }
            }

            // <--[tag]
            // @attribute <util.random.decimal[<#.#>].to[<#.#>]>
            // @returns ElementTag(Decimal)
            // @description
            // Returns a random decimal number between the 2 specified decimal numbers, inclusive.
            // @Example
            // # Will narrate '1.5', or '1.75', or '1.01230123', or any other decimal in range.
            // - narrate <util.random.decimal[1].to[2]>
            // -->
            if (attribute.startsWith("decimal") && attribute.hasParam()) {
                String stc = attribute.getParam();
                attribute.fulfill(1);
                if (attribute.startsWith("to")) {
                    if (ArgumentHelper.matchesDouble(stc) && ArgumentHelper.matchesDouble(attribute.getParam())) {
                        double min = Double.parseDouble(stc);
                        double max = Double.parseDouble(attribute.getParam());
                        // in case the first number is larger than the second, reverse them
                        if (min > max) {
                            double store = min;
                            min = max;
                            max = store;
                        }
                        return new ElementTag(CoreUtilities.getRandom().nextDouble() * (max - min) + min);
                    }
                }
            }

            // <--[tag]
            // @attribute <util.random.decimal>
            // @returns ElementTag(Decimal)
            // @deprecated use 'util.random_decimal' with a '_'
            // @description
            // Deprecated in favor of <@link tag util.random_decimal>
            // -->
            else if (attribute.startsWith("decimal")) {
                Deprecations.oldUtilRandomTags.warn(attribute.context);
                return new ElementTag(CoreUtilities.getRandom().nextDouble());
            }

            // <--[tag]
            // @attribute <util.random.boolean>
            // @returns ElementTag(Boolean)
            // @deprecated use 'util.random_boolean' with a '_'
            // @description
            // Deprecated in favor of <@link tag util.random_boolean>
            // -->
            else if (attribute.startsWith("boolean")) {
                Deprecations.oldUtilRandomTags.warn(attribute.context);
                return new ElementTag(CoreUtilities.getRandom().nextBoolean());
            }

            // <--[tag]
            // @attribute <util.random.gauss>
            // @returns ElementTag(Decimal)
            // @deprecated use 'util.random_gauss' with a '_'
            // @description
            // Deprecated in favor of <@link tag util.random_gauss>
            // -->
            else if (attribute.startsWith("gauss")) {
                Deprecations.oldUtilRandomTags.warn(attribute.context);
                return new ElementTag(CoreUtilities.getRandom().nextGaussian());
            }

            // <--[tag]
            // @attribute <util.random.uuid>
            // @returns ElementTag
            // @deprecated use 'util.random_uuid' with a '_'
            // @description
            // Deprecated in favor of <@link tag util.random_uuid>
            // -->
            else if (attribute.startsWith("uuid")) {
                Deprecations.oldUtilRandomTags.warn(attribute.context);
                return new ElementTag(UUID.randomUUID().toString());
            }

            // <--[tag]
            // @attribute <util.random.duuid[(<source>)]>
            // @returns ElementTag
            // @description
            // Returns a random 'denizen' unique ID, which is made of a randomly generated sentence.
            // Optionally specify the source context to base the value on.
            // There is no guarantee of format or content of the returned value - generally, use any other random tag instead of this.
            // -->
            else if (attribute.startsWith("duuid")) {
                int size = QueueWordList.FinalWordList.size();
                String id = (attribute.hasParam() ? attribute.getParam() : "DUUID") + "_"
                        + QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size))
                        + QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size))
                        + QueueWordList.FinalWordList.get(CoreUtilities.getRandom().nextInt(size));
                return new ElementTag(id);
            }
            return null;
        });

        // <--[tag]
        // @attribute <util.random_decimal>
        // @returns ElementTag(Decimal)
        // @description
        // Returns a random decimal number from 0 to 1.
        // -->
        tagProcessor.registerTag(ElementTag.class, "random_decimal", (attribute, object) -> {
            return new ElementTag(CoreUtilities.getRandom().nextDouble());
        });

        // <--[tag]
        // @attribute <util.random_chance[<percent>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns a random boolean (true or false) with the given percent chance (from 0 to 100).
        // @Example
        // - if <util.random_chance[25]>:
        //     - narrate "This happens 25% of the time"
        // - else:
        //     - narrate "This happens 75% of the time"
        // -->
        tagProcessor.registerTag(ElementTag.class, ElementTag.class, "random_chance", (attribute, object, chance) -> {
            return new ElementTag(CoreUtilities.getRandom().nextDouble() * 100 <= chance.asDouble());
        });

        // <--[tag]
        // @attribute <util.random_boolean>
        // @returns ElementTag(Boolean)
        // @description
        // Returns a random boolean (true or false). Essentially a coin flip.
        // -->
        tagProcessor.registerTag(ElementTag.class, "random_boolean", (attribute, object) -> {
            return new ElementTag(CoreUtilities.getRandom().nextBoolean());
        });

        // <--[tag]
        // @attribute <util.random_gauss>
        // @returns ElementTag(Decimal)
        // @description
        // Returns a random decimal number with a gaussian distribution.
        // 70% of all results will be within the range of -1 to 1.
        // -->
        tagProcessor.registerTag(ElementTag.class, "random_gauss", (attribute, object) -> {
            return new ElementTag(CoreUtilities.getRandom().nextGaussian());
        });

        // <--[tag]
        // @attribute <util.random_uuid>
        // @returns ElementTag
        // @description
        // Returns a random unique ID.
        // -->
        tagProcessor.registerTag(ElementTag.class, "random_uuid", (attribute, object) -> {
            return new ElementTag(UUID.randomUUID().toString());
        });

        // <--[tag]
        // @attribute <util.random_simplex[x=<#.#>;(y=<#.#>);(z=<#.#>);(w=<#.#>)]>
        // @returns ElementTag(Decimal)
        // @description
        // Returns a pseudo-random decimal number from -1 to 1, based on a Simplex Noise algorithm. See <@link url https://en.wikipedia.org/wiki/Simplex_noise>
        // Input map is like "x=1.0", or "x=1.0;y=2.0", or "x=1.0;y=2.0;z=3" or "x=1;y=2;z=3;w=4"
        // (That is: 1d, 2d, 3d, or 4d).
        // -->
        tagProcessor.registerTag(ElementTag.class, MapTag.class, "random_simplex", (attribute, object, input) -> {
            ObjectTag objX = input.getRequiredObjectAs("x", ElementTag.class, attribute), objY = input.getObject("y"), objZ = input.getObject("z"), objW = input.getObject("w");
            double x = objX == null ? 0 : objX.asElement().asDouble(), y = objY == null ? 0 : objY.asElement().asDouble(), z = objZ == null ? 0 : objZ.asElement().asDouble(), w = objW == null ? 0 : objW.asElement().asDouble();
            double res = 0;
            if (objW != null && objZ != null && objY != null && objX != null) {
                res = SimplexNoise.noise(x, y, z, w);
            }
            else if (objZ != null && objY != null && objX != null && objW == null) {
                res = SimplexNoise.noise(x, y, z);
            }
            else if (objY != null && objX != null && objW == null && objZ == null) {
                res = SimplexNoise.noise(y, x);
            }
            else if (objX != null && objW == null && objZ == null && objY == null) {
                res = SimplexNoise.noise(y, x);
            }
            else {
                return null;
            }
            return new ElementTag(res);
        });

        // <--[tag]
        // @attribute <util.list_numbers[to=<#>;(from=<#>/{1});(every=<#>/{1})]>
        // @returns ListTag
        // @description
        // Returns a list of integer numbers in the specified range.
        // You must specify at least the 'to' input, you can optionally specify 'from' (default 1), and 'every' (default 1).
        // Note that this generally should not be used as input to the 'foreach' command. Instead, use <@link command repeat>.
        // @example
        // # Narrates "1, 2, and 3"
        // - narrate <util.list_numbers[to=3].formatted>
        // @example
        // # Narrates "3, 4, and 5"
        // - narrate <util.list_numbers[from=3;to=5].formatted>
        // @example
        // # Narrates "4, 8, and 12"
        // - narrate <util.list_numbers[from=4;to=12;every=4].formatted>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, MapTag.class, "list_numbers", (attribute, object, input) -> {
            ElementTag toElement = input.getRequiredObjectAs("to", ElementTag.class, attribute);
            if (toElement == null || !toElement.isInt()) {
                return null;
            }
            long to = toElement.asInt();
            long from = input.getElement("from", "1").asInt();
            long every = input.getElement("every", "1").asInt();
            ListTag result = new ListTag();
            for (long i = from; i <= to; i += every) {
                result.addObject(new ElementTag(i));
            }
            return result;
        });

        // <--[tag]
        // @attribute <util.list_numbers_to[<#>]>
        // @returns ListTag
        // @description
        // Returns a list of integer numbers from 1 to the specified input number (inclusive).
        // Note that you should NEVER use this as the input to a "foreach" command. Instead, use <@link command repeat>.
        // In most cases, there's a better way to do what you're trying to accomplish than using this tag.
        // Consider instead using <@link tag util.list_numbers>
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ElementTag.class, "list_numbers_to", (attribute, object, toElement) -> {
            int to = toElement.asInt();
            ListTag result = new ListTag(to);
            for (int i = 1; i <= to; i++) {
                result.addObject(new ElementTag(i));
            }
            return result;
        });

        // <--[tag]
        // @attribute <util.empty_list_entries[<#>]>
        // @returns ListTag
        // @description
        // Returns a list of the specified size where each entry is blank (zero characters long).
        // -->
        tagProcessor.registerStaticTag(ListTag.class, ElementTag.class, "empty_list_entries", (attribute, object, toElement) -> {
            int to = toElement.asInt();
            ListTag result = new ListTag(to);
            for (int i = 1; i <= to; i++) {
                result.addObject(new ElementTag("", true));
            }
            return result;
        });

        // <--[tag]
        // @attribute <util.short_max>
        // @returns ElementTag(Number)
        // @description
        // Returns the maximum value of a 16 bit signed integer (a java 'short'): 32767
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "short_max", (attribute, object) -> {
            return new ElementTag(Short.MAX_VALUE);
        });

        // <--[tag]
        // @attribute <util.short_min>
        // @returns ElementTag(Number)
        // @description
        // Returns the minimum value of a 16 bit signed integer (a java 'short'): -32768
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "short_min", (attribute, object) -> {
            return new ElementTag(Short.MIN_VALUE);
        });

        // <--[tag]
        // @attribute <util.int_max>
        // @returns ElementTag(Number)
        // @description
        // Returns the maximum value of a 32 bit signed integer (a java 'int'): 2147483647
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "int_max", (attribute, object) -> {
            return new ElementTag(Integer.MAX_VALUE);
        });

        // <--[tag]
        // @attribute <util.int_min>
        // @returns ElementTag(Number)
        // @description
        // Returns the minimum value of a 32 bit signed integer (a java 'int'): -2147483648
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "int_min", (attribute, object) -> {
            return new ElementTag(Integer.MIN_VALUE);
        });

        // <--[tag]
        // @attribute <util.long_max>
        // @returns ElementTag(Number)
        // @description
        // Returns the maximum value of a 64 bit signed integer (a java 'long'): 9223372036854775807
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "long_max", (attribute, object) -> {
            return new ElementTag(Long.MAX_VALUE);
        });

        // <--[tag]
        // @attribute <util.long_min>
        // @returns ElementTag(Number)
        // @description
        // Returns the minimum value of a 64 bit signed integer (a java 'long'): -9223372036854775808
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "long_min", (attribute, object) -> {
            return new ElementTag(Long.MIN_VALUE);
        });

        // <--[tag]
        // @attribute <util.pi>
        // @returns ElementTag(Decimal)
        // @description
        // Returns PI: 3.14159265358979323846
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "pi", (attribute, object) -> {
            return new ElementTag(Math.PI);
        });

        // <--[tag]
        // @attribute <util.tau>
        // @returns ElementTag(Decimal)
        // @description
        // Returns Tau: 6.28318530717958647692
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "tau", (attribute, object) -> {
            return new ElementTag(Math.PI * 2);
        });

        // <--[tag]
        // @attribute <util.e>
        // @returns ElementTag(Decimal)
        // @description
        // Returns e (Euler's number): 2.7182818284590452354
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "e", (attribute, object) -> {
            return new ElementTag(Math.E);
        });

        // <--[tag]
        // @attribute <util.list_denizen_commands>
        // @returns ListTag
        // @description
        // Returns a list of all currently loaded Denizen commands.
        // -->
        tagProcessor.registerTag(ListTag.class, "list_denizen_commands", (attribute, object) -> {
            return new ListTag(DenizenCore.commandRegistry.instances.keySet(), true);
        });

        // <--[tag]
        // @attribute <util.color_names>
        // @returns ListTag
        // @description
        // Returns a list of all color names recognized by <@link objecttype ColorTag>.
        // -->
        tagProcessor.registerTag(ListTag.class, "color_names", (attribute, object) -> {
            return new ListTag(ColorTag.colorNames, true);
        });

        // <--[tag]
        // @attribute <util.list_tag_bases>
        // @returns ListTag
        // @description
        // Returns a list of all currently loaded Denizen tag bases (including "player", "context", "util", "server", etc).
        // -->
        tagProcessor.registerTag(ListTag.class, "list_tag_bases", (attribute, object) -> {
            return new ListTag(TagManager.baseTags.keySet(), true);
        });

        tagProcessor.registerTag(TimeTag.class, "time_at", (attribute, object) -> {
            Deprecations.timeTagRewrite.warn(attribute.context);
            String[] dateComponents = attribute.getParam().split(" ");
            String[] ymd = dateComponents[0].split("/");
            int year = Integer.parseInt(ymd[0]);
            int month = Integer.parseInt(ymd[1]) - 1;
            int day = Integer.parseInt(ymd[2]);
            int hour = 0, minute = 0, second = 0, millisecond = 0;
            if (dateComponents.length > 1) {
                String[] hms = dateComponents[1].split(":");
                hour = Integer.parseInt(hms[0]);
                minute = Integer.parseInt(hms[1]);
                second = Integer.parseInt(hms[2]);
                if (hms.length > 3) {
                    millisecond = Integer.parseInt(hms[3]);
                }
            }
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, day, hour, minute, second);
            TimeTag result = new TimeTag(calendar.getTimeInMillis() + millisecond);
            return result;
        });

        // <--[tag]
        // @attribute <util.time_now>
        // @returns TimeTag
        // @description
        // Returns the current system date/time.
        // This value may be wrong if a server is currently heavily lagging, as it only updates once each tick.
        // Use <@link tag util.current_time_millis> if you need sub-tick precision.
        // -->
        tagProcessor.registerTag(TimeTag.class, "time_now", (attribute, object) -> {
            return TimeTag.now();
        });

        tagProcessor.registerTag(ObjectTag.class, "date", (attribute, object) -> {
            Deprecations.timeTagRewrite.warn(attribute.context);
            Calendar calendar = Calendar.getInstance();
            Date currentDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat();
            if (attribute.startsWith("time", 2)) {
                attribute.fulfill(1);
                if (attribute.startsWith("twentyfour_hour", 2)) {
                    attribute.fulfill(1);
                    format.applyPattern("k:mm");
                    return new ElementTag(format.format(currentDate));
                }
                else if (attribute.startsWith("year", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(calendar.get(Calendar.YEAR));
                }
                else if (attribute.startsWith("month", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(calendar.get(Calendar.MONTH) + 1);
                }
                else if (attribute.startsWith("week", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(calendar.get(Calendar.WEEK_OF_YEAR));
                }
                else if (attribute.startsWith("day_of_week", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(calendar.get(Calendar.DAY_OF_WEEK));
                }
                else if (attribute.startsWith("day", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(calendar.get(Calendar.DAY_OF_MONTH));
                }
                else if (attribute.startsWith("hour", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(calendar.get(Calendar.HOUR_OF_DAY));
                }
                else if (attribute.startsWith("minute", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(calendar.get(Calendar.MINUTE));
                }
                else if (attribute.startsWith("second", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(calendar.get(Calendar.SECOND));
                }
                else if (attribute.startsWith("duration", 2)) {
                    attribute.fulfill(1);
                    return new DurationTag(System.currentTimeMillis() / 50);
                }
                else if (attribute.startsWith("zone", 2)) {
                    attribute.fulfill(1);
                    TimeZone tz = Calendar.getInstance().getTimeZone();
                    return new ElementTag(tz.getDisplayName(tz.inDaylightTime(currentDate), TimeZone.SHORT));
                }
                else if (attribute.startsWith("formatted_zone", 2)) {
                    attribute.fulfill(1);
                    TimeZone tz = Calendar.getInstance().getTimeZone();
                    return new ElementTag(tz.getDisplayName(tz.inDaylightTime(currentDate), TimeZone.LONG));
                }
                else {
                    format.applyPattern("K:mm a");
                    return new ElementTag(format.format(currentDate));
                }
            }
            else if (attribute.startsWith("format", 2) && attribute.hasParam()) {
                try {
                    attribute.fulfill(1);
                    format.applyPattern(attribute.getParam());
                    return new ElementTag(format.format(currentDate));
                }
                catch (Exception ex) {
                    Debug.echoError("Error: invalid pattern '" + attribute.getParam() + "'");
                    Debug.echoError(ex);
                }
            }
            else {
                format.applyPattern("EEE, MMM d, yyyy");
                return new ElementTag(format.format(currentDate));
            }
            return null;
        });

        // <--[tag]
        // @attribute <util.parse_yaml[<yaml>]>
        // @returns MapTag
        // @description
        // Parses the input YAML or JSON text into a MapTag.
        // -->
        tagProcessor.registerStaticTag(MapTag.class, ElementTag.class, "parse_yaml", (attribute, object, rawYaml) -> {
            YamlConfiguration yaml = YamlConfiguration.load(rawYaml.asString());
            if (yaml == null) {
                attribute.echoError("Could not load input parameter as YAML.");
                return null;
            }
            return (MapTag) CoreUtilities.objectToTagForm(yaml.contents, attribute.context);
        });

        // <--[tag]
        // @attribute <util.queues>
        // @returns ListTag(QueueTag)
        // @description
        // Returns a list of all currently running queues on the server.
        // -->
        tagProcessor.registerTag(ListTag.class, "queues", (attribute, object) -> {
            return new ListTag(ScriptQueue.getQueues(), QueueTag::new);
        });

        // <--[tag]
        // @attribute <util.event_stats>
        // @returns ElementTag
        // @description
        // Returns a simple debuggable stats report for all ScriptEvents during this server session.
        // -->
        tagProcessor.registerTag(ElementTag.class, "event_stats", (attribute, object) -> {
            return new ElementTag(ScriptQueue.getStats());
        });

        // <--[tag]
        // @attribute <util.event_stats_data>
        // @returns ListTag(MapTag)
        // @description
        // Returns the raw data for <@link tag util.event_stats>, as a ListTag of MapTags.
        // -->
        tagProcessor.registerTag(ListTag.class, "event_stats_data", (attribute, object) -> {
            return ScriptQueue.getStatsRawData();
        });

        // <--[tag]
        // @attribute <util.default_encoding>
        // @returns ElementTag
        // @description
        // Returns the name of the default system text encoding charset, such as "UTF-8".
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "default_encoding", (attribute, object) -> {
            return new ElementTag(Charset.defaultCharset().name());
        });

        // <--[tag]
        // @attribute <util.runlater_ids>
        // @returns ListTag
        // @description
        // Returns a list of all scheduled task IDs for <@link command runlater>.
        // Note that usages of runlater that didn't specify an ID will not show up here.
        // -->
        tagProcessor.registerTag(ListTag.class, "runlater_ids", (attribute, object) -> {
            return new ListTag(RunLaterCommand.trackedById.keySet(), true);
        });

        // <--[tag]
        // @attribute <util.java_version>
        // @returns ElementTag
        // @description
        // Returns the current Java version of the server.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "java_version", (attribute, object) -> {
            return new ElementTag(System.getProperty("java.version"));
        });

        // <--[tag]
        // @attribute <util.has_file[<name>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the specified file exists. The starting path is /plugins/Denizen.
        // -->
        tagProcessor.registerTag(ElementTag.class, ElementTag.class, "has_file", (attribute, object, fileName) -> {
            File f = new File(DenizenCore.implementation.getDataFolder(), fileName.asString());
            try {
                if (!DenizenCore.implementation.canReadFile(f)) {
                    Debug.echoError("Cannot read from that file path due to security settings in Denizen/config.yml.");
                    return null;
                }
            }
            catch (Exception e) {
                Debug.echoError(e);
                return null;
            }
            return new ElementTag(f.exists());
        });

        // <--[tag]
        // @attribute <util.list_files[<path>]>
        // @returns ListTag
        // @description
        // Returns a list of all files (and directories) in the specified directory. The starting path is /plugins/Denizen.
        // -->
        tagProcessor.registerTag(ListTag.class, ElementTag.class, "list_files", (attribute, object, folderName) -> {
            File folder = new File(DenizenCore.implementation.getDataFolder(), folderName.asString());
            try {
                if (!DenizenCore.implementation.canReadFile(folder)) {
                    Debug.echoError("Cannot read from that file path due to security settings in Denizen/config.yml.");
                    return null;
                }
                if (!folder.exists() || !folder.isDirectory()) {
                    attribute.echoError("Invalid path specified. No directory exists at that path.");
                    return null;
                }
            }
            catch (Exception e) {
                Debug.echoError(e);
                return null;
            }
            File[] files = folder.listFiles();
            if (files == null) {
                return null;
            }
            return new ListTag(Arrays.asList(files), file -> new ElementTag(file.getName(), true));
        });

        // <--[tag]
        // @attribute <util.started_time>
        // @returns TimeTag
        // @description
        // Returns the time the server started.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "started_time", (attribute, object) -> {
            return new TimeTag(CoreUtilities.monotonicMillisToReal(DenizenCore.startTime));
        });

        // <--[tag]
        // @attribute <util.disk_free>
        // @returns ElementTag(Number)
        // @description
        // How much remaining disk space is available to this server, in bytes.
        // This counts only the drive the server folder is on, not any other drives.
        // This may be limited below the actual drive capacity by operating system settings.
        // -->
        tagProcessor.registerTag(ElementTag.class, "disk_free", (attribute, object) -> {
            File folder = DenizenCore.implementation.getDataFolder();
            return new ElementTag(folder.getUsableSpace());
        });

        // <--[tag]
        // @attribute <util.disk_total>
        // @returns ElementTag(Number)
        // @description
        // How much total disk space is on the drive containing this server, in bytes.
        // This counts only the drive the server folder is on, not any other drives.
        // -->
        tagProcessor.registerTag(ElementTag.class, "disk_total", (attribute, object) -> {
            File folder = DenizenCore.implementation.getDataFolder();
            return new ElementTag(folder.getTotalSpace());
        });

        // <--[tag]
        // @attribute <util.disk_usage>
        // @returns ElementTag(Number)
        // @description
        // How much space on the drive is already in use, in bytes.
        // This counts only the drive the server folder is on, not any other drives.
        // This is approximately equivalent to "disk_total" minus "disk_free", but is not always exactly the same,
        // as this tag will not include space "used" by operating system settings that simply deny the server write access.
        // -->
        tagProcessor.registerTag(ElementTag.class, "disk_usage", (attribute, object) -> {
            File folder = DenizenCore.implementation.getDataFolder();
            return new ElementTag(folder.getTotalSpace() - folder.getFreeSpace());
        });

        // <--[tag]
        // @attribute <util.ram_allocated>
        // @returns ElementTag(Number)
        // @description
        // How much RAM is allocated to the server, in bytes (total memory).
        // This is how much of the system memory is reserved by the Java process, NOT how much is actually in use by the minecraft server.
        // -->
        tagProcessor.registerTag(ElementTag.class, "ram_allocated", (attribute, object) -> {
            return new ElementTag(Runtime.getRuntime().totalMemory());
        });

        // <--[tag]
        // @attribute <util.ram_max>
        // @returns ElementTag(Number)
        // @description
        // How much RAM is available to the server (total), in bytes (max memory).
        // -->
        tagProcessor.registerTag(ElementTag.class, "ram_max", (attribute, object) -> {
            return new ElementTag(Runtime.getRuntime().maxMemory());
        });

        // <--[tag]
        // @attribute <util.ram_free>
        // @returns ElementTag(Number)
        // @description
        // How much RAM is unused but available on the server, in bytes (free memory).
        // -->
        tagProcessor.registerTag(ElementTag.class, "ram_free", (attribute, object) -> {
            return new ElementTag(Runtime.getRuntime().freeMemory());
        });

        // <--[tag]
        // @attribute <util.ram_usage>
        // @returns ElementTag(Number)
        // @description
        // How much RAM is used by the server, in bytes (free memory).
        // Equivalent to ram_max minus ram_free
        // -->
        tagProcessor.registerTag(ElementTag.class, "ram_usage", (attribute, object) -> {
            return new ElementTag(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory());
        });

        // <--[tag]
        // @attribute <util.available_processors>
        // @returns ElementTag(Number)
        // @description
        // How many virtual processors are available to the server.
        // (In general, Minecraft only uses one, unfortunately.)
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "available_processors", (attribute, object) -> {
            return new ElementTag(Runtime.getRuntime().availableProcessors());
        });

        // <--[tag]
        // @attribute <util.current_tick>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of ticks since the server was started.
        // Note that this is NOT an accurate indicator for real server uptime, as ticks fluctuate based on server lag.
        // -->
        tagProcessor.registerTag(ElementTag.class, "current_tick", (attribute, object) -> {
            return new ElementTag(TickScriptEvent.instance.ticks);
        });

        // <--[tag]
        // @attribute <util.delta_time_since_start>
        // @returns DurationTag
        // @description
        // Returns the duration of delta time since the server started.
        // Note that this is delta time, not real time, meaning it is calculated based on the server tick,
        // which may change longer or shorter than expected due to lag or other influences.
        // If you want real time instead of delta time, use <@link tag util.real_time_since_start>.
        // -->
        tagProcessor.registerTag(DurationTag.class, "delta_time_since_start", (attribute, object) -> {
            return new DurationTag(TickScriptEvent.instance.ticks);
        });

        // <--[tag]
        // @attribute <util.real_time_since_start>
        // @returns DurationTag
        // @description
        // Returns the duration of real time since the server started.
        // Note that this is real time, not delta time, meaning that the it is accurate to the system clock, not the server's tick.
        // System clock changes may cause this value to become inaccurate.
        // In many cases <@link tag util.delta_time_since_start> is preferable.
        // -->
        tagProcessor.registerTag(DurationTag.class, "real_time_since_start", (attribute, object) -> {
            return new DurationTag((CoreUtilities.monotonicMillis() - serverStartTimeMillis) / 1000.0);
        });

        // <--[tag]
        // @attribute <util.current_time_millis>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of milliseconds since Jan 1, 1970.
        // Note that this can change every time the tag is read!
        // Use <@link tag util.time_now> if you need stable time.
        // -->
        tagProcessor.registerTag(ElementTag.class, "current_time_millis", (attribute, object) -> {
            return new ElementTag(System.currentTimeMillis());
        });

        // <--[tag]
        // @attribute <util.notes[<type>]>
        // @returns ListTag
        // @description
        // Lists all saved notable objects of a specific type currently on the server.
        // Valid types: locations, cuboids, ellipsoids, inventories, polygons
        // This is primarily intended for debugging purposes, and it's best to avoid using this in a live script if possible.
        // -->
        tagProcessor.registerTag(ListTag.class, "notes", (attribute, object) -> {
            if (attribute.hasParam()) {
                String type = CoreUtilities.toLowerCase(attribute.getParam());
                for (Map.Entry<String, Class> typeClass : NoteManager.namesToTypes.entrySet()) {
                    if (type.equals(CoreUtilities.toLowerCase(typeClass.getKey()))) {
                        return new ListTag(NoteManager.getAllType(typeClass.getValue()), notable -> (ObjectTag) notable);
                    }
                }
            }
            else {
                return new ListTag(NoteManager.nameToObject.values(), notable -> (ObjectTag) notable);
            }
            return new ListTag();
        });

        // <--[tag]
        // @attribute <util.sql_connections>
        // @returns ListTag
        // @description
        // Returns a list of all SQL connections opened by <@link command sql>.
        // -->
        tagProcessor.registerTag(ListTag.class, "sql_connections", (attribute, object) -> {
            ListTag list = new ListTag();
            for (Map.Entry<String, Connection> entry : SQLCommand.connections.entrySet()) {
                try {
                    if (!entry.getValue().isClosed()) {
                        list.addObject(new ElementTag(entry.getKey(), true));
                    }
                    else {
                        SQLCommand.connections.remove(entry.getKey());
                    }
                }
                catch (SQLException e) {
                    Debug.echoError(attribute.getScriptEntry(), e);
                }
            }
            return list;
        });

        // <--[tag]
        // @attribute <util.redis_connections>
        // @returns ListTag
        // @description
        // Returns a list of all Redis connections opened by <@link command redis>.
        // -->
        if (CommandRegistry.shouldRegisterByClass("Redis command", "redis.clients.jedis.Jedis")) {
            tagProcessor.registerTag(ListTag.class, "redis_connections", (attribute, object) -> {
                return new ListTag(RedisHelper.connections.keySet(), true);
            });
        }

        // <--[tag]
        // @attribute <util.mongo_connections>
        // @returns ElementTag
        // @description
        // Returns a list of all Mongo connections opened by <@link command mongo>.
        // -->
        if (CommandRegistry.shouldRegisterByClass("Mongo command", "com.mongodb.client.MongoClient")) {
            tagProcessor.registerTag(ListTag.class, "mongo_connections", (attribute, object) -> {
                return new ListTag(MongoCommand.mongoConnections.keySet(), true);
            });
        }

        // <--[tag]
        // @attribute <util.scripts>
        // @returns ListTag(ScriptTag)
        // @description
        // Gets a list of all scripts currently loaded into Denizen.
        // -->
        tagProcessor.registerTag(ListTag.class, "scripts", (attribute, object) -> {
            return new ListTag(ScriptRegistry.scriptContainers.values(), ScriptTag::new);
        });

        // <--[tag]
        // @attribute <util.last_reload>
        // @returns TimeTag
        // @description
        // Returns the time that Denizen scripts were last reloaded.
        // -->
        tagProcessor.registerTag(TimeTag.class, "last_reload", (attribute, object) -> {
            return new TimeTag(CoreUtilities.monotonicMillisToReal(DenizenCore.lastReloadTime));
        });

        // <--[tag]
        // @attribute <util.reflect_class[<name>]>
        // @returns JavaReflectedObjectTag
        // @description
        // Returns a reflected reference to the class of the given full class name.
        // Note that the class-name input is case-sensitive.
        // This can be used like having an object of that class, but cannot read non-static fields.
        // @example
        // # Narrates the current NMS version known to Denizen, for example "v1_19"
        // - narrate <util.reflect_class[com.denizenscript.denizen.nms.NMSHandler].read_field[version]>
        // -->
        tagProcessor.registerStaticTag(JavaReflectedObjectTag.class, ElementTag.class, "reflect_class", (attribute, object, name) -> {
            if (!CoreConfiguration.allowReflectionFieldReads) {
                attribute.echoError("Cannot reflect a class due to config reflection restrictions.");
                return null;
            }
            if (!attribute.hasParam()) {
                return null;
            }
            try {
                Class<?> clazz = Class.forName(name.asString());
                if (clazz.isAnnotationPresent(ReflectionRefuse.class)) {
                    attribute.echoError("Cannot reflect class '" + clazz.getName() + "' as it is marked for reflection refusal.");
                    return null;
                }
                return new JavaReflectedObjectTag(clazz);
            }
            catch (ClassNotFoundException ex) {
                attribute.echoError("Class not found.");
                return null;
            }
        });

        // <--[tag]
        // @attribute <util.stack_trace>
        // @returns ElementTag
        // @description
        // Generates and shows a stack trace for the current context.
        // This tag is strictly for internal debugging reasons.
        // WARNING: Different Java versions generate different stack trace formats and details.
        // WARNING: Java internally limits stack trace generation in a variety of ways. This tag cannot be relied on to output anything.
        // For gathering stable context, prefer <@link tag util.java_class_context>
        // -->
        tagProcessor.registerTag(ElementTag.class, "stack_trace", (attribute, object) -> {
            return new ElementTag(DebugInternals.getFullExceptionMessage(new RuntimeException("TRACE"), false));
        });

        // <--[tag]
        // @attribute <util.java_class_context>
        // @returns ListTag
        // @description
        // Returns a list of class names in the current stack history.
        // More stable than <@link tag util.stack_trace>
        // This contains a lot of stray content, the first 4 to 20 or so classes in this list are likely irrelevant to what you're searching for.
        // Each entry in the list is a raw class name, like "com.denizenscript.denizencore.tags.core.UtilTagBase".
        // Class names may appear multiple times in a row if that class contains methods that call each other.
        // -->
        tagProcessor.registerTag(ListTag.class, "java_class_context", (attribute, object) -> {
            return new ListTag((Collection<? extends ObjectTag>) StackWalker.getInstance(StackWalker.Option.SHOW_HIDDEN_FRAMES).walk(stackFrameStream ->
                    stackFrameStream.map(stackFrame -> new ElementTag(stackFrame.getClassName(), true)).toList()));
        });

        // <--[tag]
        // @attribute <util.debug_enabled>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether script debug is currently globally enabled.
        // -->
        tagProcessor.registerTag(ElementTag.class, "debug_enabled", (attribute, object) -> {
            return new ElementTag(CoreConfiguration.shouldShowDebug);
        });
        // <--[mechanism]
        // @object system
        // @name redirect_logging
        // @input ElementTag(Boolean)
        // @description
        // Tells the server to redirect logging to a world event or not.
        // Note that this redirects *all console output* not just Denizen output.
        // Note: don't enable /denizen debug -e while this is active.
        // Requires config file setting "Debug.Allow console redirection"!
        // @example
        // - adjust system redirect_logging:true
        // -->
        tagProcessor.registerMechanism("redirect_logging", false, ElementTag.class, (object, mechanism, input) -> {
            if (!CoreConfiguration.allowConsoleRedirection) {
                mechanism.echoError("Console redirection disabled by administrator (refer to mechanism documentation).");
                return;
            }
            if (input.asBoolean()) {
                DenizenCore.logInterceptor.redirectOutput();
            }
            else {
                DenizenCore.logInterceptor.standardOutput();
            }
        });

        // <--[mechanism]
        // @object system
        // @name cancel_runlater
        // @input ElementTag
        // @description
        // Cancels a task scheduled in <@link command runlater> by its specified unique ID.
        // If the ID isn't in use, will silently do nothing.
        // Use <@link tag util.runlater_ids> to check whether there is already a scheduled task with the given ID.
        // -->
        tagProcessor.registerMechanism("cancel_runlater", false, ElementTag.class, (object, mechanism, input) -> {
            RunLaterCommand.FutureRunData runner = RunLaterCommand.trackedById.remove(input.asLowerString());
            if (runner != null) {
                runner.cancelled = true;
            }
        });

        // <--[mechanism]
        // @object system
        // @name reset_event_stats
        // @input None
        // @description
        // Resets the statistics on events used for <@link tag util.event_stats>
        // @tags
        // <util.event_stats>
        // <util.event_stats_data>
        // -->
        tagProcessor.registerMechanism("reset_event_stats", false, (object, mechanism) -> {
            for (ScriptEvent scriptEvent : ScriptEvent.events) {
                scriptEvent.eventData.stats_fires = 0;
                scriptEvent.eventData.stats_scriptFires = 0;
                scriptEvent.eventData.stats_nanoTimes = 0;
            }
        });

        // <--[mechanism]
        // @object system
        // @name cleanmem
        // @input None
        // @description
        // Suggests to the internal systems that it's a good time to clean the memory.
        // Does NOT force a memory cleaning.
        // This should generally not be used unless you have a very good specific reason to use it.
        // @tags
        // <util.ram_free>
        // -->
        tagProcessor.registerMechanism("cleanmem", false, (object, mechanism) -> {
            System.gc();
        });

        // <--[mechanism]
        // @object system
        // @name delete_file
        // @input ElementTag
        // @description
        // Deletes the given file from the server.
        // File path starts in the Denizen folder.
        // Require config file setting "Commands.Delete.Allow file deletion".
        // @example
        // - adjust system delete_file:data/logs/latest.txt
        // @tags
        // <util.has_file[<file>]>
        // -->
        tagProcessor.registerMechanism("delete_file", false, ElementTag.class, (object, mechanism, input) -> {
            if (!CoreConfiguration.allowFileDeletion) {
                mechanism.echoError("File deletion disabled by administrator (refer to mechanism documentation).");
                return;
            }
            File file = new File(DenizenCore.implementation.getDataFolder(), input.asString());
            if (!DenizenCore.implementation.canWriteToFile(file)) {
                mechanism.echoError("Cannot write to that file path due to security settings in Denizen/config.yml.");
                return;
            }
            try {
                if (!file.delete()) {
                    mechanism.echoError("Failed to delete file: returned false");
                }
            }
            catch (Exception e) {
                mechanism.echoError("Failed to delete file: " + e.getMessage());
            }
        });
    }

    public static final long serverStartTimeMillis = CoreUtilities.monotonicMillis();
}
