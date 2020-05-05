package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.ArgumentHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScriptBuilder {

    public static char LINE_PREFIX_CHAR = '^'; // This would be an invisible special character... if SnakeYAML allowed them!

    public static String stripLinePrefix(String rawLine) {
        if (!rawLine.startsWith(String.valueOf(LINE_PREFIX_CHAR))) {
            return rawLine;
        }
        int infoEnd = rawLine.indexOf(LINE_PREFIX_CHAR, 1);
        return rawLine.substring(infoEnd + 2); // Skip the symbol and the space after.
    }

    public static List<ScriptEntry> buildScriptEntries(List<Object> contents, ScriptContainer parent, ScriptEntryData data) {

        if (contents == null || contents.isEmpty()) {
            if (Debug.showScriptBuilder) {
                Debug.echoError("Building script entries... no entries to build!");
            }
            return null;
        }

        if (Debug.showScriptBuilder) {
            Debug.echoDebug(parent, "Building script entries:");
        }

        List<ScriptEntry> scriptCommands = new ArrayList<>(contents.size());
        for (Object ientry : contents) {

            if (ientry == null) {
                ientry = "null";
            }

            String entry;
            List<Object> inside;

            if (ientry instanceof Map) {
                Object key = ((Map) ientry).keySet().toArray()[0];
                entry = key.toString();
                Object rawValue = ((Map) ientry).get(key);
                if (!(rawValue instanceof List)) {
                    Debug.echoError("Script '" + parent.getName() + "' has invalid line " + ientry + ": line ends with ':' but no script body inside.");
                    return null;
                }
                inside = (List<Object>) rawValue;
            }
            else {
                entry = ientry.toString();
                inside = null;
            }

            int lineNum = 1;
            if (entry.startsWith(String.valueOf(LINE_PREFIX_CHAR))) {
                int infoEnd = entry.indexOf(LINE_PREFIX_CHAR, 1);
                String lineNumStr = entry.substring(1, infoEnd);
                entry = entry.substring(infoEnd + 2); // Skip the symbol and the space after.
                lineNum = Integer.valueOf(lineNumStr);
            }

            String[] scriptEntry = entry.split(" ", 2);

            try {
                /* Build new script commands */
                String[] args = scriptEntry.length > 1 ?  ArgumentHelper.buildArgs(scriptEntry[1]) : null;
                if (Debug.showScriptBuilder) {
                    Debug.echoDebug(parent, "Adding '" + scriptEntry[0] + "'  Args: " + Arrays.toString(args));
                }
                ScriptEntry newEntry = new ScriptEntry(scriptEntry[0], args, parent, inside);
                newEntry.internal.lineNumber = lineNum;
                newEntry.internal.originalLine = entry;
                newEntry.entryData.transferDataFrom(data);
                scriptCommands.add(newEntry);
            }
            catch (Exception e) {
                Debug.echoError("Exception while building script '" + parent.getName() + "'...");
                Debug.echoError(e);
            }
        }

        return scriptCommands;
    }
}
