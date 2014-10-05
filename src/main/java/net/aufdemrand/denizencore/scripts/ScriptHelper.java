package net.aufdemrand.denizencore.scripts;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;
import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        }

        DenizenCore.getImplementation().buildCoreContainers(getScripts());
    }

    public static YamlConfiguration _gs() {
        return getScripts();
    }

    private static YamlConfiguration getScripts() {
        if (_yamlScripts == null)
            reloadScripts();
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
        for (String str: config.getKeys(false)) {
            String up = str.toUpperCase();
            if (list.contains(up)) {
                hadError = true;
                DenizenCore.getImplementation().debugError("There is more than one script named '" + up + "'!");
            }
            else {
                list.add(up);
            }
        }
    }

    private static HashMap<String, String> scriptSources = new HashMap<String, String>();

    public static String getSource(String script) {
        return scriptSources.get(script.toUpperCase());
    }

    private static String ClearComments(String filename, String input) {
        StringBuilder result = new StringBuilder(input.length());
        String[] lines = input.replace("\t", "    ").replace("\r", "").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String trimStart = lines[i].replaceAll("^[\\s\\t]+", "");
            if (trimStart.length() == lines[i].length() && line.endsWith(":") && line.length() > 1) {
                scriptSources.put(line.substring(0, line.length() - 1).toUpperCase().replace('\"', '\'').replace("'", ""), filename);
            }
            if (!line.startsWith("#")) {
                if ((line.startsWith("}") || line.startsWith("{") || line.startsWith("else")) && !line.endsWith(":"))
                {
                    result.append(' ').append(lines[i].replace('\0', ' ')).append("\n");
                }
                else
                {
                    result.append(lines[i].replace('\0', ' ')).append("\n");
                }
            }
            else {
                result.append("\n");
            }
        }
        result.append("\n");
        return result.toString();
    }
    static String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static YamlConfiguration loadConfig(String filename, InputStream resource) throws IOException {

        return YamlConfiguration.load(ClearComments(filename, convertStreamToString(resource)));
    }

    private static String _concatenateCoreScripts() {

        scriptSources.clear();
        try {
            File file = null;
            file = DenizenCore.getImplementation().getScriptFolder();

            // Check if the directory exists
            if(!file.exists()) {
                dB.echoError("No script folder found, please create one.");
                hadError = true;
                return "";
            }


            // Get files using script directory
            List<File> files = CoreUtilities.listDScriptFiles(file);

            if (files.size() > 0) {
                StringBuilder sb = new StringBuilder();
                List<String> scriptNames = new ArrayList<String>();

                YamlConfiguration yaml;

                dB.log("Processing outside scripts... ");
                for (YamlConfiguration outsideConfig : DenizenCore.getImplementation().getOutsideScripts()) {
                    try {
                        dB.log("Processing unnamed script...");
                        sb.append(outsideConfig.saveToString()).append("\r\n");
                    }
                    catch (Exception e) {
                        dB.echoError("Woah! Error parsing outside scripts!");
                        hadError = true;
                    }
                }

                for (File f : files) {
                    String fileName = f.getAbsolutePath().substring(file.getAbsolutePath().length());
                    dB.log("Processing '" + fileName + "'... ");

                    try {
                        yaml = loadConfig(f.getAbsolutePath(), new FileInputStream(f));
                        String saved = yaml.saveToString();
                        if (yaml != null && saved.length() > 0) {
                            handleListing(yaml, scriptNames);
                            sb.append(saved).append("\r\n");
                        }
                        else {
                            dB.echoError("Woah! Error parsing " + fileName + "! This script has been skipped. No internal error - is the file empty?");
                            hadError = true;
                        }

                    } catch (RuntimeException e) {
                        dB.echoError("Woah! Error parsing " + fileName + "!");
                        hadError = true;
                        dB.echoError(e);
                    }
                }

                dB.echoApproval("All scripts loaded!");
                return yamlKeysToUpperCase(sb.toString());
            } else {
                dB.echoError("Woah! No scripts in /plugins/Denizen/scripts/ to load!");
                hadError = true;
            }

        } catch (Exception e) {
            dB.echoError("Woah! No script folder found in /plugins/Denizen/scripts/");
            hadError = true;
            dB.echoError(e);
        }

        return "";
    }


    static Pattern pattern = Pattern.compile("(^.*?[^\\s](:\\s))", Pattern.MULTILINE);

    /**
     * Changes YAML 'keys' to all Upper Case to de-sensitize case sensitivity when
     * reading and parsing scripts.
     */
    private static String yamlKeysToUpperCase(String string) {
        StringBuffer sb = new StringBuffer();
        Matcher matcher = pattern.matcher(string);
        while (matcher.find())
            matcher.appendReplacement(sb, matcher.group().toUpperCase());
        matcher.appendTail(sb);
        return sb.toString();
    }
}
