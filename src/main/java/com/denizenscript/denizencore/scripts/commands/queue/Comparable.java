package com.denizenscript.denizencore.scripts.commands.queue;

import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.dB;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.Duration;
import com.denizenscript.denizencore.objects.aH;
import com.denizenscript.denizencore.objects.dList;
import com.denizenscript.denizencore.objects.dScript;

public class Comparable {

    // TODO: Expand upon this.
    //
    // <--[language]
    // @name Comparable
    // @group Comparables
    // @description
    // A Comparable is a method that the IF command and Element dObject uses to compare objects.
    // (This lang is TODO! )
    // See <@link language operator>
    // -->

    // <--[language]
    // @name Operator
    // @group Comparables
    // @description
    // An operator is the type of comparison that a comparable will check. Not all types of
    // comparables are compatible with all operators. See <@link language comparable> for more information.
    //
    // Available Operators include:
    // EQUALS (==), MATCHES, OR_MORE (>=), OR_LESS (<=), MORE (>), LESS (<), CONTAINS, and IS_EMPTY.
    //
    // Operators which have a symbol alternative (as marked by parenthesis) can be referred to by either
    // their name or symbol. Using a '!' in front of the operator will also reverse logic, effectively
    // turning 'EQUALS' into 'DOES NOT EQUAL', for example.
    //
    // == <= >= > < all compare arguments as text or numbers.
    //
    // CONTAINS checks whether a list contains an element, or an element contains another element.
    //
    // IS_EMPTY checks whether a list is empty. (This exists for back-support).
    //
    // MATCHES checks whether the first element matches a given type.
    // For example: "if 1 matches number" or "if p@bob matches player".
    // Match types: location, material, materiallist, script, entity, spawnedentity, entitytype,
    // npc, player, offlineplayer, onlineplayer, item, pose, duration, cuboid, decimal,
    // number, even number, odd number, boolean.
    //
    // Note: When using an operator in a replaceable tag (such as <el@element.is[...].than[...]>),
    // keep in mind that < and >, and even >= and <= must be either escaped, or referred to by name.
    // Example: "<player.health.is[<&lt>].than[10]>" or "<player.health.is[LESS].than[10]>",
    // but <player.health.is[<].than[10]> will produce undesired results. <>'s must be escaped or replaced since
    // they are normally notation for a replaceable tag. Escaping is not necessary when the argument
    // contains no replaceable tags.
    //
    // -->
    public static enum Operator {
        EQUALS, MATCHES, OR_MORE, OR_LESS, MORE,
        LESS, CONTAINS, IS_EMPTY
    }

    public static final Operator[] OperatorValues = Operator.values();

    public static enum Bridge {
        OR, AND, FIRST, THEN, ELSE
    }

    public static final Bridge[] BridgeValues = Bridge.values();

    public static enum Logic {
        REGULAR, NEGATIVE
    }

    public Logic logic = Logic.REGULAR;
    public Bridge bridge = Bridge.FIRST;
    public Object comparable = null;
    public Operator operator = Operator.EQUALS;
    public Object comparedto = "true";
    public Boolean outcome = null;


    public void setNegativeLogic() {
        logic = Logic.NEGATIVE;
    }


    public void setOperator(Operator operator) {
        this.operator = operator;
    }


    public void setComparable(String arg) {

        // If a Number
        if (arg.length() > 0 && aH.matchesDouble(arg)) {
            comparable = aH.getDoubleFrom(arg);
        }

        // If a List<Object>
        else if (arg.length() > 0 && dList.matches(arg)) {
            comparable = dList.valueOf(arg);
        }

        // If none of the above, must be a String! :D
        // 'arg' is already a String.
        else {
            comparable = arg;
        }
    }


    public void setComparedto(String arg) {

        // If MATCHES, change comparable to String
        if (operator == Comparable.Operator.MATCHES) {
            comparable = String.valueOf(comparable);
        }

        // Comparable is String, return String
        if (comparable instanceof String) {
            comparedto = arg;
        }

        // Comparable is a Number, return Double
        else if (comparable instanceof Double || comparable instanceof Long) {
            if (aH.matchesDouble(arg)) {
                comparedto = aH.getDoubleFrom(arg);
            }
            else {
                comparable = String.valueOf(comparable);
                comparedto = arg;
            }
        }

        else if (comparable instanceof Boolean) {
            comparedto = aH.getBooleanFrom(arg);
        }

        else if (comparable instanceof dList) {
            if (dList.matches(arg)) {
                comparedto = dList.valueOf(arg);
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

        // or... compare 'compared_to' as the type of 'comparable'
        if (comparable instanceof String) {
            compare_as_strings();
        }

        else if (comparable instanceof dList) {
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

        dList comparable = (dList) this.comparable;

        switch (operator) {

            case CONTAINS:
                for (String string : comparable) {
                    if (comparedto instanceof Long) {
                        if (aH.matchesInteger(string)
                                && aH.getLongFrom(string) == (Long) comparedto) {
                            outcome = true;
                            break;
                        }
                    }
                    else if (comparedto instanceof Double) {
                        if (aH.matchesDouble(string) &&
                                aH.getDoubleFrom(string) == (Double) comparedto) {
                            outcome = true;
                            break;
                        }
                    }
                    else if (comparedto instanceof String) {
                        if (string.equalsIgnoreCase((String) comparedto)) {
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
                if (comparedto instanceof dList) {
                    dList list2 = (dList) comparedto;
                    outcome = list2.identify().equalsIgnoreCase(comparable.identify());
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
            // For checking if a FLAG is empty.
            case IS_EMPTY:
                outcome = comparable.length() == 0;
                break;

            // For checking straight up if comparable is equal to (ignoring case) comparedto
            case EQUALS:
                outcome = comparable.equalsIgnoreCase(comparedto);
                break;

            // For checking if the comparable contains comparedto
            case CONTAINS:
                outcome = CoreUtilities.toLowerCase(comparable).contains(CoreUtilities.toLowerCase(comparedto));
                break;

            // OR_MORE/OR_LESS/etc. invalid for text
            case OR_MORE:
            case OR_LESS:
            case MORE:
            case LESS:
                dB.echoError("Comparing text as if it were a number - comparison automatically false");
                outcome = false;
                break;

            // Check if the string comparable MATCHES a specific argument type,
            // as specified by comparedto
            case MATCHES:
                comparedto = comparedto.replace("_", "");

                if (comparedto.equalsIgnoreCase("script")) {
                    outcome = dScript.matches(comparable);
                }

                else if (comparedto.equalsIgnoreCase("duration")) {
                    outcome = Duration.matches(comparable);
                }

                else if (comparedto.equalsIgnoreCase("double")
                        || comparedto.equalsIgnoreCase("decimal")) {
                    outcome = aH.matchesDouble(comparable);
                }

                else if (comparedto.equalsIgnoreCase("integer")
                        || comparedto.equalsIgnoreCase("number")) {
                    outcome = aH.matchesInteger(comparable);
                }

                else if (comparedto.equalsIgnoreCase("even integer")
                        || comparedto.equalsIgnoreCase("even number")) {
                    outcome = aH.matchesInteger(comparable) && (aH.getLongFrom(comparable) % 2) == 0;
                }

                else if (comparedto.equalsIgnoreCase("odd integer")
                        || comparedto.equalsIgnoreCase("odd number")) {
                    outcome = aH.matchesInteger(comparable) && (aH.getLongFrom(comparable) % 2) == 1;
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
        dB.log("Warning: Unknown comparable type: " + str);
        return str;
    }

    @Override
    public String toString() {
        return (logic != Logic.REGULAR ? "<G>Logic='<A>" + logic.toString() + "<G>', " : "")
                + "<G>Comparable='" + (comparable == null ? "null'" : (comparable instanceof Double ? "Decimal" :
                comparable instanceof String ? "Element" : (comparable instanceof Long ? "Number" : (comparable instanceof dList ? "dList" : log(comparable.getClass().getSimpleName()))))
                + "<G>(<A>" + comparable + "<G>)'")
                + "<G>, Operator='<A>" + operator.toString()
                + "<G>', ComparedTo='" + (comparedto == null ? "null'" : (comparedto instanceof Double ? "Decimal" :
                comparedto instanceof String ? "Element" : (comparedto instanceof Long ? "Number" : (comparedto instanceof dList ? "dList" : log(comparedto.getClass().getSimpleName()))))
                + "<G>(<A>" + comparedto + "<G>)' ")
                + "<Y>--> OUTCOME='" + outcome + "'";
    }
}
