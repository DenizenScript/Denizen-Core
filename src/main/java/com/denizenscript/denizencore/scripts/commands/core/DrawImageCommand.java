package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.objects.core.ImageTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;

import java.awt.*;

public class DrawImageCommand extends AbstractCommand {

    // <--[command]
    // @Name DrawImage
    // @Syntax drawimage [id:<id>] [draw:<image>] [x:<x>] [y:<y>] (width:<width>) (height:<height>)
    // @Required 4
    // @Maximum 6
    // @Short Draws an image onto another image.
    // @Group image
    //
    // @Description
    // Draws an image onto another image, optionally rescaling it.
    //
    // "id:" is the id of the image to draw on, see <@link command image>.
    // "draw:" is the image to draw.
    // "x:" and "y:" are the position in the image the second image should be drawn on.
    // They're the position of the top left corner of the image being drawn, relative to the top left corner of the image being drawn on (with that corner being 0,0).
    // "width:" and "height:" are optional, and will rescale the image being drawn.
    //
    // @Tags
    // None
    //
    // @Usage
    // Use to draw the image with id "star" into the image with id "sky".
    // - drawimage id:sky draw:star x:50 y:20
    //
    // @Usage
    // Use to draw the image defined as "mcmonkey" into the image with id "denizen", rescaling it.
    // - drawimage id:denizen draw:<[mcmonkey]> x:25 y:25 width:50 height:50
    // -->

    public DrawImageCommand() {
        setName("drawimage");
        setSyntax("drawimage [id:<id>] [draw:<image>] [x:<x>] [y:<y>] (width:<width>) (height:<height>)");
        setRequiredArguments(4, 6);
        autoCompile();
    }

    public static void autoExecute(@ArgName("id") @ArgPrefixed String id,
                                   @ArgName("draw") @ArgPrefixed ImageTag toDraw,
                                   @ArgName("x") @ArgPrefixed int x,
                                   @ArgName("y") @ArgPrefixed int y,
                                   @ArgName("width") @ArgPrefixed @ArgDefaultText("-1") int width,
                                   @ArgName("height") @ArgPrefixed @ArgDefaultText("-1") int height) {
        ImageTag image = DrawCommand.getImageFrom(id);
        if (width == -1) {
            width = toDraw.image.getWidth();
        }
        if (height == -1) {
            height = toDraw.image.getHeight();
        }
        Graphics2D graphics = image.image.createGraphics();
        graphics.drawImage(toDraw.image, x, y, width, height, null);
        graphics.dispose();
        toDraw.markChanged();
    }
}
