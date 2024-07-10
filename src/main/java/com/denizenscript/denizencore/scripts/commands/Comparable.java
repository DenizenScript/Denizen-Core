package com.denizenscript.denizencore.scripts.commands;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.core.ListTag;

import java.math.BigDecimal;

public class Comparable {

    // <--[language]
    // @name Operator
    // @group Comparables
    // @description
    // An operator is a tool for comparing values, used by commands like <@link command if>, <@link command while>, <@link command waituntil>, ... and tags like <@link tag ObjectTag.is.than>
    //
    // Available Operators include:
    // "Equals" is written as "==" or "equals".
    // "Does not equal" is written as "!=".
    // "Is more than" is written as ">" or "more".
    // "Is less than" is written as "<" or "less".
    // "Is more than or equal to" is written as ">=" or "or_more".
    // "Is less than or equal to" is written as "<=" or "or_less".
    // "does this list or map contain" is written as "contains". For example, "- if a|b|c contains b:" or "- if [a=1;b=2] contains b:"
    // "is this in the list or map" is written as "in". For example, "- if b in a|b|c:", or "- if [a=1;b=2] contains b:"
    // "does this object or text match an advanced matcher" is written as "matches". For example, "- if <player.location.below> matches stone:"
    //
    // Note: When using an operator in a tag,
    // keep in mind that < and >, and even >= and <= must be either escaped, or referred to by name.
    // Example: "<player.health.is[<&lt>].than[10]>" or "<player.health.is[less].than[10]>",
    // but <player.health.is[<].than[10]> will produce undesired results. <>'s must be escaped or replaced since
    // they are normally notation for a replaceable tag. Escaping is not necessary when the argument
    // contains no replaceable tags.
    //
    // There are also special boolean operators (&&, ||, ...) documented at: <@link command if>
    //
    // -->

    public enum Operator {
        EQUALS, OR_MORE, OR_LESS, MORE, LESS, CONTAINS, IN, MATCHES
    }

    public static Operator getOperatorFor(String text) {
        switch (CoreUtilities.toLowerCase(text)) {
            case "equals":
            case "==":
            case "=":
                return Operator.EQUALS;
            case "or_more":
            case ">=":
                return Operator.OR_MORE;
            case "or_less":
            case "<=":
                return Operator.OR_LESS;
            case "more":
            case ">":
                return Operator.MORE;
            case "less":
            case "<":
                return Operator.LESS;
            case "contains":
                return Operator.CONTAINS;
            case "in":
                return Operator.IN;
            case "matches":
                return Operator.MATCHES;
            default:
                return null;
        }
    }

    private static boolean compareDecimal(ObjectTag objA, ObjectTag objB, Operator operator, TagContext context) {
        try {
            BigDecimal bigDecA = objA.asElement().asBigDecimal();
            BigDecimal bigDecB = objB.asElement().asBigDecimal();
            int compared = bigDecA.compareTo(bigDecB);
            switch (operator) {
                case LESS:
                    return compared < 0;
                case MORE:
                    return compared > 0;
                case OR_LESS:
                    return compared <= 0;
                case OR_MORE:
                    return compared >= 0;
            }
        }
        catch (NumberFormatException ex) {
            if (context.showErrors()) {
                Debug.echoError("Cannot compare as numbers '" + objA + "' vs '" + objB + "' - one or both values are not numerical. Returning false.");
            }
        }
        return false;
    }

    private static boolean listContains(ObjectTag listObj, ObjectTag entry, TagContext context) {
        ListTag list = null;
        MapTag map = null;
        if (listObj instanceof ListTag) {
            list = (ListTag) listObj;
        }
        else if (listObj instanceof MapTag) {
            map = (MapTag) listObj;
        }
        else {
            String text = listObj.toString();
            if (text.startsWith("li@")) {
                list = ListTag.valueOf(text, context);
            }
            else if (text.startsWith("map@") || text.startsWith("[")) {
                map = MapTag.valueOf(text, context);
            }
            else {
                list = ListTag.valueOf(text, context);
            }
        }
        String search = entry.toString();
        if (list != null) {
            for (String string : list) {
                if (CoreUtilities.equalsIgnoreCase(string, search)) {
                    return true;
                }
            }
        }
        else if (map != null) {
            return map.getDeepObject(search) != null;
        }
        return false;
    }

    public static boolean compare(ObjectTag objA, ObjectTag objB, Operator operator, boolean negative, TagContext context) {
        boolean outcome;
        switch (operator) {
            case EQUALS:
                outcome = CoreUtilities.equalsIgnoreCase(objA.toString(), objB.toString());
                break;
            case OR_MORE:
            case OR_LESS:
            case MORE:
            case LESS:
                return compareDecimal(objA, objB, operator, context);
            case CONTAINS:
                outcome = listContains(objA, objB, context);
                break;
            case IN:
                outcome = listContains(objB, objA, context);
                break;
            case MATCHES:
                outcome = objA.tryAdvancedMatcher(objB.toString(), context);
                break;
            default:
                // Impossible to reach
                return false;
        }
        return negative ? !outcome : outcome;
    }
}
