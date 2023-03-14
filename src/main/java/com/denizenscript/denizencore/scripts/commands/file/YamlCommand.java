package com.denizenscript.denizencore.scripts.commands.file;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptHelper;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.data.ActionableDataProvider;
import com.denizenscript.denizencore.utilities.data.DataAction;
import com.denizenscript.denizencore.utilities.data.DataActionHelper;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.text.StringHolder;
import org.json.JSONObject;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;

public class YamlCommand extends AbstractCommand implements Holdable {

    public YamlCommand() {
        setName("yaml");
        setSyntax("yaml [create]/[load:<file>]/[loadtext:<text> raw_format]/[unload]/[savefile:<file>]/[copykey:<source_key> <target_key> (to_id:<name>)]/[set <key>([<#>])(:<action>):<value> (data_type:{string}/integer/double/boolean/auto)] [id:<name>]");
        setRequiredArguments(2, 5);
        TagManager.registerTagHandler(ObjectTag.class, "yaml", this::yamlTagProcess);
        isProcedural = false;
        allowedDynamicPrefixes = true;
    }

    // <--[command]
    // @Name Yaml
    // @Syntax yaml [create]/[load:<file>]/[loadtext:<text> raw_format]/[unload]/[savefile:<file>]/[copykey:<source_key> <target_key> (to_id:<name>)]/[set <key>([<#>])(:<action>):<value> (data_type:{string}/integer/double/boolean/auto)] [id:<name>]
    // @Required 2
    // @Maximum 5
    // @Short Edits YAML data, especially for YAML files.
    // @Group file
    //
    // @Description
    // Edits YAML configuration data.
    //
    // This commands exists primarily for interoperability with pre-existing data files and other plugins.
    // It should never be used for storing data that only Denizen needs to use. Consider instead using <@link command flag>.
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
    // When loading, optionally specify 'raw_format' to indicate that this YAML file needs to maintain compatibility with some external system using raw YAML data
    // (for example, when altering YAML data files used by external plugins).
    // Note that this can have side effects of custom data disappearing (for example, the value "yes" gets magically converted to "true") or strange data parsing in.
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
    // When setting a value directly, you can optionally specify "data_type" as "string", "integer", "double", "boolean", or "auto",
    // to force the input to a specific data type, which may be needed for compatibility with some external YAML files.
    // Only applicable when setting a single value, not lists/maps/etc.
    // 'Auto' will attempt to choose the best type for the value.
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

    public enum DataType { STRING, INTEGER, DOUBLE, BOOLEAN, AUTO }

    @Override
    public void addCustomTabCompletions(TabCompletionsBuilder tab) {
        tab.addWithPrefix("id:", yamlDocuments.keySet());
    }

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        boolean isSet = false;
        boolean isCopyKey = false;
        for (Argument arg : scriptEntry) {
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
                isCopyKey = true;
            }
            else if (!scriptEntry.hasObject("action") &&
                    arg.matches("unload")) {
                scriptEntry.addObject("action", new ElementTag("UNLOAD"));
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
                Deprecations.yamlFixFormatting.warn(scriptEntry);
            }
            else if (!scriptEntry.hasObject("raw_format") &&
                    arg.matches("raw_format")) {
                scriptEntry.addObject("raw_format", new ElementTag("true"));
            }
            else if (!scriptEntry.hasObject("data_type") &&
                    arg.matchesPrefix("data_type") &&
                    arg.matchesEnum(DataType.class)) {
                scriptEntry.addObject("data_type", arg.asElement());
            }
            // Check for key:value/action
            else if (isSet &&
                    !scriptEntry.hasObject("data_action")) {
                scriptEntry.addObject("yaml_action", YAML_Action.SET_VALUE);
                scriptEntry.addObject("data_action", DataActionHelper.parse(new YamlActionProvider(), arg, scriptEntry.context));
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
        ElementTag toId = scriptEntry.getElement("to_id");
        ElementTag dataType = scriptEntry.getElement("data_type");
        ElementTag rawFormat = scriptEntry.getElement("raw_format");
        DataAction dataAction = (DataAction) scriptEntry.getObject("data_action");
        YamlConfiguration yamlConfiguration;
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), idElement, actionElement, filename, key, value, split, rawText, toId, dataType, rawFormat, (yaml_action != null ? db("yaml_action", yaml_action.name()) : null), dataAction);
        }
        // Do action
        Action action = actionElement.asEnum(Action.class);
        final String id = idElement.asLowerString();
        if (action != Action.LOAD && action != Action.SAVE && scriptEntry.shouldWaitFor()) {
            scriptEntry.setFinished(true);
        }
        switch (action) {
            case LOAD:
                File file = new File(DenizenCore.implementation.getDataFolder(), filename.asString());
                if (!DenizenCore.implementation.canReadFile(file)) {
                    Debug.echoError("Cannot read from that file path due to security settings in Denizen/config.yml.");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (!file.exists()) {
                    Debug.echoError("File cannot be found!");
                    scriptEntry.setFinished(true);
                    return;
                }
                YamlConfiguration[] runnableConfigs = new YamlConfiguration[1];
                Runnable onLoadCompleted = () -> {
                    yamlDocuments.remove(id);
                    yamlDocuments.put(id, runnableConfigs[0]);
                    scriptEntry.setFinished(true);
                };
                Runnable loadRunnable = () -> {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        String str = ScriptHelper.convertStreamToString(fis);
                        fis.close();
                        runnableConfigs[0] = YamlConfiguration.load(str, rawFormat == null || !rawFormat.asBoolean());
                        if (runnableConfigs[0] == null) {
                            runnableConfigs[0] = new YamlConfiguration();
                        }
                        DenizenCore.runOnMainThread(onLoadCompleted);
                    }
                    catch (Exception e) {
                        Debug.echoError("Failed to load yaml file: " + e);
                    }
                };
                if (scriptEntry.shouldWaitFor()) {
                    DenizenCore.runAsync(loadRunnable);
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
                        if (!CoreConfiguration.allowStrangeFileSaves) {
                            File fileObj = new File(DenizenCore.implementation.
                                    getDataFolder().getAbsolutePath() + "/" + filename.asString());
                            String directory = URLDecoder.decode(System.getProperty("user.dir"));
                            if (!fileObj.getCanonicalPath().startsWith(directory)) {
                                Debug.echoError("Outside-the-main-folder YAML saves disabled by administrator.");
                                scriptEntry.setFinished(true);
                                return;
                            }
                        }
                        File fileObj = new File(DenizenCore.implementation.
                                getDataFolder().getAbsolutePath() + "/" + filename.asString());
                        if (!DenizenCore.implementation.canWriteToFile(fileObj)) {
                            Debug.echoError("Cannot write to that file path due to security settings in Denizen/config.yml.");
                            scriptEntry.setFinished(true);
                            return;
                        }
                        fileObj.getParentFile().mkdirs();
                        YamlConfiguration yaml = yamlDocuments.get(id);
                        String outp = yaml.saveToString(false);
                        yaml.setDirty(false);
                        Runnable saveRunnable = () -> {
                            try {
                                Charset charset = CoreConfiguration.scriptEncoding == null ? null : CoreConfiguration.scriptEncoding.charset();
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
                        };
                        if (scriptEntry.shouldWaitFor()) {
                            DenizenCore.runAsync(saveRunnable);
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
                    if (dataAction == null) {
                        Debug.echoError("Must specify a data action to associate with SET action.");
                        return;
                    }
                    YamlConfiguration yaml = yamlDocuments.get(id);

                    int index = dataAction.index - 1;

                    String keyStr = dataAction.key;
                    value = dataAction.inputValue;
                    String valueStr = value == null ? null : value.toString();

                    switch (dataAction.type) {
                        case INCREMENT: {
                            String originalVal = Get(yaml, index, keyStr, "0");
                            if (!ArgumentHelper.matchesDouble(originalVal)) {
                                originalVal = "0";
                            }
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) + 1), dataType);
                            break;
                        }
                        case DECREMENT: {
                            String originalVal = Get(yaml, index, keyStr, "0");
                            if (!ArgumentHelper.matchesDouble(originalVal)) {
                                originalVal = "0";
                            }
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) - 1), dataType);
                            break;
                        }
                        case ADD: {
                            String originalVal = Get(yaml, index, keyStr, "0");
                            if (!ArgumentHelper.matchesDouble(originalVal)) {
                                originalVal = "0";
                            }
                            if (!ArgumentHelper.matchesDouble(valueStr)) {
                                Debug.echoError("YAML action required a decimal number, was given not-a-decimal-number: " + valueStr);
                                return;
                            }
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) + Double.parseDouble(valueStr)), dataType);
                            break;
                        }
                        case SUBTRACT: {
                            String originalVal = Get(yaml, index, keyStr, "0");
                            if (!ArgumentHelper.matchesDouble(originalVal)) {
                                originalVal = "0";
                            }
                            if (!ArgumentHelper.matchesDouble(valueStr)) {
                                Debug.echoError("YAML action required a decimal number, was given not-a-decimal-number: " + valueStr);
                                return;
                            }
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) - Double.parseDouble(valueStr)), dataType);
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
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) * Double.parseDouble(valueStr)), dataType);
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
                            Set(yaml, index, keyStr, CoreUtilities.doubleToString(Double.parseDouble(originalVal) / Double.parseDouble(valueStr)), dataType);
                            break;
                        }
                        case CLEAR:
                            yaml.set(keyStr, null);
                            break;
                        case AUTO_SET:
                            Set(yaml, index, keyStr, new ElementTag(true), dataType);
                            break;
                        case SET:
                            Set(yaml, index, keyStr, value, dataType);
                            break;
                        case INSERT: {
                            List<Object> list = yaml.getList(keyStr);
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            list.add(autoConvertObject(value));
                            yaml.set(keyStr, list);
                            break;
                        }
                        case REMOVE: {
                            List<String> list = yaml.getStringList(keyStr);
                            if (list == null) {
                                if (CoreConfiguration.debugVerbose) {
                                    Debug.echoDebug(scriptEntry, "List null!");
                                }
                                break;
                            }
                            if (index > -1 && index < list.size()) {
                                if (CoreConfiguration.debugVerbose) {
                                    Debug.echoDebug(scriptEntry, "Remove ind: " + index);
                                }
                                list.remove(index);
                                yaml.set(keyStr, list);
                            }
                            else {
                                if (CoreConfiguration.debugVerbose) {
                                    Debug.echoDebug(scriptEntry, "Remove value: " + valueStr);
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
                            Set(yaml, index, keyStr, value.asType(ListTag.class, scriptEntry.getContext()), dataType);
                            break;
                        }
                        case SPLIT: {
                            List<Object> list = yaml.getList(keyStr);
                            if (list == null) {
                                list = new ArrayList<>();
                            }
                            for (ObjectTag obj : value.asType(ListTag.class, scriptEntry.getContext()).objectForms) {
                                list.add(autoConvertObject(obj));
                            }
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

    public Object deepCopyObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof YamlConfiguration) {
            return copySection((YamlConfiguration) obj);
        }
        else if (obj instanceof List) {
            ArrayList outList = new ArrayList(((List) obj).size());
            for (Object subValue : (List) obj) {
                outList.add(deepCopyObject(subValue));
            }
            return outList;
        }
        else if (obj instanceof Map) {
            LinkedHashMap newMap = new LinkedHashMap();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) obj).entrySet()) {
                newMap.put(deepCopyObject(entry.getKey()), deepCopyObject(entry.getValue()));
            }
            return newMap;
        }
        return obj;
    }

    public YamlConfiguration copySection(YamlConfiguration section) {
        YamlConfiguration newSection = new YamlConfiguration();
        for (StringHolder key : section.getKeys(false)) {
            Object obj = section.get(key.str);
            newSection.set(key.str, deepCopyObject(obj));
        }
        return newSection;
    }

    public static class YamlActionProvider extends ActionableDataProvider {

        @Override
        public ObjectTag getValueAt(String keyName) {
            throw new RuntimeException("Yaml Action Provider mis-called");
        }

        @Override
        public void setValueAt(String keyName, ObjectTag value) {
            throw new RuntimeException("Yaml Action Provider mis-called");
        }
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

    public Object autoConvertObject(Object value) {
        if (value instanceof ElementTag || value instanceof String) {
            String val = value.toString();
            if (val.startsWith("map@")) {
                MapTag map = MapTag.valueOf(val, CoreUtilities.noDebugContext);
                if (map != null) {
                    value = map;
                }
            }
            else if (val.startsWith("li@")) {
                value = ListTag.valueOf(val, CoreUtilities.noDebugContext);
            }
        }
        if (value instanceof ListTag || value instanceof MapTag) {
            return CoreUtilities.objectTagToJavaForm((ObjectTag) value, true, false);
        }
        return value.toString();
    }

    public void Set(YamlConfiguration yaml, int index, String key, Object value, ElementTag dataType) {
        value = autoConvertObject(value);
        if (dataType != null && value instanceof String) {
            String rawValue = value.toString();
            switch (dataType.asEnum(DataType.class)) {
                case DOUBLE:
                    value = Double.parseDouble(rawValue);
                    break;
                case INTEGER:
                    value = Long.parseLong(rawValue);
                    break;
                case BOOLEAN:
                    value = CoreUtilities.equalsIgnoreCase(rawValue, "true");
                    break;
                case AUTO:
                    if (CoreUtilities.equalsIgnoreCase(rawValue, "true")) {
                        value = true;
                    }
                    else if (CoreUtilities.equalsIgnoreCase(rawValue, "false")) {
                        value = false;
                    }
                    else if (ArgumentHelper.matchesDouble(rawValue)) {
                        if (ArgumentHelper.matchesInteger(rawValue)) {
                            value = Long.parseLong(rawValue);
                        }
                        else {
                            value = Double.parseDouble(rawValue);
                        }
                    }
                    break;
            }
        }
        if (index == -1) {
            yaml.set(key, value);
        }
        else {
            List<Object> list = yaml.getList(key);
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
        String id = attribute.hasParam() ? CoreUtilities.toLowerCase(attribute.getParam()) : null;
        attribute.fulfill(1);

        // <--[tag]
        // @attribute <yaml.list>
        // @returns ListTag
        // @description
        // Returns a list of all currently loaded YAML ID's.
        // -->
        if (attribute.startsWith("list")) {
            return new ListTag(yamlDocuments.keySet(), true);
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
        if (attribute.startsWith("contains") && attribute.hasParam()) {
            return new ElementTag(yaml.contains(attribute.getParam()));
        }

        // <--[tag]
        // @attribute <yaml[<id>].is_list[<path>]>
        // @returns ElementTag(Boolean)
        // @description
        // Returns true if the specified path results in a list.
        // -->
        if (attribute.startsWith("is_list") && attribute.hasParam()) {
            return new ElementTag(yaml.isList(attribute.getParam()));
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
        if (attribute.startsWith("parsed_key") && attribute.hasParam()) {
            Object obj = yaml.get(attribute.getParam());
            if (obj == null) {
                return null;
            }
            return CoreUtilities.objectToTagForm(obj, attribute.context, false, true);
        }

        // <--[tag]
        // @attribute <yaml[<id>].read[<path>]>
        // @returns ObjectTag
        // @description
        // Returns the value from a data key on the YAML document as an ElementTag, ListTag, or MapTag.
        // -->
        if (attribute.startsWith("read") && attribute.hasParam()) {
            Object obj = yaml.get(attribute.getParam());
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
        if (attribute.startsWith("list_deep_keys") && attribute.hasParam()) {
            Set<StringHolder> keys;
            String path = attribute.getParam();
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
                return new ListTag(keys, stringHolder -> new ElementTag(stringHolder.str, true));
            }
        }

        // <--[tag]
        // @attribute <yaml[<id>].list_keys[<path>]>
        // @returns ListTag
        // @description
        // Returns a ListTag of all the keys at the path (and not sub-keys).
        // Use empty path input to represent the root of the yaml document tree.
        // -->
        if (attribute.startsWith("list_keys") && attribute.hasParam()) {
            Set<StringHolder> keys;
            String path = attribute.getParam();
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
                return new ListTag(keys, stringHolder -> new ElementTag(stringHolder.str, true));
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
