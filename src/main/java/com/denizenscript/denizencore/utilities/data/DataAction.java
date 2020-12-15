package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.ObjectTag;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DataAction {

    public ActionableDataProvider provider;

    public DataActionType type;

    public String key;

    /**
     * Zero = no index needed.
     * Positive number = list index (starting at 1).
     */
    public int index = 0;

    public ObjectTag inputValue = null;

    public String debug() {
        String keyDebug = index == 0 ? key : (key + "[" + index + "]");
        return ArgumentHelper.debugObj("action", "(" + keyDebug + ":" + type + ":" + inputValue + ")");
    }

    public ListTag autoList(String key, TagContext context) {
        ObjectTag obj = provider.getValueAt(key);
        if (obj == null) {
            return new ListTag();
        }
        else {
            return autoList(ListTag.getListFor(obj, context));
        }
    }

    public ListTag autoList(ListTag list) {
        return new ListTag(list);
    }

    public ObjectTag autoDup(ObjectTag object) {
        if (object == null) {
            return null;
        }
        if (object instanceof ListTag) {
            return autoList((ListTag) object);
        }
        return object.duplicate();
    }

    public BigDecimal autoNumber(TagContext context) {
        ObjectTag obj = provider.getValueAt(key);
        if (index != 0) {
            ListTag subList = ListTag.getListFor(obj, context);
            if (index < 0 || index > subList.size()) {
                return BigDecimal.ZERO;
            }
            obj = subList.getObject(index - 1);
        }
        try {
            return autoNumber(obj);
        }
        catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    public BigDecimal autoNumber(ObjectTag obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(obj.toString());
    }

    public ElementTag autoNumber(BigDecimal decimal) {
        return new ElementTag(decimal);
    }

    public void autoSet(ObjectTag value, TagContext context) {
        if (index != 0) {
            ObjectTag obj = provider.getValueAt(key);
            ListTag subList = ListTag.getListFor(obj, context);
            subList.setObject(index - 1, value);
            value = subList;
        }
        provider.setValueAt(key, value);
    }

    public void requiresInputValue() {
        if (inputValue == null) {
            throw new DataActionException("Input value required for data action " + type + ".");
        }
    }

    public void execute(TagContext context) {
        switch (type) {
            case INCREMENT: {
                BigDecimal num = autoNumber(context);
                num = num.add(BigDecimal.ONE);
                autoSet(autoNumber(num), context);
                break;
            }
            case DECREMENT: {
                BigDecimal num = autoNumber(context);
                num = num.subtract(BigDecimal.ONE);
                autoSet(autoNumber(num), context);
                break;
            }
            case ADD: {
                requiresInputValue();
                BigDecimal num = autoNumber(context);
                num = num.add(autoNumber(inputValue));
                autoSet(autoNumber(num), context);
                break;
            }
            case SUBTRACT: {
                requiresInputValue();
                BigDecimal num = autoNumber(context);
                num = num.subtract(autoNumber(inputValue));
                autoSet(autoNumber(num), context);
                break;
            }
            case MULTIPLY: {
                requiresInputValue();
                BigDecimal num = autoNumber(context);
                num = num.multiply(autoNumber(inputValue));
                autoSet(autoNumber(num), context);
                break;
            }
            case DIVIDE: {
                requiresInputValue();
                BigDecimal num = autoNumber(context);
                num = num.setScale(15, RoundingMode.HALF_UP);
                num = num.divide(autoNumber(inputValue), RoundingMode.HALF_UP);
                autoSet(autoNumber(num), context);
                break;
            }
            case INSERT: {
                requiresInputValue();
                ListTag list = autoList(key, context);
                list.addObject(inputValue);
                provider.setValueAt(key, list);
                break;
            }
            case REMOVE: {
                ListTag list = autoList(key, context);
                if (index != 0) {
                    list.remove(index - 1);
                }
                else {
                    requiresInputValue();
                    String findValue = inputValue.toString();
                    for (int i = 0; i < list.size(); i++) {
                        if (CoreUtilities.equalsIgnoreCase(list.get(i), findValue)) {
                            list.remove(i);
                            break;
                        }
                    }
                }
                provider.setValueAt(key, list);
                break;
            }
            case SPLIT: {
                requiresInputValue();
                ListTag list = autoList(key, context);
                list.addObjects(ListTag.getListFor(inputValue, context).objectForms);
                provider.setValueAt(key, list);
                break;
            }
            case SPLIT_NEW:
                requiresInputValue();
                provider.setValueAt(key, autoList(ListTag.getListFor(inputValue, context)));
                break;
            case SET:
                requiresInputValue();
                autoSet(autoDup(inputValue), context);
                break;
            case AUTO_SET:
                provider.setValueAt(key, new ElementTag(true));
                break;
            case CLEAR:
                provider.setValueAt(key, null);
                break;
        }
    }
}
