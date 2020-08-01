package com.denizenscript.denizencore.scripts.commands.file;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptHelper;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagManager;
import org.json.JSONObject;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;

public class YamlCommand extends AbstractCommand implements Holdable {

    public YamlCommand() {
        setName("yaml");
        setSyntax("yaml [create]/[load:<file>]/[loadtext:<text>]/[unload]/[savefile:<file>]/[copykey:<source key> <target key> (to_id:<name>)]/[set <key>([<#>])(:<action>):<value>] [id:<name>]");
        setRequiredArguments(2, 4);
        TagManager.registerTagHandler("yaml", this::yamlTagProcess);
        isProcedural = false;
    }

    // <--[command]
    // @Name Yaml
    // @Syntax yaml [create]/[load:<file>]/[loadtext:<text>]/[unload]/[savefile:<file>]/[copykey:<source key> <target key> (to_id:<name>)]/[set <key>([<#>])(:<action>):<value>] [id:<name>]
    // @Required 2
    // @Maximum 4
    // @Short Edits a YAML configuration file.
    // @Group file
    //
    // @Description
    // Edits a YAML configuration file.
    // This can be used for interacting with other plugins' configuration files.
    // It can also be used for storing your own script's data.
    //
    // Use waitable syntax ("- ~yaml load:...") with load or savefile actions to avoid locking up the server during file IO.
    // Refer to <@link language ~waitable>.
    //
    // For loading and saving, the starting path is within 'plugins/Denizen'.
    // The file path follows standard system file path rules. That means '/' separators folders,
    // and '..' as a folder name means go-up-one folder, for example '../WorldGuard/config.yml' would load the WorldGuard plugin config.
    // Also be aware that some servers (Linux/Mac based) have case sensitive file systems while others (Windows based) don't.
    // Generally, when using existing paths, make sure your casing is correct. When creating new paths, prefer all-lowercase to reduce risk of issues.
    //
    // Please note that all usages of the YAML command except for "load" and "savefile" arguments are purely in memory.
    // That means, if you use "set" to make changes, those changes will not be saved to any file, until you use "savefile".
    // Similarly, "create" does not create any file, instead it only creates a YAML object in RAM.
    //
    // In-memory changes to a loaded YAML object will mark that object as having changes. Before saving,
    // you can check whether the YAML object needs to be written to disk with the has_changes tag.
    //
    // Note that the '.yml' extension is not automatically appended, and you will have to include that in filenames.
    //
    // All usages of the YAML command must include the "id:" argument. This is any arbitrary name, as plaintext or from a tag,
    // to uniquely and globally identify the YAML object in memory. This ID can only be used by one YAML object at a type.
    // IDs are stored when "create" or "load" arguments are used, and only removed when "unload" is used.
    // If, for example, you have a unique YAML data container per-player, you might use something like "id:myscript_<player>".
    //
    // For ways to use the "set" argument, refer to <@link language data actions>.
    //
    // @Tags
    // <yaml[<idname>].contains[<path>]>
    // <yaml[<idname>].read[<path>]>
    // <yaml[<idname>].list_keys[<path>]>
    // <yaml[<idname>].has_changes>
    //
    // @Usage
    // Use to create a new YAML file.
    // - yaml create id:myfile
    //
    // @Usage
    // Use to load a YAML file from disk.
    // - ~yaml load:myfile.yml id:myfile
    //
    // @Usage
    // Use to modify a YAML file similarly to a flag.
    // - yaml id:myfile set my.key:HelloWorld
    //
    // @Usage
    // Use to save a YAML file to disk.
    // - ~yaml savefile:myfile.yml id:myfile
    //
    // @Usage
    // Use to unload a YAML file from memory.
    // - yaml unload id:myfile
    //
    // @Usage
    // Use to modify a YAML file similarly to a flag.
    // - yaml id:myfile set my.key:+:2
    //
    // @Usage
    // Use to modify a YAML file similarly to a flag.
    // - yaml id:myfile set my.key[2]:hello
    //
    // @Usage
    // Use to modify a copy the contents of one YAML key to a new owning key.
    // - yaml id:myfile copykey:my.first.key my.new.key
    //
    // @Usage
    // Use to modify a copy the contents of one YAML key to a new owning key on a different YAML file.
    // - yaml id:myfile copykey:my.first.key my.new.key to_id:myotherfile
    // -->

    public Map<String, YamlConfiguration> yamlDocuments = new HashMap<>();

    private YamlConfiguration getYaml(String id) {
        if (id == null) {
            Debug.echoError("Trying to get YAML file with NULL ID!");
            return null;
        }
        return yamlDocuments.get(CoreUtilities.toLowerCase(id));
    }

    public enum Action {LOAD, LOADTEXT, UNLOAD, CREATE, SAVE, SET, COPYKEY}

    public enum YAML_Action {
        SET_VALUE, INCREASE, DECREASE, MULTIPLY,
        DIVIDE, INSERT, REMOVE, SPLIT, DELETE, SPLIT_NEW
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        boolean isSet = false;
        boolean isCopyKey = false;
        for (Argument arg : scriptEntry.getProcessedArgs()) {
            if (!scriptEntry.hasObject("action") &&
                    arg.matchesPrefix("load")) {
                scriptEntry.addObject("action", new ElementTag("LOAD"));
                scriptEntry.addObject("filename", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action") &&
                    arg.matchesPrefix("loadtext")) {
                scriptEntry.addObject("action", new ElementTag("LOADTEXT"));
                scriptEntry.addObject("raw_text", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action") &&
                    arg.matchesPrefix("savefile", "filesave")) {
                scriptEntry.addObject("action", new ElementTag("SAVE"));
                scriptEntry.addObject("filename", arg.asElement());
            }
            else if (!scriptEntry.hasObject("action") &&
                    arg.matches("create")) {
                scriptEntry.addObject("action", new ElementTag("CREATE"));
            }
            else if (!scriptEntry.hasObject("action") &&
                    arg.matches("set")) {
                scriptEntry.addObject("action", new ElementTag("SET"));
                isSet = true;
            }
            else if (!scriptEntry.hasObject("action") &&
                    arg.matchesPrefix("copykey")) {
                scriptEntry.addObject("action", new ElementTag("COPYKEY"));
                scriptEntry.addObject("key", arg.asElement());
                isSet = true;
                isCopyKey = true;
            }
            else if (!scriptEntry.hasObject("action") &&
                    arg.matches("unload")) {
                scriptEntry.addObject("action", new ElementTag("UNLOAD"));
            }
            else if (!scriptEntry.hasObject("value") &&
                    arg.matchesPrefix("value")) {
                if (arg.matchesArgumentType(ListTag.class)) {
                    scriptEntry.addObject("value", arg.asType(ListTag.class));
                }
                else {
                    scriptEntry.addObject("value", arg.asElement());
                }
            }
            else if (!scriptEntry.hasObject("id") &&
                    arg.matchesPrefix("id")) {
                scriptEntry.addObject("id", arg.asElement());
            }
            else if (!scriptEntry.hasObject("to_id") &&
                    arg.matchesPrefix("to_id")) {
                scriptEntry.addObject("to_id", arg.asElement());
            }
            else if (!scriptEntry.hasObject("split") &&
                    arg.matches("split_list")) {
                scriptEntry.addObject("split", new ElementTag("true"));
            }
            else if (!scriptEntry.hasObject("fix_formatting") &&
                    arg.matches("fix_formatting")) {
                scriptEntry.addObject("fix_formatting", new ElementTag("true"));
            }
            // Check for key:value/action
            else if (isSet &&
                    !scriptEntry.hasObject("value") &&
                    arg.raw_value.split(":", 3).length == 2) {

                String[] flagArgs = arg.raw_value.split(":", 2);
                scriptEntry.addObject("key", new ElementTag(flagArgs[0]));

                if (flagArgs[1].equals("++") || flagArgs[1].equals("+")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.INCREASE);
                    scriptEntry.addObject("value", new ElementTag(1));
                }
                else if (flagArgs[1].equals("--") || flagArgs[1].equals("-")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.DECREASE);
                    scriptEntry.addObject("value", new ElementTag(1));
                }
                else if (flagArgs[1].equals("!")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.DELETE);
                    scriptEntry.addObject("value", new ElementTag(false));
                }
                else if (flagArgs[1].equals("<-")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.REMOVE);
                    scriptEntry.addObject("value", new ElementTag(false));
                }
                else {
                    // No ACTION, we're just setting a value...
                    scriptEntry.addObject("yaml_action", YAML_Action.SET_VALUE);
                    scriptEntry.addObject("value", new ElementTag(flagArgs[1]));
                }
            }
            // Check for key:action:value
            else if (isSet &&
                    !scriptEntry.hasObject("value") &&
                    arg.raw_value.split(":", 3).length == 3) {
                String[] flagArgs = arg.raw_value.split(":", 3);
                scriptEntry.addObject("key", new ElementTag(flagArgs[0]));

                if (flagArgs[1].equals("->")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.INSERT);
                }
                else if (flagArgs[1].equals("<-")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.REMOVE);
                }
                else if (flagArgs[1].equals("||") || flagArgs[1].equals("|")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.SPLIT);
                }
                else if (flagArgs[1].equals("!|")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.SPLIT_NEW);
                }
                else if (flagArgs[1].equals("++") || flagArgs[1].equals("+")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.INCREASE);
                }
                else if (flagArgs[1].equals("--") || flagArgs[1].equals("-")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.DECREASE);
                }
                else if (flagArgs[1].equals("**") || flagArgs[1].equals("*")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.MULTIPLY);
                }
                else if (flagArgs[1].equals("//") || flagArgs[1].equals("/")) {
                    scriptEntry.addObject("yaml_action", YAML_Action.DIVIDE);
                }
                else {
                    scriptEntry.addObject("yaml_action", YAML_Action.SET_VALUE);
                    scriptEntry.addObject("value", new ElementTag(arg.raw_value.split(":", 2)[1]));
                    continue;
                }
                scriptEntry.addObject("value", new ElementTag(flagArgs[2]));
            }
            else if (isCopyKey && !scriptEntry.hasObject("value")) {
                scriptEntry.addObject("value", arg.asElement());
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("id")) {
            throw new InvalidArgumentsException("Must specify an id!");
        }
        if (!scriptEntry.hasObject("action")) {
            throw new InvalidArgumentsException("Must specify an action!");
        }
        scriptEntry.defaultObject("value", new ElementTag(""));
        scriptEntry.defaultObject("fix_formatting", new ElementTag("false"));
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        ElementTag filename = scriptEntry.getElement("filename");
        ElementTag rawText = scriptEntry.getElement("raw_text");
        ElementTag key = scriptEntry.getElement("key");
        ObjectTag value = scriptEntry.getObjectTag("value");
        ElementTag split = scriptEntry.getElement("split");
        YAML_Action yaml_action = (YAML_Action) scriptEntry.getObject("yaml_action");
        ElementTag actionElement = scriptEntry.getElement("action");
        ElementTag idElement = scriptEntry.getElement("id");
        ElementTag fixFormatting = scriptEntry.getElement("fix_formatting");
        ElementTag toId = scriptEntry.getElement("to_id");
        YamlConfiguration yamlConfiguration;
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(),
                    idElement.debug()
                            + actionElement.debug()
                            + (filename != null ? filename.debug() : "")
                            + (yaml_action != null ? ArgumentHelper.debugObj("yaml_action", yaml_action.name()) : "")
                            + (key != null ? key.debug() : "")
                            + (value != null ? value.debug() : "")
                            + (split != null ? split.debug() : "")
                            + (rawText != null ? rawText.debug() : "")
                            + (toId != null ? toId.debug() : ""));
        }

        // Do action
        Action action = Action.valueOf(actionElement.asString().toUpperCase());
        final String id = CoreUtilities.toLowerCase(idElement.asString());

        if (action != Action.LOAD && action != Action.SAVE && scriptEntry.shouldWaitFor()) {
            scriptEntry.setFinished(true);
        }
        switch (action) {

            case LOAD:
                File file = new File(DenizenCore.getImplementation().getDataFolder(), filename.asString());
                if (!DenizenCore.getImplementation().canReadFile(file)) {
                    Debug.echoError("Server config denies reading files in that location.");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (!file.exists()) {
                    Debug.echoError("File cannot be found!");
                    scriptEntry.setFinished(true);
                    return;
                }
                YamlConfiguration[] runnableConfigs = new YamlConfiguration[1];
                Runnable onLoadCompleted = new Runnable() {
                    @Override
                    public void run() {
                        yamlDocuments.remove(id);
                        yamlDocuments.put(id, runnableConfigs[0]);
                        scriptEntry.setFinished(true);
                    }
                };
                Runnable loadRunnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            FileInputStream fis = new FileInputStream(file);
                            String str = ScriptHelper.convertStreamToString(fis);
                            if (fixFormatting.asBoolean()) {
                                str = ScriptHelper.clearComments("", str, false);
                                Deprecations.yamlFixFormatting.warn(scriptEntry);
                            }
                            runnableConfigs[0] = YamlConfiguration.load(str);
                            fis.close();
                            if (runnableConfigs[0] == null) {
                                runnableConfigs[0] = new YamlConfiguration();
                            }
                            if (scriptEntry.shouldWaitFor()) {
                                DenizenCore.schedule(new OneTimeSchedulable(onLoadCompleted, 0));
                            }
                            else {
                                onLoadCompleted.run();
                            }
                        }
                        catch (Exception e) {
                            Debug.echoError("Failed to load yaml file: " + e);
                        }
                    }
                };
                if (scriptEntry.shouldWaitFor()) {
                    DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(loadRunnable, 0)));
                }
                else {
                    loadRunnable.run();
                }
                break;

            case LOADTEXT:
                String str = rawText.asString();
                YamlConfiguration config = YamlConfiguration.load(str);
                yamlDocuments.remove(id);
                yamlDocuments.put(id, config);
                scriptEntry.setFinished(true);
                break;

            case UNLOAD:
                if (yamlDocuments.containsKey(id)) {
                    yamlDocuments.remove(id);
                }
                else {
                    Debug.echoError("Unknown YAML ID '" + id + "'");
                }
                break;

            case SAVE:
                if (yamlDocuments.containsKey(id)) {
                    try {
                        if (!DenizenCore.getImplementation().allowStrangeYAMLSaves()) {
                            File fileObj = new File(DenizenCore.getImplementation().
                                    getDataFolder().getAbsolutePath() + "/" + filename.asString());
                            String directory = URLDecoder.decode(System.getProperty("user.dir"));
                            if (!fileObj.getCanonicalPath().startsWith(directory)) {
                                Debug.echoError("Outside-the-main-folder YAML saves disabled by administrator.");
                                scriptEntry.setFinished(true);
                                return;
                            }
                        }
                        File fileObj = new File(DenizenCore.getImplementation().
                                getDataFolder().getAbsolutePath() + "/" + filename.asString());
                        if (!DenizenCore.getImplementation().canWriteToFile(fileObj)) {
                            Debug.echoError(scriptEntry.getResidingQueue(), "Cannot edit that file!");
                            scriptEntry.setFinished(true);
                            return;
                        }
                        fileObj.getParentFile().mkdirs();
                        YamlConfiguration yaml = yamlDocuments.get(id);
                        String outp = yaml.saveToString(false);
                        yaml.setDirty(false);
                        Runnable saveRunnable = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Charset charset = ScriptHelper.encoding == null ? null : ScriptHelper.encoding.charset();
                                    FileOutputStream fiout = new FileOutputStream(fileObj);
                                    OutputStreamWriter writer;
                                    if (charset == null) {
                                        writer = new OutputStreamWriter(fiout);
                                    }
                                    else {
                                        writer = new OutputStreamWriter(fiout, charset);
                                    }
                                    writer.write(outp);
                                    writer.close();
                                }
                                catch (IOException e) {
                                    Debug.echoError(e);
                                }
                                scriptEntry.setFinished(true);
                            }
                        };
                        if (scriptEntry.shouldWaitFor()) {
                            DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(saveRunnable, 0)));
                        }
                        else {
                            saveRunnable.run();
                        }
                    }
                    catch (IOException e) {
                        Debug.echoError(e);
                    }
                }
                else {
                    Debug.echoError("Unknown YAML ID '" + id + "'");
                    scriptEntry.setFinished(true);
                }
                break;

            case COPYKEY: {
                if (!yamlDocuments.containsKey(id)) {
                    break;
                }
                YamlConfiguration yaml = yamlDocuments.get(id);
                YamlConfiguration destYaml = yaml;
                if (toId != null) {
                    destYaml = getYaml(toId.toString());
                    if (destYaml == null) {
                        Debug.echoError("Unknown YAML TO-ID '" + id + "'");
                        break;
                    }
                }
                YamlConfiguration sourceSection = yaml.getConfigurationSection(key.asString());
                if (sourceSection == null) {
                    Debug.echoError("Invalid YAML section key name '" + key.asString() + "'.");
                    break;
                }
                YamlConfiguration newSection = copySection(sourceSection);
                destYaml.set(value.toString(), newSection);
                break;
            }

            case SET:
                if (yamlDocuments.containsKey(id)) {
                    if (yaml_action == null || key == null || value == null) {
                        Debug.echoError("Must specify a YAML action and value!");
                        return;
                    }
                    YamlConfiguration yaml = yamlDocuments.get(id);

                    int index = -1;
                    if (key.asString().contains("[")) {
                        try {
                            if (Debug.verbose) {
                                Debug.echoDebug(scriptEntry, "Try index: " + key.asString().split("\\[")[1].replace("]", ""));
                            }
                            index = Integer.valueOf(key.asString().split("\\[")[1].replace("]", "")) - 1;
                        }
                        catch (Exception e) {
                            if (Debug.verbose) {
                                Debug.echoError(scriptEntry.getResidingQueue(), e);
                            }
                            index = -1;
                        }
                        key = new ElementTag(key.asString().split("\\[")[0]);
                    }

                    String keyStr = key.asString();
                    String valueStr = value.identify();

                    switch (yaml_action) {
                        case INCREASE: {
                            String originalVal = Get(yaml, index, keyStr, "0");
                            if (!ArgumentHelper.matchesDouble(originalVal)) {
                                originalVal = "0";
                            }
                            if (!ArgumentHelper.matchesDouble(valueStr)) {
                                Debug.echoError("YAML action required a decimal number, was given not-a-decimal-number: " + valueStr);
                                return;
                            }
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) + Double.parseDouble(valueStr)));
                            break;
                        }
                        case DECREASE: {
                            String originalVal = Get(yaml, index, keyStr, "0");
                            if (!ArgumentHelper.matchesDouble(originalVal)) {
                                originalVal = "0";
                            }
                            if (!ArgumentHelper.matchesDouble(valueStr)) {
                                Debug.echoError("YAML action required a decimal number, was given not-a-decimal-number: " + valueStr);
                                return;
                            }
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) - Double.parseDouble(valueStr)));
                            break;
                        }
                        case MULTIPLY: {
                            String originalVal = Get(yaml, index, keyStr, "1");
                            if (!ArgumentHelper.matchesDouble(originalVal)) {
                                originalVal = "0";
                            }
                            if (!ArgumentHelper.matchesDouble(valueStr)) {
                                Debug.echoError("YAML action required a decimal number, was given not-a-decimal-number: " + valueStr);
                                return;
                            }
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) * Double.parseDouble(valueStr)));
                            break;
                        }
                        case DIVIDE: {
                            String originalVal = Get(yaml, index, keyStr, "1");
                            if (!ArgumentHelper.matchesDouble(originalVal)) {
                                originalVal = "0";
                            }
                            if (!ArgumentHelper.matchesDouble(valueStr)) {
                                Debug.echoError("YAML action required a decimal number, was given not-a-decimal-number: " + valueStr);
                                return;
                            }
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) / Double.parseDouble(valueStr)));
                            break;
                        }
                        case DELETE:
                            yaml.set(keyStr, null);
                            break;
                        case SET_VALUE:
                            Set(yaml, index, keyStr, valueStr);
                            break;
                        case INSERT: {
                            List<String> list = yaml.getStringList(keyStr);
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            list.add(valueStr);
                            yaml.set(keyStr, list);
                            break;
                        }
                        case REMOVE: {
                            List<String> list = yaml.getStringList(keyStr);
                            if (list == null) {
                                if (Debug.verbose) {
                                    Debug.echoDebug(scriptEntry, "List null!");
                                }
                                break;
                            }
                            if (index > -1 && index < list.size()) {
                                if (Debug.verbose) {
                                    Debug.echoDebug(scriptEntry, "Remove ind: " + index);
                                }
                                list.remove(index);
                                yaml.set(keyStr, list);
                            }
                            else {
                                if (Debug.verbose) {
                                    Debug.echoDebug(scriptEntry, "Remvoe value: " + valueStr);
                                }
                                for (int i = 0; i < list.size(); i++) {
                                    if (list.get(i).equalsIgnoreCase(valueStr)) {
                                        list.remove(i);
                                        break;
                                    }
                                }
                                yaml.set(keyStr, list);
                                break;
                            }
                            break;
                        }
                        case SPLIT_NEW: {
                            yaml.set(keyStr, new ArrayList<>(ListTag.valueOf(valueStr, scriptEntry.getContext())));
                            break;
                        }
                        case SPLIT: {
                            List<String> list = yaml.getStringList(keyStr);
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            list.addAll(ListTag.valueOf(valueStr, scriptEntry.getContext()));
                            yaml.set(keyStr, list);
                            break;
                        }
                    }
                }
                else {
                    Debug.echoError("Unknown YAML ID '" + id + "'");
                }
                break;

            case CREATE:
                yamlDocuments.remove(id);
                yamlConfiguration = new YamlConfiguration();
                yamlDocuments.put(id, yamlConfiguration);
                break;
        }

    }

    public YamlConfiguration copySection(YamlConfiguration section) {
        YamlConfiguration newSection = new YamlConfiguration();
        for (StringHolder key : section.getKeys(false)) {
            Object obj = section.get(key.str);
            if (obj instanceof YamlConfiguration) {
                obj = copySection((YamlConfiguration) obj);
            }
            newSection.set(key.str, obj);
        }
        return newSection;
    }

    public String Get(YamlConfiguration yaml, int index, String key, String def) {
        if (index == -1) {
            return yaml.getString(key, def);
        }
        else {
            List<String> list = yaml.getStringList(key);
            if (index < 0) {
                index = 0;
            }
            if (index > list.size()) {
                index = list.size() - 1;
            }
            if (list.isEmpty()) {
                return "";
            }
            return list.get(index);
        }
    }

    public void Set(YamlConfiguration yaml, int index, String key, String value) {
        if (index == -1) {
            if (value.startsWith("map@")) {
                MapTag map = MapTag.valueOf(value, CoreUtilities.noDebugContext);
                if (map != null) {
                    yaml.set(key, CoreUtilities.objectTagToJavaForm(map, true));
                    return;
                }
            }
            yaml.set(key, value);
        }
        else {
            List<String> list = yaml.getStringList(key);
            if (list == null) {
                list = new ArrayList<>();
            }
            if (index < 0) {
                index = 0;
            }
            if (index >= list.size()) {
                list.add(value);
            }
            else {
                list.set(index, value);
            }
            yaml.set(key, list);
        }
    }

    public ObjectTag yamlTagProcess(Attribute attribute) {
        String id = attribute.hasContext(1) ? CoreUtilities.toLowerCase(attribute.getContext(1)) : null;
        attribute.fulfill(1);

        // <--[tag]
        // @attribute <yaml.list>
        // @returns ListTag
        // @description
        // Returns a list of all currently loaded YAML ID's.
        // -->
        if (attribute.startsWith("list")) {
            ListTag list = new ListTag();
            list.addAll(yamlDocuments.keySet());
            return list;
        }
        if (id == null) {
            attribute.echoError("yaml[...] tag must specify a YAML id.");
            return null;
        }
        YamlConfiguration yaml = getYaml(id);
        if (yaml == null) {
            attribute.echoError("YAML tag has specified an invalid ID, or the specified id has already been closed. Tag replacement aborted. ID given: '" + id + "'.");
            return null;
        }

        // <--[tag]
        // @attribute <yaml[<id>].contains[<path>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the file has the specified path.
        // Otherwise, returns false.
        // -->
        if (attribute.startsWith("contains") && attribute.hasContext(1)) {
            return new ElementTag(yaml.contains(attribute.getContext(1)));
        }

        // <--[tag]
        // @attribute <yaml[<id>].is_list[<path>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the specified path results in a list.
        // -->
        if (attribute.startsWith("is_list") && attribute.hasContext(1)) {
            return new ElementTag(yaml.isList(attribute.getContext(1)));
        }

        // <--[tag]
        // @attribute <yaml[<id>].parsed_key[<path>]>
        // @returns ElementTag
        // @description
        // Returns the value from a data key on the YAML document as an ElementTag, ListTag, or MapTag.
        // Will automatically parse any tags contained within the value of the key, preserving key data structure
        // (meaning, a tag that returns a ListTag, inside a data list, will insert a ListTag inside the returned ListTag, as you would expect).
        // Generally, prefer to use <@link tag yaml.read>.
        // -->
        if (attribute.startsWith("parsed_key") && attribute.hasContext(1)) {
            Object obj = yaml.get(attribute.getContext(1));
            if (obj == null) {
                return null;
            }
            return CoreUtilities.objectToTagForm(obj, attribute.context, false, true);
        }

        // <--[tag]
        // @attribute <yaml[<id>].read[<path>]>
        // @returns ElementTag
        // @description
        // Returns the value from a data key on the YAML document as an ElementTag, ListTag, or MapTag.
        // -->
        if (attribute.startsWith("read") && attribute.hasContext(1)) {
            Object obj = yaml.get(attribute.getContext(1));
            if (obj == null) {
                return null;
            }
            return CoreUtilities.objectToTagForm(obj, attribute.context);
        }

        // <--[tag]
        // @attribute <yaml[<id>].list_deep_keys[<path>]>
        // @returns ListTag
        // @description
        // Returns a ListTag of all the keys at the path and all subpaths.
        // Use empty path input to represent the root of the yaml document tree.
        // -->
        if (attribute.startsWith("list_deep_keys") && attribute.hasContext(1)) {
            Set<StringHolder> keys;
            String path = attribute.getContext(1);
            if (path != null && path.length() > 0) {
                YamlConfiguration section = yaml.getConfigurationSection(path);
                if (section == null) {
                    return null;
                }
                keys = section.getKeys(true);
            }
            else {
                keys = yaml.getKeys(true);
            }
            if (keys == null) {
                return null;

            }
            else {
                return new ListTag(keys);
            }
        }

        // <--[tag]
        // @attribute <yaml[<id>].list_keys[<path>]>
        // @returns ListTag
        // @description
        // Returns a ListTag of all the keys at the path (and not sub-keys).
        // Use empty path input to represent the root of the yaml document tree.
        // -->
        if (attribute.startsWith("list_keys") && attribute.hasContext(1)) {
            Set<StringHolder> keys;
            String path = attribute.getContext(1);
            if (path != null && path.length() > 0) {
                YamlConfiguration section = yaml.getConfigurationSection(path);
                if (section == null) {
                    return null;
                }
                keys = section.getKeys(false);
            }
            else {
                keys = yaml.getKeys(false);
            }
            if (keys == null) {
                return null;

            }
            else {
                return new ListTag(keys);
            }
        }

        // <--[tag]
        // @attribute <yaml[<id>].has_changes>
        // @returns ElementTag(Boolean)
        // @description
        // Returns whether this YAML object has had changes since the last save or load.
        // -->
        if (attribute.startsWith("has_changes")) {
            return new ElementTag(yaml.isDirty());
        }

        // <--[tag]
        // @attribute <yaml[<id>].to_json>
        // @returns ElementTag
        // @description
        // Converts the YAML container to a JSON array.
        // -->
        if (attribute.startsWith("to_json")) {
            return new ElementTag(new JSONObject(yaml.getMap()).toString());
        }

        // <--[tag]
        // @attribute <yaml[<id>].to_text>
        // @returns ElementTag
        // @description
        // Converts the YAML container to raw YAML text.
        // -->
        if (attribute.startsWith("to_text")) {
            return new ElementTag(yaml.saveToString(false));
        }
        return null;
    }
}
