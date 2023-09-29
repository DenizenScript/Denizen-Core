package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.core.ColorTag;
import com.denizenscript.denizencore.objects.core.ImageTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;
import com.denizenscript.denizencore.utilities.CoreUtilities;

import java.awt.*;

public class DrawCommand extends AbstractCommand {

    public DrawCommand() {
        setName("draw");
        setSyntax("draw [id:<id>/image:<image>] [PIXEL/[RECTANGLE/OVAL [width:<width>] [height:<height>] (filled)]] [x:<x>] [y:<y>] [color:<color>]");
        autoCompile();
    }

    public enum Drawable {PIXEL, RECTANGLE, OVAL}

    public static ImageTag getImageFrom(ImageTag image, String id) {
        if (image == null) {
            if (id == null) {
                throw new InvalidArgumentsRuntimeException("Must specify either an image or an id.");
            }
            image = ImageCommand.loadedImages.get(CoreUtilities.toLowerCase(id));
            if (image == null) {
                throw new InvalidArgumentsRuntimeException("No loaded image with id '" + id + "'.");
            }
        }
        return image;
    }

    public static void autoExecute(@ArgName("id") @ArgPrefixed @ArgDefaultNull String id,
                                   @ArgName("image") @ArgPrefixed @ArgDefaultNull ImageTag image,
                                   @ArgName("draw") Drawable drawable,
                                   @ArgName("x") @ArgPrefixed int x,
                                   @ArgName("y") @ArgPrefixed int y,
                                   @ArgName("width") @ArgPrefixed @ArgDefaultText("-1") int width,
                                   @ArgName("height") @ArgPrefixed @ArgDefaultText("-1") int height,
                                   @ArgName("color") @ArgPrefixed ColorTag color,
                                   @ArgName("filled") boolean filled) {
        image = getImageFrom(image, id);
        if (drawable == Drawable.PIXEL) {
            image.image.setRGB(x, y, color.asARGB());
            return;
        }
        if (width == -1 || height == -1) {
            throw new InvalidArgumentsRuntimeException("Must specify width and height.");
        }
        Graphics2D graphics = image.image.createGraphics();
        graphics.setColor(color.getAWTColor());
        switch (drawable) {
            case RECTANGLE -> {
                if (filled) {
                    graphics.fillRect(x, y, width, height);
                }
                else {
                    graphics.drawRect(x, y, width - 1, height - 1);
                }
            }
            case OVAL -> {
                if (filled) {
                    graphics.fillOval(x, y, width, height);
                }
                else {
                    graphics.drawOval(x, y, width - 1, height - 1);
                }
            }
        }
        graphics.dispose();
    }
}
