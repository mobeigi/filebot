
package net.sourceforge.filebot.ui.panel.analyze;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.panel.analyze.tools.SplitPanel;
import net.sourceforge.filebot.ui.panel.analyze.tools.ToolPanel;
import net.sourceforge.filebot.ui.panel.analyze.tools.TypePanel;


public class AnalyzePanel extends FileBotPanel {
	
	private FileTreePanel filePanel = new FileTreePanel();
	private JTabbedPane toolsPanel = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	
	
	public AnalyzePanel() {
		super("Analyze", ResourceManager.getIcon("panel.analyze"));
		
		Box panel = new Box(BoxLayout.X_AXIS);
		
		panel.add(filePanel);
		
		panel.add(Box.createHorizontalStrut(50));
		
		JPanel right = new JPanel();
		right.setLayout(new BorderLayout());
		right.setBorder(BorderFactory.createTitledBorder("Tools"));
		
		right.add(toolsPanel, BorderLayout.CENTER);
		
		panel.add(right);
		
		add(panel, BorderLayout.CENTER);
		
		filePanel.getFileTree().addPropertyChangeListener(FileTree.CONTENT_PROPERTY, fileTreeChangeListener);
		
		addTool(new TypePanel());
		addTool(new SplitPanel());
		
		Dimension min = new Dimension(300, 300);
		filePanel.setMinimumSize(min);
		toolsPanel.setMinimumSize(min);
	}
	
	private PropertyChangeListener fileTreeChangeListener = new PropertyChangeListener() {
		
		@SuppressWarnings("unchecked")
		public void propertyChange(PropertyChangeEvent evt) {
			List<File> files = (List<File>) evt.getNewValue();
			
			for (ToolPanel toolPanel : toolPanels)
				toolPanel.update(files);
		}
	};
	
	private List<ToolPanel> toolPanels = new ArrayList<ToolPanel>();
	
	
	public void addTool(ToolPanel toolPanel) {
		toolsPanel.addTab(toolPanel.getName(), toolPanel);
		toolPanels.add(toolPanel);
	}
	
}
