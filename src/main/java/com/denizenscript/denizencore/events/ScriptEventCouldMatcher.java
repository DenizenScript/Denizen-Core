package com.denizenscript.denizencore.events;

import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.*;

/**
 * Helper to generate automatic logic for ScriptEvent#couldMatch.
 */
public class ScriptEventCouldMatcher {

    /**
     * Registry of validators, as a map from name to instance object.
     */
    public static HashMap<String, PathArgumentValidator> knownValidatorTypes = new HashMap<>();

    /**
     * The raw format string used to construct this couldMatcher.
     */
    public String format;

    /**
     * The array of validator objects for this couldMatcher.
     * The path length should equal the array length, and each argument match the validator.
     */
    public PathArgumentValidator[] validators;

    /**
     * Special optimization trick: an array of argument indices to control testing order.
     * The simplest tests are run first.
     */
    public int[] argOrder;

    public ScriptEventCouldMatcher(String format) {
        this.format = format;
        ArrayList<PathArgumentValidator> validatorList = new ArrayList<>();
        List<String> args = CoreUtilities.split(format, ' ');
        for (String arg : args) {
            if (arg.isEmpty()) {
                Debug.echoError("Event matcher format error: '" + format + "' has a double space?");
                continue;
            }
            if (arg.startsWith("<")) {
                if (!arg.endsWith(">")) {
                    Debug.echoError("Event matcher format error: '" + format + "' has an unclosed fill-in part.");
                    continue;
                }
                String toUse = arg.substring(1, arg.length() - 1);
                if (toUse.startsWith("'") && toUse.endsWith("'")) {
                    validatorList.add(new StringBasedValidator(arg));
                }
                else {
                    PathArgumentValidator validator = knownValidatorTypes.get(toUse);
                    if (validator == null) {
                        Debug.echoError("Event matcher format error: '" + format + "' has an unrecognized input type '" + toUse + "'");
                        continue;
                    }
                    validatorList.add(validator);
                }
            }
            else if (CoreUtilities.contains(arg, '|')) {
                validatorList.add(new StringSetBasedValidator(CoreUtilities.split(arg, '|')));
            }
            else {
                validatorList.add(new StringBasedValidator(arg));
            }
        }
        validators = validatorList.toArray(new PathArgumentValidator[0]);
        argOrder = new int[validators.length];
        int index = 0;
        for (int i = 0; i < validators.length; i++) {
            if (shouldPrioritize(validators[i])) {
                argOrder[index++] = i;
            }
        }
        for (int i = 0; i < validators.length; i++) {
            if (!shouldPrioritize(validators[i])) {
                argOrder[index++] = i;
            }
        }
    }

    private static boolean shouldPrioritize(PathArgumentValidator valid) {
        return valid instanceof StringBasedValidator || valid instanceof StringSetBasedValidator;
    }

    /**
     * Returns true if the path could-match this event.
     */
    public boolean doesMatch(ScriptEvent.ScriptPath path) {
        if (path.eventArgsLower.length != validators.length) {
            return false;
        }
        for (int i : argOrder) {
            if (!validators[i].doesMatch(path.eventArgsLower[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Interface for validating a single argument of a script path in a couldMatch call.
     */
    @FunctionalInterface
    public interface PathArgumentValidator {
        boolean doesMatch(String arg);
    }

    /**
     * The simplest possible PathArgumentValidator: a string equality comparison.
     */
    public static class StringBasedValidator implements PathArgumentValidator {

        public String word;

        public StringBasedValidator(String word) {
            this.word = word;
        }

        @Override
        public final boolean doesMatch(String arg) {
            return arg.equals(word);
        }
    }

    /**
     * The second-simplest possible PathArgumentValidator: a set of possible exact strings.
     */
    public static class StringSetBasedValidator implements PathArgumentValidator {

        public HashSet<String> words;

        public StringSetBasedValidator(Collection<String> words) {
            this.words = new HashSet<>(words);
        }

        @Override
        public final boolean doesMatch(String arg) {
            return words.contains(arg);
        }
    }
}
