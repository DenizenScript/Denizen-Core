package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;

import java.util.List;

public class DataActionHelper {

    public static DataAction parse(ActionableDataProvider provider, Argument arg, TagContext context) {
        if (arg.object instanceof ElementTag || !arg.hasPrefix()) {
            return parse(provider, arg.getRawValue(), context);
        }
        DataAction action = new DataAction();
        action.provider = provider;
        parseKey(action, arg.getPrefix().getValue());
        action.type = DataActionType.SET;
        action.inputValue = arg.object;
        return action;
    }

    public static void parseKey(DataAction action, String key) {
        action.key = key;
        int bracketIndex = action.key.indexOf('[');
        if (bracketIndex >= 0) {
            String index = action.key.substring(bracketIndex + 1, action.key.lastIndexOf(']'));
            if (ArgumentHelper.matchesInteger(index)) {
                action.key = action.key.substring(0, bracketIndex);
                action.index = Integer.parseInt(index);
            }
        }
    }

    public static DataAction parse(ActionableDataProvider provider, String actionArgument, TagContext context) {
        DataAction toReturn = new DataAction();
        toReturn.provider = provider;
        List<String> split = CoreUtilities.split(actionArgument, ':', 3);
        parseKey(toReturn, split.get(0));
        if (split.size() == 1) {
            toReturn.type = DataActionType.AUTO_SET;
            return toReturn;
        }
        String action = split.get(1);
        if (split.size() == 2) {
            if (action.equals("++")) {
                toReturn.type = DataActionType.INCREMENT;
            }
            else if (action.equals("--")) {
                toReturn.type = DataActionType.DECREMENT;
            }
            else if (action.equals("!")) {
                toReturn.type = DataActionType.CLEAR;
            }
            else if (action.equals("<-")) {
                toReturn.type = DataActionType.REMOVE;
            }
            else {
                toReturn.type = DataActionType.SET;
                toReturn.inputValue = ObjectFetcher.pickObjectFor(action, context);
            }
            return toReturn;
        }
        toReturn.inputValue = ObjectFetcher.pickObjectFor(split.get(2), context);
        if (action.equals("->")) {
            toReturn.type = DataActionType.INSERT;
        }
        else if (action.equals("<-")) {
            toReturn.type = DataActionType.REMOVE;
        }
        else if (action.equals("|")) {
            toReturn.type = DataActionType.SPLIT;
        }
        else if (action.equals("!|")) {
            toReturn.type = DataActionType.SPLIT_NEW;
        }
        else if (action.equals("+")) {
            toReturn.type = DataActionType.ADD;
        }
        else if (action.equals("-")) {
            toReturn.type = DataActionType.SUBTRACT;
        }
        else if (action.equals("*")) {
            toReturn.type = DataActionType.MULTIPLY;
        }
        else if (action.equals("/")) {
            toReturn.type = DataActionType.DIVIDE;
        }
        else {
            toReturn.type = DataActionType.SET;
            toReturn.inputValue = ObjectFetcher.pickObjectFor(split.get(1) + ":" + split.get(2), context);
        }
        return toReturn;
    }
}
