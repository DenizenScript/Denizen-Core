package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.properties.Property;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class PropertyMatchHelper<T extends ObjectTag> {

    public static LinkedHashMap<String, PropertyMatchHelper<?>> matchHelperCache = new LinkedHashMap<>();

    public static int MAX_MATCH_HELPER_CACHE = 1024;

    public static <T extends ObjectTag> PropertyMatchHelper<T> getPropertyMatchHelper(Class<T> clazz, String text, BiPredicate<T, T> isBaseMatch) {
        if (CoreConfiguration.debugVerbose) {
            Debug.verboseLog("[PropertyMatchHelper] getting helper for " + text);
        }
        String mapKey = DebugInternals.getClassNameOpti(clazz) + "," + text;
        PropertyMatchHelper matchHelper = matchHelperCache.get(mapKey);
        if (matchHelper != null) {
            return matchHelper;
        }
        T actualObj = ObjectFetcher.getObjectFrom(clazz, text, CoreUtilities.noDebugContext);
        if (actualObj == null) {
            Debug.verboseLog("[PropertyMatchHelper] rejecting because parsed object is null");
            return null;
        }
        matchHelper = new PropertyMatchHelper(clazz, actualObj, isBaseMatch);
        List<String> propertiesGiven = ObjectFetcher.separateProperties(text);
        if (propertiesGiven == null) {
            return matchHelper;
        }
        PropertyParser.ClassPropertiesInfo itemInfo = PropertyParser.propertiesByClass.get(clazz);
        for (int i = 1; i < propertiesGiven.size(); i++) {
            String property = propertiesGiven.get(i);
            int equalSign = property.indexOf('=');
            if (equalSign == -1) {
                if (CoreConfiguration.debugVerbose) {
                    Debug.verboseLog("[PropertyMatchHelper] rejecting item because " + property + " lacks an equal sign");
                }
                return null;
            }
            String label = ObjectFetcher.unescapeProperty(property.substring(0, equalSign));
            PropertyParser.PropertyGetter getter = itemInfo.propertiesByMechanism.get(label);
            if (getter == null) {
                continue;
            }
            Property realProp = getter.get(actualObj);
            if (realProp == null) {
                continue;
            }
            matchHelper.comparisons.add(new PropertyMatchHelper.PropertyComparison(realProp.getPropertyString(), getter));
        }
        if (matchHelperCache.size() > MAX_MATCH_HELPER_CACHE) {
            String firstMost = matchHelperCache.keySet().iterator().next();
            matchHelperCache.remove(firstMost);
        }
        if (CoreConfiguration.debugVerbose) {
            Debug.verboseLog("[PropertyMatchHelper] stored final result as " + matchHelper);
        }
        matchHelperCache.put(mapKey, matchHelper);
        return matchHelper;
    }

    public PropertyMatchHelper(Class<T> clazz, T properObj, BiPredicate<T, T> isBaseMatch) {
        this.clazz = clazz;
        this.properObj = properObj;
        this.isBaseMatch = isBaseMatch;
    }

    public Class<T> clazz;

    public T properObj;

    public BiPredicate<T, T> isBaseMatch;

    public static class PropertyComparison {

        public String compareValue;

        public PropertyParser.PropertyGetter getter;

        public PropertyComparison(String compareValue, PropertyParser.PropertyGetter getter) {
            this.compareValue = compareValue;
            this.getter = getter;
        }
    }

    public List<PropertyComparison> comparisons = new ArrayList<>();

    public final boolean doesMatch(T obj) {
        if (obj == null) {
            return false;
        }
        if (!isBaseMatch.test(properObj, obj)) {
            Debug.verboseLog("[PropertyMatchHelper] deny because base match failed");
            return false;
        }
        for (PropertyComparison comparison : comparisons) {
            Property p = comparison.getter.get(obj);
            if (p == null) {
                Debug.verboseLog("[PropertyMatchHelper] deny because property is null");
                return false;
            }
            String val = p.getPropertyString();
            if (comparison.compareValue == null) {
                if (val != null) {
                    Debug.verboseLog("[PropertyMatchHelper] deny because nullity");
                    return false;
                }
            }
            else {
                if (val == null || !CoreUtilities.equalsIgnoreCase(comparison.compareValue, val)) {
                    Debug.verboseLog("[PropertyMatchHelper] deny because unequal");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "object=" + properObj + ", comparisons=" + comparisons.stream().map(c -> c.compareValue).collect(Collectors.joining(", "));
    }
}
