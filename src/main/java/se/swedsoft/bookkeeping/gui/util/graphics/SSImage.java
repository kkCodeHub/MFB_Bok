package se.swedsoft.bookkeeping.gui.util.graphics;


import org.fribok.bookkeeping.app.Path;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Date: 2006-feb-08
 * Time: 10:51:02
 */
public class SSImage {    private static final Logger LOG = LoggerFactory.getLogger(SSImage.class);


    // Library of all graphics
    private static Map<String, BufferedImage> graphics = new HashMap<>();

    // Ensure non-instantiability
    private SSImage() {
        throw new AssertionError("Don't instantiate this class");
    }

    // Standard graphics
    static {
        loadImage("BACKGROUND", "Background.png");
        loadImage("LOGO", "Logo.png");
        loadImage("OCRAVI", "OCRAvi.png");
        loadImage("OCRBackground", "OCRBackground.png");
        loadImage("CHECK", "Check.png");
        loadImage("SERVER", "Server.png");

        loadImage("ABOUT_LOGO", "Logo.png");
        loadImage("ICON_LOGO_16", "Logo_Icon16.png");
        loadImage("ICON_LOGO_24", "Logo_Icon24.png");
        loadImage("ICON_LOGO_32", "Logo_Icon32.png");
        loadImage("ICON_LOGO_64", "Logo_Icon64.png");
        loadImage("ICON_LOGO_128", "Logo_Icon128.png");
        loadImage("ICON_LOGO_256", "Logo_Icon256.png");
        loadImage("ICON_LOGO_512", "Logo_Icon512.png");

    }

    /**
     * Load an image from disk
     * @param name     unique name of the image
     * @param filename the name of the image file
     */
    private static void loadImage(String name, String filename) {
        if (graphics.containsKey(name)) {
            LOG.info("(SSImage): Already loaded image: " + name);
            return;
        }

        URL url = Path.class.getResource("/graphics/" + filename);

        if (url == null) {
            LOG.warn("(SSImage): Missing image resource: " + filename);
            return;
        }

        BufferedImage image = null;

        try {
            image = ImageIO.read(url);
        } catch (IOException e) {
            LOG.error("Unexpected error", e);
        }

        if (image != null) {
            graphics.put(name, image);
        } else {
            LOG.info("(SSImage): Failed to load image: " + filename);
        }
    }

    /**
     * Check if an image with the specified name exists
     * @param name the unique name of the image
     * @return true if image exists
     */
    public static boolean hasImage(String name) {
        return graphics.containsKey(name);
    }

    /**
     * Gets the image by the specified name
     * @param name the unique name of the image
     * @return the image
     */
    public static BufferedImage getImage(String name) {
        if (!graphics.containsKey(name)) {
            LOG.info("(SSImage): Image not found: " + name);
        }
        return graphics.get(name);
    }

    /**
     * Gets all bundled application icon sizes for window managers that prefer
     * larger icons than 16x16 (for example Linux Alt-Tab and task bars).
     *
     * @return immutable list of application icon images
     */
    public static List<Image> getApplicationIcons() {
        List<Image> images = new ArrayList<>();

        addApplicationIcon(images, "ICON_LOGO_16");
        addApplicationIcon(images, "ICON_LOGO_24");
        addApplicationIcon(images, "ICON_LOGO_32");
        addApplicationIcon(images, "ICON_LOGO_64");
        addApplicationIcon(images, "ICON_LOGO_128");
        addApplicationIcon(images, "ICON_LOGO_256");
        addApplicationIcon(images, "ICON_LOGO_512");

        return Collections.unmodifiableList(images);
    }

    private static void addApplicationIcon(List<Image> images, String name) {
        Image image = getImage(name);

        if (image != null) {
            images.add(image);
        }
    }
}
