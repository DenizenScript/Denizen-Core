package com.denizenscript.denizencore.scripts.commands.file;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.BinaryTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileReadCommand extends AbstractCommand implements Holdable {

    public FileReadCommand() {
        setName("fileread");
        setSyntax("fileread [path:<path>]");
        setRequiredArguments(1, 1);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name FileRead
    // @Syntax fileread [path:<path>]
    // @Required 1
    // @Maximum 1
    // @Short Reads the file at the given path.
    // @Group file
    //
    // @Description
    // Reads the file at the given path.
    //
    // The starting directory is server/plugins/Denizen.
    //
    // Note that in most cases this command should be ~waited for (like "- ~fileread ..."). Refer to <@link language ~waitable>.
    //
    // This command must be enabled by setting Denizen config option "Commands.File.Allow read" to true.
    //
    // @Tags
    // <entry[saveName].data> returns a BinaryTag of the raw file content.
    //
    // @Usage
    // Use to read 'myfile' and narrate the text content.
    // - ~fileread path:data/myfile.dat save:read
    // - narrate "Read data: <entry[read].data.utf8_decode>"
    //
    // -->

    public static void autoExecute(final ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("path") final String path) {
        File file = getFileIfSafe(path, scriptEntry);
        if (file == null) {
            return;
        }
        Runnable runme = () -> {
            try {
                FileInputStream stream = new FileInputStream(file);
                byte[] data = stream.readAllBytes();
                stream.close();
                scriptEntry.saveObject("data", new BinaryTag(data));
                scriptEntry.setFinished(true);
            }
            catch (Exception e) {
                Debug.echoError(scriptEntry, e);
                scriptEntry.setFinished(true);
            }
        };
        if (scriptEntry.shouldWaitFor()) {
            DenizenCore.runAsync(runme);
        }
        else {
            runme.run();
        }
    }

    public static File getFileIfSafe(String path, ScriptEntry scriptEntry) {
        if (!CoreConfiguration.allowFileRead) {
            Debug.echoError(scriptEntry, "FileRead disabled in Denizen/config.yml (refer to command documentation).");
            scriptEntry.setFinished(true);
            return null;
        }
        File file = new File(DenizenCore.implementation.getDataFolder(), path);
        if (!DenizenCore.implementation.canReadFile(file)) {
            Debug.echoError("Cannot read from that file path due to security settings in Denizen/config.yml.");
            scriptEntry.setFinished(true);
            return null;
        }
        try {
            if (!CoreConfiguration.filePathLimit.equals("none")) {
                File root = new File(DenizenCore.implementation.getDataFolder(), CoreConfiguration.filePathLimit);
                if (!file.getCanonicalPath().startsWith(root.getCanonicalPath())) {
                    Debug.echoError("File path '" + path + "' is not within the config's restricted data file path.");
                    scriptEntry.setFinished(true);
                    return null;
                }
            }
            if (!file.exists()) {
                Debug.echoError(scriptEntry, "File read failed, file does not exist!");
                scriptEntry.setFinished(true);
                return null;
            }
        }
        catch (Exception e) {
            Debug.echoError(scriptEntry, e);
            scriptEntry.setFinished(true);
            return null;
        }
        return file;
    }
}
