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
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ImageCommand extends AbstractCommand implements Holdable {

    // <--[command]
    // @Name Image
    // @Syntax image [id:<id>] [load [image:<image>/path:<path>]]/[save [path:<path>] [format:<format>]]/[unload]
    // @Required 2
    // @Maximum 4
    // @Short Loads, saves, and unloads images.
    // @Group image
    //
    // @Description
    // Loads, saves, and unloads images.
    //
    // With "load", specify either a file path to read from or an image object to load.
    // With "save", specify a file path to save the image to and a format to save the image in (e.g. "png", "jpg", "bmp"...), defaults to "png".
    // For both of these the starting path is "plugins/Denizen".
    // Use waitable syntax ("- ~image") when loading or saving from a file to avoid locking up the server during file IO, refer to <@link language ~waitable>.
    //
    // All uses of the image command must include the "id:" argument. This is any arbitrary name, as plaintext or from a tag,
    // to uniquely and globally identify the image object in memory. This ID can only be used by one image object at a time.
    // IDs are stored when "load" is used, and only removed when "unload" is used.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to load an image from a file into memory under the id "image".
    // - ~image id:image load path:data/image.png
    //
    // @Usage
    // Use to load an image object from a definition, draw on it, then store it back into the definition.
    // - define image <image[width=50;height=50;background=blue]>
    // - image id:to_edit load image:<[image]>
    // - draw id:to_edit oval x:0 y:0 width:50 height:50 color:green
    // # Copy the image to avoid it auto-updating with the image loaded in under that id, can skip '.copy' if this doesn't matter for your use case.
    // - define image <image[to_edit].copy>
    //
    // @Usage
    // Use to save an image into file and unload it.
    // - ~image id:image save path:data/image.png
    // - image id:image unload
    // -->

    public static final Map<String, ImageTag> loadedImages = new HashMap<>();

    public ImageCommand() {
        setName("image");
        setSyntax("image [id:<id>] [load [image:<image>/path:<path>]]/[save [path:<path>] [format:<format>]]/[unload]");
        setRequiredArguments(2, 4);
        autoCompile();
    }

    public static ImageTag getImageFrom(String id) {
        ImageTag image = loadedImages.get(CoreUtilities.toLowerCase(id));
        if (image == null) {
            throw new InvalidArgumentsRuntimeException("No loaded image with id '" + id + "'.");
        }
        return image;
    }

    public enum Operation {LOAD, SAVE, UNLOAD}

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("operation") Operation operation,
                                   @ArgName("id") @ArgPrefixed String id,
                                   @ArgName("path") @ArgPrefixed @ArgDefaultNull String path,
                                   @ArgName("format") @ArgPrefixed @ArgDefaultText("png") String format,
                                   @ArgName("image") @ArgPrefixed @ArgDefaultNull ImageTag toLoad) {
        String idLower = CoreUtilities.toLowerCase(id);
        switch (operation) {
            case LOAD -> {
                if (toLoad == null && path == null) {
                    throw new InvalidArgumentsRuntimeException("Must specify a path or image to load.");
                }
                if (loadedImages.containsKey(idLower)) {
                    Debug.echoError(scriptEntry, "Id '" + id + "' is already in use.");
                    scriptEntry.setFinished(true);
                    return;
                }
                if (toLoad != null) {
                    ImageTag toLoadCopy = toLoad.duplicateIgnoreId();
                    toLoadCopy.id = idLower;
                    loadedImages.put(idLower, toLoadCopy);
                    scriptEntry.setFinished(true);
                    return;
                }
                File imageFile = FileReadCommand.getFileIfSafe(path, scriptEntry);
                if (imageFile == null) {
                    return;
                }
                Runnable readImage = () -> {
                    try {
                        BufferedImage image = ImageIO.read(imageFile);
                        if (image == null) {
                            Debug.echoError(scriptEntry, "Failed to recognize image format for image.");
                            scriptEntry.setFinished(true);
                            return;
                        }
                        DenizenCore.runOnMainThread(() -> {
                            ImageTag imageTag = new ImageTag(image);
                            imageTag.id = idLower;
                            loadedImages.put(idLower, imageTag);
                            scriptEntry.setFinished(true);
                        });
                    }
                    catch (IOException e) {
                        Debug.echoError(scriptEntry, "An error occurred while trying to load image, see stacktrace below:");
                        Debug.echoError(scriptEntry, e);
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
                ImageTag image = getImageFrom(idLower);
                File imageFile = FileWriteCommand.getFileIfSafe(path, scriptEntry);
                if (imageFile == null) {
                    return;
                }
                Runnable saveImage = () -> {
                    try {
                        ImageTag corrected = image.correctType(format);
                        if (corrected == null) {
                            Debug.echoError(scriptEntry, "Invalid/unfitting image format specified: " + format + '.');
                            scriptEntry.setFinished(true);
                            return;
                        }
                        ImageIO.write(corrected.image, format, imageFile);
                        scriptEntry.setFinished(true);
                    }
                    catch (IOException e) {
                        Debug.echoError(scriptEntry, "An error occurred while trying to save image, see stacktrace below:");
                        Debug.echoError(scriptEntry, e);
                        scriptEntry.setFinished(true);
                    }
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
