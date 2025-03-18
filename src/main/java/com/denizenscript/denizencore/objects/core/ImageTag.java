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
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.spi.ImageWriterSpi;
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
    // - draw id:artwork image:%VALUE% x:5 y:5
    // @format
    // The identity format for ImageTag is a hex encoding of the image's raw bytes, in order.
    // Each byte is encoded as an individual big-endian hexadecimal pair, like "FF" for byte=255, "00" for byte=0, "0F" for 15, "F0" for 240, ... etc.
    // Alternatively, an image's identity format can be an image id (from images loaded in with <@link command Image>), see description for more information.
    //
    // @description
    // ImageTags represent an image of any supported format.
    // They are represented by either a hex encoding of their bytes or their image id, but new images can also be created by providing a MapTag with the following keys:
    // - "width": the new image's width, required.
    // - "height": the new image's height, required.
    // - "background": a <@link ObjectType ColorTag> for the new image's background, optional.
    //
    // For id-based images, the image will directly reference the image loaded in under that id, so for example:
    // <code>
    // - define image <image[drawing]>
    // - draw id:drawing pixel x:0 y:0 color:red
    // # The "image" definition has changed as well, since it's an id-based image and the image under that id was edited.
    // </code>
    // See <@link tag ImageTag.copy> for getting normal image objects from id-based ones.
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
                return imageById;
            }
        }
        if (!BinaryTag.isValidHex(stringLower)) {
            MapTag map = MapTag.valueOf(string, CoreUtilities.noDebugContext);
            if (map != null) {
                ElementTag width = map.getElement("width");
                ElementTag height = map.getElement("height");
                if (width == null || height == null || !width.isInt() || !height.isInt()) {
                    if ((context == null || context.showErrors()) && !TagManager.isStaticParsing) {
                        Debug.echoError("valueOf ImageTag returning null: must specify valid width/height.");
                    }
                    return null;
                }
                ImageTag image = new ImageTag(new BufferedImage(width.asInt(), height.asInt(), BufferedImage.TYPE_INT_ARGB));
                ColorTag background = map.getObjectAs("background", ColorTag.class, context);
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
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(rawBytes));
            if (image == null) {
                if (context == null || context.showErrors()) {
                    Debug.echoError("valueOf ImageTag returning null: could not recognize image format for binary: " + string);
                }
                return null;
            }
            ImageTag imageTag = new ImageTag(image);
            imageTag.setHexEncodedBytesCache(stringLower);
            return imageTag;
        }
        catch (IOException e) {
            if (context == null || context.showErrors()) {
                Debug.echoError("valueOf ImageTag returning null: invalid binary data '" + string + "' (see stacktrace below)");
                Debug.echoError(e);
            }
            return null;
        }
    }

    public static boolean matches(String string) {
        if (string.startsWith("image@")) {
            return true;
        }
        return valueOf(string, CoreUtilities.noDebugContext) != null;
    }

    public BufferedImage image;
    public String id;

    public ImageTag(BufferedImage image) {
        this.image = image;
    }

    public static void register() {

        // <--[tag]
        // @attribute <ImageTag.pixel_at[x=<x>;y=<y>]>
        // @returns ColorTag
        // @description
        // Returns the color of a specific pixel in an image.
        // "x" and "y" are the position of the pixel, see <@link language Image positions>.
        // @example
        // # Gets the color of the pixel at 43,21
        // - narrate "The color is: <[image].pixel_at[x=43;y=21]>."
        // -->
        tagProcessor.registerTag(ColorTag.class, MapTag.class, "pixel_at", (attribute, object, param) -> {
            ElementTag x = param.getRequiredObjectAs("x", ElementTag.class, attribute);
            ElementTag y = param.getRequiredObjectAs("y", ElementTag.class, attribute);
            if (x == null || y == null) {
                return null;
            }
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
        // Returns the image's width (in pixels).
        // @example
        // # Narrates an image's width.
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
        // Returns the image's height (in pixels).
        // @example
        // # Narrates an image's height.
        // - narrate "The image is <[image].height> tall."
        // -->
        tagProcessor.registerTag(ElementTag.class, "height", (attribute, object) -> {
            return new ElementTag(object.image.getHeight());
        });

        // <--[tag]
        // @attribute <ImageTag.to_binary[<image_format>]>
        // @returns BinaryTag
        // @description
        // Returns a BinaryTag of the image's raw binary data, in the specified image format.
        // @example
        // # Gets a base64 encoded string of the image, commonly used in web APIs.
        // - define base64 <[image].to_binary[png].to_base64>
        // -->
        tagProcessor.registerTag(BinaryTag.class, ElementTag.class, "to_binary", (attribute, object, imageFormat) -> {
            ImageTag corrected = object.correctType(imageFormat.asString());
            if (corrected == null) {
                attribute.echoError("Invalid/unfitting image format specified: " + imageFormat + '.');
                return null;
            }
            return new BinaryTag(corrected.getRawBytes(imageFormat.asString()));
        });

        // <--[tag]
        // @attribute <ImageTag.copy>
        // @returns ImageTag
        // @description
        // Returns a copy of the image, useful for getting normal images from id-based ones (see <@link command Image>).
        // @example
        // # Gets a normal image object from an id-based one.
        // - define image <image[drawing].copy>
        // # The "image" definition won't change.
        // - draw id:drawing pixel x:0 y:0 color:red
        // -->
        tagProcessor.registerTag(ImageTag.class, "copy", (attribute, object) -> {
            return object.duplicateIgnoreId();
        });

        // <--[tag]
        // @attribute <ImageTag.sub_image[x=<x>;y=<y>;width=<width>;height=<height>]>
        // @returns ImageTag
        // @description
        // Returns a part of the image.
        // "x" and "y" are the position of the upper left corner of the image part, see <@link language Image positions>.
        // "width" and "height" are the size of the image part.
        // @example
        // # Gets the top right corner of a 100x100 image.
        // - define corner <[image].sub_image[x=50;y=0;with=50;height=50]>
        // -->
        tagProcessor.registerTag(ImageTag.class, MapTag.class, "sub_image", (attribute, object, param) -> {
            ElementTag x = param.getRequiredObjectAs("x", ElementTag.class, attribute);
            ElementTag y = param.getRequiredObjectAs("y", ElementTag.class, attribute);
            ElementTag width = param.getRequiredObjectAs("width", ElementTag.class, attribute);
            ElementTag height = param.getRequiredObjectAs("height", ElementTag.class, attribute);
            if (x == null || y == null || width == null || height == null) {
                return null;
            }
            if (!x.isInt() || !y.isInt() || !width.isInt() || !height.isInt()) {
                attribute.echoError("Invalid x/y/width/height '" + x + '/' + y + '/' + width  + '/' + height + "' specified: must be valid numbers.");
                return null;
            }
            return new ImageTag(object.image.getSubimage(x.asInt(), y.asInt(), width.asInt(), height.asInt())).duplicate();
        });

        // <--[mechanism]
        // @object ImageTag
        // @name scale
        // @input MapTag
        // @description
        // Rescales an image.
        // The input is a <@link ObjectType MapTag> with "width" and "height" keys.
        // Both are optional, and default to the image's current respective value.
        // @example
        // # Rescales an image to be 50x50
        // - adjust def:image scale:[width=50;height=50]
        // @example
        // # Makes an image taller, keeping its existing width.
        // - adjust def:short_image scale:[height=100]
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

    public byte[] getRawBytes(String imageFormat) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, imageFormat, outputStream);
            return outputStream.toByteArray();
        }
        catch (IOException e) {
            Debug.echoError("Exception when converting image to bytes:");
            throw new RuntimeException(e);
        }
    }

    public ImageTag correctType(String imageFormat) {
        Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(imageFormat);
        if (!iter.hasNext()) {
            return null;
        }
        ImageWriterSpi imageWriterSpi = iter.next().getOriginatingProvider();
        if (imageWriterSpi.canEncodeImage(image)) {
            return this;
        }
        int correctType = -1;
        if (isSupportedType(imageWriterSpi, BufferedImage.TYPE_INT_ARGB)) { // Preferred type
            correctType = BufferedImage.TYPE_INT_ARGB;
        }
        else if (isSupportedType(imageWriterSpi, BufferedImage.TYPE_INT_RGB)) { // Next preferred type
            correctType = BufferedImage.TYPE_INT_RGB;
        }
        else { // Find the type it wants
            for (int type = BufferedImage.TYPE_INT_ARGB_PRE; type <= BufferedImage.TYPE_BYTE_INDEXED; type++) {
                if (isSupportedType(imageWriterSpi, type)) {
                    correctType = type;
                    break;
                }
            }
        }
        if (correctType == -1) {
            Debug.echoError("Image format '" + imageFormat + "' doesn't allow any type?");
            return null;
        }
        BufferedImage correctedImage = new BufferedImage(image.getWidth(), image.getHeight(), correctType);
        Graphics2D graphics = correctedImage.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return new ImageTag(correctedImage);
    }

    private static boolean isSupportedType(ImageWriterSpi imageWriterSpi, int type) {
        return imageWriterSpi.canEncodeImage(ImageTypeSpecifier.createFromBufferedImageType(type));
    }

    String hexEncodedBytesCache;

    public void setHexEncodedBytesCache(String hexEncodedBytes) {
        hexEncodedBytesCache = "image@" + hexEncodedBytes;
    }

    public void markChanged() {
        hexEncodedBytesCache = null;
    }

    @Override
    public String identify() {
        if (id != null) {
            return "image@" + id;
        }
        if (hexEncodedBytesCache == null) {
            setHexEncodedBytesCache(BinaryTag.hexEncode(getRawBytes("png"), false));
        }
        return hexEncodedBytesCache;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String debuggable() {
        if (id != null) {
            return "<LG>image@<Y>" + id;
        }
        MapTag imageData = new MapTag();
        imageData.putObject("width", new ElementTag(image.getWidth()));
        imageData.putObject("height", new ElementTag(image.getHeight()));
        return "<LG>image@<Y>" + imageData.debuggable().substring("<LG>map@".length());
    }

    @Override
    public boolean isUnique() {
        return id != null;
    }

    @Override
    public BufferedImage getJavaObject() {
        return image;
    }

    @Override
    public ImageTag duplicate() {
        if (isUnique()) {
            return this;
        }
        return duplicateIgnoreId();
    }

    public ImageTag duplicateIgnoreId() {
        BufferedImage clone = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D graphics = clone.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        ImageTag duplicate = new ImageTag(clone);
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
