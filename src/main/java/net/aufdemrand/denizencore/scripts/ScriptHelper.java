package net.aufdemrand.denizencore.scripts;

import net.aufdemrand.denizencore.DenizenCore;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.YamlConfiguration;
import net.aufdemrand.denizencore.utilities.debugging.dB;
import net.aufdemrand.denizencore.utilities.text.StringHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

        DenizenCore.getImplementation().buildCoreContainers(_yamlScripts);
    }

    public static YamlConfiguration _gs() {
        return getScripts();
    }

    private static YamlConfiguration getScripts() {
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
                dB.echoError("There is more than one script named '" + up + "'!");
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

    public static String ClearComments(String filename, String input, boolean trackSources) {
        StringBuilder result = new StringBuilder(input.length());
        String[] lines = input.replace("\t", "    ").replace("\r", "").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            String trimStart = lines[i].replaceAll("^[\\s\\t]+", "");
            if (trackSources && !line.startsWith("#") && trimStart.length() == lines[i].length() && line.endsWith(":") && line.length() > 1) {
                String name = line.substring(0, line.length() - 1).replace('\"', '\'').replace("'", "");
                scriptSources.put(name.toUpperCase(), filename);
                scriptOriginalNames.put(name.toUpperCase(), name);
                result.append(name.toUpperCase() + ":\n");
            }
            else if (!line.startsWith("#")) {
                if ((line.startsWith("}") || line.startsWith("{") || line.startsWith("else")) && !line.endsWith(":")) {
                    result.append(' ').append(lines[i].replace('\0', ' ')
                            .replace(": ", "<&co>").replace("#", "<&ns>")).append("\n");
                }
                else {
                    String liner = lines[i].replace('\0', ' ');
                    if (!line.endsWith(":") && line.startsWith("-")) {
                        liner = liner.replace(": ", "<&co> ");
                        liner = liner.replace("#", "<&ns>");
                    }
                    result.append(liner.replace('\0', ' ')).append("\n");
                }
            }
            else {
                result.append("\n");
            }
        }
        result.append("\n");
        return result.toString();
    }

    public static String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static YamlConfiguration loadConfig(String filename, InputStream resource) throws IOException {
        try {
            String script = ClearComments(filename, convertStreamToString(resource), true);
            return YamlConfiguration.load(script);
        }
        finally {
            resource.close();
        }
    }

    private static String _concatenateCoreScripts() {

        scriptSources.clear();
        try {
            File file = null;
            file = DenizenCore.getImplementation().getScriptFolder();

            // Check if the directory exists
            if (!file.exists()) {
                dB.echoError("No script folder found, please create one.");
                hadError = true;
                return "";
            }


            // Get files using script directory
            List<File> files = CoreUtilities.listDScriptFiles(file);

            if (files.size() > 0) {
                StringBuilder sb = new StringBuilder();
                List<String> scriptNames = new ArrayList<>();

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
                        String saved = yaml != null ? yaml.saveToString() : null;
                        if (saved != null && saved.length() > 0) {
                            handleListing(yaml, scriptNames);
                            sb.append(saved).append("\r\n");
                        }
                        else {
                            dB.echoError("Woah! Error parsing " + fileName + "! This script has been skipped. No internal error - is the file empty?");
                            hadError = true;
                        }

                    }
                    catch (Exception e) {
                        dB.echoError("Woah! Error parsing " + fileName + "!");
                        hadError = true;
                        dB.echoError(e);
                    }
                }

                dB.echoApproval("All scripts loaded!");
                return sb.toString();
            }
            else {
                dB.echoError("Woah! No scripts in /plugins/Denizen/scripts/ to load!");
                hadError = true;
            }

        }
        catch (Exception e) {
            dB.echoError("Woah! No script folder found in /plugins/Denizen/scripts/");
            hadError = true;
            dB.echoError(e);
        }

        return "";
    }
}
