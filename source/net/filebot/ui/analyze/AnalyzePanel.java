package net.sourceforge.filebot.ui.analyze;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

public class AnalyzePanel extends JComponent {

	private final FileTreePanel fileTreePanel = new FileTreePanel();
	private final JTabbedPane toolsPanel = new JTabbedPane();

	public AnalyzePanel() {
		toolsPanel.setBorder(BorderFactory.createTitledBorder("Tools"));

		setLayout(new MigLayout("insets dialog, gapx 50, fill"));

		add(fileTreePanel, "grow, sizegroupx column");
		add(toolsPanel, "grow, sizegroupx column");

		addTool(new ExtractTool());
		addTool(new SplitTool());
		addTool(new TypeTool());
		addTool(new AttributeTool());

		putClientProperty("transferablePolicy", fileTreePanel.getTransferablePolicy());

		fileTreePanel.addPropertyChangeListener("filetree", filetreeListener);
	}

	private void addTool(Tool<?> tool) {
		toolsPanel.addTab(tool.getName(), tool);
	}

	private final PropertyChangeListener filetreeListener = new PropertyChangeListener() {

		public void propertyChange(PropertyChangeEvent evt) {
			// stopped loading, refresh tools
			for (int i = 0; i < toolsPanel.getTabCount(); i++) {
				Tool<?> tool = (Tool<?>) toolsPanel.getComponentAt(i);
				tool.setSourceModel(fileTreePanel.getFileTree().getRoot());
			}
		}
	};

}
