
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JPanel;

import net.sourceforge.filebot.ui.panel.analyze.AnalyzePanel;
import net.sourceforge.filebot.ui.panel.list.ListPanel;
import net.sourceforge.filebot.ui.panel.rename.RenamePanel;
import net.sourceforge.filebot.ui.panel.search.SearchPanel;
import net.sourceforge.filebot.ui.panel.sfv.SfvPanel;
import net.sourceforge.filebot.ui.panel.subtitle.SubtitlePanel;


public class FileBotPanel extends JPanel {
	
	private static final List<FileBotPanel> registry = new ArrayList<FileBotPanel>();
	
	static {
		registry.add(new ListPanel());
		registry.add(new RenamePanel());
		registry.add(new AnalyzePanel());
		registry.add(new SearchPanel());
		registry.add(new SubtitlePanel());
		registry.add(new SfvPanel());
	}
	
	
	public static List<FileBotPanel> getAvailablePanels() {
		return Collections.unmodifiableList(registry);
	}
	

	public static FileBotPanel forName(String name) {
		for (FileBotPanel panel : registry) {
			if (name.equalsIgnoreCase(panel.getPanelName()))
				return panel;
		}
		
		return null;
	}
	
	private final String name;
	private final Icon icon;
	
	
	public FileBotPanel(String title, Icon icon) {
		super(new BorderLayout(10, 0));
		this.name = title;
		this.icon = icon;
	}
	

	public Icon getIcon() {
		return icon;
	}
	

	public String getPanelName() {
		return name;
	}
}
