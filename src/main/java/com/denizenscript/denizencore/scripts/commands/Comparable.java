package com.denizenscript.denizencore.scripts.commands;

import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;

public class Comparable {

    // <--[language]
    // @name Comparable
    // @group Comparables
    // @description
    // A Comparable is a method that the If command, While command, and 'element.is[...].to[...]' tag uses to compare objects.
    //
    // These are usually written in the format "VALUE OPERATOR VALUE".
    //
    // For example, if you use ">=" as the operator, and "3" and "5" as the values, you'd write "3 >= 5",
    // which would return false (as 3 is NOT greater-than-or-equal-to 5).
    //
    // For a list of valid operators and their usages, see <@link language operator>.
    // -->

    // <--[language]
    // @name Operator
    // @group Comparables
    // @description
    // An operator is the type of comparison that a comparable will check. Not all types of
    // comparables are compatible with all operators. See <@link language comparable> for more information.
    //
    // Available Operators include:
    // "Equals" is written as "=="
    // "Does not equal" is written as "!="
    // "Is more than" is written as ">" or "MORE".
    // "Is less than" is written as "<" or "LESS".
    // "Is more than or equal to" is written as ">=" or "OR_MORE".
    // "Is less than or equal to" is written as "<=" or "OR_LESS".
    //
    // Note: When using an operator in a replaceable tag (such as <ElementTag.is[...].than[...]>),
    // keep in mind that < and >, and even >= and <= must be either escaped, or referred to by name.
    // Example: "<player.health.is[<&lt>].than[10]>" or "<player.health.is[LESS].than[10]>",
    // but <player.health.is[<].than[10]> will produce undesired results. <>'s must be escaped or replaced since
    // they are normally notation for a replaceable tag. Escaping is not necessary when the argument
    // contains no replaceable tags.
    //
    // There are also special boolean operators (&&, ||, ...) documented at: <@link command if>
    //
    // -->

    public enum Operator {
        EQUALS, MATCHES, OR_MORE, OR_LESS, MORE, LESS, CONTAINS, IS_EMPTY
    }

    public enum Logic {
        REGULAR, NEGATIVE
    }

    public Logic logic = Logic.REGULAR;
    public Object comparable = null;
    public Operator operator = Operator.EQUALS;
    public Object comparedto = "true";
    public Boolean outcome = null;
    public TagContext context = null;

    public void setNegativeLogic() {
        logic = Logic.NEGATIVE;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public void setComparable(String arg) {
        if (arg.length() == 0) {
            comparable = arg;
        }
        else if (ArgumentHelper.matchesDouble(arg)) {
            comparable = Double.parseDouble(arg);
        }
        else if (DurationTag.matches(arg)) {
            comparable = DurationTag.valueOf(arg, context).getSeconds();
        }
        else if (ListTag.matches(arg)) {
            comparable = ListTag.valueOf(arg, context);
        }
        else {
            comparable = arg;
        }
    }

    public void setComparedto(String arg) {
        if (operator == Comparable.Operator.MATCHES) {
            comparable = String.valueOf(comparable);
        }
        if (comparable instanceof String) {
            comparedto = arg;
        }
        else if (comparable instanceof Double || comparable instanceof Long) {
            if (ArgumentHelper.matchesDouble(arg)) {
                comparedto = Double.parseDouble(arg);
            }
            else if (DurationTag.matches(arg)) {
                comparedto = DurationTag.valueOf(arg, context).getSeconds();
            }
            else {
                comparable = String.valueOf(comparable);
                comparedto = arg;
            }
        }
        else if (comparable instanceof Boolean) {
            comparedto = CoreUtilities.equalsIgnoreCase(arg, "true");
        }
        else if (comparable instanceof ListTag) {
            if (ListTag.matches(arg)) {
                comparedto = ListTag.valueOf(arg, context);
            }
            else {
                comparedto = arg;
            }
        }
        else {
            comparedto = arg;
        }
    }

    public boolean determineOutcome() {
        outcome = false;
        if (comparable instanceof String) {
            compare_as_strings();
        }
        else if (comparable instanceof ListTag) {
            compare_as_list();
        }
        else if (comparable instanceof Double || comparable instanceof Long) {
            if (comparedto instanceof Double || comparedto instanceof Long) {
                compare_as_numbers();
            }
        }
        else if (comparable instanceof Boolean) {
            // Check to make sure comparedto is Boolean
            if (comparedto instanceof Boolean) {
                // Comparing booleans.. let's do the logic
                outcome = comparable.equals(comparedto);
            }
            // Not comparing booleans, outcome = false
        }
        if (logic == Comparable.Logic.NEGATIVE) {
            outcome = !outcome;
        }
        return outcome;
    }

    private void compare_as_numbers() {
        outcome = false;
        Double comparable;
        if (this.comparable instanceof Double) {
            comparable = (Double) this.comparable;
        }
        else {
            comparable = ((Long) this.comparable).doubleValue();
        }
        Double comparedto;
        if (this.comparedto instanceof Double) {
            comparedto = (Double) this.comparedto;
        }
        else {
            comparedto = ((Long) this.comparedto).doubleValue();
        }
        switch (operator) {
            case EQUALS:
                if (comparable.doubleValue() == comparedto.doubleValue()) {
                    outcome = true;
                }
                break;
            case OR_MORE:
                if (comparable.compareTo(comparedto) >= 0) {
                    outcome = true;
                }
                break;
            case OR_LESS:
                if (comparable.compareTo(comparedto) <= 0) {
                    outcome = true;
                }
                break;
            case MORE:
                if (comparable.compareTo(comparedto) > 0) {
                    outcome = true;
                }
                break;
            case LESS:
                if (comparable.compareTo(comparedto) < 0) {
                    outcome = true;
                }
                break;
        }
    }

    private void compare_as_list() {
        outcome = false;
        ListTag comparable = (ListTag) this.comparable;
        switch (operator) {
            case CONTAINS:
                for (String string : comparable) {
                    if (comparedto instanceof Long) {
                        if (ArgumentHelper.matchesInteger(string)
                                && Long.parseLong(string) == (Long) comparedto) {
                            outcome = true;
                            break;
                        }
                    }
                    else if (comparedto instanceof Double) {
                        if (ArgumentHelper.matchesDouble(string) &&
                                Double.parseDouble(string) == (Double) comparedto) {
                            outcome = true;
                            break;
                        }
                    }
                    else if (comparedto instanceof String) {
                        if (CoreUtilities.equalsIgnoreCase(string, (String) comparedto)) {
                            outcome = true;
                            break;
                        }
                    }
                }
                break;
            case OR_MORE:
                if (!(comparedto instanceof Double)) {
                    break;
                }
                outcome = (comparable.size() >= ((Double) comparedto).intValue());
                break;
            case OR_LESS:
                if (!(comparedto instanceof Double)) {
                    break;
                }
                outcome = (comparable.size() <= ((Double) comparedto).intValue());
                break;
            case MORE:
                if (!(comparedto instanceof Double)) {
                    break;
                }
                outcome = (comparable.size() > ((Double) comparedto).intValue());
                break;
            case LESS:
                if (!(comparedto instanceof Double)) {
                    break;
                }
                outcome = (comparable.size() < ((Double) comparedto).intValue());
                break;
            case EQUALS:
                if (comparedto instanceof ListTag) {
                    ListTag list2 = (ListTag) comparedto;
                    outcome = CoreUtilities.equalsIgnoreCase(list2.identify(), comparable.identify());
                }
                break;
        }
    }

    private void compare_as_strings() {
        outcome = false;
        String comparable = String.valueOf(this.comparable);
        String comparedto = String.valueOf(this.comparedto);
        if (comparable == null || comparedto == null) {
            return;
        }
        switch (operator) {
            case IS_EMPTY:
                Deprecations.oldMatchesOperator.warn();
                outcome = comparable.length() == 0;
                break;
            case EQUALS:
                outcome = CoreUtilities.equalsIgnoreCase(comparable, comparedto);
                break;
            case CONTAINS:
                Deprecations.oldMatchesOperator.warn();
                outcome = CoreUtilities.toLowerCase(comparable).contains(CoreUtilities.toLowerCase(comparedto));
                break;
            case OR_MORE:
            case OR_LESS:
            case MORE:
            case LESS:
                Debug.echoError("Comparing text as if it were a number - comparison automatically false");
                outcome = false;
                break;
            case MATCHES:
                Deprecations.oldMatchesOperator.warn();
                comparedto = comparedto.replace("_", "");
                if (comparedto.equalsIgnoreCase("script")) {
                    outcome = ScriptTag.matches(comparable);
                }
                else if (comparedto.equalsIgnoreCase("duration")) {
                    outcome = DurationTag.matches(comparable);
                }
                else if (comparedto.equalsIgnoreCase("double")
                        || comparedto.equalsIgnoreCase("decimal")) {
                    outcome = ArgumentHelper.matchesDouble(comparable);
                }
                else if (comparedto.equalsIgnoreCase("integer")
                        || comparedto.equalsIgnoreCase("number")) {
                    outcome = ArgumentHelper.matchesInteger(comparable);
                }
                else if (comparedto.equalsIgnoreCase("even integer")
                        || comparedto.equalsIgnoreCase("even number")) {
                    outcome = ArgumentHelper.matchesInteger(comparable) && (Long.parseLong(comparable) % 2) == 0;
                }
                else if (comparedto.equalsIgnoreCase("odd integer")
                        || comparedto.equalsIgnoreCase("odd number")) {
                    outcome = ArgumentHelper.matchesInteger(comparable) && (Long.parseLong(comparable) % 2) == 1;
                }
                else if (comparedto.equalsIgnoreCase("boolean")) {
                    outcome = (comparable.equalsIgnoreCase("true") || comparable.equalsIgnoreCase("false"));
                }
                else {
                    outcome = DenizenCore.getImplementation().matchesType(comparable, comparedto);
                }
                break;
        }
    }

    public String log(String str) {
        Debug.echoError("Warning: Unknown comparable type: " + str);
        return str;
    }

    @Override
    public String toString() {
        return (logic != Logic.REGULAR ? "<G>Logic='<A>" + logic.toString() + "<G>', " : "")
                + "<G>Comparable='" + (comparable == null ? "null'" : (comparable instanceof Double ? "Decimal" :
                comparable instanceof String ? "Element" : (comparable instanceof Long ? "Number" : (comparable instanceof ListTag ? "ListTag" : log(comparable.getClass().getSimpleName()))))
                + "<G>(<A>" + comparable + "<G>)'")
                + "<G>, Operator='<A>" + operator.toString()
                + "<G>', ComparedTo='" + (comparedto == null ? "null'" : (comparedto instanceof Double ? "Decimal" :
                comparedto instanceof String ? "Element" : (comparedto instanceof Long ? "Number" : (comparedto instanceof ListTag ? "ListTag" : log(comparedto.getClass().getSimpleName()))))
                + "<G>(<A>" + comparedto + "<G>)' ")
                + "<Y>--> OUTCOME='" + outcome + "'";
    }
}
