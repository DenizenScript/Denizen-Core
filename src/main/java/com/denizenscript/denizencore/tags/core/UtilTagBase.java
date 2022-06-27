package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.events.core.TickScriptEvent;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.*;
import com.denizenscript.denizencore.objects.notable.Notable;
import com.denizenscript.denizencore.objects.notable.NoteManager;
import com.denizenscript.denizencore.scripts.ScriptRegistry;
import com.denizenscript.denizencore.scripts.commands.core.SQLCommand;
import com.denizenscript.denizencore.scripts.commands.queue.RunLaterCommand;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.*;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class UtilTagBase {

    public UtilTagBase() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                event.getAttributes().fulfill(1);
                utilTag(event);
            }
        }, "util");
    }

    public static void utilTag(ReplaceableTagEvent event) {
        Attribute attribute = event.getAttributes();

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
        // @attribute <util.random_simplex[x=<#.#>;(y=<#.#>);(z=<#.#>);(w=<#.#>)]>
        // @returns ElementTag(Decimal)
        // @description
        // Returns a pseudo-random decimal number from -1 to 1, based on a Simplex Noise algorithm. See <@link url https://en.wikipedia.org/wiki/Simplex_noise>
        // Input map is like "x=1.0", or "x=1.0;y=2.0", or "x=1.0;y=2.0;z=3" or "x=1;y=2;z=3;w=4"
        // (That is: 1d, 2d, 3d, or 4d).
        // -->
        else if (attribute.startsWith("random_simplex") && attribute.hasParam()) {
            MapTag input = attribute.inputParameterMap();
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
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(res), attribute.fulfill(1)));
        }

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
        else if (attribute.startsWith("list_numbers") && attribute.hasParam()) {
            MapTag input = attribute.inputParameterMap();
            ElementTag toElement = input.getRequiredObjectAs("to", ElementTag.class, attribute);
            if (toElement == null || !toElement.isInt()) {
                return;
            }
            long to = toElement.asInt();
            long from = input.getElement("from", "1").asInt();
            long every = input.getElement("every", "1").asInt();
            ListTag result = new ListTag();
            for (long i = from; i <= to; i += every) {
                result.addObject(new ElementTag(i));
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(result, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.list_numbers_to[<#>]>
        // @returns ListTag
        // @description
        // Returns a list of integer numbers from 1 to the specified input number (inclusive).
        // Note that you should NEVER use this as the input to a "foreach" command. Instead, use <@link command repeat>.
        // In most cases, there's a better way to do what you're trying to accomplish than using this tag.
        // Consider instead using <@link tag util.list_numbers>
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
        // Use <@link tag util.current_time_millis> if you need sub-tick precision.
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

        // <--[tag]
        // @attribute <util.runlater_ids>
        // @returns ListTag
        // @description
        // Returns a list of all scheduled task IDs for <@link command runlater>.
        // Note that usages of runlater that didn't specify an ID will not show up here.
        // -->
        else if (attribute.startsWith("runlater_ids")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ListTag(new ArrayList<>(RunLaterCommand.trackedById.keySet()), true), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.java_version>
        // @returns ElementTag
        // @description
        // Returns the current Java version of the server.
        // -->
        else if (attribute.startsWith("java_version")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(System.getProperty("java.version")), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.has_file[<name>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the specified file exists. The starting path is /plugins/Denizen.
        // -->
        else if (attribute.startsWith("has_file") && attribute.hasParam()) {
            File f = new File(DenizenCore.implementation.getDataFolder(), attribute.getParam());
            try {
                if (!DenizenCore.implementation.canReadFile(f)) {
                    Debug.echoError("Cannot read from that file path due to security settings in Denizen/config.yml.");
                    return;
                }
            }
            catch (Exception e) {
                Debug.echoError(e);
                return;
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(f.exists()), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.list_files[<path>]>
        // @returns ListTag
        // @description
        // Returns a list of all files (and directories) in the specified directory. The starting path is /plugins/Denizen.
        // -->
        else if (attribute.startsWith("list_files") && attribute.hasParam()) {
            File folder = new File(DenizenCore.implementation.getDataFolder(), attribute.getParam());
            try {
                if (!DenizenCore.implementation.canReadFile(folder)) {
                    Debug.echoError("Cannot read from that file path due to security settings in Denizen/config.yml.");
                    return;
                }
                if (!folder.exists() || !folder.isDirectory()) {
                    attribute.echoError("Invalid path specified. No directory exists at that path.");
                    return;
                }
            }
            catch (Exception e) {
                Debug.echoError(e);
                return;
            }
            File[] files = folder.listFiles();
            if (files == null) {
                return;
            }
            ListTag list = new ListTag();
            for (File file : files) {
                list.add(file.getName());
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(list, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.started_time>
        // @returns TimeTag
        // @description
        // Returns the time the server started.
        // -->
        if (attribute.startsWith("started_time")) {
            event.setReplacedObject(new TimeTag(CoreUtilities.monotonicMillisToReal(DenizenCore.startTime))
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.disk_free>
        // @returns ElementTag(Number)
        // @description
        // How much remaining disk space is available to this server, in bytes.
        // This counts only the drive the server folder is on, not any other drives.
        // This may be limited below the actual drive capacity by operating system settings.
        // -->
        if (attribute.startsWith("disk_free")) {
            File folder = DenizenCore.implementation.getDataFolder();
            event.setReplacedObject(new ElementTag(folder.getUsableSpace())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.disk_total>
        // @returns ElementTag(Number)
        // @description
        // How much total disk space is on the drive containing this server, in bytes.
        // This counts only the drive the server folder is on, not any other drives.
        // -->
        if (attribute.startsWith("disk_total")) {
            File folder = DenizenCore.implementation.getDataFolder();
            event.setReplacedObject(new ElementTag(folder.getTotalSpace())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.disk_usage>
        // @returns ElementTag(Number)
        // @description
        // How much space on the drive is already in use, in bytes.
        // This counts only the drive the server folder is on, not any other drives.
        // This is approximately equivalent to "disk_total" minus "disk_free", but is not always exactly the same,
        // as this tag will not include space "used" by operating system settings that simply deny the server write access.
        // -->
        if (attribute.startsWith("disk_usage")) {
            File folder = DenizenCore.implementation.getDataFolder();
            event.setReplacedObject(new ElementTag(folder.getTotalSpace() - folder.getFreeSpace())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.ram_allocated>
        // @returns ElementTag(Number)
        // @description
        // How much RAM is allocated to the server, in bytes (total memory).
        // This is how much of the system memory is reserved by the Java process, NOT how much is actually in use by the minecraft server.
        // -->
        if (attribute.startsWith("ram_allocated")) {
            event.setReplacedObject(new ElementTag(Runtime.getRuntime().totalMemory())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.ram_max>
        // @returns ElementTag(Number)
        // @description
        // How much RAM is available to the server (total), in bytes (max memory).
        // -->
        if (attribute.startsWith("ram_max")) {
            event.setReplacedObject(new ElementTag(Runtime.getRuntime().maxMemory())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.ram_free>
        // @returns ElementTag(Number)
        // @description
        // How much RAM is unused but available on the server, in bytes (free memory).
        // -->
        if (attribute.startsWith("ram_free")) {
            event.setReplacedObject(new ElementTag(Runtime.getRuntime().freeMemory())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.ram_usage>
        // @returns ElementTag(Number)
        // @description
        // How much RAM is used by the server, in bytes (free memory).
        // Equivalent to ram_max minus ram_free
        // -->
        if (attribute.startsWith("ram_usage")) {
            event.setReplacedObject(new ElementTag(Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.available_processors>
        // @returns ElementTag(Number)
        // @description
        // How many virtual processors are available to the server.
        // (In general, Minecraft only uses one, unfortunately.)
        // -->
        if (attribute.startsWith("available_processors")) {
            event.setReplacedObject(new ElementTag(Runtime.getRuntime().availableProcessors())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.current_tick>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of ticks since the server was started.
        // Note that this is NOT an accurate indicator for real server uptime, as ticks fluctuate based on server lag.
        // -->
        if (attribute.startsWith("current_tick")) {
            event.setReplacedObject(new ElementTag(TickScriptEvent.instance.ticks)
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.delta_time_since_start>
        // @returns DurationTag
        // @description
        // Returns the duration of delta time since the server started.
        // Note that this is delta time, not real time, meaning it is calculated based on the server tick,
        // which may change longer or shorter than expected due to lag or other influences.
        // If you want real time instead of delta time, use <@link tag util.real_time_since_start>.
        // -->
        if (attribute.startsWith("delta_time_since_start")) {
            event.setReplacedObject(new DurationTag(TickScriptEvent.instance.ticks)
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.real_time_since_start>
        // @returns DurationTag
        // @description
        // Returns the duration of real time since the server started.
        // Note that this is real time, not delta time, meaning that the it is accurate to the system clock, not the server's tick.
        // System clock changes may cause this value to become inaccurate.
        // In many cases <@link tag util.delta_time_since_start> is preferable.
        // -->
        if (attribute.startsWith("real_time_since_start")) {
            event.setReplacedObject(new DurationTag((CoreUtilities.monotonicMillis() - serverStartTimeMillis) / 1000.0)
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.current_time_millis>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of milliseconds since Jan 1, 1970.
        // Note that this can change every time the tag is read!
        // Use <@link tag util.time_now> if you need stable time.
        // -->
        if (attribute.startsWith("current_time_millis")) {
            event.setReplacedObject(new ElementTag(System.currentTimeMillis())
                    .getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.notes[<type>]>
        // @returns ListTag
        // @description
        // Lists all saved notable objects of a specific type currently on the server.
        // Valid types: locations, cuboids, ellipsoids, inventories, polygons
        // This is primarily intended for debugging purposes, and it's best to avoid using this in a live script if possible.
        // -->
        if (attribute.startsWith("notes")) {
            ListTag allNotables = new ListTag();
            if (attribute.hasParam()) {
                String type = CoreUtilities.toLowerCase(attribute.getParam());
                for (Map.Entry<String, Class> typeClass : NoteManager.namesToTypes.entrySet()) {
                    if (type.equals(CoreUtilities.toLowerCase(typeClass.getKey()))) {
                        for (Object notable : NoteManager.getAllType(typeClass.getValue())) {
                            allNotables.addObject((ObjectTag) notable);
                        }
                        break;
                    }
                }
            }
            else {
                for (Notable notable : NoteManager.nameToObject.values()) {
                    allNotables.addObject((ObjectTag) notable);
                }
            }
            event.setReplacedObject(allNotables.getObjectAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.sql_connections>
        // @returns ListTag
        // @description
        // Returns a list of all SQL connections opened by <@link command sql>.
        // -->
        if (attribute.startsWith("sql_connections")) {
            ListTag list = new ListTag();
            for (Map.Entry<String, Connection> entry : SQLCommand.connections.entrySet()) {
                try {
                    if (!entry.getValue().isClosed()) {
                        list.add(entry.getKey());
                    }
                    else {
                        SQLCommand.connections.remove(entry.getKey());
                    }
                }
                catch (SQLException e) {
                    Debug.echoError(attribute.getScriptEntry(), e);
                }
            }
            event.setReplacedObject(list.getObjectAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <util.scripts>
        // @returns ListTag(ScriptTag)
        // @description
        // Gets a list of all scripts currently loaded into Denizen.
        // -->
        if (attribute.startsWith("scripts")) {
            ListTag scripts = new ListTag();
            for (ScriptContainer script : ScriptRegistry.scriptContainers.values()) {
                scripts.addObject(new ScriptTag(script));
            }
            event.setReplacedObject(scripts.getObjectAttribute(attribute.fulfill(1)));
            return;
        }

        // <--[tag]
        // @attribute <util.last_reload>
        // @returns TimeTag
        // @description
        // Returns the time that Denizen scripts were last reloaded.
        // -->
        else if (attribute.startsWith("last_reload")) {
            event.setReplacedObject(new TimeTag(CoreUtilities.monotonicMillisToReal(DenizenCore.lastReloadTime)).getObjectAttribute(attribute.fulfill(1)));
        }
    }

    public static final long serverStartTimeMillis = CoreUtilities.monotonicMillis();

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

        // <--[mechanism]
        // @object system
        // @name cancel_runlater
        // @input ElementTag
        // @description
        // Cancels a task scheduled in <@link command runlater> by its specified unique ID.
        // If the ID isn't in use, will silently do nothing.
        // Use <@link tag util.runlater_ids> to check whether there is already a scheduled task with the given ID.
        // -->
        if (mechanism.matches("cancel_runlater") && mechanism.hasValue()) {
            RunLaterCommand.FutureRunData runner = RunLaterCommand.trackedById.remove(CoreUtilities.toLowerCase(mechanism.getValue().asString()));
            if (runner != null) {
                runner.cancelled = true;
            }
        }

        if (!mechanism.fulfilled()) {
            mechanism.reportInvalid();
        }
    }
}
