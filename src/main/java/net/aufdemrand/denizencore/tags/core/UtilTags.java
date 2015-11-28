package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.objects.Duration;
import net.aufdemrand.denizencore.objects.Element;
import net.aufdemrand.denizencore.objects.Mechanism;
import net.aufdemrand.denizencore.objects.aH;
import net.aufdemrand.denizencore.scripts.queues.ScriptQueue;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

public class UtilTags {

    public UtilTags() {
        TagManager.registerTagEvents(this);
    }

    @TagManager.TagEvents
    public void utilTag(ReplaceableTagEvent event) {
        if (!event.matches("util", "u")) return;

        Attribute attribute = event.getAttributes().fulfill(1);

        if (attribute.startsWith("random")) {

            attribute = attribute.fulfill(1);

            // <--[tag]
            // @attribute <util.random.int[<#>].to[<#>]>
            // @returns Element(Number)
            // @description
            // Returns a random number between the 2 specified numbers, inclusive.
            // For example: random.int[1].to[3] could return 1, 2, or 3.
            // -->
            if (attribute.startsWith("int")) {
                String stc = attribute.getContext(1);
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("to")) {
                    if (aH.matchesInteger(stc) && aH.matchesInteger(attribute.getContext(1))) {
                        int min = aH.getIntegerFrom(stc);
                        int max = aH.getIntegerFrom(attribute.getContext(1));

                        // in case the first number is larger than the second, reverse them
                        if (min > max) {
                            int store = min;
                            min = max;
                            max = store;
                        }

                        event.setReplaced(new Element(
                                String.valueOf(CoreUtilities.getRandom().nextInt(max - min + 1) + min))
                                .getAttribute(attribute.fulfill(1)));
                    }
                }
            }

            // <--[tag]
            // @attribute <util.random.decimal[<#>].to[<#>]>
            // @returns Element(Decimal)
            // @description
            // Returns a random number between the 2 specified numbers, inclusive.
            // For example: random.int[1].to[3] could return 1, 2, or 3.
            // -->
            if (attribute.startsWith("decimal")
                    && attribute.hasContext(1)) {
                String stc = attribute.getContext(1);
                attribute = attribute.fulfill(1);
                if (attribute.startsWith("to")) {
                    if (aH.matchesDouble(stc) && aH.matchesDouble(attribute.getContext(1))) {
                        double min = aH.getDoubleFrom(stc);
                        double max = aH.getDoubleFrom(attribute.getContext(1));

                        // in case the first number is larger than the second, reverse them
                        if (min > max) {
                            double store = min;
                            min = max;
                            max = store;
                        }

                        event.setReplaced(new Element(
                                String.valueOf(CoreUtilities.getRandom().nextDouble() * (max - min) + min))
                                .getAttribute(attribute.fulfill(1)));
                    }
                }
            }

            // <--[tag]
            // @attribute <util.random.decimal>
            // @returns Element
            // @description
            // Returns a random decimal number from 0 to 1
            // -->
            else if (attribute.startsWith("decimal")) {
                event.setReplaced(new Element(CoreUtilities.getRandom().nextDouble())
                        .getAttribute(attribute.fulfill(1)));
            }

            // <--[tag]
            // @attribute <util.random.gauss>
            // @returns Element
            // @description
            // Returns a random decimal number with a gaussian distribution.
            // 70% of all results will be within the range of -1 to 1.
            // -->
            else if (attribute.startsWith("gauss")) {
                event.setReplaced(new Element(CoreUtilities.getRandom().nextGaussian())
                        .getAttribute(attribute.fulfill(1)));
            }

            // <--[tag]
            // @attribute <util.random.uuid>
            // @returns Element
            // @description
            // Returns a random unique ID.
            // -->
            else if (attribute.startsWith("uuid")) {
                event.setReplaced(new Element(UUID.randomUUID().toString())
                        .getAttribute(attribute.fulfill(1)));
            }

            // <--[tag]
            // @attribute <util.random.duuid>
            // @returns Element
            // @description
            // Returns a random 'denizen' unique ID, which is made of a randomly generated sentence.
            // -->
            else if (attribute.startsWith("duuid")) {
                event.setReplaced(new Element(ScriptQueue
                        .getNextId(attribute.hasContext(1) ? attribute.getContext(1) : "DUUID"))
                        .getAttribute(attribute.fulfill(1)));
            }
        }

        // <--[tag]
        // @attribute <util.pi>
        // @returns Element
        // @description
        // Returns PI: 3.141592653589793
        // -->
        else if (attribute.startsWith("pi")) {
            event.setReplaced(new Element(Math.PI)
                    .getAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.tau>
        // @returns Element
        // @description
        // Returns Tau: 6.283185307179586
        // -->
        else if (attribute.startsWith("tau")) {
            event.setReplaced(new Element(Math.PI * 2)
                    .getAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.date>
        // @returns Element
        // @description
        // Returns the current system date.
        // -->
        else if (attribute.startsWith("date")) {
            Calendar calendar = Calendar.getInstance();
            Date currentDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat();

            attribute = attribute.fulfill(1);

            // <--[tag]
            // @attribute <util.date.time>
            // @returns Element
            // @description
            // Returns the current system time.
            // -->
            if (attribute.startsWith("time")) {

                attribute = attribute.fulfill(1);

                // <--[tag]
                // @attribute <util.date.time.twentyfour_hour>
                // @returns Element
                // @description
                // Returns the current system time in 24-hour format.
                // -->
                if (attribute.startsWith("twentyfour_hour")) {
                    format.applyPattern("k:mm");
                    event.setReplaced(new Element(format.format(currentDate))
                            .getAttribute(attribute.fulfill(1)));
                }
                // <--[tag]
                // @attribute <util.date.time.year>
                // @returns Element(Number)
                // @description
                // Returns the current year of the system time.
                // -->
                else if (attribute.startsWith("year")) {
                    event.setReplaced(new Element(calendar.get(Calendar.YEAR))
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.month>
                // @returns Element(Number)
                // @description
                // Returns the current month of the system time.
                // -->
                else if (attribute.startsWith("month")) {
                    event.setReplaced(new Element(calendar.get(Calendar.MONTH) + 1)
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.week>
                // @returns Element(Number)
                // @description
                // Returns the current week of the system time.
                // -->
                else if (attribute.startsWith("week")) {
                    event.setReplaced(new Element(calendar.get(Calendar.WEEK_OF_YEAR) + 1)
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.day>
                // @returns Element(Number)
                // @description
                // Returns the current day of the system time.
                // -->
                else if (attribute.startsWith("day")) {
                    event.setReplaced(new Element(calendar.get(Calendar.DAY_OF_MONTH))
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.hour>
                // @returns Element(Number)
                // @description
                // Returns the current hour of the system time.
                // -->
                else if (attribute.startsWith("hour")) {
                    event.setReplaced(new Element(calendar.get(Calendar.HOUR_OF_DAY))
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.minute>
                // @returns Element(Number)
                // @description
                // Returns the current minute of the system time.
                // -->
                else if (attribute.startsWith("minute")) {
                    event.setReplaced(new Element(calendar.get(Calendar.MINUTE))
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.second>
                // @returns Element(Number)
                // @description
                // Returns the current second of the system time.
                // -->
                else if (attribute.startsWith("second")) {
                    event.setReplaced(new Element(calendar.get(Calendar.SECOND))
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.duration>
                // @returns Duration
                // @description
                // Returns the current system time as a duration.
                // To get the exact millisecond count, use <@link tag server.current_time_millis>.
                // -->
                else if (attribute.startsWith("duration")) {
                    event.setReplaced(new Duration(System.currentTimeMillis() / 50)
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.zone>
                // @returns Element
                // @description
                // Returns the abbreviated timezone of the server.
                // -->
                else if (attribute.startsWith("zone")) {
                    TimeZone tz = Calendar.getInstance().getTimeZone();
                    event.setReplaced(new Element(tz.getDisplayName(tz.inDaylightTime(currentDate), TimeZone.SHORT))
                            .getAttribute(attribute.fulfill(1)));
                }

                // <--[tag]
                // @attribute <util.date.time.formatted_zone>
                // @returns Element
                // @description
                // Returns the timezone of the server.
                // -->
                else if (attribute.startsWith("formatted_zone")) {
                    TimeZone tz = Calendar.getInstance().getTimeZone();
                    event.setReplaced(new Element(tz.getDisplayName(tz.inDaylightTime(currentDate), TimeZone.LONG))
                            .getAttribute(attribute.fulfill(1)));
                }

                else {
                    format.applyPattern("K:mm a");
                    event.setReplaced(new Element(format.format(currentDate))
                            .getAttribute(attribute));
                }

            }

            // @description
            // Returns the current system time, formatted as specified
            // Example format: [EEE, MMM d, yyyy K:mm a] will become "Mon, Jan 1, 2112 0:01 AM"
            // -->
            else if (attribute.startsWith("format")
                    && attribute.hasContext(1)) {
                try {
                    format.applyPattern(attribute.getContext(1));
                    event.setReplaced(format.format(currentDate));
                }
                catch (Exception ex) {
                    dB.echoError("Error: invalid pattern '" + attribute.getContext(1) + "'");
                    dB.echoError(ex);
                }
            }
            else {
                format.applyPattern("EEE, MMM d, yyyy");
                event.setReplaced(new Element(format.format(currentDate))
                        .getAttribute(attribute));
            }
        }
    }


    public static void adjustSystem(Mechanism mechanism) {
        Element value = mechanism.getValue();

        // <--[mechanism]
        // @object system
        // @name redirect_logging
        // @input Element(Boolean)
        // @description
        // Tells the server to redirect logging to a world event or not.
        // Note that this redirects *all console output* not just Denizen output.
        // Note: don't enable /denizen debug -e while this is active.
        // @tags
        // None
        // -->
        if (mechanism.matches("redirect_logging") && mechanism.hasValue()) {
            if (!DenizenCore.getImplementation().allowConsoleRedirection()) {
                dB.echoError("Console redirection disabled by administrator.");
                return;
            }
            if (mechanism.getValue().asBoolean()) {
                DenizenCore.logInterceptor.redirectOutput();
            }
            else {
                DenizenCore.logInterceptor.standardOutput();
            }
        }
    }
}
