
package net.sourceforge.filebot.ui.panel.analyze;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.FileBotPanel;
import net.sourceforge.filebot.ui.FileTransferableMessageHandler;
import net.sourceforge.filebot.ui.panel.analyze.tools.SplitPanel;
import net.sourceforge.filebot.ui.panel.analyze.tools.ToolPanel;
import net.sourceforge.filebot.ui.panel.analyze.tools.TypePanel;
import net.sourceforge.tuned.MessageBus;


public class AnalyzePanel extends FileBotPanel {
	
	private final FileTreePanel fileTreePanel = new FileTreePanel();
	private final JTabbedPane toolsPanel = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
	
	
	public AnalyzePanel() {
		super("Analyze", ResourceManager.getIcon("panel.analyze"));
		
		toolsPanel.setBorder(BorderFactory.createTitledBorder("Tools"));
		
		setLayout(new MigLayout("insets 0,gapx 50, fill"));
		
		add(fileTreePanel, "grow");
		add(toolsPanel, "grow");
		
		addTool(new TypePanel());
		addTool(new SplitPanel());
		
		fileTreePanel.getFileTree().addPropertyChangeListener(FileTree.CONTENT_PROPERTY, fileTreeChangeListener);
		
		MessageBus.getDefault().addMessageHandler(getPanelName(), new FileTransferableMessageHandler(this, fileTreePanel.getFileTree().getTransferablePolicy()));
	}
	

	private void addTool(ToolPanel toolPanel) {
		toolsPanel.addTab(toolPanel.getToolName(), toolPanel);
	}
	
	private PropertyChangeListener fileTreeChangeListener = new PropertyChangeListener() {
		
		@SuppressWarnings("unchecked")
		public void propertyChange(PropertyChangeEvent evt) {
			List<File> files = (List<File>) evt.getNewValue();
			
			for (int i = 0; i < toolsPanel.getTabCount(); i++) {
				ToolPanel toolPanel = (ToolPanel) toolsPanel.getComponentAt(i);
				toolPanel.update(files);
			}
		}
	};
	
}
