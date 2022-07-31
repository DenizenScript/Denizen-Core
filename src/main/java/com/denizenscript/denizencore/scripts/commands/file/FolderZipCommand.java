package com.denizenscript.denizencore.scripts.commands.file;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsException;
import com.denizenscript.denizencore.objects.Argument;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.OneTimeSchedulable;

import java.io.File;

public class FolderZipCommand extends AbstractCommand implements Holdable {

    public FolderZipCommand() {
        setName("folderzip");
        setSyntax("folderzip [folder:<folder>] [destination:<destination>] (overwrite)");
        setRequiredArguments(2, 3);
        isProcedural = false;
    }

    // <--[command]
    // @Name FolderZip
    // @Syntax folderzip [folder: <folder>] [destination:<destination>] (overwrite)
    // @Required 2
    // @Maximum 3
    // @Short Zips a folder to the specified destination.
    // @Group file
    //
    // @Warning This command should almost always not be placed within scripts!
    //
    // @Description
    // Creates a .zip file from a folder.
    //
    // The starting directory is server/plugins/Denizen.
    //
    // Note that in most cases this command should be ~waited for (like "- ~folderzip ..."). Refer to <@link language ~waitable>.
    //
    // This command can be disabled by setting Denizen config option "Commands.Filecopy.Allow copying files" to false.
    //
    // @Tags
    // <entry[saveName].success> returns whether the copy succeeded (if not, either an error or occurred, or there is an existing file in the destination.)
    //
    // @Usage
    // Zip a folder containing information about players.
    // - ~filecopy f:data/players/ d:data/players.zip overwrite save:zipper
    // - narrate "Zip success<&co> <entry[zipper].success>"
    //
    // -->

    @Override
    public void parseArgs(ScriptEntry scriptEntry) throws InvalidArgumentsException {
        for (Argument arg : scriptEntry) {
            if (!scriptEntry.hasObject("folder")
                    && arg.matchesPrefix("folder", "f", "origin", "o")) {
                scriptEntry.addObject("folder", arg.asElement());
            }
            else if (!scriptEntry.hasObject("destination")
                    && arg.matchesPrefix("destination", "d")) {
                scriptEntry.addObject("destination", arg.asElement());
            }
            else if (!scriptEntry.hasObject("overwrite")
                    && arg.matches("overwrite")) {
                scriptEntry.addObject("overwrite", new ElementTag("true"));
            }
            else {
                arg.reportUnhandled();
            }
        }
        if (!scriptEntry.hasObject("folder")) {
            throw new InvalidArgumentsException("Must have a valid origin!");
        }
        if (!scriptEntry.hasObject("destination")) {
            throw new InvalidArgumentsException("Must have a valid destination!");
        }
        scriptEntry.defaultObject("overwrite", new ElementTag("false"));
    }

    @Override
    public void execute(final ScriptEntry scriptEntry) {
        ElementTag folder = scriptEntry.getElement("folder");
        ElementTag destination = scriptEntry.getElement("destination");
        ElementTag overwrite = scriptEntry.getElement("overwrite");
        if (scriptEntry.dbCallShouldDebug()) {
            Debug.report(scriptEntry, getName(), folder, destination, overwrite);
        }
        if (!CoreConfiguration.allowFileCopy) {
            Debug.echoError(scriptEntry, "Folder zipping/file copying disabled by server administrator (refer to command documentation).");
            scriptEntry.addObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }
        File f = new File(DenizenCore.implementation.getDataFolder(), folder.asString());
        File d = new File(DenizenCore.implementation.getDataFolder(), destination.asString());
        File[] files = f.listFiles();
        boolean ow = overwrite.asBoolean();
        boolean dexists = d.exists();
        boolean disdir = d.isDirectory() || destination.asString().endsWith("/");
        if (!DenizenCore.implementation.canReadFile(f)) {
            Debug.echoError("Cannot read from that folder due to security settings in Denizen/config.yml.");
            scriptEntry.addObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }
        if (!f.exists()) {
            Debug.echoError(scriptEntry, "Folder zipping failed, origin folder does not exist!");
            scriptEntry.addObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }
        if (!f.isDirectory()) {
            Debug.echoError(scriptEntry, "Folder zipping failed, specified file instead of a folder!");
            scriptEntry.addObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }
        if (!DenizenCore.implementation.canWriteToFile(d)) {
            Debug.echoError("Cannot write to that file path due to security settings in Denizen/config.yml.");
            scriptEntry.addObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }
        if (dexists && !disdir && !ow) {
            Debug.echoDebug(scriptEntry, "Folder zip ignored, destination file already exists!");
            scriptEntry.addObject("success", new ElementTag("false"));
            scriptEntry.setFinished(true);
            return;
        }
        if (files != null && files.length == 0) {
            Debug.echoDebug(scriptEntry, "Folder specified is empty, creating empty zip file.");
        }
        Runnable runme = () -> {
            try {
                if (dexists && !disdir) {
                    d.delete();
                }
                if (disdir && !dexists) {
                    d.mkdirs();
                }
                else if (!dexists && !d.getParentFile().exists()) {
                    d.getParentFile().mkdirs();
                }
                CoreUtilities.zipDirectory(f.toPath(), d.toPath());

                scriptEntry.addObject("success", new ElementTag("true"));
                scriptEntry.setFinished(true);
            }
            catch (Exception e) {
                Debug.echoError(scriptEntry, e);
                scriptEntry.addObject("success", new ElementTag("false"));
                scriptEntry.setFinished(true);
            }
        };
        if (scriptEntry.shouldWaitFor()) {
            DenizenCore.schedule(new AsyncSchedulable(new OneTimeSchedulable(runme, 0)));
        }
        else {
            runme.run();
        }
    }
}
