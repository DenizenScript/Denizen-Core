package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class UtilTagBase {

    public UtilTagBase() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                utilTag(event);
            }
        }, "util");
    }

    public void utilTag(ReplaceableTagEvent event) {
        if (!event.matches("util")) {
            return;
        }

        Attribute attribute = event.getAttributes().fulfill(1);

        if (attribute.startsWith("random")) {

            attribute = attribute.fulfill(1);

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
                attribute = attribute.fulfill(1);
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

                        event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(
                                        String.valueOf(CoreUtilities.getRandom().nextInt(max - min + 1) + min)),
                                attribute.fulfill(1)));
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
            if (attribute.startsWith("decimal")
                    && attribute.hasParam()) {
                String stc = attribute.getParam();
                attribute = attribute.fulfill(1);
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

                        event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(
                                        String.valueOf(CoreUtilities.getRandom().nextDouble() * (max - min) + min)),
                                attribute.fulfill(1)));
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
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextDouble())
                        , attribute.fulfill(1)));
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
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextBoolean())
                        , attribute.fulfill(1)));
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
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextGaussian())
                        , attribute.fulfill(1)));
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
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(UUID.randomUUID().toString())
                        , attribute.fulfill(1)));
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
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(id), attribute.fulfill(1)));
            }
        }

        // <--[tag]
        // @attribute <util.random_decimal>
        // @returns ElementTag(Decimal)
        // @description
        // Returns a random decimal number from 0 to 1.
        // -->
        else if (attribute.startsWith("random_decimal")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextDouble()), attribute.fulfill(1)));
        }

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
        else if (attribute.startsWith("random_chance") && attribute.hasParam()) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextDouble() * 100 <= attribute.getDoubleParam()), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.random_boolean>
        // @returns ElementTag(Boolean)
        // @description
        // Returns a random boolean (true or false). Essentially a coin flip.
        // -->
        else if (attribute.startsWith("random_boolean")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextBoolean()), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.random_gauss>
        // @returns ElementTag(Decimal)
        // @description
        // Returns a random decimal number with a gaussian distribution.
        // 70% of all results will be within the range of -1 to 1.
        // -->
        else if (attribute.startsWith("random_gauss")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextGaussian()), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.random_uuid>
        // @returns ElementTag
        // @description
        // Returns a random unique ID.
        // -->
        else if (attribute.startsWith("random_uuid")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(UUID.randomUUID().toString()), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.random_simplex[<input_map>]>
        // @returns ElementTag(Decimal)
        // @description
        // Returns a pseudo-random decimal number from -1 to 1, based on a Simplex Noise algorithm. See <@link url https://en.wikipedia.org/wiki/Simplex_noise>
        // Input map is like "x=1.0", or "x=1.0;y=2.0", or "x=1.0;y=2.0;z=3" or "x=1;y=2;z=3;w=4"
        // (That is: 1d, 2d, 3d, or 4d).
        // -->
        else if (attribute.startsWith("random_simplex") && attribute.hasParam()) {
            MapTag input = attribute.paramAsType(MapTag.class);
            if (input == null) {
                return;
            }
            ObjectTag objX = input.getObject("x"), objY = input.getObject("y"), objZ = input.getObject("z"), objW = input.getObject("w");
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
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(res), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.list_numbers_to[<#>]>
        // @returns ListTag
        // @description
        // Returns a list of integer numbers from 1 to the specified input number (inclusive).
        // Note that you should NEVER use this as the input to a "foreach" command. Instead, use "repeat".
        // In most cases, there's a better way to do what you're trying to accomplish than using this tag.
        // -->
        else if (attribute.startsWith("list_numbers_to") && attribute.hasParam()) {
            int to = attribute.getIntParam();
            ListTag result = new ListTag();
            for (int i = 1; i <= to; i++) {
                result.add(String.valueOf(i));
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(result, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.empty_list_entries[<#>]>
        // @returns ListTag
        // @description
        // Returns a list of the specified size where each entry is blank (zero characters long).
        // -->
        else if (attribute.startsWith("empty_list_entries") && attribute.hasParam()) {
            int to = attribute.getIntParam();
            ListTag result = new ListTag();
            for (int i = 1; i <= to; i++) {
                result.add("");
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(result, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.short_max>
        // @returns ElementTag(Number)
        // @description
        // Returns the maximum value of a 16 bit signed integer (a java 'short'): 32767
        // -->
        else if (attribute.startsWith("short_max")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Short.MAX_VALUE), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.short_min>
        // @returns ElementTag(Number)
        // @description
        // Returns the minimum value of a 16 bit signed integer (a java 'short'): -32768
        // -->
        else if (attribute.startsWith("short_min")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Short.MIN_VALUE), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.int_max>
        // @returns ElementTag(Number)
        // @description
        // Returns the maximum value of a 32 bit signed integer (a java 'int'): 2147483647
        // -->
        else if (attribute.startsWith("int_max")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Integer.MAX_VALUE), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.int_min>
        // @returns ElementTag(Number)
        // @description
        // Returns the minimum value of a 32 bit signed integer (a java 'int'): -2147483648
        // -->
        else if (attribute.startsWith("int_min")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Integer.MIN_VALUE), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.long_max>
        // @returns ElementTag(Number)
        // @description
        // Returns the maximum value of a 64 bit signed integer (a java 'long'): 9223372036854775807
        // -->
        else if (attribute.startsWith("long_max")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Long.MAX_VALUE), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.long_min>
        // @returns ElementTag(Number)
        // @description
        // Returns the minimum value of a 64 bit signed integer (a java 'long'): -9223372036854775808
        // -->
        else if (attribute.startsWith("long_min")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Long.MIN_VALUE), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.pi>
        // @returns ElementTag(Decimal)
        // @description
        // Returns PI: 3.14159265358979323846
        // -->
        else if (attribute.startsWith("pi")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Math.PI), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.tau>
        // @returns ElementTag(Decimal)
        // @description
        // Returns Tau: 6.28318530717958647692
        // -->
        else if (attribute.startsWith("tau")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Math.PI * 2), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.e>
        // @returns ElementTag(Decimal)
        // @description
        // Returns e (Euler's number): 2.7182818284590452354
        // -->
        else if (attribute.matches("e")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Math.E), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.list_denizen_commands>
        // @returns ListTag
        // @description
        // Returns a list of all currently loaded Denizen commands.
        // -->
        else if (attribute.startsWith("list_denizen_commands")) {
            ListTag result = new ListTag(DenizenCore.commandRegistry.instances.keySet());
            event.setReplacedObject(CoreUtilities.autoAttrib(result, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.list_tag_bases>
        // @returns ListTag
        // @description
        // Returns a list of all currently loaded Denizen tag bases (including "player", "context", "util", "server", etc).
        // -->
        else if (attribute.startsWith("list_tag_bases")) {
            ListTag result = new ListTag(TagManager.baseTags.keySet());
            event.setReplacedObject(CoreUtilities.autoAttrib(result, attribute.fulfill(1)));
        }

        else if (attribute.matches("time_at") && attribute.hasParam()) {
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
            event.setReplacedObject(result.getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.time_now>
        // @returns TimeTag
        // @description
        // Returns the current system date/time.
        // This value may be wrong if a server is currently heavily lagging, as it only updates once each tick.
        // Use <@link tag server.current_time_millis> if you need sub-tick precision.
        // -->
        else if (attribute.startsWith("time_now")) {
            event.setReplacedObject(TimeTag.now().getObjectAttribute(attribute.fulfill(1)));
        }

        else if (attribute.startsWith("date")) {
            Deprecations.timeTagRewrite.warn(attribute.context);
            Calendar calendar = Calendar.getInstance();
            Date currentDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat();
            attribute = attribute.fulfill(1);
            if (attribute.startsWith("time")) {
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("twentyfour_hour")) {
                    format.applyPattern("k:mm");
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(format.format(currentDate))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("year")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(calendar.get(Calendar.YEAR))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("month")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(calendar.get(Calendar.MONTH) + 1)
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("week")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(calendar.get(Calendar.WEEK_OF_YEAR))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("day_of_week")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(calendar.get(Calendar.DAY_OF_WEEK))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("day")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(calendar.get(Calendar.DAY_OF_MONTH))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("hour")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(calendar.get(Calendar.HOUR_OF_DAY))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("minute")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(calendar.get(Calendar.MINUTE))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("second")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(calendar.get(Calendar.SECOND))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("duration")) {
                    event.setReplacedObject(CoreUtilities.autoAttrib(new DurationTag(System.currentTimeMillis() / 50)
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("zone")) {
                    TimeZone tz = Calendar.getInstance().getTimeZone();
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(tz.getDisplayName(tz.inDaylightTime(currentDate), TimeZone.SHORT))
                            , attribute.fulfill(1)));
                }
                else if (attribute.startsWith("formatted_zone")) {
                    TimeZone tz = Calendar.getInstance().getTimeZone();
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(tz.getDisplayName(tz.inDaylightTime(currentDate), TimeZone.LONG))
                            , attribute.fulfill(1)));
                }
                else {
                    format.applyPattern("K:mm a");
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(format.format(currentDate))
                            , attribute));
                }
            }
            else if (attribute.startsWith("format")
                    && attribute.hasParam()) {
                try {
                    format.applyPattern(attribute.getParam());
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(format.format(currentDate)), attribute.fulfill(1)));
                }
                catch (Exception ex) {
                    Debug.echoError("Error: invalid pattern '" + attribute.getParam() + "'");
                    Debug.echoError(ex);
                }
            }
            else {
                format.applyPattern("EEE, MMM d, yyyy");
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(format.format(currentDate))
                        , attribute));
            }
        }

        // <--[tag]
        // @attribute <util.parse_yaml[<yaml>]>
        // @returns MapTag
        // @description
        // Parses the input YAML or JSON text into a MapTag.
        // -->
        else if (attribute.matches("parse_yaml") && attribute.hasParam()) {
            ObjectTag tagForm = CoreUtilities.objectToTagForm(YamlConfiguration.load(attribute.getParam()).contents, attribute.context);
            event.setReplacedObject(CoreUtilities.autoAttrib(tagForm, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.queues>
        // @returns ListTag(QueueTag)
        // @description
        // Returns a list of all currently running queues on the server.
        // -->
        else if (attribute.startsWith("queues")) {
            ListTag list = new ListTag();
            for (ScriptQueue queue : ScriptQueue.getQueues()) {
                list.addObject(new QueueTag(queue));
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(list, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.event_stats>
        // @returns ElementTag
        // @description
        // Returns a simple debuggable stats report for all ScriptEvents during this server session.
        // -->
        else if (attribute.startsWith("event_stats")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(ScriptQueue.getStats()), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.event_stats_data>
        // @returns ListTag(MapTag)
        // @description
        // Returns the raw data for <@link tag util.event_stats>, as a ListTag of MapTags.
        // -->
        else if (attribute.startsWith("event_stats_data")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(ScriptQueue.getStatsRawData(), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.default_encoding>
        // @returns ElementTag
        // @description
        // Returns the name of the default system text encoding charset, such as "UTF-8".
        // -->
        else if (attribute.startsWith("default_encoding")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Charset.defaultCharset().name()), attribute.fulfill(1)));
        }
    }

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

    public static void adjustSystem(Mechanism mechanism) {

        // <--[mechanism]
        // @object system
        // @name redirect_logging
        // @input ElementTag(Boolean)
        // @description
        // Tells the server to redirect logging to a world event or not.
        // Note that this redirects *all console output* not just Denizen output.
        // Note: don't enable /denizen debug -e while this is active.
        // Requires config file setting "Debug.Allow console redirection"!
        // For example: - adjust system redirect_logging:true
        // -->
        if (mechanism.matches("redirect_logging") && mechanism.hasValue()) {
            if (!CoreConfiguration.allowConsoleRedirection) {
                Debug.echoError("Console redirection disabled by administrator (refer to mechanism documentation).");
                return;
            }
            if (mechanism.getValue().asBoolean()) {
                DenizenCore.logInterceptor.redirectOutput();
            }
            else {
                DenizenCore.logInterceptor.standardOutput();
            }
        }

        if (!mechanism.fulfilled()) {
            mechanism.reportInvalid();
        }
    }
}
