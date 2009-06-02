
package net.sourceforge.filebot;


import java.awt.Image;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;


public final class ResourceManager {
	
	private static final Cache cache = CacheManager.getInstance().getCache("resource");
	

	public static Icon getIcon(String name) {
		return getIcon(name, null);
	}
	

	public static Icon getIcon(String name, String def) {
		Icon icon = probeCache(name, Icon.class);
		
		if (icon == null) {
			URL resource = getImageResource(name, def);
			
			if (resource != null) {
				icon = populateCache(name, Icon.class, new ImageIcon(resource));
			}
		}
		
		return icon;
	}
	

	public static Icon getFlagIcon(String languageCode) {
		return getIcon(String.format("flags/%s", languageCode));
	}
	

	public static Image getImage(String name) {
		try {
			return ImageIO.read(getImageResource(name));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

	/**
	 * Get the URL of an image resource in this jar. Image must be located in <code>resources/</code> and the file type
	 * is assumed to be png.
	 * 
	 * @param name simple name of the resource (without extension)
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
	

	private static <T> T probeCache(String name, Class<T> type) {
		Element entry = cache.get(type.getName() + ":" + name);
		
		if (entry != null) {
			return type.cast(entry.getObjectValue());
		}
		
		return null;
	}
	

	private static <T> T populateCache(String name, Class<? super T> type, T value) {
		cache.put(new Element(type.getName() + ":" + name, value));
		
		return value;
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private ResourceManager() {
		throw new UnsupportedOperationException();
	}
	
}
