
package net.sourceforge.filebot.resources;


import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

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
	
	private static final Map<String, ImageIcon> iconCache = Collections.synchronizedMap(new WeakHashMap<String, ImageIcon>());
	
	
	public static ImageIcon getIcon(String name) {
		return getIcon(name, null);
	}
	

	public static ImageIcon getIcon(String name, String def) {
		ImageIcon icon = iconCache.get(name);
		
		if (icon == null) {
			// load image if not in cache
			URL resource = getResource(name, def);
			
			if (resource != null) {
				icon = new ImageIcon(resource);
				iconCache.put(name, icon);
			}
		}
		
		return icon;
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
		String resource = null;
		
		if (aliasMap.containsKey(name))
			resource = aliasMap.get(name);
		else
			resource = name + ".png";
		
		return ResourceManager.class.getResource(resource);
	}
	

	private static URL getResource(String name, String def) {
		URL resource = getResource(name);
		
		if (resource == null)
			resource = getResource(def);
		
		return resource;
	}
	
}
