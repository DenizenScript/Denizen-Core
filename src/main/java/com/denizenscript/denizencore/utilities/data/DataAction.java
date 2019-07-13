package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.objects.Element;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.dList;
import com.denizenscript.denizencore.objects.dObject;

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

    public dObject inputValue = null;

    public String debug() {
        return ArgumentHelper.debugObj("action", "(" + key + "[" + index + "]:" + type + ":" + inputValue + ")");
    }

    public dList autoList(String key) {
        dObject obj = provider.getValueAt(key);
        if (obj == null) {
            return new dList();
        }
        else {
            return autoList(dList.getListFor(obj));
        }
    }

    public dList autoList(dList list) {
        return new dList(list);
    }

    public dObject autoDup(dObject object) {
        if (object == null) {
            return null;
        }
        if (object instanceof dList) {
            return autoList((dList) object);
        }
        return new Element(object.toString());
    }

    public BigDecimal autoNumber() {
        dObject obj = provider.getValueAt(key);
        if (index != 0) {
            dList subList = dList.getListFor(obj);
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

    public BigDecimal autoNumber(dObject obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(obj.toString());
    }

    public Element autoNumber(BigDecimal decimal) {
        return new Element(decimal);
    }

    public void autoSet(dObject value) {
        if (index != 0) {
            dObject obj = provider.getValueAt(key);
            dList subList = dList.getListFor(obj);
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

    public void execute() {
        switch (type) {
            case INCREMENT: {
                BigDecimal num = autoNumber();
                num = num.add(BigDecimal.ONE);
                autoSet(autoNumber(num));
                break;
            }
            case DECREMENT: {
                BigDecimal num = autoNumber();
                num = num.subtract(BigDecimal.ONE);
                autoSet(autoNumber(num));
                break;
            }
            case ADD: {
                requiresInputValue();
                BigDecimal num = autoNumber();
                num = num.add(autoNumber(inputValue));
                autoSet(autoNumber(num));
                break;
            }
            case SUBTRACT: {
                requiresInputValue();
                BigDecimal num = autoNumber();
                num = num.subtract(autoNumber(inputValue));
                autoSet(autoNumber(num));
                break;
            }
            case MULTIPLY: {
                requiresInputValue();
                BigDecimal num = autoNumber();
                num = num.multiply(autoNumber(inputValue));
                autoSet(autoNumber(num));
                break;
            }
            case DIVIDE: {
                requiresInputValue();
                BigDecimal num = autoNumber();
                num = num.setScale(15, RoundingMode.HALF_UP);
                num = num.divide(autoNumber(inputValue), RoundingMode.HALF_UP);
                autoSet(autoNumber(num));
                break;
            }
            case INSERT: {
                requiresInputValue();
                dList list = autoList(key);
                list.addObject(inputValue);
                provider.setValueAt(key, list);
                break;
            }
            case REMOVE: {
                dList list = autoList(key);
                if (index != 0) {
                    list.remove(index - 1);
                }
                requiresInputValue();
                String findValue = CoreUtilities.toLowerCase(inputValue.toString());
                for (int i = 0; i < list.size(); i++) {
                    if (CoreUtilities.toLowerCase(list.get(i)).equals(findValue)) {
                        list.remove(i);
                        break;
                    }
                }
                provider.setValueAt(key, list);
                break;
            }
            case SPLIT: {
                requiresInputValue();
                dList list = autoList(key);
                list.addObjects(dList.getListFor(inputValue).objectForms);
                provider.setValueAt(key, list);
                break;
            }
            case SPLIT_NEW:
                requiresInputValue();
                provider.setValueAt(key, autoList(dList.getListFor(inputValue)));
                break;
            case SET:
                requiresInputValue();
                provider.setValueAt(key, autoDup(inputValue));
                break;
            case AUTO_SET:
                provider.setValueAt(key, new Element(true));
                break;
            case CLEAR:
                provider.setValueAt(key, null);
                break;
        }
    }
}
