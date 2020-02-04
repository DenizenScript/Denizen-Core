package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.denizenscript.denizencore.DenizenCore;

import java.io.*;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Reloads and retrieves information from the scripts folder.
 */
public class ScriptHelper {
    public static YamlConfiguration _yamlScripts = null;

    public static void reloadScripts() {
        String concatenated = _concatenateCoreScripts();

        try {
            _yamlScripts = YamlConfiguration.load(concatenated);
        }
        catch (Exception e) {
            hadError = true;
            DenizenCore.getImplementation().debugError("Could not load scripts!");
            DenizenCore.getImplementation().debugException(e);
            _yamlScripts = YamlConfiguration.load("scripts_failed_to_load:\n  type: yaml data\n");
        }

        ScriptRegistry.buildCoreYamlScriptContainers(_yamlScripts);
    }

    public static YamlConfiguration getScripts() {
        if (_yamlScripts == null) {
            reloadScripts();
        }
        return _yamlScripts;
    }

    /**
     * Console will be alerted if error was had during reload
     */
    private static boolean hadError = false;

    public static boolean hadError() {
        return hadError;
    }

    public static void resetError() {
        hadError = false;
    }

    public static void setHadError() {
        hadError = true;
    }

    static void handleListing(YamlConfiguration config, List<String> list) {
        for (StringHolder str : config.getKeys(false)) {
            String up = str.str.toUpperCase();
            if (list.contains(up)) {
                hadError = true;
                Debug.echoError("There is more than one script named '" + up + "'!");
            }
            else {
                list.add(up);
            }
        }
    }

    private static HashMap<String, String> scriptSources = new HashMap<>();
    private static HashMap<String, String> scriptOriginalNames = new HashMap<>();

    public static String getSource(String script) {
        return scriptSources.get(script.toUpperCase());
    }

    public static String getOriginalName(String script) {
        return scriptOriginalNames.get(script.toUpperCase());
    }

    public static String clearComments(String filename, String input, boolean trackSources) {
        StringBuilder result = new StringBuilder(input.length());
        String[] lines = input.replace("\t", "    ").replace("\r", "").split("\n");
        boolean hasAnyScript = false;
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String trimmedLine = lines[lineNum].trim();
            String trimStart = lines[lineNum].replaceAll("^[\\s]+", "");
            if (trackSources && !trimmedLine.startsWith("#") && trimStart.length() == lines[lineNum].length() && trimmedLine.endsWith(":") && trimmedLine.length() > 1) {
                String name = trimmedLine.substring(0, trimmedLine.length() - 1).replace('\"', '\'').replace("'", "");
                scriptSources.put(name.toUpperCase(), filename);
                scriptOriginalNames.put(name.toUpperCase(), name);
                result.append(name.toUpperCase()).append(":\n");
                hasAnyScript = true;
            }
            else if (!trimmedLine.startsWith("#")) {
                if (trackSources && !hasAnyScript && trimmedLine.endsWith(":")) {
                    Debug.echoError("Script '" + filename + "' is broken: script container title has spaces in front.");
                    hasAnyScript = true;
                }
                if ((trimmedLine.startsWith("}") || trimmedLine.startsWith("{") || trimmedLine.startsWith("else")) && !trimmedLine.endsWith(":")) {
                    result.append(' ').append(lines[lineNum].replace('\0', ' ')
                            .replace(": ", "<&co>").replace("#", "<&ns>")).append("\n");
                }
                else {
                    String curLine = lines[lineNum].replace('\0', ' ');
                    boolean endsColon = trimmedLine.endsWith(":");
                    boolean startsDash = trimmedLine.startsWith("-");
                    if (!endsColon && startsDash) {
                        curLine = curLine.replace(": ", "<&co> ");
                        curLine = curLine.replace("#", "<&ns>");
                    }
                    else if (endsColon && !startsDash) {
                        if (curLine.contains(".")) {
                            curLine = curLine.replace("&", "&amp").replace(".", "&dot");
                            Debug.log("Originally " + trimmedLine + " became " + curLine);
                        }
                    }
                    if (trimmedLine.startsWith("- ") && !trimmedLine.startsWith("- \"") && !trimmedLine.startsWith("- '")) {
                        int dashIndex = curLine.indexOf('-');
                        curLine = curLine.substring(0, dashIndex + 1) + " " + ScriptBuilder.LINE_PREFIX_CHAR + (lineNum + 1) + ScriptBuilder.LINE_PREFIX_CHAR + curLine.substring(dashIndex + 1);
                    }
                    result.append(curLine).append("\n");
                }
            }
            else {
                result.append("\n");
            }
        }
        result.append("\n");
        return result.toString();
    }

    public static CharsetDecoder encoding = null;

    public static String convertStreamToString(InputStream is) {
        return convertStreamToString(is, false);
    }

    public static String convertStreamToString(InputStream is, boolean defaultUTF8) {
        Scanner s;
        if (encoding == null && !defaultUTF8) {
            s = new Scanner(is);
        }
        else {
            s = new Scanner(new InputStreamReader(is, encoding == null ? StandardCharsets.UTF_8.newDecoder() : encoding));
        }
        s.useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static YamlConfiguration loadConfig(String filename, InputStream resource) throws IOException {
        try {
            String script = clearComments(filename, convertStreamToString(resource, filename.endsWith(".dsc")), true);
            return YamlConfiguration.load(script);
        }
        finally {
            resource.close();
        }
    }

    private static String _concatenateCoreScripts() {

        scriptSources.clear();
        try {
            File file = DenizenCore.getImplementation().getScriptFolder();
            // Check if the directory exists
            if (!file.exists()) {
                Debug.echoError("No script folder found, please create one.");
                hadError = true;
                return "";
            }
            // Get files using script directory
            List<File> files = CoreUtilities.listDScriptFiles(file);
            if (files.size() > 0) {
                StringBuilder sb = new StringBuilder();
                List<String> scriptNames = new ArrayList<>(files.size() * 2);
                YamlConfiguration yaml;
                for (File f : files) {
                    String fileName = f.getAbsolutePath().substring(file.getAbsolutePath().length());
                    if (Debug.showLoading) {
                        Debug.log("Processing '" + fileName + "'... ");
                    }
                    try {
                        yaml = loadConfig(f.getAbsolutePath(), new FileInputStream(f));
                        String saved = yaml != null ? yaml.saveToString(false) : null;
                        if (saved != null && saved.length() > 0) {
                            handleListing(yaml, scriptNames);
                            sb.append(saved).append("\r\n");
                        }
                        else {
                            Debug.echoError("Woah! Error parsing " + fileName + "! This script has been skipped. No internal error - is the file empty?");
                            hadError = true;
                        }
                    }
                    catch (Exception e) {
                        Debug.echoError("Woah! Error parsing " + fileName + "!");
                        hadError = true;
                        Debug.echoError(e);
                    }
                }
                if (Debug.showLoading) {
                    Debug.echoApproval("All scripts loaded!");
                }
                return sb.toString();
            }
            else {
                Debug.echoError("Woah! No scripts in /plugins/Denizen/scripts/ to load!");
                hadError = true;
            }
        }
        catch (Exception e) {
            Debug.echoError("Woah! No script folder found in " + DenizenCore.getImplementation().getScriptFolder());
            hadError = true;
            Debug.echoError(e);
        }

        return "";
    }
}
