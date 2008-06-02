
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
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.panel.analyze.tools.SplitPanel;
import net.sourceforge.filebot.ui.panel.analyze.tools.ToolPanel;
import net.sourceforge.filebot.ui.panel.analyze.tools.TypePanel;
import net.sourceforge.tuned.MessageBus;


public class AnalyzePanel extends FileBotPanel {
	
	private final FileTreePanel fileTreePanel = new FileTreePanel();
	private final JTabbedPane toolsPanel = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	
	private final List<ToolPanel> toolPanels = new ArrayList<ToolPanel>();
	
	
	public AnalyzePanel() {
		super("Analyze", ResourceManager.getIcon("panel.analyze"));
		
		Box panel = new Box(BoxLayout.X_AXIS);
		
		panel.add(fileTreePanel);
		
		panel.add(Box.createHorizontalStrut(50));
		
		JPanel right = new JPanel();
		right.setLayout(new BorderLayout());
		right.setBorder(BorderFactory.createTitledBorder("Tools"));
		
		right.add(toolsPanel, BorderLayout.CENTER);
		
		panel.add(right);
		
		add(panel, BorderLayout.CENTER);
		
		Dimension min = new Dimension(300, 300);
		fileTreePanel.setMinimumSize(min);
		toolsPanel.setMinimumSize(min);
		
		addTool(new TypePanel());
		addTool(new SplitPanel());
		
		fileTreePanel.getFileTree().addPropertyChangeListener(FileTree.CONTENT_PROPERTY, fileTreeChangeListener);
		
		MessageBus.getDefault().addMessageHandler(getPanelName(), new FileTransferableMessageHandler(getPanelName(), fileTreePanel.getFileTree().getTransferablePolicy()));
	}
	

	private void addTool(ToolPanel toolPanel) {
		toolsPanel.addTab(toolPanel.getToolName(), toolPanel);
		toolPanels.add(toolPanel);
	}
	
	private PropertyChangeListener fileTreeChangeListener = new PropertyChangeListener() {
		
		@SuppressWarnings("unchecked")
		public void propertyChange(PropertyChangeEvent evt) {
			List<File> files = (List<File>) evt.getNewValue();
			
			for (ToolPanel toolPanel : toolPanels) {
				toolPanel.update(files);
			}
		}
	};
	
}
