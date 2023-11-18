package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.objects.core.ColorTag;
import com.denizenscript.denizencore.objects.core.ImageTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;

import java.awt.*;

public class DrawCommand extends AbstractCommand {

    // <--[command]
    // @Name Draw
    // @Syntax draw [id:<id>] [pixel]/[rectangle/oval [width:<width>] [height:<height>] (filled)] [x:<x>] [y:<y>] [color:<color>]
    // @Required 5
    // @Maximum 8
    // @Short Draws on an image.
    // @Group image
    //
    // @Description
    // Draws onto an image, potentially with certain predetermined shapes.
    //
    // "id:" is the id of the image to draw on, see <@link command Image>.
    // "color:" is a <@link ObjectType ColorTag> of the color to draw in.
    // "x:" and "y:" are the position that should be drawn on, see <@link language Image positions>.
    //
    // If you are drawing a shape (not a pixel), you must also specify:
    // "width:" and "height:" - the size of the shape being drawn, required.
    // "filled" - whether the shape should be filled or just a border. optional, defaults to false.
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
        setSyntax("draw [id:<id>] [pixel]/[rectangle/oval [width:<width>] [height:<height>] (filled)] [x:<x>] [y:<y>] [color:<color>]");
        setRequiredArguments(5, 8);
        autoCompile();
    }

    public enum Drawable {PIXEL, RECTANGLE, OVAL}

    public static void autoExecute(@ArgName("id") @ArgPrefixed String id,
                                   @ArgName("draw") Drawable drawable,
                                   @ArgName("x") @ArgPrefixed int x,
                                   @ArgName("y") @ArgPrefixed int y,
                                   @ArgName("width") @ArgPrefixed @ArgDefaultText("-1") int width,
                                   @ArgName("height") @ArgPrefixed @ArgDefaultText("-1") int height,
                                   @ArgName("color") @ArgPrefixed ColorTag color,
                                   @ArgName("filled") boolean filled) {
        ImageTag image = ImageCommand.getImageFrom(id);
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
