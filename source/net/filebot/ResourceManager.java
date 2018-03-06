package net.filebot;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BaseMultiResolutionImage;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public final class ResourceManager {

	private static final Map<String, Icon> cache = synchronizedMap(new HashMap<String, Icon>(256));

	public static Icon getIcon(String name) {
		return cache.computeIfAbsent(name, i -> {
			// load image
			URL[] resource = getMultiResolutionImageResource(i);
			if (resource.length > 0) {
				return new ImageIcon(getMultiResolutionImage(resource));
			}

			// default image
			return null;
		});
	}

	public static Stream<URL> getApplicationIconResources() {
		return Stream.of("window.icon.large", "window.icon.medium", "window.icon.small").map(ResourceManager::getImageResource);
	}

	public static List<Image> getApplicationIconImages() {
		return getApplicationIconResources().map(ResourceManager::getToolkitImage).collect(toList());
	}

	public static Icon getFlagIcon(String languageCode) {
		return getIcon("flags/" + languageCode);
	}

	private static Image getToolkitImage(URL resource) {
		// load sun.awt.image.ToolkitImage or sun.awt.image.MultiResolutionToolkitImage (via @2x convention)
		return Toolkit.getDefaultToolkit().getImage(resource);
	}

	private static Image getMultiResolutionImage(URL[] resource) {
		try {
			Image[] image = new Image[resource.length];
			for (int i = 0; i < image.length; i++) {
				image[i] = ImageIO.read(resource[i]);
			}
			return new BaseMultiResolutionImage(image);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static URL[] getMultiResolutionImageResource(String name) {
		return Stream.of(name, name + "@2x").map(ResourceManager::getImageResource).filter(Objects::nonNull).toArray(URL[]::new);
	}

	private static URL getImageResource(String name) {
		return ResourceManager.class.getResource("resources/" + name + ".png");
	}

	private ResourceManager() {
		throw new UnsupportedOperationException();
	}

}
