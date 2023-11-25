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
import java.io.FileOutputStream;

public class FileWriteCommand extends AbstractCommand implements Holdable {

    public FileWriteCommand() {
        setName("filewrite");
        setSyntax("filewrite [path:<path>] [data:<binary>]");
        setRequiredArguments(2, 2);
        isProcedural = false;
        autoCompile();
    }

    // <--[command]
    // @Name FileWrite
    // @Syntax filewrite [path:<path>] [data:<binary>]
    // @Required 2
    // @Maximum 2
    // @Short Writes the given raw data to the file at the given path.
    // @Group file
    //
    // @Description
    // Writes the given raw data to the file at the given path.
    //
    // Will overwrite any existing file at the path.
    //
    // The starting directory is server/plugins/Denizen.
    //
    // Directories will automatically be generated as-needed.
    //
    // Note that in most cases this command should be ~waited for (like "- ~filewrite ..."). Refer to <@link language ~waitable>.
    //
    // This command must be enabled by setting Denizen config option "Commands.File.Allow write" to true.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to write some simple text to 'myfile'
    // - ~filewrite path:data/myfile.dat data:<element[Hello].utf8_encode>
    //
    // -->

    public static void autoExecute(final ScriptEntry scriptEntry,
                                   @ArgPrefixed @ArgName("path") final String path,
                                   @ArgPrefixed @ArgName("data") BinaryTag data) {
        File file = getFileIfSafe(path, scriptEntry);
        if (file == null) {
            return;
        }
        Runnable runme = () -> {
            try {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                FileOutputStream stream = new FileOutputStream(file);
                stream.write(data.data);
                stream.close();
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
        if (!CoreConfiguration.allowFileWrite) {
            Debug.echoError(scriptEntry, "File write disabled in Denizen/config.yml (refer to command documentation).");
            scriptEntry.setFinished(true);
            return null;
        }
        File file = new File(DenizenCore.implementation.getDataFolder(), path);
        if (!DenizenCore.implementation.canWriteToFile(file)) {
            Debug.echoError("Cannot write to that file path due to security settings in Denizen/config.yml.");
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
        }
        catch (Exception ex) {
            Debug.echoError(ex);
            scriptEntry.setFinished(true);
            return null;
        }
        return file;
    }
}
