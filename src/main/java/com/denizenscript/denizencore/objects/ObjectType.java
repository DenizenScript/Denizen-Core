package com.denizenscript.denizencore.objects;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;

public class ObjectType<T extends ObjectTag> {

    @FunctionalInterface
    public interface MatchesInterface {

        boolean matches(String str);
    }

    public interface ValueOfInterface<T extends ObjectTag> {

        T valueOf(String str, TagContext context);
    }

    @FunctionalInterface
    public interface TypeComparisonRunnable {
        boolean doesCompare(ObjectTag inp);

        TypeComparisonRunnable trueAlways = (o) -> true;
    }

    @FunctionalInterface
    public interface TagTypeConverter<T extends ObjectTag> {
        T convert(ObjectTag inp, TagContext context);
    }

    public Class<T> clazz;

    public MatchesInterface matches;

    public ValueOfInterface<T> valueOf;

    public ObjectTagProcessor<T> tagProcessor;

    public String prefix;

    public boolean isAdjustable;

    public String longName, shortName;

    public TagTypeConverter<T> typeConverter;

    public TypeComparisonRunnable typeChecker, typeShouldBeChecker;

    public void setAsNOtherCode() {
        typeChecker = (inp) -> {
            if (inp == null) {
                return false;
            }
            Class<? extends ObjectTag> inpType = inp.getClass();
            if (inpType == clazz) {
                return true;
            }
            if (inpType == ElementTag.class) {
                String simple = inp.toString();
                int atIndex = simple.indexOf('@');
                if (atIndex != -1) {
                    String code = simple.substring(0, atIndex);
                    if (!code.equals(prefix) && !code.equals("el")) {
                        if (ObjectFetcher.objectsByPrefix.containsKey(code)) {
                            return false;
                        }
                    }
                }
                return true;
            }
            return false;
        };
    }

    @Override
    public String toString() {
        return longName;
    }
}
