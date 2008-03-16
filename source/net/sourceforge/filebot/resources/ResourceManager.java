
package net.sourceforge.filebot.resources;


import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;


public class ResourceManager {
	
	private static HashMap<String, String> aliasMap = new HashMap<String, String>();
	
	static {
		aliasMap.put("tab.loading", "tab.loading.gif");
		aliasMap.put("tab.history", "action.find.png");
		
		aliasMap.put("loading", "loading.gif");
	}
	
	
	public static Image getImage(String name) {
		try {
			return ImageIO.read(getResource(name));
		} catch (IOException e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, e.toString(), e);
		}
		
		return null;
	}
	

	public static ImageIcon getIcon(String name) {
		return new ImageIcon(getResource(name));
	}
	

	public static ImageIcon getFlagIcon(String countryCode) {
		URL url = ResourceManager.class.getResource(String.format("flags/%s.gif", countryCode));
		
		if (url == null)
			url = ResourceManager.class.getResource(String.format("flags/default.gif", countryCode));
		
		return new ImageIcon(url);
	}
	

	public static ImageIcon getArchiveIcon(String type) {
		URL url = ResourceManager.class.getResource(String.format("archive/%s.png", type.toLowerCase()));
		
		if (url == null)
			url = ResourceManager.class.getResource(String.format("archive/default.png"));
		
		return new ImageIcon(url);
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
