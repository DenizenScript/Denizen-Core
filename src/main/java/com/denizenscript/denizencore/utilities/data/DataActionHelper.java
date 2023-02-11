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
        if ((arg.object instanceof ElementTag element && element.isRawInput) || !arg.hasPrefix()) {
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
            if (CoreUtilities.equalsIgnoreCase(index, "last")) {
                action.index = Integer.MAX_VALUE;
            }
            else if (ArgumentHelper.matchesInteger(index)) {
                action.index = Integer.parseInt(index);
            }
            else {
                return;
            }
            action.key = action.key.substring(0, bracketIndex);
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
            switch (action) {
                case "++":
                    toReturn.type = DataActionType.INCREMENT;
                    break;
                case "--":
                    toReturn.type = DataActionType.DECREMENT;
                    break;
                case "!":
                    toReturn.type = DataActionType.CLEAR;
                    break;
                case "<-":
                    toReturn.type = DataActionType.REMOVE;
                    break;
                default:
                    toReturn.type = DataActionType.SET;
                    toReturn.inputValue = ObjectFetcher.pickObjectFor(action, context);
                    break;
            }
            return toReturn;
        }
        toReturn.inputValue = new ElementTag(split.get(2));
        switch (action) {
            case "->":
                toReturn.inputValue = ObjectFetcher.pickObjectFor(split.get(2), context);
                toReturn.type = DataActionType.INSERT;
                break;
            case "<-":
                toReturn.type = DataActionType.REMOVE;
                break;
            case "|":
                toReturn.type = DataActionType.SPLIT;
                break;
            case "!|":
                toReturn.type = DataActionType.SPLIT_NEW;
                break;
            case "+":
                toReturn.type = DataActionType.ADD;
                break;
            case "-":
                toReturn.type = DataActionType.SUBTRACT;
                break;
            case "*":
                toReturn.type = DataActionType.MULTIPLY;
                break;
            case "/":
                toReturn.type = DataActionType.DIVIDE;
                break;
            default:
                toReturn.type = DataActionType.SET;
                toReturn.inputValue = ObjectFetcher.pickObjectFor(split.get(1) + ":" + split.get(2), context);
                break;
        }
        return toReturn;
    }
}
