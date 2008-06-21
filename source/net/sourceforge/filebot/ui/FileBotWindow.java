
package net.sourceforge.filebot.ui;


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

import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.MessageBus;
import net.sourceforge.tuned.MessageHandler;
import net.sourceforge.tuned.ui.ShadowBorder;
import net.sourceforge.tuned.ui.SimpleListModel;


public class FileBotWindow extends JFrame implements ListSelectionListener {
	
	private JPanel pagePanel = new JPanel(new CardLayout());
	
	private FileBotPanelSelectionList selectionListPanel = new FileBotPanelSelectionList();
	
	private HeaderPanel headerPanel = new HeaderPanel();
	
	
	public FileBotWindow() {
		super(Settings.NAME);
		setLocationByPlatform(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		ArrayList<Image> icons = new ArrayList<Image>(2);
		icons.add(ResourceManager.getImage("window.icon.small"));
		icons.add(ResourceManager.getImage("window.icon.big"));
		setIconImages(icons);
		
		selectionListPanel.addListSelectionListener(this);
		
		JComponent contentPane = createContentPane();
		
		setContentPane(contentPane);
		
		setSize(760, 615);
		
		selectionListPanel.setSelectedIndex(Settings.getSettings().getInt(Settings.SELECTED_PANEL, 3));
		
		MessageBus.getDefault().addMessageHandler("panel", panelMessageHandler);
	}
	

	public void valueChanged(ListSelectionEvent e) {
		FileBotPanel currentPanel = (FileBotPanel) selectionListPanel.getSelectedValue();
		
		headerPanel.setTitle(currentPanel.getPanelName());
		CardLayout cardLayout = (CardLayout) pagePanel.getLayout();
		cardLayout.show(pagePanel, currentPanel.getPanelName());
		
		JComponent c = (JComponent) getContentPane();
		
		c.revalidate();
		c.repaint();
		
		Settings.getSettings().putInt(Settings.SELECTED_PANEL, selectionListPanel.getSelectedIndex());
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
	

	@SuppressWarnings("unchecked")
	private JComponent createPageLayer() {
		JPanel pageLayer = new JPanel(new BorderLayout());
		
		pagePanel.setBorder(new EmptyBorder(10, 110, 10, 10));
		
		pageLayer.add(headerPanel, BorderLayout.NORTH);
		pageLayer.add(pagePanel, BorderLayout.CENTER);
		
		SimpleListModel model = (SimpleListModel) selectionListPanel.getModel();
		
		for (FileBotPanel panel : (List<FileBotPanel>) model.getCopy()) {
			panel.setVisible(false);
			pagePanel.add(panel, panel.getPanelName());
		}
		
		pageLayer.setAlignmentX(0.0f);
		pageLayer.setAlignmentY(0.0f);
		
		return pageLayer;
	}
	

	private JComponent createContentPane() {
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new OverlayLayout(contentPane));
		
		contentPane.add(createSelectionListLayer());
		contentPane.add(createPageLayer());
		
		return contentPane;
	}
	
	private final MessageHandler panelMessageHandler = new MessageHandler() {
		
		@Override
		public void handle(String topic, String... messages) {
			for (String panel : messages) {
				selectionListPanel.setSelectedValue(FileBotPanel.forName(panel), true);
			}
		}
		
	};
	
}
