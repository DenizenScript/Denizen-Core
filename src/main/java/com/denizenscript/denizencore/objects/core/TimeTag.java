package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.flags.AbstractFlagTracker;
import com.denizenscript.denizencore.flags.FlaggableObject;
import com.denizenscript.denizencore.flags.RedirectionFlagTracker;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class TimeTag implements ObjectTag, Adjustable, FlaggableObject {

    // <--[ObjectType]
    // @name TimeTag
    // @prefix time
    // @base ElementTag
    // @implements FlaggableObject
    // @ExampleTagBase util.time_now
    // @ExampleValues <util.time_now>
    // @ExampleForReturns
    // - flag server myflag expire:%VALUE%
    // @format
    // The identity format for TimeTags is "yyyy/mm/dd_hh:mm:ss:mill_offset"
    // So, for example, 'time@2020/05/23_02:20:31:123_-07:00'
    //
    // @description
    // A TimeTag represents a real world date/time value.
    //
    // TimeTags can also be constructed from 'yyyy/mm/dd', 'yyyy/mm/dd_hh:mm:ss', or 'yyyy/mm/dd_hh:mm:ss:mill'.
    // (Meaning: the offset is optional, the milliseconds are optional, and the time-of-day is optional,
    // but if you exclude an optional part, you must immediately end the input there, without specifying more).
    //
    // This object type is flaggable.
    // Flags on this object type will be stored in the server saves file, under special sub-key "__time"
    //
    // -->

    public static TimeTag now() {
        return new TimeTag(DenizenCore.currentTimeMillis, ZoneId.systemDefault());
    }

    @Fetchable("time")
    public static TimeTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        if (string.startsWith("time@") && string.length() > "time@".length()) {
            string = string.substring("time@".length());
        }
        List<String> coreParts = CoreUtilities.split(string, '_');
        if (coreParts.size() > 3) {
            return null;
        }
        List<String> dateParts = CoreUtilities.split(coreParts.get(0), '/');
        if (dateParts.size() != 3) {
            return null;
        }
        List<String> timeParts = null;
        if (coreParts.size() > 1) {
            timeParts = CoreUtilities.split(coreParts.get(1), ':');
            if (timeParts.size() != 3 && timeParts.size() != 4) {
                return null;
            }
        }
        try {
            int year = Integer.parseInt(dateParts.get(0));
            int month = Integer.parseInt(dateParts.get(1));
            int day = Integer.parseInt(dateParts.get(2));
            int hour = 0, minute = 0, second = 0, millisecond = 0;
            if (timeParts != null) {
                hour = Integer.parseInt(timeParts.get(0));
                minute = Integer.parseInt(timeParts.get(1));
                second = Integer.parseInt(timeParts.get(2));
                if (timeParts.size() == 4) {
                    millisecond = Integer.parseInt(timeParts.get(3));
                }
            }
            ZoneOffset offset = ZoneOffset.UTC;
            if (coreParts.size() > 2) {
                offset = ZoneOffset.of(coreParts.get(2));
            }
            return new TimeTag(year, month, day, hour, minute, second, millisecond, offset);
        }
        catch (DateTimeException | NumberFormatException ex) {
            if (context == null || context.showErrors()) {
                Debug.echoError(ex);
            }
            return null;
        }
    }

    public static boolean matches(String string) {
        if (CoreUtilities.toLowerCase(string).startsWith("time@")) {
            return true;
        }
        return valueOf(string, CoreUtilities.noDebugContext) != null;
    }

    public ZonedDateTime instant;

    public TimeTag() {
    }

    public TimeTag(int year, int month, int day, int hour, int minute, int second, int millisecond, ZoneOffset offset) {
        this(ZonedDateTime.of(year, month, day, hour, minute, second, millisecond * 1_000_000, offset));
    }

    public TimeTag(ZonedDateTime instant) {
        this.instant = instant;
    }

    public TimeTag(long millis, ZoneId zone) {
        this(Instant.ofEpochSecond(millis / 1000, (millis % 1000) * 1_000_000).atZone(zone));
    }

    public static ZoneId UTC_Zone = ZoneId.of("UTC");

    public TimeTag(long millis) {
        this(millis, UTC_Zone);
    }

    String prefix = "Time";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public TimeTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String getObjectType() {
        return "time";
    }

    public static String pad0(int value, int len) {
        String outputStr = String.valueOf(value);
        while (outputStr.length() < len) {
            outputStr = "0" + outputStr;
        }
        return outputStr;
    }

    @Override
    public String debuggable() {
        return "<LG>time@ <Y>" +
                // The identity format for TimeTags is "yyyy/mm/dd_hh:mm:ss:mill"
                pad0(instant.get(ChronoField.YEAR), 4) + "<LG> / <Y>" +
                pad0(instant.get(ChronoField.MONTH_OF_YEAR), 2) +
                " <LG>(<GR>" + Month.of(instant.get(ChronoField.MONTH_OF_YEAR)).name() + "<LG>)" + "<G> / <Y>" +
                pad0(instant.get(ChronoField.DAY_OF_MONTH), 2) +
                " <LG>(<GR>" + DayOfWeek.of(instant.get(ChronoField.DAY_OF_WEEK)).name() + "<LG>)" + "<G> _ <Y>" +
                pad0(instant.get(ChronoField.HOUR_OF_DAY), 2) + "<LG> : <Y>" +
                pad0(instant.get(ChronoField.MINUTE_OF_HOUR), 2) + "<LG> : <Y>" +
                pad0(instant.get(ChronoField.SECOND_OF_MINUTE), 2) + "<LG> : <Y>" +
                pad0(instant.get(ChronoField.MILLI_OF_SECOND), 4) + "<LG> _ <Y>" +
                instant.getOffset().getId();
    }

    @Override
    public String identify() {
        return "time@" +
                // The identity format for TimeTags is "yyyy/mm/dd_hh:mm:ss:mill"
                pad0(instant.get(ChronoField.YEAR), 4) + "/" +
                pad0(instant.get(ChronoField.MONTH_OF_YEAR), 2) + "/" +
                pad0(instant.get(ChronoField.DAY_OF_MONTH), 2) + "_" +
                pad0(instant.get(ChronoField.HOUR_OF_DAY), 2) + ":" +
                pad0(instant.get(ChronoField.MINUTE_OF_HOUR), 2) + ":" +
                pad0(instant.get(ChronoField.SECOND_OF_MINUTE), 2) + ":" +
                pad0(instant.get(ChronoField.MILLI_OF_SECOND), 4) + "_" +
                instant.getOffset().getId();
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public Object getJavaObject() {
        return instant;
    }

    public int year() {
        return instant.get(ChronoField.YEAR);
    }

    public int month() {
        return instant.get(ChronoField.MONTH_OF_YEAR);
    }

    public int day() {
        return instant.get(ChronoField.DAY_OF_MONTH);
    }

    public int hour() {
        return instant.get(ChronoField.HOUR_OF_DAY);
    }

    public int minute() {
        return instant.get(ChronoField.MINUTE_OF_HOUR);
    }

    public int second() {
        return instant.get(ChronoField.SECOND_OF_MINUTE);
    }

    public int millisecondComponent() {
        return instant.get(ChronoField.MILLI_OF_SECOND);
    }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return new RedirectionFlagTracker(DenizenCore.serverFlagMap, "__time." + millis());
    }

    @Override
    public void reapplyTracker(AbstractFlagTracker tracker) {
        // Nothing to do.
    }

    public static void registerTags() {

        AbstractFlagTracker.registerFlagHandlers(tagProcessor);

        // <--[tag]
        // @attribute <TimeTag.year>
        // @returns ElementTag(Number)
        // @description
        // Returns the year of this TimeTag, like '2020'.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "year", (attribute, object) -> {
            return new ElementTag(object.year());
        });

        // <--[tag]
        // @attribute <TimeTag.month>
        // @returns ElementTag(Number)
        // @description
        // Returns the month of this TimeTag, where January is 1 and December is 12.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "month", (attribute, object) -> {
            return new ElementTag(object.month());
        });

        // <--[tag]
        // @attribute <TimeTag.month_name>
        // @returns ElementTag
        // @description
        // Returns the name of the month of this TimeTag, like 'JANUARY'.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "month_name", (attribute, object) -> {
            return new ElementTag(Month.of(object.month()).name());
        });

        // <--[tag]
        // @attribute <TimeTag.days_in_month>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of days in the month that this TimeTag is within.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "days_in_month", (attribute, object) -> {
            return new ElementTag(YearMonth.of(object.year(), object.month()).lengthOfMonth());
        });

        // <--[tag]
        // @attribute <TimeTag.day>
        // @returns ElementTag(Number)
        // @description
        // Returns the day-of-month of this TimeTag, starting at 1.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "day", (attribute, object) -> {
            return new ElementTag(object.day());
        });

        // <--[tag]
        // @attribute <TimeTag.day_of_year>
        // @returns ElementTag(Number)
        // @description
        // Returns the day-of-year of this TimeTag, from 1 to 365 (or 366 in leap years).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "day_of_year", (attribute, object) -> {
            return new ElementTag(object.instant.get(ChronoField.DAY_OF_YEAR));
        });

        // <--[tag]
        // @attribute <TimeTag.day_of_week>
        // @returns ElementTag(Number)
        // @description
        // Returns the day-of-week of this TimeTag, with Monday as 1 and Sunday as 7 (per ISO standard).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "day_of_week", (attribute, object) -> {
            return new ElementTag(object.instant.get(ChronoField.DAY_OF_WEEK));
        });

        // <--[tag]
        // @attribute <TimeTag.day_of_week_name>
        // @returns ElementTag
        // @description
        // Returns the name of the day-of-week of this TimeTag, like 'MONDAY'.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "day_of_week_name", (attribute, object) -> {
            return new ElementTag(DayOfWeek.of(object.instant.get(ChronoField.DAY_OF_WEEK)).name());
        });

        // <--[tag]
        // @attribute <TimeTag.hour>
        // @returns ElementTag(Number)
        // @description
        // Returns the hour-of-day of this TimeTag, from 1 to 24.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "hour", (attribute, object) -> {
            return new ElementTag(object.hour());
        });

        // <--[tag]
        // @attribute <TimeTag.minute>
        // @returns ElementTag(Number)
        // @description
        // Returns the minute-of-hour of this TimeTag, from 0 to 59.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "minute", (attribute, object) -> {
            return new ElementTag(object.minute());
        });

        // <--[tag]
        // @attribute <TimeTag.second>
        // @returns ElementTag(Number)
        // @description
        // Returns the second-of-minute of this TimeTag, from 0 to 59.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "second", (attribute, object) -> {
            return new ElementTag(object.second());
        });

        // <--[tag]
        // @attribute <TimeTag.millisecond>
        // @returns ElementTag(Number)
        // @description
        // Returns the millisecond of this TimeTag, from 0 to 999.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "millisecond", (attribute, object) -> {
            return new ElementTag(object.millisecondComponent());
        });

        // <--[tag]
        // @attribute <TimeTag.epoch_millis>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of milliseconds between this TimeTag and the Unix Epoch (Jan. 1st 1970).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "epoch_millis", (attribute, object) -> {
            return new ElementTag(object.millis());
        });

        // <--[tag]
        // @attribute <TimeTag.time_zone_offset>
        // @returns ElementTag
        // @description
        // Returns the time zone offset (from UTC) of this TimeTag.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "time_zone_offset", (attribute, object) -> {
            return new ElementTag(object.instant.getOffset().getId());
        });

        // <--[tag]
        // @attribute <TimeTag.time_zone_id>
        // @returns ElementTag
        // @description
        // Returns the ID of the time zone of this TimeTag.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "time_zone_id", (attribute, object) -> {
            return new ElementTag(object.instant.getZone().getId());
        });

        // <--[tag]
        // @attribute <TimeTag.time_zone_name>
        // @returns ElementTag
        // @description
        // Returns the display name of the time zone of this TimeTag.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "time_zone_name", (attribute, object) -> {
            return new ElementTag(object.instant.getZone().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        });

        // <--[tag]
        // @attribute <TimeTag.to_zone[<zone>]>
        // @returns TimeTag
        // @description
        // Returns a copy of the time zone, converted to the specified time zone.
        // Zone input can be like 'UTC-5' or like 'America/New_York'
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "to_zone", (attribute, object) -> {
            try {
                return new TimeTag(object.instant.withZoneSameInstant(ZoneId.of(attribute.getParam())));
            }
            catch (DateTimeException ex) {
                attribute.echoError("Timezone '" + attribute.getParam() + "' is invalid or doesn't exist.");
                return null;
            }
        });

        // <--[tag]
        // @attribute <TimeTag.to_local>
        // @returns TimeTag
        // @description
        // Returns a copy of the time zone, converted to the local time zone.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "to_local", (attribute, object) -> {
            return new TimeTag(object.instant.withZoneSameInstant(ZoneId.systemDefault()));
        });

        // <--[tag]
        // @attribute <TimeTag.to_utc>
        // @returns TimeTag
        // @description
        // Returns a copy of the time zone, converted to Universal Coordinated Time.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "to_utc", (attribute, object) -> {
            return new TimeTag(object.instant.withZoneSameInstant(ZoneOffset.UTC));
        });

        // <--[tag]
        // @attribute <TimeTag.last_hour_of_day[<hour>]>
        // @returns TimeTag
        // @description
        // Returns the timetag that represents the previous time the specified hour number was hit.
        // For example, if the input hour is '14', and the original TimeTag is 5 AM, the return will be 2 PM yesterday.
        // If the input is '14' and the TimeTag is 5 PM, the return will be 2 PM today.
        // The minute/second/millisecond will be zeroed.
        // If the input hour is 5, and the TimeTag is at 5 AM, will return the same day.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "last_hour_of_day", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("time.last_hour_of_day[...] must have input.");
                return null;
            }
            int hour = attribute.getIntParam();
            int todayHour = object.hour();
            TimeTag result = new TimeTag(object.year(), object.month(), object.day(), 0, 0, 0, 0, object.instant.getOffset());
            if (hour > todayHour) {
                result = new TimeTag(result.instant.minusDays(1));
            }
            return new TimeTag(result.instant.plusHours(hour));
        });

        // <--[tag]
        // @attribute <TimeTag.next_hour_of_day[<hour>]>
        // @returns TimeTag
        // @description
        // Returns the timetag that represents the next time the specified hour number will be hit.
        // For example, if the input hour is '14', and the original TimeTag is 5 AM, the return will be 2 PM today.
        // If the input is '14' and the TimeTag is 5 PM, the return will be 2 PM tomorrow.
        // The minute/second/millisecond will be zeroed.
        // If the input hour is 5, and the TimeTag is at 5 AM, will return the next day.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "next_hour_of_day", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("time.next_hour_of_day[...] must have input.");
                return null;
            }
            int hour = attribute.getIntParam();
            int todayHour = object.hour();
            TimeTag result = new TimeTag(object.year(), object.month(), object.day(), 0, 0, 0, 0, object.instant.getOffset());
            if (hour <= todayHour) {
                result = new TimeTag(result.instant.plusDays(1));
            }
            return new TimeTag(result.instant.plusHours(hour));
        });

        // <--[tag]
        // @attribute <TimeTag.last_day_of_week[<day>]>
        // @returns TimeTag
        // @description
        // Returns the timetag of the previous day of the specified input day-of-week (like 'sunday').
        // The hour/minute/second/millisecond will be zeroed.
        // If the TimeTag is on the input day, will return a day 7 days before then.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "last_day_of_week", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("time.last_day_of_week[...] must have input.");
                return null;
            }
            DayOfWeek day;
            try {
                day = DayOfWeek.valueOf(attribute.getParam().toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                attribute.echoError("'" + attribute.getParam() + "' is not a valid day-of-week.");
                return null;
            }
            ZonedDateTime time = object.instant;
            if (time.getDayOfWeek().equals(day)) { // If already on that day, jump back a bit.
                time = time.minus(3, ChronoUnit.DAYS);
            }
            while (!time.getDayOfWeek().equals(day)) {
                time = time.minus(12, ChronoUnit.HOURS); // Subtract by 12 hours instead of 1 day to avoid most edge-cases.
            }
            TimeTag outTime = new TimeTag(time);
            return new TimeTag(outTime.year(), outTime.month(), outTime.day(), 0, 0, 0, 0, object.instant.getOffset());
        });

        // <--[tag]
        // @attribute <TimeTag.next_day_of_week[<day>]>
        // @returns TimeTag
        // @description
        // Returns the timetag of the next day of the specified input day-of-week (like 'thursday').
        // The hour/minute/second/millisecond will be zeroed.
        // If the TimeTag is on the input day, will return a day 7 days from then.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "next_day_of_week", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("time.next_day_of_week[...] must have input.");
                return null;
            }
            DayOfWeek day;
            try {
                day = DayOfWeek.valueOf(attribute.getParam().toUpperCase());
            }
            catch (IllegalArgumentException ex) {
                attribute.echoError("'" + attribute.getParam() + "' is not a valid day-of-week.");
                return null;
            }
            ZonedDateTime time = object.instant;
            if (time.getDayOfWeek().equals(day)) { // If already on that day, jump ahead a bit.
                time = time.plus(3, ChronoUnit.DAYS);
            }
            while (!time.getDayOfWeek().equals(day)) {
                time = time.plus(12, ChronoUnit.HOURS); // Add by 12 hours instead of 1 day to avoid most edge-cases.
            }
            TimeTag outTime = new TimeTag(time);
            return new TimeTag(outTime.year(), outTime.month(), outTime.day(), 0, 0, 0, 0, object.instant.getOffset());
        });

        // <--[tag]
        // @attribute <TimeTag.last_day_of_month[<day>]>
        // @returns TimeTag
        // @description
        // Returns the timetag of the last occurrence of the specified day-of-month.
        // This can either be in the same month, or the previous month.
        // For example, last_day_of_month[3] on a TimeTag on February 1st will return the 3rd of January.
        // The hour/minute/second/millisecond will be zeroed.
        // Be careful with inputs of 29/30/31, as only some months contain those days.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "last_day_of_month", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("time.last_day_of_month[...] must have input.");
                return null;
            }
            int day = attribute.getIntParam();
            if (day < 1 || day > 31) {
                return null;
            }
            if (object.day() < day) {
                if (object.month() == 1) {
                    return new TimeTag(object.year() - 1, 12, day, 0, 0, 0, 0, object.instant.getOffset());
                }
                else {
                    return new TimeTag(object.year(), object.month() - 1, day, 0, 0, 0, 0, object.instant.getOffset());
                }
            }
            else {
                return new TimeTag(object.year(), object.month(), day, 0, 0, 0, 0, object.instant.getOffset());
            }
        });

        // <--[tag]
        // @attribute <TimeTag.next_day_of_month[<day>]>
        // @returns TimeTag
        // @description
        // Returns the timetag of the next occurrence of the specified day-of-month.
        // This can either be in the same month, or the next month.
        // For example, next_day_of_month[1] on a TimeTag on January 3rd will return the 1st of February.
        // The hour/minute/second/millisecond will be zeroed.
        // Be careful with inputs of 29/30/31, as only some months contain those days.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "next_day_of_month", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("time.next_day_of_month[...] must have input.");
                return null;
            }
            int day = attribute.getIntParam();
            if (day < 1 || day > 31) {
                return null;
            }
            if (object.day() >= day) {
                if (object.month() == 12) {
                    return new TimeTag(object.year() + 1, 1, day, 0, 0, 0, 0, object.instant.getOffset());
                }
                else {
                    return new TimeTag(object.year(), object.month() + 1, day, 0, 0, 0, 0, object.instant.getOffset());
                }
            }
            else {
                return new TimeTag(object.year(), object.month(), day, 0, 0, 0, 0, object.instant.getOffset());
            }
        });

        // <--[tag]
        // @attribute <TimeTag.start_of_year>
        // @returns TimeTag
        // @description
        // Returns this time tag, with the month/day/hour/minute/second/millisecond zeroed (that is, midnight the morning of the first day of the same year).
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "start_of_year", (attribute, object) -> {
            return new TimeTag(object.year(), 1, 1, 0, 0, 0, 0, object.instant.getOffset());
        });

        // <--[tag]
        // @attribute <TimeTag.start_of_month>
        // @returns TimeTag
        // @description
        // Returns this time tag, with the day/hour/minute/second/millisecond zeroed (that is, midnight the morning of the first day of the same month).
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "start_of_month", (attribute, object) -> {
            return new TimeTag(object.year(), object.month(), 1, 0, 0, 0, 0, object.instant.getOffset());
        });

        // <--[tag]
        // @attribute <TimeTag.start_of_day>
        // @returns TimeTag
        // @description
        // Returns this time tag, with the hour/minute/second/millisecond zeroed (that is, midnight the morning of the same day).
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "start_of_day", (attribute, object) -> {
            return new TimeTag(object.year(), object.month(), object.day(), 0, 0, 0, 0, object.instant.getOffset());
        });

        // <--[tag]
        // @attribute <TimeTag.add[<duration>]>
        // @returns TimeTag
        // @description
        // Returns the time that is this TimeTag plus a duration.
        // For example, a TimeTag on Monday, '.add[1d]', will return a time on Tuesday.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "add", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag TimeTag.add[...] must have an input.");
                return null;
            }
            DurationTag toAdd = attribute.paramAsType(DurationTag.class);
            return new TimeTag(object.millis() + toAdd.getMillis(), object.instant.getZone());
        });

        // <--[tag]
        // @attribute <TimeTag.sub[<duration>]>
        // @returns TimeTag
        // @description
        // Returns the time that is this TimeTag minus a duration.
        // For example, a TimeTag on Monday, '.sub[1d]', will return a time on Sunday.
        // -->
        tagProcessor.registerStaticTag(TimeTag.class, "sub", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag TimeTag.sub[...] must have an input.");
                return null;
            }
            DurationTag toSub = attribute.paramAsType(DurationTag.class);
            return new TimeTag(object.millis() - toSub.getMillis(), object.instant.getZone());
        });

        // <--[tag]
        // @attribute <TimeTag.from_now>
        // @returns DurationTag
        // @description
        // Returns the DurationTag between this time object and the real current system time.
        // The value will always be positive.
        // This is equivalent to the absolute value of ".duration_since[<util.time_now>]", and exists primarily as a convenience tag.
        // For example, a TimeTag for Tuesday will return a DurationTag of '1d' when the tag is used on a Monday.
        // A TimeTag for Sunday will also return a DurationTag of '1d' when used on a Monday.
        // If positive/negative differences are required, consider instead using <@link tag TimeTag.duration_since>.
        // -->
        tagProcessor.registerStaticTag(DurationTag.class, "from_now", (attribute, object) -> {
            return new DurationTag(Math.abs(object.millis() - DenizenCore.currentTimeMillis) / 1000.0);
        });

        // <--[tag]
        // @attribute <TimeTag.duration_since[<time>]>
        // @returns DurationTag
        // @description
        // Returns the DurationTag that passed between the input time and this time.
        // That is, a.duration_since[b] returns (a - b).
        // For example, a time on Monday, .duration_since[a time on Sunday], will return '1d'.
        // -->
        tagProcessor.registerStaticTag(DurationTag.class, "duration_since", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag TimeTag.duration_since[...] must have an input.");
                return null;
            }
            TimeTag toSub = attribute.paramAsType(TimeTag.class);
            return new DurationTag((object.millis() - toSub.millis()) / 1000.0);
        });

        // <--[tag]
        // @attribute <TimeTag.is_after[<time>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if this time object comes after the input time value, or false if it's before (or equal).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_after", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag TimeTag.is_after[...] must have an input.");
                return null;
            }
            TimeTag toCompare = attribute.paramAsType(TimeTag.class);
            return new ElementTag(object.millis() > toCompare.millis());
        });

        // <--[tag]
        // @attribute <TimeTag.is_before[<time>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if this time object comes before the input time value, or false if it's after (or equal).
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "is_before", (attribute, object) -> {
            if (!attribute.hasParam()) {
                attribute.echoError("The tag TimeTag.is_before[...] must have an input.");
                return null;
            }
            TimeTag toCompare = attribute.paramAsType(TimeTag.class);
            return new ElementTag(object.millis() < toCompare.millis());
        });

        // <--[tag]
        // @attribute <TimeTag.format[(<format>)]>
        // @returns ElementTag
        // @description
        // Returns the time, formatted to the date format specification given.
        // If no format input is given, uses "yyyy/MM/dd HH:mm:ss".
        // The optional input to this tag uses semi-standard date format symbols, such as 'yyyy' to mean a 4-digit year or 'MM' to mean a 2-digit month.
        // There are a variety of additional symbols accepted, as listed under "Patterns for Formatting and Parsing" on <@link url https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html>.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "format", (attribute, object) -> {
            return new ElementTag(object.format(attribute.hasParam() ? attribute.getParam() : null));
        });

        tagProcessor.registerTag(DurationTag.class, "duration_compat", (attribute, object) -> {
            Deprecations.timeTagRewrite.warn(attribute.context);
            return new DurationTag(object.millis() / 1000.0);
        }, "in_years", "in_weeks", "in_days", "in_hours", "in_minutes", "in_seconds", "in_milliseconds", "in_ticks");

        tagProcessor.registerTag(TimeTag.class, "time", (attribute, object) -> {
            Deprecations.timeTagRewrite.warn(attribute.context);
            return object;
        });
    }

    public String format() {
        return format(null);
    }

    public String format(String formatText) {
        if (formatText == null) {
            formatText = "yyyy/MM/dd HH:mm:ss";
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern(formatText);
        return instant.format(format);
    }

    public long millis() {
        return instant.toEpochSecond() * 1000 + instant.getNano() / 1_000_000;
    }

    public static ObjectTagProcessor<TimeTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        Debug.echoError("TimeTags can not hold properties.");
    }

    @Override
    public void adjust(Mechanism mechanism) {
        CoreUtilities.autoPropertyMechanism(this, mechanism);
    }
}
