package net.filebot.ui.analyze;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

public class AnalyzePanel extends JComponent {

	private final FileTreePanel fileTreePanel = new FileTreePanel();
	private final JTabbedPane toolsPanel = new JTabbedPane();

	public AnalyzePanel() {
		setLayout(new MigLayout("insets dialog, gapx 50, fill"));
		add(fileTreePanel, "grow, sizegroupx column");
		add(toolsPanel, "grow, sizegroupx column");

		putClientProperty("transferablePolicy", fileTreePanel.getTransferablePolicy());

		fileTreePanel.addPropertyChangeListener("filetree", evt -> {
			// stopped loading, refresh tools
			for (int i = 0; i < toolsPanel.getTabCount(); i++) {
				Tool<?> tool = (Tool<?>) toolsPanel.getComponentAt(i);
				tool.updateRoot(fileTreePanel.getFileTree().getRoot().getFile());
			}
		});
	}

	public void addTool(Tool<?> tool) {
		toolsPanel.addTab(tool.getName(), tool);
	}

}
