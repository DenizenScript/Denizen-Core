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

    // <--[ObjectType]
    // @name ImageTag
    // @prefix image
    // @base ElementTag
    // @ExampleTagBase image[width=100;height=100;background=blue]
    // @ExampleValues width=100;height=100;background=blue,width=5;height=5,loaded_image_id
    // @ExampleForReturns
    // - drawimage id:artwork draw:%VALUE% x:5 y:5
    // @format
    // The identity format for ImageTag is a hex encoding of the image's raw bytes, in order.
    // Each byte is encoded as an individual big-endian hexadecimal pair, like "FF" for byte=255, "00" for byte=0, "0F" for 15, "F0" for 240, ... etc.
    //
    // @description
    // ImageTags represent an image of any supported format.
    // They are represented using a hex encoding of their bytes, but new images can also be created by providing a MapTag with the following keys:
    // - "width": the new image's width, required.
    // - "height": the new image's height, required.
    // - "background": a <@link ObjectType ColorTag> for the new image's background, optional.
    // - "type": the image's format/encoding ("png", "jpg", "webp", etc.). optional, defaults to "png".
    // Alternatively, specific an id from <@link command Image> to get the image loaded in under that id.
    //
    // -->

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
            image.hexEncodedBytesCache = stringLower;
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

        // <--[tag]
        // @attribute <ImageTag.pixel_at[x=<x>;y=<y>]>
        // @returns ColorTag
        // @description
        // Returns the color of a specific pixel in an image.
        // "x" and "y" are the position of the pixel, see <@link language Image positions>.
        // @example
        // Get the color of the pixel in 43,21
        // - narrate "The color is: <[image].pixel_at[x=43;y=21]>."
        // -->
        tagProcessor.registerTag(ColorTag.class, MapTag.class, "pixel_at", (attribute, object, param) -> {
            ElementTag x = param.getRequiredObjectAs("x", ElementTag.class, attribute);
            ElementTag y = param.getRequiredObjectAs("y", ElementTag.class, attribute);
            if (!x.isInt() || !y.isInt()) {
                attribute.echoError("Invalid x/y '" + x + '/' + y + "' specified: must be valid numbers.");
                return null;
            }
            return ColorTag.fromARGB(object.image.getRGB(x.asInt(), y.asInt()));
        });

        // <--[tag]
        // @attribute <ImageTag.width>
        // @returns ElementTag(Number)
        // @mechanism ImageTag.scale
        // @description
        // Returns image's width (in pixels).
        // @example
        // Narrates an image's width.
        // - narrate "The image is <[image].width> wide."
        // -->
        tagProcessor.registerTag(ElementTag.class, "width", (attribute, object) -> {
            return new ElementTag(object.image.getWidth());
        });

        // <--[tag]
        // @attribute <ImageTag.height>
        // @returns ElementTag(Number)
        // @mechanism ImageTag.scale
        // @description
        // Returns image's height (in pixels).
        // @example
        // Narrates an image's height.
        // - narrate "The image is <[image].height> tall."
        // -->
        tagProcessor.registerTag(ElementTag.class, "height", (attribute, object) -> {
            return new ElementTag(object.image.getHeight());
        });

        // <--[tag]
        // @attribute <ImageTag.type>
        // @returns ElementTag
        // @description
        // Returns image's type ("png", "jpg", "webp", etc.).
        // @example
        // Checks if an image is a png.
        // - if <[image].type> == png:
        //   - narrate "It's a png!"
        // -->
        tagProcessor.registerTag(ElementTag.class, "type", (attribute, object) -> {
            return new ElementTag(object.imageType, true);
        });

        // <--[tag]
        // @attribute <ImageTag.to_binary>
        // @returns BinaryTag
        // @description
        // Returns a <@link ObjectType BinaryTag> of the image's raw binary data.
        // @example
        // Gets a base64 string of the image, sometimes used in web APIs.
        // - define encoded <[image].to_binary.to_base64>
        // -->
        tagProcessor.registerTag(BinaryTag.class, "to_binary", (attribute, object) -> {
            return new BinaryTag(object.getRawBytes());
        });

        // <--[tag]
        // @attribute <ImageTag.sub_image[x=<x>;y=<y>;width=<width>;height=<height>]>
        // @returns ImageTag
        // @description
        // Returns a part of the image.
        // "x" and "y" are the position of the upper left corner of the image part, see <@link language Image positions>.
        // "width" and "height" are the size of the part that should be returned.
        // @example
        // Gets the top right corner of a 100x100 image.
        // - define corner <[image].sub_image[x=50;y=0;with=50;height=50]>
        // -->
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

        // <--[mechanism]
        // @object ImageTag
        // @name scale
        // @input MapTag
        // @description
        // Rescales an image.
        // The input is a <@link ObjectType MapTag> with "width" and "height" keys
        // @example
        // Rescales an image to be 50x50
        // - adjust def:image scale:[width=50;height=50]
        // @tags
        // <ImageTag.width>
        // <ImageTag.height>
        // -->
        tagProcessor.registerMechanism("scale", false, MapTag.class, (object, mechanism, input) -> {
            ElementTag widthElement = input.getElement("width");
            ElementTag heightElement = input.getElement("height");
            if ((widthElement != null && !widthElement.isInt()) || (heightElement != null && !heightElement.isInt())) {
                mechanism.echoError("Invalid width/height '" + widthElement  + '/' + heightElement + "' specified: must be valid numbers.");
                return;
            }
            int width = widthElement != null ? widthElement.asInt() : object.image.getWidth();
            int height = heightElement != null ? heightElement.asInt() : object.image.getHeight();
            BufferedImage rescaled = new BufferedImage(width, height, object.image.getType());
            Graphics2D graphics = rescaled.createGraphics();
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(object.image, 0, 0, width, height, null);
            graphics.dispose();
            object.image = rescaled;
            object.markChanged();
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

    String hexEncodedBytesCache;

    public void markChanged() {
        hexEncodedBytesCache = null;
    }

    @Override
    public String identify() {
        if (hexEncodedBytesCache == null) {
            hexEncodedBytesCache = BinaryTag.hexEncode(getRawBytes(), false);
        }
        return "image@" + hexEncodedBytesCache;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String debuggable() {
        MapTag imageData = new MapTag();
        imageData.putObject("width", new ElementTag(image.getWidth()));
        imageData.putObject("height", new ElementTag(image.getHeight()));
        imageData.putObject("type", new ElementTag(imageType, true));
        return "<LG>image@<Y>" + imageData.debuggable().substring("<LG>map@".length());
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
        ImageTag duplicate = new ImageTag(clone, imageType);
        duplicate.hexEncodedBytesCache = hexEncodedBytesCache;
        return duplicate;
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
