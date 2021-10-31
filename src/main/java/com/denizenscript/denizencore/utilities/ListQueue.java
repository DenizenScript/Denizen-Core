package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.scripts.ScriptEntry;

import java.util.Iterator;
import java.util.List;

public class ListQueue implements Iterable<ScriptEntry> {

    public ScriptEntry[] values;

    public int count;

    public int start;

    public ListQueue(int capacity) {
        values = new ScriptEntry[capacity];
        start = 0;
    }

    public final void clear() {
        count = 0;
        start = 0;
    }

    public final ScriptEntry removeFirst() {
        count--;
        return values[start++];
    }

    public final int size() {
        return count;
    }

    public final boolean isEmpty() {
        return count == 0;
    }

    public final ScriptEntry get(int index) {
        return values[index + start];
    }

    public final void ensureCapacity(int cap) {
        if (cap < values.length - start) {
            return;
        }
        ScriptEntry[] newSet = new ScriptEntry[cap];
        System.arraycopy(values, start, newSet, 0, count);
        values = newSet;
        start = 0;
    }

    public final void injectAtStart(ScriptEntry entry) {
        if (count == 0 && values.length > 0) {
            start = 0;
        }
        else if (start > 0) {
            start--;
        }
        else {
            ScriptEntry[] newSet = new ScriptEntry[Math.max(count + 5, values.length)];
            System.arraycopy(values, start, newSet, 3, count);
            values = newSet;
            start = 2;
        }
        values[start] = entry;
        count++;
    }

    public final void add(ScriptEntry entry) {
        ensureCapacity(count * 2 + 1);
        values[count + start] = entry;
        count++;
    }

    public final void addAll(List<ScriptEntry> entries) {
        ensureCapacity(count * 2 + entries.size());
        if (count == 0) {
            start = 0;
        }
        int firstIndex = start + count;
        for (int i = 0; i < entries.size(); i++) {
            values[firstIndex + i] = entries.get(i);
        }
        count += entries.size();
    }

    public final void addAllToStart(List<ScriptEntry> entries) {
        if (count == 0) {
            addAll(entries);
            return;
        }
        if (start >= entries.size()) {
            start -= entries.size();
            for (int i = 0; i < entries.size(); i++) {
                values[start + i] = entries.get(i);
            }
        }
        else {
            ScriptEntry[] newSet = new ScriptEntry[values.length + entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                newSet[i] = entries.get(i);
            }
            if (count > 0) {
                System.arraycopy(values, start, newSet, entries.size(), count);
            }
            values = newSet;
            start = 0;
        }
        count += entries.size();
    }

    public class ListQueueIterator implements Iterator<ScriptEntry> {

        public int index;

        @Override
        public final boolean hasNext() {
            return index < count;
        }

        @Override
        public final ScriptEntry next() {
            return get(index++);
        }
    }

    @Override
    public final Iterator<ScriptEntry> iterator() {
        return new ListQueueIterator();
    }
}
