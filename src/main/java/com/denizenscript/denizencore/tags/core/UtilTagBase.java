package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.TimeTag;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

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
        }, "util", "u");
    }

    public void utilTag(ReplaceableTagEvent event) {
        if (!event.matches("util", "u")) {
            return;
        }

        if (event.matches("u")) {
            Deprecations.utilShorthand.warn(event.getScriptEntry());
        }

        Attribute attribute = event.getAttributes().fulfill(1);

        if (attribute.startsWith("random")) {

            attribute = attribute.fulfill(1);

            // <--[tag]
            // @attribute <util.random.int[<#>].to[<#>]>
            // @returns ElementTag(Number)
            // @description
            // Returns a random number between the 2 specified numbers, inclusive.
            // For example: random.int[1].to[3] could return 1, 2, or 3.
            // -->
            if (attribute.startsWith("int")) {
                String stc = attribute.getContext(1);
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("to")) {
                    if (ArgumentHelper.matchesInteger(stc) && ArgumentHelper.matchesInteger(attribute.getContext(1))) {
                        int min = Integer.parseInt(stc);
                        int max = attribute.getIntContext(1);

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
            // Returns a random number between the 2 specified numbers, inclusive.
            // For example: random.decimal[1].to[2] could return 1.5, 1.75, or a massive number of other options.
            // -->
            if (attribute.startsWith("decimal")
                    && attribute.hasContext(1)) {
                String stc = attribute.getContext(1);
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("to")) {
                    if (ArgumentHelper.matchesDouble(stc) && ArgumentHelper.matchesDouble(attribute.getContext(1))) {
                        double min = Double.parseDouble(stc);
                        double max = Double.parseDouble(attribute.getContext(1));

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
            // @returns ElementTag
            // @description
            // Returns a random decimal number from 0 to 1
            // -->
            else if (attribute.startsWith("decimal")) {
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextDouble())
                        , attribute.fulfill(1)));
            }

            // <--[tag]
            // @attribute <util.random.gauss>
            // @returns ElementTag
            // @description
            // Returns a random decimal number with a gaussian distribution.
            // 70% of all results will be within the range of -1 to 1.
            // -->
            else if (attribute.startsWith("gauss")) {
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(CoreUtilities.getRandom().nextGaussian())
                        , attribute.fulfill(1)));
            }

            // <--[tag]
            // @attribute <util.random.uuid>
            // @returns ElementTag
            // @description
            // Returns a random unique ID.
            // -->
            else if (attribute.startsWith("uuid")) {
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(UUID.randomUUID().toString())
                        , attribute.fulfill(1)));
            }

            // <--[tag]
            // @attribute <util.random.duuid[(<source>)]>
            // @returns ElementTag
            // @description
            // Returns a random 'denizen' unique ID, which is made of a randomly generated sentence.
            // Optionally specify the source context to base the value on.
            // -->
            else if (attribute.startsWith("duuid")) {
                event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(
                        ScriptQueue.getNextId(attribute.hasContext(1) ? attribute.getContext(1) : "DUUID")),
                        attribute.fulfill(1)));
            }
        }

        // <--[tag]
        // @attribute <util.list_numbers_to[<#>]>
        // @returns ListTag
        // @description
        // Returns a list of numbers from 1 to the specified input number (inclusive).
        // Note that you should NEVER use this as the input to a "foreach" command. Instead, use "repeat".
        // In most cases, there's a better way to do what you're trying to accomplish than using this tag.
        // -->
        if (attribute.startsWith("list_numbers_to") && attribute.hasContext(1)) {
            int to = attribute.getIntContext(1);
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
        if (attribute.startsWith("empty_list_entries") && attribute.hasContext(1)) {
            int to = attribute.getIntContext(1);
            ListTag result = new ListTag();
            for (int i = 1; i <= to; i++) {
                result.add("");
            }
            event.setReplacedObject(CoreUtilities.autoAttrib(result, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.pi>
        // @returns ElementTag
        // @description
        // Returns PI: 3.14159265358979323846
        // -->
        else if (attribute.startsWith("pi")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Math.PI), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.tau>
        // @returns ElementTag
        // @description
        // Returns Tau: 6.28318530717958647692
        // -->
        else if (attribute.startsWith("tau")) {
            event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(Math.PI * 2), attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.e>
        // @returns ElementTag
        // @description
        // Returns e: 2.7182818284590452354
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
            ListTag result = new ListTag(DenizenCore.getCommandRegistry().instances.keySet());
            event.setReplacedObject(CoreUtilities.autoAttrib(result, attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.list_tag_bases>
        // @returns ListTag
        // @description
        // Returns a list of all currently loaded Denizen tag bases (including "player", "context", "util", "server", etc).
        // -->
        else if (attribute.startsWith("list_tag_bases")) {
            ListTag result = new ListTag(TagManager.handlers.keySet());
            event.setReplacedObject(CoreUtilities.autoAttrib(result, attribute.fulfill(1)));
        }

        else if (attribute.matches("time_at") && attribute.hasContext(1)) {
            Deprecations.timeTagRewrite.warn(attribute.context);
            String[] dateComponents = attribute.getContext(1).split(" ");
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
                    && attribute.hasContext(1)) {
                try {
                    format.applyPattern(attribute.getContext(1));
                    event.setReplacedObject(CoreUtilities.autoAttrib(new ElementTag(format.format(currentDate)), attribute.fulfill(1)));
                }
                catch (Exception ex) {
                    Debug.echoError("Error: invalid pattern '" + attribute.getContext(1) + "'");
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
        else if (attribute.matches("parse_yaml") && attribute.hasContext(1)) {
            ObjectTag tagForm = CoreUtilities.objectToTagForm(YamlConfiguration.load(attribute.getContext(1)).contents, attribute.context);
            event.setReplacedObject(CoreUtilities.autoAttrib(tagForm, attribute.fulfill(1)));
        }
    }

    public static void adjustSystem(Mechanism mechanism) {

        // <--[mechanism]
        // @object system
        // @name redirect_logging
        // @input ElementTag(Boolean)
        // @description
        // Tells the server to redirect logging to a world event or not.
        // Note that this redirects *all console output* not just Denizen output.
        // Note: don't enable /denizen debug -e while this is active.
        // -->
        if (mechanism.matches("redirect_logging") && mechanism.hasValue()) {
            if (!DenizenCore.getImplementation().allowConsoleRedirection()) {
                Debug.echoError("Console redirection disabled by administrator.");
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
