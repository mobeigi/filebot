
package net.sourceforge.filebot.resources;


import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;


public class ResourceManager {
	
	private static HashMap<String, URL> resourceMap = new HashMap<String, URL>();
	
	static {
		putResource("window.icon.small", "window.icon.small.png");
		putResource("window.icon.big", "window.icon.big.png");
		
		putResource("panel.analyze", "panel.analyze.png");
		putResource("panel.rename", "panel.rename.png");
		putResource("panel.create", "panel.create.png");
		putResource("panel.list", "panel.list.png");
		putResource("panel.search", "panel.search.png");
		
		putResource("tree.open", "tree.open.png");
		putResource("tree.closed", "tree.closed.png");
		putResource("tree.leaf", "tree.leaf.png");
		
		putResource("action.rename", "action.rename.png");
		putResource("action.down", "action.down.png");
		putResource("action.up", "action.up.png");
		putResource("action.save", "action.save.png");
		putResource("action.load", "action.load.png");
		putResource("action.clear", "action.clear.png");
		putResource("action.find", "action.find.png");
		
		putResource("action.match.file2name", "action.match.file2name.png");
		putResource("action.match.name2file", "action.match.name2file.png");
		
		putResource("message.info", "message.info.png");
		putResource("message.warning", "message.warning.png");
		
		putResource("search.anidb", "search.anidb.png");
		putResource("search.tvdotcom", "search.tvdotcom.png");
		putResource("search.tvrage", "search.tvrage.png");
		
		putResource("tab.close", "tab.close.png");
		putResource("tab.close.hover", "tab.close.hover.png");
		putResource("tab.loading", "tab.loading.gif");
		putResource("tab.history", "action.find.png");
		
		putResource("loading", "loading.gif");
		
		putResource("tree.expand", "tree.expand.png");
		putResource("tree.collapse", "tree.collapse.png");
		
		putResource("panel.sfv", "panel.sfv.png");
		putResource("status.error", "status.error.png");
		putResource("status.ok", "status.ok.png");
		putResource("status.unknown", "status.unknown.png");
		putResource("status.warning", "status.warning.png");
		
		putResource("decoration.header", "decoration.header.png");
	}
	
	
	private static void putResource(String name, String file) {
		resourceMap.put(name, ResourceManager.class.getResource(file));
	}
	

	public static Image getImage(String name) {
		if (!resourceMap.containsKey(name))
			return null;
		
		try {
			return ImageIO.read(resourceMap.get(name));
		} catch (IOException e) {
			return null;
		}
	}
	

	public static ImageIcon getIcon(String name) {
		return new ImageIcon(resourceMap.get(name));
	}
	
}
