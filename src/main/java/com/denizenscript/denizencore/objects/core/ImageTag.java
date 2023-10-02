package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.Adjustable;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.commands.core.ImageCommand;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class ImageTag implements Adjustable {

    @Fetchable("image")
    public static ImageTag valueOf(String string, TagContext context) {
        if (string.startsWith("image@")) {
            string = string.substring("image@".length());
        }
        String stringLower = CoreUtilities.toLowerCase(string);
        if (!TagManager.isStaticParsing) {
            ImageTag imageById = ImageCommand.loadedImages.get(stringLower);
            if (imageById != null) {
                return imageById.duplicate();
            }
        }
        if (!BinaryTag.isValidHex(stringLower)) {
            MapTag map = MapTag.valueOf(string, CoreUtilities.noDebugContext);
            if (map != null) {
                ElementTag width = map.getElement("width");
                ElementTag height = map.getElement("height");
                ElementTag type = map.getElement("type", "png");
                ColorTag background = map.getObjectAs("background", ColorTag.class, context);
                if (width == null || height == null || !width.isInt() || !height.isInt()) {
                    if ((context == null || context.showErrors()) && !TagManager.isStaticParsing) {
                        Debug.echoError("valueOf ImageTag returning null: must specify valid width/height.");
                    }
                    return null;
                }
                ImageTag image = new ImageTag(new BufferedImage(width.asInt(), height.asInt(), BufferedImage.TYPE_INT_ARGB), type.asString());
                if (background != null) {
                    // Optimization hack, since the internal data structure is guaranteed here
                    int[] data = ((DataBufferInt) image.image.getRaster().getDataBuffer()).getData();
                    Arrays.fill(data, background.asARGB());
                }
                return image;
            }
            if ((context == null || context.showErrors()) && !TagManager.isStaticParsing) {
                Debug.echoError("valueOf ImageTag returning null: invalid binary data: " + string);
            }
            return null;
        }
        byte[] rawBytes = CoreUtilities.hexDecode(stringLower);
        try {
            ImageTag image = read(new ByteArrayInputStream(rawBytes));
            if (image == null) {
                if (context == null || context.showErrors()) {
                    Debug.echoError("valueOf ImageTag returning null: could not recognize image format for binary: " + string);
                }
                return null;
            }
            return image;
        }
        catch (IOException e) {
            if (context == null || context.showErrors()) {
                Debug.echoError("valueOf ImageTag returning null: invalid binary data '" + string + "' (see stacktrace below)");
                Debug.echoError(e);
            }
            return null;
        }
    }

    // Copied from ImageIO and modified to preserve the image type
    public static ImageTag read(Object input) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(input);
        Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            return null;
        }
        ImageReader reader = iter.next();
        ImageReadParam param = reader.getDefaultReadParam();
        reader.setInput(stream, true, true);
        BufferedImage bi;
        try {
            bi = reader.read(0, param);
        } finally {
            reader.dispose();
            stream.close();
        }
        return new ImageTag(bi, reader.getFormatName());
    }

    public static boolean matches(String string) {
        if (string.startsWith("image@")) {
            return true;
        }
        return valueOf(string, CoreUtilities.noDebugContext) != null;
    }

    public BufferedImage image;
    public String imageType;

    public ImageTag(BufferedImage image, String imageType) {
        this.image = image;
        this.imageType = imageType;
    }

    public static void register() {
        tagProcessor.registerTag(ColorTag.class, MapTag.class, "pixel_at", (attribute, object, param) -> {
            ElementTag x = param.getRequiredObjectAs("x", ElementTag.class, attribute);
            ElementTag y = param.getRequiredObjectAs("y", ElementTag.class, attribute);
            if (!x.isInt() || !y.isInt()) {
                attribute.echoError("Invalid x/y '" + x + '/' + y + "' specified: must be valid numbers.");
                return null;
            }
            return ColorTag.fromARGB(object.image.getRGB(x.asInt(), y.asInt()));
        });

        tagProcessor.registerTag(ElementTag.class, "type", (attribute, object) -> {
            return new ElementTag(object.imageType, true);
        });

        tagProcessor.registerTag(BinaryTag.class, "to_binary", (attribute, object) -> {
            return new BinaryTag(object.getRawBytes());
        });

        tagProcessor.registerTag(ElementTag.class, "formatting", (attribute, object) -> {
            return new ElementTag(object.image.getType());
        });

        tagProcessor.registerTag(ImageTag.class, MapTag.class, "sub_image", (attribute, object, param) -> {
            ElementTag x = param.getRequiredObjectAs("x", ElementTag.class, attribute);
            ElementTag y = param.getRequiredObjectAs("y", ElementTag.class, attribute);
            ElementTag width = param.getRequiredObjectAs("width", ElementTag.class, attribute);
            ElementTag height = param.getRequiredObjectAs("height", ElementTag.class, attribute);
            if (!x.isInt() || !y.isInt() || !width.isInt() || !height.isInt()) {
                attribute.echoError("Invalid x/y/width/height '" + x + '/' + y + '/' + width  + '/' + height + "' specified: must be valid numbers.");
                return null;
            }
            return new ImageTag(object.image.getSubimage(x.asInt(), y.asInt(), width.asInt(), height.asInt()), object.imageType).duplicate();
        });

        tagProcessor.registerMechanism("scale", false, MapTag.class, (object, mechanism, input) -> {
            ElementTag widthElement = input.getElement("width");
            ElementTag heightElement = input.getElement("height");
            if ((widthElement != null && !widthElement.isInt()) || (heightElement != null && !heightElement.isInt())) {
                mechanism.echoError("Invalid width/height '" + widthElement  + '/' + heightElement + "' specified: must be valid numbers.");
                return;
            }
            int width = widthElement != null ? widthElement.asInt() : object.image.getWidth();
            int height = heightElement != null ? heightElement.asInt() : object.image.getHeight();
            Image rescaled = object.image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
            object.image = new BufferedImage(width, height, object.image.getType());
            Graphics2D graphics = object.image.createGraphics();
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(rescaled, 0, 0, null);
            graphics.dispose();
        });

        tagProcessor.registerMechanism("fix_type", false, (object, mechanism) -> {
            BufferedImage fixed = new BufferedImage(object.image.getWidth(), object.image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = fixed.createGraphics();
            graphics.drawImage(object.image, 0, 0, null);
            graphics.dispose();
            object.image = fixed;
        });
    }

    public static final ObjectTagProcessor<ImageTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public void adjust(Mechanism mechanism) {
        tagProcessor.processMechanism(this, mechanism);
    }

    @Override
    public void applyProperty(Mechanism mechanism) {
        mechanism.echoError("Cannot apply properties to an ImageTag.");
    }

    public byte[] getRawBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, imageType, outputStream);
            return outputStream.toByteArray();
        }
        catch (IOException e) {
            Debug.echoError("Exception when converting image to bytes:");
            throw new RuntimeException(e);
        }
    }

    @Override
    public String identify() {
        return "image@" + BinaryTag.hexEncode(getRawBytes(), false);
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String debuggable() {
        return "<LG>image@<Y>" + BinaryTag.hexEncode(getRawBytes(), true);
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public BufferedImage getJavaObject() {
        return image;
    }

    @Override
    public ImageTag duplicate() {
        BufferedImage clone = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D graphics = clone.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return new ImageTag(clone, imageType);
    }

    @Override
    public String toString() {
        return identify();
    }

    String prefix = "Image";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ImageTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }
}
