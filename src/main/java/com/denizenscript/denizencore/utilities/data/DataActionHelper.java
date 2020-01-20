package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;

import java.util.List;

public class DataActionHelper {

    public static DataAction parse(ActionableDataProvider provider, String actionArgument) {
        DataAction toReturn = new DataAction();
        toReturn.provider = provider;
        List<String> split = CoreUtilities.split(actionArgument, ':', 3);
        toReturn.key = split.get(0);
        int bracketIndex = toReturn.key.indexOf('[');
        if (bracketIndex >= 0) {
            String index = toReturn.key.substring(bracketIndex + 1, toReturn.key.lastIndexOf(']'));
            if (ArgumentHelper.matchesInteger(index)) {
                toReturn.key = toReturn.key.substring(0, bracketIndex);
                toReturn.index = Integer.parseInt(index);
            }
        }
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
                toReturn.inputValue = new ElementTag(action);
            }
            return toReturn;
        }
        toReturn.inputValue = new ElementTag(split.get(2));
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
            toReturn.inputValue = new ElementTag(split.get(1) + ":" + split.get(2));
        }
        return toReturn;
    }
}
