package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.core.ImageTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.Holdable;
import com.denizenscript.denizencore.scripts.commands.file.FileReadCommand;
import com.denizenscript.denizencore.scripts.commands.file.FileWriteCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageCommand extends AbstractCommand implements Holdable {

    public static final Map<String, ImageTag> loadedImages = new HashMap<>();

    public ImageCommand() {
        setName("image");
        setSyntax("image [id:<id>] [load/save [path:<path>]]/[unload]");
        setRequiredArguments(2, 3);
        autoCompile();
    }

    public enum Operation {LOAD, SAVE, UNLOAD}

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("operation") Operation operation,
                                   @ArgName("id") @ArgPrefixed String id,
                                   @ArgName("path") @ArgPrefixed @ArgDefaultNull String path) {
        String idLower = CoreUtilities.toLowerCase(id);
        switch (operation) {
            case LOAD -> {
                if (path == null) {
                    throw new InvalidArgumentsRuntimeException("Must specify a path.");
                }
                if (loadedImages.containsKey(idLower)) {
                    Debug.echoError("Id '" + id + "' is already in use.");
                    scriptEntry.setFinished(true);
                    return;
                }
                File imageFile = FileReadCommand.canReadFile(path, scriptEntry);
                if (imageFile == null) {
                    return;
                }
                Runnable readImage = () -> {
                    try {
                        ImageTag image = ImageTag.read(imageFile);
                        if (image == null) {
                            Debug.echoError("Failed to recognize image format for image.");
                            return;
                        }
                        DenizenCore.runOnMainThread(() -> {
                            loadedImages.put(idLower, image);
                            scriptEntry.setFinished(true);
                        });
                    }
                    catch (IOException e) {
                        Debug.echoError("An error occurred while trying to load image, see stacktrace below:");
                        Debug.echoError(e);
                        scriptEntry.setFinished(true);
                    }
                };
                if (scriptEntry.shouldWaitFor()) {
                    DenizenCore.runAsync(readImage);
                }
                else {
                    readImage.run();
                }
            }
            case SAVE -> {
                if (path == null) {
                    throw new InvalidArgumentsRuntimeException("Must specify a path.");
                }
                ImageTag image = loadedImages.get(idLower);
                if (image == null) {
                    Debug.echoError("No loaded image with id '" + id + "'.");
                    scriptEntry.setFinished(true);
                    return;
                }
                File imageFile = FileWriteCommand.canWrite(path, scriptEntry);
                if (imageFile == null) {
                    return;
                }
                Runnable saveImage = () -> {
                    try {
                        ImageIO.write(image.image, image.imageType, imageFile);
                    }
                    catch (IOException e) {
                        Debug.echoError("An error occurred while trying to save image, see stacktrace below:");
                        Debug.echoError(e);
                    }
                    scriptEntry.setFinished(true);
                };
                if (scriptEntry.shouldWaitFor()) {
                    DenizenCore.runAsync(saveImage);
                }
                else {
                    saveImage.run();
                }
            }
            case UNLOAD -> {
                if (loadedImages.remove(idLower) == null) {
                    Debug.echoError("No loaded image with id '" + id + "'.");
                }
            }
        }
    }
}
