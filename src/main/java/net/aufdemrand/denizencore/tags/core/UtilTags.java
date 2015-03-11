package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.Duration;
import net.aufdemrand.denizencore.objects.Element;
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
import java.util.UUID;

public class UtilTags {

    public UtilTags() {
        TagManager.registerTagEvents(this);
    }

    @TagManager.TagEvents
    public void utilTag(ReplaceableTagEvent event) {
        if (!event.matches("util", "u")) return;

        String type = event.getType() != null ? event.getType() : "";
        String typeContext = event.getTypeContext() != null ? event.getTypeContext() : "";
        String subType = event.getSubType() != null ? event.getSubType() : "";
        String subTypeContext = event.getSubTypeContext() != null ? event.getSubTypeContext().toUpperCase() : "";
        String specifier = event.getSpecifier() != null ? event.getSpecifier() : "";
        String specifierContext = event.getSpecifierContext() != null ? event.getSpecifierContext().toUpperCase() : "";
        Attribute attribute = event.getAttributes().fulfill(1);

        if (type.equalsIgnoreCase("RANDOM")) {

            // <--[tag]
            // @attribute <util.random.int[<#>].to[<#>]>
            // @returns Element(Number)
            // @description
            // Returns a random number between the 2 specified numbers, inclusive.
            // EG, random.int[1].to[3] could return 1, 2, or 3.
            // -->
            if (subType.equalsIgnoreCase("INT")) {
                if (specifier.equalsIgnoreCase("TO")) {
                    if (aH.matchesInteger(subTypeContext) && aH.matchesInteger(specifierContext)) {
                        int min = aH.getIntegerFrom(subTypeContext);
                        int max = aH.getIntegerFrom(specifierContext);

                        // in case the first number is larger than the second, reverse them
                        if (min > max) {
                            int store = min;
                            min = max;
                            max = store;
                        }

                        event.setReplaced(new Element(
                                String.valueOf(CoreUtilities.getRandom().nextInt(max - min + 1) + min))
                                .getAttribute(attribute.fulfill(3)));
                    }
                }
            }

            // <--[tag]
            // @attribute <util.random.decimal>
            // @returns Element
            // @description
            // Returns a random decimal number from 0 to 1
            // -->
            else if (subType.equalsIgnoreCase("DECIMAL"))
                event.setReplaced(new Element(CoreUtilities.getRandom().nextDouble())
                        .getAttribute(attribute.fulfill(2)));

                // <--[tag]
                // @attribute <util.random.gauss>
                // @returns Element
                // @description
                // Returns a random decimal number with a gaussian distribution.
                // 70% of all results will be within the range of -1 to 1.
                // -->
            else if (subType.equalsIgnoreCase("GAUSS"))
                event.setReplaced(new Element(CoreUtilities.getRandom().nextGaussian())
                        .getAttribute(attribute.fulfill(2)));

                // <--[tag]
                // @attribute <util.random.uuid>
                // @returns Element
                // @description
                // Returns a random unique ID.
                // -->
            else if (subType.equalsIgnoreCase("UUID"))
                event.setReplaced(new Element(UUID.randomUUID().toString())
                        .getAttribute(attribute.fulfill(2)));

                // <--[tag]
                // @attribute <util.random.duuid>
                // @returns Element
                // @description
                // Returns a random 'denizen' unique ID, which is made of a randomly generated sentence.
                // -->
            else if (subType.equalsIgnoreCase("DUUID"))
                event.setReplaced(new Element(ScriptQueue
                        .getNextId(event.hasSubTypeContext() ? event.getSubTypeContext() : "DUUID"))
                        .getAttribute(attribute.fulfill(2)));
        }

        // <--[tag]
        // @attribute <util.pi>
        // @returns Element
        // @description
        // Returns PI: 3.141592653589793
        // -->
        else if (type.equalsIgnoreCase("pi")) {
            event.setReplaced(new Element(Math.PI)
                    .getAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.tau>
        // @returns Element
        // @description
        // Returns Tau: 6.283185307179586
        // -->
        else if (type.equalsIgnoreCase("tau")) {
            event.setReplaced(new Element(Math.PI * 2)
                    .getAttribute(attribute.fulfill(1)));
        }

        // <--[tag]
        // @attribute <util.date>
        // @returns Element
        // @description
        // Returns the current system date.
        // -->
        else if (type.equalsIgnoreCase("DATE")) {
            Calendar calendar = Calendar.getInstance();
            Date currentDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat();

            // <--[tag]
            // @attribute <util.date.time>
            // @returns Element
            // @description
            // Returns the current system time.
            // -->
            if (subType.equalsIgnoreCase("TIME")) {

                // <--[tag]
                // @attribute <util.date.time.twentyfour_hour>
                // @returns Element
                // @description
                // Returns the current system time in 24-hour format.
                // -->
                if (specifier.equalsIgnoreCase("TWENTYFOUR_HOUR")) {
                    format.applyPattern("k:mm");
                    event.setReplaced(new Element(format.format(currentDate))
                            .getAttribute(attribute.fulfill(3)));
                }
                // <--[tag]
                // @attribute <util.date.time.year>
                // @returns Element(Number)
                // @description
                // Returns the current year of the system time.
                // -->
                else if (specifier.equalsIgnoreCase("year"))
                    event.setReplaced(new Element(calendar.get(Calendar.YEAR)).getAttribute(attribute.fulfill(3)));
                    // <--[tag]
                    // @attribute <util.date.time.month>
                    // @returns Element(Number)
                    // @description
                    // Returns the current month of the system time.
                    // -->
                else if (specifier.equalsIgnoreCase("month"))
                    event.setReplaced(new Element(calendar.get(Calendar.MONTH) + 1).getAttribute(attribute.fulfill(3)));
                    // <--[tag]
                    // @attribute <util.date.time.day>
                    // @returns Element(Number)
                    // @description
                    // Returns the current day of the system time.
                    // -->
                else if (specifier.equalsIgnoreCase("day"))
                    event.setReplaced(new Element(calendar.get(Calendar.DAY_OF_MONTH)).getAttribute(attribute.fulfill(3)));
                    // <--[tag]
                    // @attribute <util.date.time.hour>
                    // @returns Element(Number)
                    // @description
                    // Returns the current hour of the system time.
                    // -->
                else if (specifier.equalsIgnoreCase("hour"))
                    event.setReplaced(new Element(calendar.get(Calendar.HOUR_OF_DAY)).getAttribute(attribute.fulfill(3)));
                    // <--[tag]
                    // @attribute <util.date.time.minute>
                    // @returns Element(Number)
                    // @description
                    // Returns the current minute of the system time.
                    // -->
                else if (specifier.equalsIgnoreCase("minute"))
                    event.setReplaced(new Element(calendar.get(Calendar.MINUTE)).getAttribute(attribute.fulfill(3)));
                    // <--[tag]
                    // @attribute <util.date.time.second>
                    // @returns Element(Number)
                    // @description
                    // Returns the current second of the system time.
                    // -->
                else if (specifier.equalsIgnoreCase("second"))
                    event.setReplaced(new Element(calendar.get(Calendar.SECOND)).getAttribute(attribute.fulfill(3)));
                    // <--[tag]
                    // @attribute <util.date.time.duration>
                    // @returns Duration
                    // @description
                    // Returns the current system time as a duration.
                    // To get the exact millisecond count, use <@link tag server.current_time_millis>.
                    // -->
                else if (specifier.equalsIgnoreCase("duration"))
                    event.setReplaced(new Duration(System.currentTimeMillis() / 50).getAttribute(attribute.fulfill(3)));
                else {
                    format.applyPattern("K:mm a");
                    event.setReplaced(format.format(currentDate));
                }

            }

            // @description
            // Returns the current system time, formatted as specified
            // Example format: [EEE, MMM d, yyyy K:mm a] will become "Mon, Jan 1, 2112 0:01 AM"
            // -->
            else if (subType.equalsIgnoreCase("FORMAT") && !subTypeContext.equalsIgnoreCase("")) {
                try {
                    format.applyPattern(subTypeContext);
                    event.setReplaced(format.format(currentDate));
                }
                catch (Exception ex) {
                    dB.echoError("Error: invalid pattern '" + subTypeContext + "'");
                    dB.echoError(ex);
                }
            }
            else {
                format.applyPattern("EEE, MMM d, yyyy");
                event.setReplaced(format.format(currentDate));
            }

        }

        // Deprecated
        else if (type.equalsIgnoreCase("AS_ELEMENT")) {
            event.setReplaced(new Element(typeContext).getAttribute(attribute.fulfill(1)));
        }

    }
}
