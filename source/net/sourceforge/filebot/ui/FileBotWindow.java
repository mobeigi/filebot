
package net.sourceforge.filebot.ui;


import static net.sourceforge.filebot.FileBotUtilities.asStringList;
import static net.sourceforge.filebot.Settings.getApplicationName;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
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
	
	private FileBotPanelSelectionList panelSelectionList = new FileBotPanelSelectionList();
	
	private HeaderPanel headerPanel = new HeaderPanel();
	
	
	public FileBotWindow() {
		super(getApplicationName());
		
		setLocationByPlatform(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		// set taskbar / taskswitch icons
		ArrayList<Image> icons = new ArrayList<Image>(2);
		icons.add(ResourceManager.getImage("window.icon.small"));
		icons.add(ResourceManager.getImage("window.icon.big"));
		setIconImages(icons);
		
		panelSelectionList.getPanelModel().addAll(createPanels());
		panelSelectionList.addListSelectionListener(this);
		
		JComponent contentPane = createContentPane();
		
		setContentPane(contentPane);
		
		setSize(760, 615);
		
		// restore the panel selection from last time,
		// switch to EpisodeListPanel by default (e.g. first start)
		int selectedPanel = asStringList(panelSelectionList.getPanelModel()).indexOf(Settings.userRoot().get("selectedPanel"));
		panelSelectionList.setSelectedIndex(selectedPanel);
		
		// connect message handlers to message bus
		MessageBus.getDefault().addMessageHandler("panel", panelSelectMessageHandler);
		
		for (FileBotPanel panel : panelSelectionList.getPanelModel()) {
			MessageBus.getDefault().addMessageHandler(panel.getPanelName(), panel.getMessageHandler());
		}
	}
	

	private List<FileBotPanel> createPanels() {
		List<FileBotPanel> panels = new ArrayList<FileBotPanel>(6);
		
		panels.add(new ListPanel());
		panels.add(new RenamePanel());
		panels.add(new AnalyzePanel());
		panels.add(new EpisodeListPanel());
		panels.add(new SubtitlePanel());
		panels.add(new SfvPanel());
		
		return panels;
	}
	

	public void valueChanged(ListSelectionEvent e) {
		FileBotPanel currentPanel = (FileBotPanel) panelSelectionList.getSelectedValue();
		
		headerPanel.setTitle(currentPanel.getPanelName());
		CardLayout cardLayout = (CardLayout) pagePanel.getLayout();
		cardLayout.show(pagePanel, currentPanel.getPanelName());
		
		JComponent c = (JComponent) getContentPane();
		
		c.revalidate();
		c.repaint();
		
		Settings.userRoot().put("selectedPanel", panelSelectionList.getSelectedValue().toString());
	}
	

	private JComponent createSelectionListLayer() {
		JPanel selectionListLayer = new JPanel(new BorderLayout());
		selectionListLayer.setOpaque(false);
		
		JPanel shadowBorderPanel = new JPanel(new BorderLayout());
		shadowBorderPanel.setOpaque(false);
		
		JScrollPane selectListScrollPane = new JScrollPane(panelSelectionList);
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
		pagePanel.setBorder(new EmptyBorder(0, 95, 0, 0));
		
		pageLayer.add(headerPanel, BorderLayout.NORTH);
		pageLayer.add(pagePanel, BorderLayout.CENTER);
		
		for (FileBotPanel panel : panelSelectionList.getPanelModel()) {
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
					panelSelectionList.setSelectedValue(panel, true);
			}
		}
		
	};
	
}
