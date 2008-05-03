
package net.sourceforge.filebot.resources;


import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;


public class ResourceManager {
	
	private ResourceManager() {
		// hide constructor
	}
	
	private static final Map<String, String> aliasMap = new HashMap<String, String>();
	
	static {
		aliasMap.put("tab.loading", "tab.loading.gif");
		aliasMap.put("tab.history", "action.find.png");
		aliasMap.put("loading", "loading.gif");
	}
	
	private static final Map<String, Image> cache = Collections.synchronizedMap(new WeakHashMap<String, Image>());
	
	
	public static ImageIcon getIcon(String name) {
		return new ImageIcon(getImage(name));
	}
	

	public static ImageIcon getFlagIcon(String languageCode) {
		if (languageCode == null)
			languageCode = "default";
		
		return new ImageIcon(getImage(String.format("flags/%s", languageCode.toLowerCase()), "flags/default"));
	}
	

	public static ImageIcon getArchiveIcon(String type) {
		if (type == null)
			type = "default";
		
		return new ImageIcon(getImage(String.format("archives/%s", type.toLowerCase()), "archives/default"));
	}
	

	public static Image getImage(String name) {
		Image image = cache.get(name);
		
		if (image == null) {
			try {
				// load image if not in cache
				URL resource = getResource(name);
				
				if (resource != null) {
					image = ImageIO.read(resource);
					cache.put(name, image);
				}
			} catch (IOException e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
			}
		}
		
		return image;
	}
	

	private static Image getImage(String name, String def) {
		Image image = getImage(name);
		
		// image not found, use default
		if (image == null)
			image = getImage(def);
		
		return image;
	}
	

	private static URL getResource(String name) {
		String resource = null;
		
		if (aliasMap.containsKey(name))
			resource = aliasMap.get(name);
		else
			resource = name + ".png";
		
		return ResourceManager.class.getResource(resource);
	}
	
}
