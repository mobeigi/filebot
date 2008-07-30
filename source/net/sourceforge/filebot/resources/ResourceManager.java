
package net.sourceforge.filebot.resources;


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
		URL resource = getResource(name, def);
		
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
			return ImageIO.read(getResource(name));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	

	private static URL getResource(String name) {
		return ResourceManager.class.getResource(name + ".png");
	}
	

	private static URL getResource(String name, String def) {
		URL resource = getResource(name);
		
		if (resource == null)
			resource = getResource(def);
		
		return resource;
	}
	

	private ResourceManager() {
		throw new UnsupportedOperationException();
	}
	
}
