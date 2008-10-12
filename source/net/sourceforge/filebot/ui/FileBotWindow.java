
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.ui.panel.analyze.AnalyzePanel;
import net.sourceforge.filebot.ui.panel.episodelist.EpisodeListPanel;
import net.sourceforge.filebot.ui.panel.list.ListPanel;
import net.sourceforge.filebot.ui.panel.rename.RenamePanel;
import net.sourceforge.filebot.ui.panel.sfv.SfvPanel;
import net.sourceforge.filebot.ui.panel.subtitle.SubtitlePanel;
import net.sourceforge.tuned.MessageBus;
import net.sourceforge.tuned.MessageHandler;
import net.sourceforge.tuned.ui.ShadowBorder;


public class FileBotWindow extends JFrame implements ListSelectionListener {
	
	private JPanel pagePanel = new JPanel(new CardLayout());
	
	private FileBotPanelSelectionList selectionListPanel = new FileBotPanelSelectionList();
	
	private HeaderPanel headerPanel = new HeaderPanel();
	
	
	public FileBotWindow() {
		super(FileBotUtil.getApplicationName());
		setLocationByPlatform(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		ArrayList<Image> icons = new ArrayList<Image>(2);
		icons.add(ResourceManager.getImage("window.icon.small"));
		icons.add(ResourceManager.getImage("window.icon.big"));
		setIconImages(icons);
		
		selectionListPanel.getPanelModel().addAll(createPanels());
		selectionListPanel.addListSelectionListener(this);
		
		JComponent contentPane = createContentPane();
		
		setContentPane(contentPane);
		
		setSize(760, 615);
		
		// restore the panel selection from last time,
		// switch to EpisodeListPanel by default (e.g. first start)
		int selectedPanel = Preferences.userNodeForPackage(getClass()).getInt("selectedPanel", 3);
		selectionListPanel.setSelectedIndex(selectedPanel);
		
		MessageBus.getDefault().addMessageHandler("panel", panelSelectMessageHandler);
	}
	

	private List<FileBotPanel> createPanels() {
		List<FileBotPanel> panels = new ArrayList<FileBotPanel>();
		
		panels.add(new ListPanel());
		panels.add(new RenamePanel());
		panels.add(new AnalyzePanel());
		panels.add(new EpisodeListPanel());
		panels.add(new SubtitlePanel());
		panels.add(new SfvPanel());
		
		return panels;
	}
	

	public void valueChanged(ListSelectionEvent e) {
		FileBotPanel currentPanel = (FileBotPanel) selectionListPanel.getSelectedValue();
		
		headerPanel.setTitle(currentPanel.getPanelName());
		CardLayout cardLayout = (CardLayout) pagePanel.getLayout();
		cardLayout.show(pagePanel, currentPanel.getPanelName());
		
		JComponent c = (JComponent) getContentPane();
		
		c.revalidate();
		c.repaint();
		
		Preferences.userNodeForPackage(getClass()).putInt("selectedPanel", selectionListPanel.getSelectedIndex());
	}
	

	private JComponent createSelectionListLayer() {
		JPanel selectionListLayer = new JPanel(new BorderLayout());
		selectionListLayer.setOpaque(false);
		
		JPanel shadowBorderPanel = new JPanel(new BorderLayout());
		shadowBorderPanel.setOpaque(false);
		
		JScrollPane selectListScrollPane = new JScrollPane(selectionListPanel);
		selectListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		selectListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		shadowBorderPanel.add(selectListScrollPane, BorderLayout.CENTER);
		shadowBorderPanel.setBorder(new ShadowBorder());
		
		selectionListLayer.setBorder(new EmptyBorder(10, 6, 12, 0));
		selectionListLayer.add(shadowBorderPanel, BorderLayout.WEST);
		
		selectionListLayer.setAlignmentX(0.0f);
		selectionListLayer.setAlignmentY(0.0f);
		selectionListLayer.setMaximumSize(selectionListLayer.getPreferredSize());
		
		return selectionListLayer;
	}
	

	private JComponent createPageLayer() {
		JPanel pageLayer = new JPanel(new BorderLayout());
		pagePanel.setBorder(new EmptyBorder(10, 110, 10, 10));
		
		pageLayer.add(headerPanel, BorderLayout.NORTH);
		pageLayer.add(pagePanel, BorderLayout.CENTER);
		
		for (FileBotPanel panel : selectionListPanel.getPanelModel()) {
			pagePanel.add(panel, panel.getPanelName());
		}
		
		pageLayer.setAlignmentX(0.0f);
		pageLayer.setAlignmentY(0.0f);
		
		return pageLayer;
	}
	

	private JComponent createContentPane() {
		JPanel contentPane = new JPanel(null);
		contentPane.setLayout(new OverlayLayout(contentPane));
		
		contentPane.add(createSelectionListLayer());
		contentPane.add(createPageLayer());
		
		return contentPane;
	}
	
	private final MessageHandler panelSelectMessageHandler = new MessageHandler() {
		
		@Override
		public void handle(String topic, Object... messages) {
			if (messages.length >= 1) {
				// get last element in array
				Object panel = messages[messages.length - 1];
				
				// switch to this panel
				if (panel instanceof FileBotPanel)
					selectionListPanel.setSelectedValue(panel, true);
			}
		}
		
	};
	
}
