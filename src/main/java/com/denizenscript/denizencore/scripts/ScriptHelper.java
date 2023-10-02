package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.DenizenCore;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Reloads and retrieves information from the scripts folder.
 */
public class ScriptHelper {
    public static volatile ArrayList<YamlConfiguration> _yamlScripts;

    @Deprecated
    public static ArrayList<YamlConfiguration> additionalScripts = new ArrayList<>();

    /**
     * Add additional script file references here as wanted/needed. Use like:
     * <pre>
     * (scripts) -> scripts.add(YamlConfiguration.load(ScriptHelper.clearComments(MY_FILENAME, MY_FILE_TEXT, true)));
     * </pre>
     * Warning: can be called async.
     */
    public static List<Consumer<List<YamlConfiguration>>> buildAdditionalScripts = new ArrayList<>();

    private static void reloadFailed(Throwable ex) {
        hadError = true;
        Debug.echoError("Could not load scripts!");
        Debug.echoError(ex);
        _yamlScripts = new ArrayList<>();
    }

    public static void reloadScripts(boolean delayable, Runnable preReload, Consumer<Long> onFinished) {
        Runnable afterLoad = () -> {
            long midPoint = CoreUtilities.monotonicMillis();
            if (preReload != null) {
                preReload.run();
            }
            postLoad();
            _yamlScripts.addAll(additionalScripts);
            ScriptRegistry.buildCoreYamlScriptContainers(_yamlScripts);
            onFinished.accept(midPoint);
        };
        if (delayable) {
            DenizenCore.runAsync(() -> {
                try {
                    _yamlScripts = buildScriptList();
                    DenizenCore.runOnMainThread(afterLoad);
                }
                catch (Throwable ex) {
                    reloadFailed(ex);
                }
            });
        }
        else {
            try {
                _yamlScripts = buildScriptList();
                afterLoad.run();
            }
            catch (Throwable ex) {
                reloadFailed(ex);
            }
        }
    }

    private static volatile boolean hadError = false; // Note: can be called async

    public static boolean hadError() {
        return hadError;
    }

    public static void resetError() {
        hadError = false;
    }

    public static void setHadError() {
        hadError = true;
    }

    private static volatile HashMap<String, String> scriptSources = new HashMap<>();
    private static volatile HashMap<String, String> scriptOriginalNames = new HashMap<>();

    private static volatile HashMap<String, String> scriptSourcesInprogress;
    private static volatile HashMap<String, String> scriptOriginalNamesInprogress;

    public static String getSource(String script) {
        return scriptSources.get(CoreUtilities.toLowerCase(script));
    }

    public static String getOriginalName(String script) {
        return scriptOriginalNames.get(CoreUtilities.toLowerCase(script));
    }

    public static String clearComments(String filename, String input, boolean trackSources) {
        StringBuilder result = new StringBuilder(input.length());
        String[] linesArray = input.replace("\t", "    ").replace("\r", "").split("\n");
        List<String> lines = new ArrayList<>(Arrays.asList(linesArray));
        boolean hasAnyScript = false;
        int lineNum = 0;
        for (int i = 0; i < lines.size(); i++) {
            lineNum++;
            String line = lines.get(i);
            String trimmedLine = line.trim();
            String trimStart = line.replaceAll("^[\\s]+", "");
            if (trackSources && !trimmedLine.startsWith("#") && trimStart.length() == line.length() && trimmedLine.endsWith(":") && trimmedLine.length() > 1) {
                String name = trimmedLine.substring(0, trimmedLine.length() - 1).replace('\"', '\'').replace("'", "");
                scriptSourcesInprogress.put(CoreUtilities.toLowerCase(name), filename);
                scriptOriginalNamesInprogress.put(CoreUtilities.toLowerCase(name), name);
                result.append(CoreUtilities.toUpperCase(name)).append(":\n");
                hasAnyScript = true;
            }
            else if (!trimmedLine.startsWith("#")) {
                if (trackSources && !hasAnyScript && trimmedLine.endsWith(":")) {
                    Debug.echoError("Script '<Y>" + filename + "<W>' is broken: script container title has spaces in front.");
                    hasAnyScript = true;
                }
                String curLine = line.replace('\0', ' ');
                boolean endsColon = trimmedLine.endsWith(":");
                boolean startsDash = trimmedLine.startsWith("-");
                if (!endsColon && startsDash) {
                    curLine = curLine.replace(": ", "<&co> ");
                    curLine = curLine.replace("#", "<&ns>");
                }
                else if (endsColon && !startsDash) {
                    if (curLine.contains(".")) {
                        curLine = CoreUtilities.replace(curLine, "&", "&amp");
                        curLine = CoreUtilities.replace(curLine, ".", "&dot");
                    }
                }
                else if (!startsDash && (trimmedLine.contains(": &") || trimmedLine.contains(": *") || trimmedLine.contains(": !"))) {
                    int colon = curLine.indexOf(':');
                    curLine = curLine.substring(0, colon) + ": \"" + curLine.substring(colon + 2).replace("\"", "<&dq>") + "\"";
                }
                else if (!endsColon) {
                    int colon = curLine.indexOf(':');
                    curLine = curLine.substring(0, colon + 1) + CoreUtilities.replace(curLine.substring(colon + 1), ":", "<&co>");
                }
                if (trimmedLine.startsWith("- ") && !trimmedLine.startsWith("- \"") && !trimmedLine.startsWith("- '")) {
                    int dashIndex = curLine.indexOf('-');
                    curLine = curLine.substring(0, dashIndex + 1) + " " + ScriptBuilder.LINE_PREFIX_CHAR + lineNum + ScriptBuilder.LINE_PREFIX_CHAR + curLine.substring(dashIndex + 1);
                    int nextLine = i + 1;
                    if (nextLine < lines.size() && lines.get(nextLine).stripLeading().startsWith(".")) {
                        int lineCountBefore = lines.size();
                        curLine = inlineTagsForCommand(curLine, i, lines);
                        lineNum += lineCountBefore - lines.size();
                    }
                }
                result.append(curLine).append("\n");
            }
            else {
                result.append("\n");
            }
        }
        result.append("\n");
        return result.toString();
    }

    public static String inlineTagsForCommand(String command, int commandLine, List<String> lines) {
        StringBuilder inlinedCommand = new StringBuilder(command);
        ListIterator<String> iterator = lines.listIterator(commandLine + 1);
        while (iterator.hasNext()) {
            String nextPart = iterator.next().trim();
            if (!nextPart.startsWith(".")) {
                break;
            }
            inlinedCommand.append(nextPart);
            iterator.remove();
        }
        return inlinedCommand.toString();
    }

    public static String convertStreamToString(InputStream is) {
        return convertStreamToString(is, false);
    }

    public static String convertStreamToString(InputStream is, boolean defaultUTF8) {
        Scanner s;
        if (CoreConfiguration.scriptEncoding == null && !defaultUTF8) {
            s = new Scanner(is);
        }
        else {
            s = new Scanner(new InputStreamReader(is, CoreConfiguration.scriptEncoding == null ? StandardCharsets.UTF_8.newDecoder() : CoreConfiguration.scriptEncoding));
        }
        s.useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static YamlConfiguration loadConfig(String filename, InputStream resource) throws IOException { // Note: can be called async
        try {
            String script = clearComments(filename, convertStreamToString(resource, filename.endsWith(".dsc")), true);
            return YamlConfiguration.load(script);
        }
        finally {
            resource.close();
        }
    }

    private static ArrayList<YamlConfiguration> buildScriptList() { // Note: can be called async
        scriptSourcesInprogress = new HashMap<>();
        scriptOriginalNamesInprogress = new HashMap<>();
        try {
            File file = DenizenCore.implementation.getScriptFolder();
            // Check if the directory exists
            if (!file.exists()) {
                Debug.echoError("No script folder found, please create one.");
                hadError = true;
                return new ArrayList<>();
            }
            // Get files using script directory
            List<File> files = CoreUtilities.listDScriptFiles(file);
            if (files.size() > 0 || buildAdditionalScripts.size() > 0) {
                ArrayList<YamlConfiguration> outList = new ArrayList<>();
                YamlConfiguration yaml;
                for (File f : files) {
                    String fileName = f.getAbsolutePath().substring(file.getAbsolutePath().length());
                    if (CoreConfiguration.debugLoadingInfo) {
                        Debug.log("Processing '" + fileName + "'... ");
                    }
                    try {
                        yaml = loadConfig(f.getAbsolutePath(), new FileInputStream(f));
                        String saved = yaml != null ? yaml.saveToString(false) : null;
                        if (saved != null && saved.length() > 0) {
                            outList.add(yaml);
                        }
                        else {
                            Debug.echoError("Error parsing '<Y>" + fileName + "<W>'! This script has been skipped. No internal error - is the file empty?");
                            hadError = true;
                        }
                    }
                    catch (Exception e) {
                        Debug.echoError("Error parsing '<Y>" + fileName + "<W>'!");
                        hadError = true;
                        Debug.echoError(e);
                    }
                }
                for (Consumer<List<YamlConfiguration>> additional : buildAdditionalScripts) {
                    try {
                        additional.accept(outList);
                    }
                    catch (Exception ex) {
                        Debug.echoError("Error parsing additional script!");
                        hadError = true;
                        Debug.echoError(ex);
                    }
                }
                if (CoreConfiguration.debugLoadingInfo) {
                    Debug.echoApproval("All scripts loaded!");
                }
                return outList;
            }
            else {
                Debug.log("No scripts in /plugins/Denizen/scripts/ to load!");
                hadError = true;
            }
        }
        catch (Exception e) {
            Debug.echoError("No script folder found in " + DenizenCore.implementation.getScriptFolder());
            hadError = true;
            Debug.echoError(e);
        }
        return new ArrayList<>();
    }

    public static void postLoad() {
        scriptSources = scriptSourcesInprogress;
        scriptOriginalNames = scriptOriginalNamesInprogress;
    }
}
