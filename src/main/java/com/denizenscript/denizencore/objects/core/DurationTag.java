package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;

/**
 * Durations are a convenient way to get a 'unit of time' within Denizen.
 */
public class DurationTag implements ObjectTag {

    // <--[ObjectType]
    // @name DurationTag
    // @prefix d
    // @base ElementTag
    // @ExampleTagBase duration[5m]
    // @ExampleValues 1s,1m,5m,12h
    // @ExampleForReturns
    // - flag server myflag expire:%VALUE%
    // @ExampleForReturns
    // - ratelimit <player> %VALUE%
    // @format
    // The identity format for DurationTags is the number of seconds, followed by an 's'.
    //
    // @description
    // Durations are a unified and convenient way to get a 'unit of time' throughout Denizen.
    // Many commands and features that require a duration can be satisfied by specifying a number and unit of time, especially command arguments that are prefixed 'duration:', etc.
    // The unit of time can be specified by using one of the following:
    // t=ticks (0.05 seconds), s=seconds, m=minutes (60 seconds), h=hours (60 minutes), d=days (24 hours), w=weeks (7 days), y=years (365 days).
    // Not using a unit will imply seconds.
    // Examples: 10s, 50m, 1d, 20.
    //
    // Specifying a range of duration will result in a randomly selected duration that is in between the range specified.
    // The smaller value should be first. Examples: '10s-25s', '1m-2m'.
    //
    // The input of 'instant' or 'infinite' will be interpreted as 0 (for use with commands where instant/infinite logic applies).
    //
    // -->

    @Fetchable("d")
    public static DurationTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }
        string = CoreUtilities.toLowerCase(string);
        if (string.startsWith("d@")) {
            string = string.substring("d@".length());
        }
        if (string.equals("instant") || string.equals("infinite")) {
            return new DurationTag(0);
        }
        if (string.isEmpty()) {
            return null;
        }
        // Pick a duration between a high and low number if there is a '-' present, but it's not "E-2" style scientific notation nor a negative.
        if (string.contains("-") && !string.startsWith("-") && !string.contains("e-")) {
            String[] split = string.split("-", 2);
            if (split.length == 2) {
                DurationTag low = DurationTag.valueOf(split[0], context);
                DurationTag high = DurationTag.valueOf(split[1], context);

                // Make sure 'low' and 'high' returned valid Durations,
                // and that 'low' is less time than 'high'.
                if (low != null && high != null) {
                    if (high.getSecondsAsInt() < low.getSecondsAsInt()) {
                        DurationTag temp = low;
                        low = high;
                        high = temp;
                    }
                    int seconds = CoreUtilities.getRandom()
                            .nextInt((high.getSecondsAsInt() - low.getSecondsAsInt() + 1))
                            + low.getSecondsAsInt();
                    if (CoreConfiguration.debugVerbose) {
                        Debug.log("Getting random duration between " + low.identify()
                                + " and " + high.identify() + "... " + seconds + "s");
                    }

                    return new DurationTag(seconds);

                }
                else {
                    return null;
                }
            }
        }
        String numericString = Character.isDigit(string.charAt(string.length() - 1)) ? string : string.substring(0, string.length() - 1);
        // Standard DurationTag. Check the type and create new DurationTag object accordingly.
        try {
            double numVal = Double.parseDouble(numericString);
            if (string.endsWith("y")) {
                return new DurationTag(numVal * (60 * 60 * 24 * 365));
            }
            else if (string.endsWith("w")) {
                return new DurationTag(numVal * (60 * 60 * 24 * 7));
            }
            else if (string.endsWith("d")) {
                return new DurationTag(numVal * (60 * 60 * 24));
            }
            else if (string.endsWith("h")) {
                return new DurationTag(numVal * (60 * 60));
            }
            else if (string.endsWith("m")) {
                return new DurationTag(numVal * 60);
            }
            else if (string.endsWith("s")) {
                // seconds
                return new DurationTag(numVal);
            }
            else if (string.endsWith("t")) {
                return new DurationTag(numVal * 0.05);
            }
            else if (numericString.equals(string)) {
                // seconds
                return new DurationTag(Double.parseDouble(numericString));
            }
            else {
                // Invalid.
                if (context == null || context.showErrors()) {
                    Debug.echoError("Duration type '" + string + "' is not valid.");
                }
                return null;
            }
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks to see if the string is a valid DurationTag.
     *
     * @param string the String to match.
     * @return true if valid.
     */
    public static boolean matches(String string) {
        try {
            return valueOf(string, CoreUtilities.noDebugContext) != null;
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * Creates a duration object when given number of seconds.
     *
     * @param seconds the number of seconds.
     */
    public DurationTag(double seconds) {
        this.seconds = seconds;
    }

    /**
     * Creates a duration object when given number of seconds.
     *
     * @param seconds the number of seconds.
     */
    public DurationTag(int seconds) {
        this.seconds = seconds;
    }

    /**
     * Creates a duration object when given number of Bukkit ticks.
     *
     * @param ticks the number of ticks.
     */
    public DurationTag(long ticks) {
        this.seconds = ticks / 20.0;
    }

    /////////////////////
    //   INSTANCE FIELDS/METHODS
    /////////////////

    // The amount of seconds in the duration.
    private double seconds;

    // Duration's default ObjectTag prefix.
    private String prefix = "Duration";

    /**
     * Gets the number of ticks of this duration. There are 20 ticks
     * per second.
     *
     * @return the number of ticks.
     */
    public long getTicks() {
        return (long) (seconds * 20);
    }

    /**
     * Gets the number of ticks of this duration as an integer. There are
     * 20 per second.
     *
     * @return the number of ticks.
     */
    public int getTicksAsInt() {
        return (int) (seconds * 20);
    }

    /**
     * Gets the number of milliseconds in this duration.
     *
     * @return the number of milliseconds.
     */
    public long getMillis() {
        double millis = seconds * 1000;
        return (long) millis;
    }

    /**
     * Gets the number of seconds of this duration.
     *
     * @return number of seconds
     */
    public double getSeconds() {
        return seconds;
    }

    /**
     * Gets the number of seconds as an integer value of the duration.
     *
     * @return number of seconds rounded to the nearest second
     */
    public int getSecondsAsInt() {
        // Durations that are a fraction of a second
        // will return as 1 when using this method.
        if (seconds < 1 && seconds > 0) {
            return 1;
        }
        return round(seconds);
    }

    private int round(double d) {
        double dAbs = Math.abs(d);
        int i = (int) dAbs;
        double result = dAbs - i;
        if (result < 0.5) {
            return d < 0 ? -i : i;
        }
        else {
            return d < 0 ? -(i + 1) : i + 1;
        }
    }

    /////////////////////
    //   ObjectTag Methods
    /////////////////

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String debuggable() {
        return identify() + " <GR>(" + formatted(false) + ")";
    }

    @Override
    public boolean isUnique() {
        // Durations are not unique, cannot be saved or persisted.
        return false;
    }

    @Override
    public String identify() {
        if (seconds == (int)seconds) {
            if (seconds == 0) {
                return "d@0s";
            }
            double minutes = seconds / 60;
            double hours = minutes / 60;
            double days = hours / 24;
            if (days == (int)days) {
                return "d@" + (int)days + "d";
            }
            if (hours == (int)hours) {
                return "d@" + (int)hours + "h";
            }
            if (minutes == (int)minutes) {
                return "d@" + (int)minutes + "m";
            }
            return "d@" + (int)seconds + "s";
        }
        return "d@" + seconds + "s";
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
    public ObjectTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public Object getJavaObject() {
        return getSeconds();
    }

    public static void registerTags() {

        /////////////////////
        //   CONVERSION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <DurationTag.in_years>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the number of years in the duration.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "in_years", (attribute, object) -> {
            return new ElementTag(object.seconds / (86400 * 365));
        }, "years");

        // <--[tag]
        // @attribute <DurationTag.in_weeks>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the number of weeks in the duration.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "in_weeks", (attribute, object) -> {
            return new ElementTag(object.seconds / 604800);
        }, "weeks");

        // <--[tag]
        // @attribute <DurationTag.in_days>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the number of days in the duration.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "in_days", (attribute, object) -> {
            return new ElementTag(object.seconds / 86400);
        }, "days");

        // <--[tag]
        // @attribute <DurationTag.in_hours>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the number of hours in the duration.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "in_hours", (attribute, object) -> {
            return new ElementTag(object.seconds / 3600);
        }, "hours");

        // <--[tag]
        // @attribute <DurationTag.in_minutes>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the number of minutes in the duration.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "in_minutes", (attribute, object) -> {
            return new ElementTag(object.seconds / 60);
        }, "minutes");

        // <--[tag]
        // @attribute <DurationTag.in_seconds>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the number of seconds in the duration.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "in_seconds", (attribute, object) -> {
            return new ElementTag(object.seconds);
        }, "seconds");

        // <--[tag]
        // @attribute <DurationTag.in_milliseconds>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the number of milliseconds in the duration.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "in_milliseconds", (attribute, object) -> {
            return new ElementTag(object.seconds * 1000);
        }, "milliseconds");

        // <--[tag]
        // @attribute <DurationTag.in_ticks>
        // @returns ElementTag(Number)
        // @description
        // Returns the number of ticks in the duration. (20t/second)
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "in_ticks", (attribute, object) -> {
            return new ElementTag((long) (object.seconds * 20L));
        }, "ticks");

        // <--[tag]
        // @attribute <DurationTag.sub[<duration>]>
        // @returns DurationTag
        // @description
        // Returns this duration minus another.
        // -->
        tagProcessor.registerStaticTag(DurationTag.class, DurationTag.class, "sub", (attribute, object, secondVal) -> {
            return new DurationTag(object.getTicks() - secondVal.getTicks());
        });

        // <--[tag]
        // @attribute <DurationTag.add[<duration>]>
        // @returns DurationTag
        // @description
        // Returns this duration plus another.
        // -->
        tagProcessor.registerStaticTag(DurationTag.class, DurationTag.class, "add", (attribute, object, secondVal) -> {
            return new DurationTag(object.getTicks() + secondVal.getTicks());
        });

        // <--[tag]
        // @attribute <DurationTag.is_more_than[<duration>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether this duration is greater than the input duration.
        // Equivalent to if comparison: >
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, DurationTag.class, "is_more_than", (attribute, object, secondVal) -> {
            return new ElementTag(object.seconds > secondVal.seconds);
        });

        // <--[tag]
        // @attribute <DurationTag.is_less_than[<duration>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether this duration is less than the input duration.
        // Equivalent to if comparison: <
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, DurationTag.class, "is_less_than", (attribute, object, secondVal) -> {
            return new ElementTag(object.seconds < secondVal.seconds);
        });

        // <--[tag]
        // @attribute <DurationTag.is_more_than_or_equal_to[<duration>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether this duration is greater than or equal to the input duration.
        // Equivalent to if comparison: >=
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, DurationTag.class, "is_more_than_or_equal_to", (attribute, object, secondVal) -> {
            return new ElementTag(object.seconds >= secondVal.seconds);
        });

        // <--[tag]
        // @attribute <DurationTag.is_less_than_or_equal_to[<duration>]>
        // @returns ElementTag(Boolean)
        // @group comparison
        // @description
        // Returns whether this duration is less than or equal to the input duration.
        // Equivalent to if comparison: <=
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, DurationTag.class, "is_less_than_or_equal_to", (attribute, object, secondVal) -> {
            return new ElementTag(object.seconds <= secondVal.seconds);
        });

        tagProcessor.registerTag(TimeTag.class, "time", (attribute, object) -> {
            Deprecations.timeTagRewrite.warn(attribute.context);
            return new TimeTag(object.getMillis());
        });

        /////////////////////
        //   FORMAT ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <DurationTag.formatted>
        // @returns ElementTag
        // @description
        // Returns the value of the duration in an easily readable format like 2h 30m,
        // where minutes are only shown if there is less than a day left and seconds are only shown if there are less than 10 minutes left.
        // Will show seconds, minutes, hours, days, and/or years.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "formatted", (attribute, object) -> {
            return new ElementTag(object.formatted(false));
        }, "value");

        // <--[tag]
        // @attribute <DurationTag.formatted_words>
        // @returns ElementTag
        // @description
        // Returns the value of the duration in an easily readable format like "2 hours 30 minutes",
        // where minutes are only shown if there is less than a day left and seconds are only shown if there are less than 10 minutes left.
        // Will show seconds, minutes, hours, days, and/or years.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "formatted_words", (attribute, object) -> {
            return new ElementTag(object.formatted(true));
        });
    }

    public static ObjectTagProcessor<DurationTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    private static String autoS(long val) {
        return val == 1 ? " " : "s ";
    }

    public String formatted(boolean words) {
        double secondsCopy = seconds;
        boolean isNegative = secondsCopy < 0;
        if (isNegative) {
            secondsCopy *= -1;
        }
        // Make sure you don't change these longs into doubles
        // and break the code
        long seconds = (long) secondsCopy;
        long yearsRaw = seconds / (60 * 60 * 24 * 365);
        long daysRaw = seconds / (60 * 60 * 24);
        long hoursRaw = seconds / (60 * 60);
        long minutesRaw = seconds / (60);
        long years = yearsRaw;
        long days = daysRaw - (years * 365);
        long hours = hoursRaw - (daysRaw * 24);
        long minutes = minutesRaw - (hours * 60);
        seconds -= minutesRaw * 60;
        String timeString = "";
        if (years > 0) {
            timeString = years + (words ? " year" + autoS(years) :  "y ");
        }
        if (days > 0) {
            timeString += days + (words ? " day" + autoS(days) :  "d ");
        }
        if (hours > 0 && years == 0) {
            timeString += hours + (words ? " hour" + autoS(hours) :  "h ");
        }
        if (minutes > 0 && days == 0 && years == 0) {
            timeString += minutes + (words ? " minute" + autoS(minutes) :  "m ");
        }
        if (seconds > 0 && minutes < 10 && hours == 0 && days == 0 && years == 0) {
            timeString += seconds + (words ? " second" + autoS(seconds) :  "s ");
        }
        if (timeString.isEmpty()) {
            if (secondsCopy == 0) {
                timeString = "forever";
            }
            else {
                timeString = ((double) ((long) (secondsCopy * 100)) / 100d) + "s";
            }
        }
        return (isNegative ? "negative " : "") + timeString.trim();
    }
}
