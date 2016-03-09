package net.filebot.ui;

import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.*;
import static javax.swing.ScrollPaneConstants.*;
import static net.filebot.Logging.*;
import static net.filebot.Settings.*;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog.ModalExclusionType;
import java.awt.FlowLayout;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.filebot.CacheManager;
import net.filebot.Settings;
import net.filebot.cli.GroovyPad;
import net.filebot.mac.MacAppUtilities;
import net.filebot.ui.analyze.AnalyzePanelBuilder;
import net.filebot.ui.episodelist.EpisodeListPanelBuilder;
import net.filebot.ui.list.ListPanelBuilder;
import net.filebot.ui.rename.RenamePanelBuilder;
import net.filebot.ui.sfv.SfvPanelBuilder;
import net.filebot.ui.subtitle.SubtitlePanelBuilder;
import net.filebot.util.PreferencesMap.PreferencesEntry;
import net.filebot.util.ui.DefaultFancyListCellRenderer;
import net.filebot.util.ui.ShadowBorder;
import net.filebot.util.ui.SwingUI;
import net.miginfocom.swing.MigLayout;

public class MainFrame extends JFrame {

	private JList selectionList;
	private HeaderPanel headerPanel;

	private static final PreferencesEntry<String> persistentSelectedPanel = Settings.forPackage(MainFrame.class).entry("panel.selected").defaultValue("0");

	public MainFrame(PanelBuilder[] panels) {
		super(isInstalled() ? getApplicationName() : String.format("%s %s", getApplicationName(), getApplicationVersion()));

		selectionList = new PanelSelectionList(panels);
		headerPanel = new HeaderPanel();

		try {
			// restore selected panel
			selectionList.setSelectedIndex(Integer.parseInt(persistentSelectedPanel.getValue()));
		} catch (NumberFormatException e) {
			// ignore
		}

		JScrollPane selectionListScrollPane = new JScrollPane(selectionList, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_NEVER);
		selectionListScrollPane.setOpaque(false);

		if (isMacApp()) {
			selectionListScrollPane.setBorder(new CompoundBorder(new ShadowBorder(), new LineBorder(new Color(0x809DB8), 1, false)));
		} else {
			selectionListScrollPane.setBorder(new CompoundBorder(new ShadowBorder(), selectionListScrollPane.getBorder()));
		}

		headerPanel.getTitleLabel().setBorder(new EmptyBorder(8, 90, 10, 0));

		JComponent c = (JComponent) getContentPane();
		c.setLayout(new MigLayout("insets 0, fill, hidemode 3", String.format("%dpx[fill]", isUbuntuApp() ? 105 : 95), "fill"));

		c.add(selectionListScrollPane, "pos 6px 10px n 100%-12px");
		c.add(headerPanel, "growx, dock north");

		// show initial panel
		showPanel((PanelBuilder) selectionList.getSelectedValue());

		selectionList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				showPanel((PanelBuilder) selectionList.getSelectedValue());

				if (!e.getValueIsAdjusting()) {
					persistentSelectedPanel.setValue(Integer.toString(selectionList.getSelectedIndex()));
				}
			}
		});

		setSize(980, 630);

		// KEYBOARD SHORTCUTS
		SwingUI.installAction(this.getRootPane(), getKeyStroke(VK_DELETE, CTRL_MASK | SHIFT_MASK), new AbstractAction("Clear Cache") {

			@Override
			public void actionPerformed(ActionEvent e) {
				CacheManager.getInstance().clearAll();
				log.info("Cache has been cleared");
			}
		});

		SwingUI.installAction(this.getRootPane(), getKeyStroke(VK_F5, 0), new AbstractAction("Run") {

			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
					setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)); // loading Groovy might take a while
					GroovyPad pad = new GroovyPad();

					pad.addWindowListener(new WindowAdapter() {
						@Override
						public void windowOpened(WindowEvent e) {
							MainFrame.this.setVisible(false);
						};

						@Override
						public void windowClosing(WindowEvent e) {
							MainFrame.this.setVisible(true);
						};
					});

					pad.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
					pad.setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
					pad.setLocationByPlatform(true);
					pad.setVisible(true);
				} catch (IOException e) {
					Logger.getLogger(GroovyPad.class.getName()).log(Level.WARNING, e.getMessage(), e);
				} finally {
					setCursor(Cursor.getDefaultCursor());
				}
			}
		});

		SwingUI.installAction(this.getRootPane(), getKeyStroke(VK_F1, 0), new AbstractAction("Help") {

			@Override
			public void actionPerformed(ActionEvent evt) {
				GettingStartedStage.start();
			}
		});

	}

	public static PanelBuilder[] createPanelBuilders() {
		return new PanelBuilder[] { new RenamePanelBuilder(), new EpisodeListPanelBuilder(), new SubtitlePanelBuilder(), new SfvPanelBuilder(), new AnalyzePanelBuilder(), new ListPanelBuilder() };
	}

	protected void showPanel(PanelBuilder selectedBuilder) {
		final JComponent contentPane = (JComponent) getContentPane();

		JComponent panel = null;

		for (int i = 0; i < contentPane.getComponentCount(); i++) {
			JComponent c = (JComponent) contentPane.getComponent(i);
			PanelBuilder builder = (PanelBuilder) c.getClientProperty("panelBuilder");

			if (builder != null) {
				if (builder.equals(selectedBuilder)) {
					panel = c;
				} else {
					c.setVisible(false);
				}
			}
		}

		if (panel == null) {
			panel = selectedBuilder.create();
			panel.setVisible(false); // invisible by default
			panel.putClientProperty("panelBuilder", selectedBuilder);

			contentPane.add(panel);
		}

		// make visible, ignore action is visible already
		if (!panel.isVisible()) {
			headerPanel.setTitle(selectedBuilder.getName());
			panel.setVisible(true);
		}
	}

	private static class PanelSelectionList extends JList {

		private static final int SELECTDELAY_ON_DRAG_OVER = 300;

		public PanelSelectionList(PanelBuilder[] builders) {
			super(builders);

			setCellRenderer(new PanelCellRenderer());
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			setBorder(new EmptyBorder(4, 5, 4, 5));

			// initialize "drag over" panel selection
			new DropTarget(this, new DragDropListener());
		}

		private class DragDropListener extends DropTargetAdapter {

			private boolean selectEnabled = false;

			private Timer dragEnterTimer;

			@Override
			public void dragOver(DropTargetDragEvent dtde) {
				if (selectEnabled) {
					int index = locationToIndex(dtde.getLocation());
					setSelectedIndex(index);
				}
			}

			@Override
			public void dragEnter(final DropTargetDragEvent dtde) {
				dragEnterTimer = SwingUI.invokeLater(SELECTDELAY_ON_DRAG_OVER, new Runnable() {

					@Override
					public void run() {
						selectEnabled = true;

						// bring window to front when on dnd
						if (isMacApp()) {
							MacAppUtilities.requestForeground();
						} else {
							SwingUtilities.getWindowAncestor(((DropTarget) dtde.getSource()).getComponent()).toFront();
						}
					}
				});
			}

			@Override
			public void dragExit(DropTargetEvent dte) {
				selectEnabled = false;

				if (dragEnterTimer != null) {
					dragEnterTimer.stop();
				}
			}

			@Override
			public void drop(DropTargetDropEvent dtde) {

			}

		}

	}

	private static class PanelCellRenderer extends DefaultFancyListCellRenderer {

		public PanelCellRenderer() {
			super(10, 0, new Color(0x163264));

			// center labels in list
			setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

			setHighlightingEnabled(false);

			setVerticalTextPosition(SwingConstants.BOTTOM);
			setHorizontalTextPosition(SwingConstants.CENTER);
		}

		@Override
		public void configureListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.configureListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			PanelBuilder panel = (PanelBuilder) value;
			setText(panel.getName());
			setIcon(panel.getIcon());
		}

	}

}
