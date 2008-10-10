
package net.sourceforge.filebot;


import java.awt.Image;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;


public class ResourceManager {
	
	public static ImageIcon getIcon(String name) {
		return getIcon(name, null);
	}
	

	public static ImageIcon getIcon(String name, String def) {
		URL resource = getImageResource(name, def);
		
		if (resource != null)
			return new ImageIcon(resource);
		
		return null;
	}
	

	public static ImageIcon getFlagIcon(String languageCode) {
		return getIcon(String.format("flags/%s", languageCode), "flags/default");
	}
	

	public static ImageIcon getArchiveIcon(String type) {
		return getIcon(String.format("archives/%s", type), "archives/default");
	}
	

	public static Image getImage(String name) {
		try {
			return ImageIO.read(getImageResource(name));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

	/**
	 * Get the URL of an image resource in this jar. Image must be located in
	 * <code>/resources</code> and the file type is assumed to be png.
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
	

	private ResourceManager() {
		throw new UnsupportedOperationException();
	}
	
}
