package com.denizenscript.denizencore.scripts.commands.core;

import com.denizenscript.denizencore.objects.core.ImageTag;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultText;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import com.denizenscript.denizencore.scripts.commands.generator.ArgPrefixed;

import java.awt.*;

public class DrawImageCommand extends AbstractCommand {

    public DrawImageCommand() {
        setName("drawimage");
        setSyntax("drawimage [id:<id>/image:<image>] [draw:<image>] [x:<x>] [y:<y>] [width:<width>] [height:<height>]");
        autoCompile();
    }

    public static void autoExecute(@ArgName("id") @ArgPrefixed @ArgDefaultNull String id,
                                   @ArgName("image") @ArgPrefixed @ArgDefaultNull ImageTag image,
                                   @ArgName("draw") @ArgPrefixed ImageTag toDraw,
                                   @ArgName("x") @ArgPrefixed int x,
                                   @ArgName("y") @ArgPrefixed int y,
                                   @ArgName("width") @ArgPrefixed @ArgDefaultText("-1") int width,
                                   @ArgName("height") @ArgPrefixed @ArgDefaultText("-1") int height) {
        image = DrawCommand.getImageFrom(image, id);
        if (width == -1) {
            width = toDraw.image.getWidth();
        }
        if (height == -1) {
            height = toDraw.image.getHeight();
        }
        Graphics2D graphics = image.image.createGraphics();
        graphics.drawImage(toDraw.image, x, y, width, height, null);
        graphics.dispose();
    }
}
