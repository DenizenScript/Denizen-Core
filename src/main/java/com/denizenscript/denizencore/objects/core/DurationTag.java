package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Durations are a convenient way to get a 'unit of time' within Denizen.
 */
public class DurationTag implements ObjectTag {

    // <--[language]
    // @name Duration
    // @group Object System
    // @description
    // Durations are a unified and convenient way to get a 'unit of time' throughout Denizen.
    // Many commands and features that require a duration can be satisfied by specifying a number
    // and unit of time, especially command arguments that are prefixed 'duration:', etc. The d@
    // object fetcher notation can also be used, and is encouraged. The unit of time can be specified
    // by using one of the following: T=ticks, M=minutes, S=seconds, H=hours, D=days, W=Weeks.
    // Not using a unit will imply seconds. Examples: d@10s, d@50m, d@1d, d@20.
    //
    // Specifying a range of duration will result in a randomly selected duration that is
    // in between the range specified. The smaller value should be first. Examples:
    // d@10s-25s, d@1m-2m.
    //
    // For format info, see <@link language d@>
    // -->


    /////////////////////
    //   STATIC METHODS AND FIELDS
    /////////////////

    // Define a 'ZERO' Duration
    final public static DurationTag ZERO = new DurationTag(0);


    /////////////////////
    //   OBJECT FETCHER
    /////////////////

    // <--[language]
    // @name d@
    // @group Object Fetcher System
    // @description
    // d@ refers to the 'object identifier' of a 'Duration'. The 'd@' is notation for Denizen's Object
    // Fetcher. Durations must be a positive number or range of numbers followed optionally by
    // a unit of time, and prefixed by d@. Examples: d@3s, d@1d, d@10s-20s.
    //
    // For general info, see <@link language Duration>
    // -->

    public static DurationTag valueOf(String string) {
        return valueOf(string, null);
    }

    @Fetchable("d")
    public static DurationTag valueOf(String string, TagContext context) {
        if (string == null) {
            return null;
        }

        string = CoreUtilities.toLowerCase(string.replace("d@", ""));

        if (string.equals("instant")) {
            return new DurationTag(0);
        }

        // Pick a duration between a high and low number if there is a '-' present, but it's not "E-2" style scientific notation.
        if (string.contains("-") && !string.contains("e-")) {
            String[] split = string.split("-", 2);
            if (split.length == 2) {
                DurationTag low = DurationTag.valueOf(split[0]);
                DurationTag high = DurationTag.valueOf(split[1]);

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
                    if (Debug.verbose) {
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

        // Standard Duration. Check the type and create new DurationTag object accordingly.
        try {
            if (string.endsWith("t")) {
                // Matches TICKS, so 1 tick = .05 seconds
                return new DurationTag(Double.valueOf(numericString) * 0.05);
            }
            else if (string.endsWith("d")) {
                // Matches DAYS, so 1 day = 86400 seconds
                return new DurationTag(Double.valueOf(numericString) * 86400);
            }
            else if (string.endsWith("w")) {
                // Matches WEEKS, so 1 week = 604800 seconds
                return new DurationTag(Double.valueOf(numericString) * 604800);
            }
            else if (string.endsWith("m")) {
                // Matches MINUTES, so 1 minute = 60 seconds
                return new DurationTag(Double.valueOf(numericString) * 60);
            }
            else if (string.endsWith("h")) {
                // Matches HOURS, so 1 hour = 3600 seconds
                return new DurationTag(Double.valueOf(numericString) * 3600);
            }
            else if (string.endsWith("s")) {
                // seconds
                return new DurationTag(Double.valueOf(numericString));
            }
            else if (numericString.equals(string)) {
                // seconds
                return new DurationTag(Double.valueOf(numericString));
            }
            else {
                // Invalid.
                if (context == null || context.debug) {
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
     * Checks to see if the string is a valid Duration.
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


    /////////////////////
    //   CONSTRUCTORS
    /////////////////

    /**
     * Creates a duration object when given number of seconds.
     *
     * @param seconds the number of seconds.
     */
    public DurationTag(double seconds) {
        this.seconds = seconds;
        if (this.seconds < 0) {
            this.seconds = 0;
        }
    }

    /**
     * Creates a duration object when given number of seconds.
     *
     * @param seconds the number of seconds.
     */
    public DurationTag(int seconds) {
        this.seconds = seconds;
        if (this.seconds < 0) {
            this.seconds = 0;
        }
    }

    /**
     * Creates a duration object when given number of Bukkit ticks.
     *
     * @param ticks the number of ticks.
     */
    public DurationTag(long ticks) {
        this.seconds = ticks / 20.0;
        if (this.seconds < 0) {
            this.seconds = 0;
        }
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
        Double millis = seconds * 1000;
        return millis.longValue();
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
        return "d@" + seconds + "s <GR>(" + formatted() + ")";
    }

    @Override
    public boolean isUnique() {
        // Durations are not unique, cannot be saved or persisted.
        return false;
    }

    @Override
    public String getObjectType() {
        return "duration";
    }

    @Override
    public String identify() {
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

    public static void registerTags() {

        /////////////////////
        //   CONVERSION ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <DurationTag.in_years>
        // @returns ElementTag(Decimal)
        // @description
        // returns the number of years in the Duration.
        // -->
        registerTag("in_years", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag(object.seconds / (86400 * 365));
            }
        });
        registerTag("years", tagProcessor.registeredObjectTags.get("in_years"));

        // <--[tag]
        // @attribute <DurationTag.in_weeks>
        // @returns ElementTag(Decimal)
        // @description
        // returns the number of weeks in the Duration.
        // -->
        registerTag("in_weeks", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag(object.seconds / 604800);
            }
        });
        registerTag("weeks", tagProcessor.registeredObjectTags.get("in_weeks"));

        // <--[tag]
        // @attribute <DurationTag.in_days>
        // @returns ElementTag(Decimal)
        // @description
        // returns the number of days in the Duration.
        // -->
        registerTag("in_days", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag(object.seconds / 86400);
            }
        });
        registerTag("days", tagProcessor.registeredObjectTags.get("in_days"));

        // <--[tag]
        // @attribute <DurationTag.in_hours>
        // @returns ElementTag(Decimal)
        // @description
        // returns the number of hours in the Duration.
        // -->
        registerTag("in_hours", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag(object.seconds / 3600);
            }
        });
        registerTag("hours", tagProcessor.registeredObjectTags.get("in_hours"));

        // <--[tag]
        // @attribute <DurationTag.in_minutes>
        // @returns ElementTag(Decimal)
        // @description
        // returns the number of minutes in the Duration.
        // -->
        registerTag("in_minutes", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag(object.seconds / 60);
            }
        });
        registerTag("minutes", tagProcessor.registeredObjectTags.get("in_minutes"));

        // <--[tag]
        // @attribute <DurationTag.in_seconds>
        // @returns ElementTag(Decimal)
        // @description
        // returns the number of seconds in the Duration.
        // -->
        registerTag("in_seconds", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag(object.seconds);
            }
        });
        registerTag("seconds", tagProcessor.registeredObjectTags.get("in_seconds"));

        // <--[tag]
        // @attribute <DurationTag.in_milliseconds>
        // @returns ElementTag(Decimal)
        // @description
        // returns the number of milliseconds in the Duration.
        // -->
        registerTag("in_milliseconds", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag(object.seconds * 1000);
            }
        });
        registerTag("milliseconds", tagProcessor.registeredObjectTags.get("in_milliseconds"));

        // <--[tag]
        // @attribute <DurationTag.in_ticks>
        // @returns ElementTag(Number)
        // @description
        // returns the number of ticks in the Duration. (20t/second)
        // -->
        registerTag("in_ticks", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag((long) (object.seconds * 20L));
            }
        });
        registerTag("ticks", tagProcessor.registeredObjectTags.get("in_ticks"));

        // <--[tag]
        // @attribute <DurationTag.sub[<duration>]>
        // @returns ElementTag(Number)
        // @description
        // returns this duration minus another.
        // -->
        registerTag("sub", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag DurationTag.sub[...] must have a value.");
                    return null;
                }
                return new DurationTag(object.getTicks() - DurationTag.valueOf(attribute.getContext(1)).getTicks());
            }
        });

        // <--[tag]
        // @attribute <DurationTag.add[<duration>]>
        // @returns ElementTag(Number)
        // @description
        // returns this duration plus another.
        // -->
        registerTag("add", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                if (!attribute.hasContext(1)) {
                    Debug.echoError("The tag DurationTag.add[...] must have a value.");
                    return null;
                }
                return new DurationTag(object.getTicks() + DurationTag.valueOf(attribute.getContext(1)).getTicks());
            }
        });

        // <--[tag]
        // @attribute <DurationTag.time>
        // @returns ElementTag
        // @description
        // returns the date-time specified by the duration object.
        // -->
        registerTag("time", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                Date currentDate = new Date(object.getTicks() * 50);
                SimpleDateFormat format = new SimpleDateFormat();

                // <--[tag]
                // @attribute <DurationTag.time.year>
                // @returns ElementTag(Number)
                // @description
                // Returns the current year of the time specified by the duration object.
                // -->
                if (attribute.startsWith("year", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(currentDate.getYear() + 1900 /* ??? */);
                }

                // <--[tag]
                // @attribute <DurationTag.time.month>
                // @returns ElementTag(Number)
                // @description
                // Returns the current month of the time specified by the duration object.
                // -->
                else if (attribute.startsWith("month", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(currentDate.getMonth() + 1);
                }

                // <--[tag]
                // @attribute <DurationTag.time.day>
                // @returns ElementTag(Number)
                // @description
                // Returns the current day of the time specified by the duration object.
                // -->
                else if (attribute.startsWith("day", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(currentDate.getDate());
                }

                // <--[tag]
                // @attribute <DurationTag.time.day_of_week>
                // @returns ElementTag(Number)
                // @description
                // Returns the current day-of-the-week of the time specified by the duration object.
                // -->
                else if (attribute.startsWith("day_of_week", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(currentDate.getDay());
                }

                // <--[tag]
                // @attribute <DurationTag.time.hour>
                // @returns ElementTag(Number)
                // @description
                // Returns the current hour of the time specified by the duration object.
                // -->
                else if (attribute.startsWith("hour", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(currentDate.getHours());
                }

                // <--[tag]
                // @attribute <DurationTag.time.minute>
                // @returns ElementTag(Number)
                // @description
                // Returns the current minute of the time specified by the duration object.
                // -->
                else if (attribute.startsWith("minute", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(currentDate.getMinutes());
                }

                // <--[tag]
                // @attribute <DurationTag.time.second>
                // @returns ElementTag(Number)
                // @description
                // Returns the current second of the time specified by the duration object.
                // -->
                else if (attribute.startsWith("second", 2)) {
                    attribute.fulfill(1);
                    return new ElementTag(currentDate.getSeconds());
                }

                // TODO: Custom format option
                else {
                    format.applyPattern("EEE, d MMM yyyy HH:mm:ss");
                    return new ElementTag(format.format(currentDate));
                }
            }
        });

        // <--[tag]
        // @attribute <DurationTag.type>
        // @returns ElementTag
        // @description
        // Always returns 'Duration' for DurationTag objects. All objects fetchable by the Object Fetcher will return the
        // type of object that is fulfilling this attribute.
        // -->
        registerTag("type", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag("Duration");
            }
        });

        /////////////////////
        //   FORMAT ATTRIBUTES
        /////////////////

        // <--[tag]
        // @attribute <DurationTag.formatted>
        // @returns ElementTag
        // @description
        // returns the value of the duration in an easily readable
        // format like 2h 30m, where minutes are only shown if there
        // is less than a day left and seconds are only shown if
        // there are less than 10 minutes left.
        // -->
        registerTag("formatted", new TagRunnable.ObjectForm<DurationTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DurationTag object) {
                return new ElementTag(object.formatted());
            }
        });
        registerTag("value", tagProcessor.registeredObjectTags.get("formatted"));

    }

    public static ObjectTagProcessor<DurationTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectForm<DurationTag> runnable) {
        tagProcessor.registerTag(name, runnable);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    public String formatted() {
        // Make sure you don't change these longs into doubles
        // and break the code

        long seconds = (long) this.seconds;
        long days = seconds / 86400;
        long hours = (seconds - days * 86400) / 3600;
        long minutes = (seconds - days * 86400 - hours * 3600) / 60;
        seconds = seconds - days * 86400 - hours * 3600 - minutes * 60;

        String timeString = "";

        if (days > 0) {
            timeString = String.valueOf(days) + "d ";
        }
        if (hours > 0) {
            timeString = timeString + String.valueOf(hours) + "h ";
        }
        if (minutes > 0 && days == 0) {
            timeString = timeString + String.valueOf(minutes) + "m ";
        }
        if (seconds > 0 && minutes < 10 && hours == 0 && days == 0) {
            timeString = timeString + String.valueOf(seconds) + "s";
        }

        if (timeString.isEmpty()) {
            if (this.seconds <= 0) {
                timeString = "forever";
            }
            else {
                timeString = ((double) ((long) (this.seconds * 100)) / 100d) + "s";
            }
        }

        return timeString.trim();
    }
}
