
package net.sourceforge.filebot.ui;


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.sourceforge.filebot.FileBotUtil;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.ui.ShadowBorder;


public class FileBotWindow extends JFrame implements ListSelectionListener {
	
	private JPanel pagePanel = new JPanel(new CardLayout());
	
	private FileBotPanelSelectionList selectionListPanel = new FileBotPanelSelectionList();
	
	private HeaderPanel headerPanel = new HeaderPanel();
	
	
	public FileBotWindow() {
		super("FileBot");
		setLocationByPlatform(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		ArrayList<Image> icons = new ArrayList<Image>(2);
		icons.add(ResourceManager.getImage("window.icon.small"));
		icons.add(ResourceManager.getImage("window.icon.big"));
		setIconImages(icons);
		
		selectionListPanel.addListSelectionListener(this);
		
		JComponent contentPane = createContentPane();
		
		setContentPane(contentPane);
		
		// Shortcut ESC
		FileBotUtil.registerActionForKeystroke(contentPane, KeyStroke.getKeyStroke("released ESCAPE"), closeAction);
		
		setSize(760, 615);
		
		selectionListPanel.setSelectedIndex(Settings.getSettings().getInt(Settings.SELECTED_PANEL, 3));
	}
	

	public void valueChanged(ListSelectionEvent e) {
		FileBotPanel currentPanel = (FileBotPanel) selectionListPanel.getSelectedValue();
		
		headerPanel.setTitle(currentPanel.getTitle());
		CardLayout cardLayout = (CardLayout) pagePanel.getLayout();
		cardLayout.show(pagePanel, currentPanel.getTitle());
		
		JComponent c = (JComponent) getContentPane();
		c.updateUI();
		
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
	

	private JComponent createPageLayer() {
		JPanel pageLayer = new JPanel(new BorderLayout());
		
		pagePanel.setBorder(new EmptyBorder(10, 110, 10, 10));
		
		pageLayer.add(headerPanel, BorderLayout.NORTH);
		pageLayer.add(pagePanel, BorderLayout.CENTER);
		
		ListModel model = selectionListPanel.getModel();
		
		for (int i = 0; i < model.getSize(); i++) {
			FileBotPanel panel = (FileBotPanel) model.getElementAt(i);
			panel.setVisible(false);
			pagePanel.add(panel, panel.getTitle());
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
	
	private final AbstractAction closeAction = new AbstractAction("Close") {
		
		public void actionPerformed(ActionEvent e) {
			setVisible(false);
			dispose();
			System.exit(0);
		}
	};
}
