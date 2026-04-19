package com.denizenscript.denizencore.utilities.data;

import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.math.BigDecimal;

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

    @Override
    public String toString() {
        String keyDebug = key;
        if (index != 0) {
            keyDebug += index == Integer.MAX_VALUE ? "[last]" : "[" + index + "]";
        }
        return "(" + keyDebug + ":" + type + ":" + inputValue + ")";
    }

    public void requiresInputValue() {
        if (inputValue == null) {
            throw new DataActionException("Input value required for data action " + type + ".");
        }
    }

    public void execute(TagContext context) {
        // Special operators
        switch (type) {
            case INCREMENT -> {
                BigDecimal base = ElementTag.parseBigDecimal(getBase(context));
                setResult(new ElementTag(base.add(BigDecimal.ONE)), context);
                return;
            }
            case DECREMENT -> {
                BigDecimal base = ElementTag.parseBigDecimal(getBase(context));
                setResult(new ElementTag(base.subtract(BigDecimal.ONE)), context);
                return;
            }
            case INSERT -> {
                requiresInputValue();
                ListTag list = autoList(key, context);
                list.addObject(inputValue);
                provider.setValueAt(key, list);
                return;
            }
            case REMOVE -> {
                ListTag list = autoList(key, context);
                if (index != 0) {
                    if (index == Integer.MAX_VALUE && !list.isEmpty()) {
                        list.removeLast();
                    }
                    else {
                        list.remove(index - 1);
                    }
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
                return;
            }
            case SPLIT -> {
                requiresInputValue();
                ListTag list = autoList(key, context);
                list.addObjects(ListTag.getListFor(inputValue, context).objectForms);
                provider.setValueAt(key, list);
                return;
            }
            case SPLIT_NEW -> {
                Deprecations.splitNewDataAction.warn(context);
                requiresInputValue();
                provider.setValueAt(key, new ListTag(ListTag.getListFor(inputValue, context)));
                return;
            }
            case SET -> {
                requiresInputValue();
                if (inputValue == null) {
                    setResult(null, context);
                }
                else if (inputValue instanceof ListTag listTag) {
                    setResult(new ListTag(listTag), context);
                }
                else {
                    setResult(inputValue.duplicate(), context);
                }
                return;
            }
            case AUTO_SET -> {
                provider.setValueAt(key, new ElementTag(true));
                return;
            }
            case CLEAR -> {
                provider.setValueAt(key, null);
                return;
            }
        }
        // Abstract operators
        requiresInputValue();
        ObjectTag base = getBase(context);
        if (!(base instanceof Actionable<?>)) {
            Debug.echoError("Cannot perform data action on non-actionable object '" + base.identify() + "'.");
            return;
        }
        Actionable<ObjectTag> actionable = (Actionable<ObjectTag>) base;
        switch (type) {
            case ADD -> setResult(actionable.additionOperation(inputValue, context), context);
            case SUBTRACT -> setResult(actionable.subtractionOperation(inputValue, context), context);
            case MULTIPLY -> setResult(actionable.multiplicationOperation(inputValue, context), context);
            case DIVIDE -> setResult(actionable.divisionOperation(inputValue, context), context);
        }
    }

    public ListTag autoList(String key, TagContext context) {
        ObjectTag obj = provider.getValueAt(key);
        return obj == null ? new ListTag() : new ListTag(ListTag.getListFor(obj, context));
    }

    public ObjectTag getBase(TagContext context) {
        if (index == 0) {
            return CoreUtilities.fixType(provider.getValueAt(key), context);
        }
        ListTag list = ListTag.getListFor(provider.getValueAt(key), context);
        return list.getObject((index == Integer.MAX_VALUE ? list.size() : index) - 1);
    }

    public void setResult(ObjectTag result, TagContext context) {
        if (index != 0) {
            ListTag list = ListTag.getListFor(provider.getValueAt(key), context);
            list.setObject((index == Integer.MAX_VALUE ? list.size() : index) - 1, result);
            result = list;
        }
        provider.setValueAt(key, result);
    }
}
