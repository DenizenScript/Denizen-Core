package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.core.ColorTag;
import com.denizenscript.denizencore.objects.core.ImageTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;

import java.awt.*;

public class DrawCommand extends AbstractCommand {

    // <--[command]
    // @Name Draw
    // @Syntax draw [id:<id>] [pixel/rectangle/oval/image:<image>] (width:<width>) (height:<height>) (filled) [x:<x>] [y:<y>] (color:<color>)
    // @Required 4
    // @Maximum 8
    // @Short Draws on an image.
    // @Group image
    //
    // @Description
    // Draws a pixel, shape, or image onto an image.
    //
    // "id:" - the id of the image to draw on, see <@link command Image>.
    // "x:" and "y:" - the position that should be drawn on, see <@link language Image positions>.
    //
    // For pixels or shapes:
    // "color:" - a <@link ObjectType ColorTag> of the color to draw in.
   //
    // For non-pixel shapes:
    // "width:" and "height:" - the size of the shape being drawn, required.
    // "filled" - whether the shape should be filled or just a border. optional, defaults to false.
    //
    // And for images:
    // "image:" - the image to draw, required.
    // "width:" and "height:" - the size to rescale the image being drawn to, optional.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to draw a single red pixel in the top left corner of an image.
    // - draw id:image pixel x:0 y:0 color:red
    //
    // @Usage
    // Use to draw a purple 100x100 filled circle in the top left of an image.
    // - draw id:image oval x:0 y:0 width:100 height:100 color:purple filled
    //
    // @Usage
    // Use to draw the image with the id 'star' into the image with id 'sky'.
    // - draw id:sky image:star x:50 y:50
    // -->

    // <--[language]
    // @name Image positions
    // @group image
    // @description
    // Some image-related features take in x and y values, which represent a point on the image.
    // The values are relative to the upper left corner of the image, which acts as 0,0.
    // If the values are meant to represent the position of a larger object (a shape, an image being drawn onto another image, etc.),
    // they represent the position of that object's upper left corner.
    // -->

    public DrawCommand() {
        setName("draw");
        setSyntax("draw [id:<id>] [pixel/rectangle/oval/image:<image>] (width:<width>) (height:<height>) (filled) [x:<x>] [y:<y>] (color:<color>)");
        setRequiredArguments(4, 8);
        autoCompile();
    }

    public enum Drawable {PIXEL, RECTANGLE, OVAL}

    public static void autoExecute(@ArgName("id") @ArgPrefixed String id,
                                   @ArgName("draw") @ArgDefaultNull Drawable drawable,
                                   @ArgName("image") @ArgPrefixed @ArgDefaultNull ImageTag toDraw,
                                   @ArgName("x") @ArgPrefixed int x,
                                   @ArgName("y") @ArgPrefixed int y,
                                   @ArgName("width") @ArgPrefixed @ArgDefaultText("-1") int width,
                                   @ArgName("height") @ArgPrefixed @ArgDefaultText("-1") int height,
                                   @ArgName("color") @ArgPrefixed @ArgDefaultNull ColorTag color,
                                   @ArgName("filled") boolean filled) {
        ImageTag image = ImageCommand.getImageFrom(id);
        if (toDraw != null) {
            if (width == -1) {
                width = toDraw.image.getWidth();
            }
            if (height == -1) {
                height = toDraw.image.getHeight();
            }
            Graphics2D graphics = image.image.createGraphics();
            graphics.drawImage(toDraw.image, x, y, width, height, null);
            graphics.dispose();
            return;
        }
        if (color == null) {
            throw new InvalidArgumentsRuntimeException("Must specify a color to draw in.");
        }
        if (drawable == Drawable.PIXEL) {
            image.image.setRGB(x, y, color.asARGB());
            return;
        }
        if (width == -1 || height == -1) {
            throw new InvalidArgumentsRuntimeException("Must specify a width and height.");
        }
        if (drawable == null) {
            throw new InvalidArgumentsRuntimeException("Must specify what to draw.");
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
