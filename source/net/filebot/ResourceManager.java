package net.filebot;

import static java.util.Arrays.*;

import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.filebot.util.WeakValueHashMap;

public final class ResourceManager {

	private static final WeakValueHashMap<String, Icon> cache = new WeakValueHashMap<String, Icon>(256);

	public static Icon getIcon(String name) {
		return getIcon(name, null);
	}

	public static Icon getIcon(String name, String def) {
		Icon icon = null;

		// try cache
		synchronized (cache) {
			icon = cache.get(name);
			if (icon != null) {
				return icon;
			}
		}

		URL resource = getImageResource(name, def);
		if (resource == null) {
			return null;
		}

		Image image = getImage(resource);
		icon = new ImageIcon(image);

		// update cache
		synchronized (cache) {
			cache.put(name, icon);
		}

		return icon;
	}

	public static List<Image> getApplicationIcons() {
		return getApplicationIconURLs().stream().map(ResourceManager::getImage).collect(Collectors.toList());
	}

	public static List<URL> getApplicationIconURLs() {
		URL[] images = new URL[3];
		images[0] = ResourceManager.getImageResource("window.icon.small");
		images[1] = ResourceManager.getImageResource("window.icon.medium");
		images[2] = ResourceManager.getImageResource("window.icon.large");
		return asList(images);
	}

	public static Icon getFlagIcon(String languageCode) {
		return getIcon("flags/" + languageCode);
	}

	private static Image getImage(URL resource) {
		// load sun.awt.image.ToolkitImage or sun.awt.image.MultiResolutionToolkitImage (via @2x convention)
		return Toolkit.getDefaultToolkit().getImage(resource);
	}

	/**
	 * Get the URL of an image resource in this jar. Image must be located in <code>resources/</code> and the file type is assumed to be png.
	 *
	 * @param name
	 *            simple name of the resource (without extension)
	 * @return URL of the resource or null if resource does not exist
	 */
	private static URL getImageResource(String name) {
		return ResourceManager.class.getResource("resources/" + name + ".png");
	}

	private static URL getImageResource(String name, String def) {
		URL resource = getImageResource(name);

		if (resource == null)
			resource = getImageResource(def);

		return resource;
	}

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private ResourceManager() {
		throw new UnsupportedOperationException();
	}

}
