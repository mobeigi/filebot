package net.sourceforge.filebot.ui;

import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.*;
import static javax.swing.ScrollPaneConstants.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import net.sf.ehcache.CacheManager;
import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.Settings;
import net.sourceforge.filebot.cli.GroovyPad;
import net.sourceforge.filebot.ui.analyze.AnalyzePanelBuilder;
import net.sourceforge.filebot.ui.episodelist.EpisodeListPanelBuilder;
import net.sourceforge.filebot.ui.list.ListPanelBuilder;
import net.sourceforge.filebot.ui.rename.RenamePanelBuilder;
import net.sourceforge.filebot.ui.sfv.SfvPanelBuilder;
import net.sourceforge.filebot.ui.subtitle.SubtitlePanelBuilder;
import net.sourceforge.tuned.PreferencesMap.PreferencesEntry;
import net.sourceforge.tuned.ui.DefaultFancyListCellRenderer;
import net.sourceforge.tuned.ui.ShadowBorder;
import net.sourceforge.tuned.ui.TunedUtilities;

public class MainFrame extends JFrame {

	private JList selectionList = new PanelSelectionList(createPanelBuilders());

	private HeaderPanel headerPanel = new HeaderPanel();

	private static final PreferencesEntry<String> persistentSelectedPanel = Settings.forPackage(MainFrame.class).entry("panel.selected").defaultValue("0");

	public MainFrame() {
		super(Settings.getApplicationName());

		// set taskbar / taskswitch icons
		List<Image> images = new ArrayList<Image>(3);
		for (String i : new String[] { "window.icon.large", "window.icon.medium", "window.icon.small" }) {
			images.add(ResourceManager.getImage(i));
		}
		setIconImages(images);

		try {
			// restore selected panel
			selectionList.setSelectedIndex(Integer.parseInt(persistentSelectedPanel.getValue()));
		} catch (NumberFormatException e) {
			// ignore
		}

		JScrollPane selectionListScrollPane = new JScrollPane(selectionList, VERTICAL_SCROLLBAR_NEVER, HORIZONTAL_SCROLLBAR_NEVER);
		selectionListScrollPane.setBorder(new CompoundBorder(new ShadowBorder(), selectionListScrollPane.getBorder()));
		selectionListScrollPane.setOpaque(false);

		headerPanel.getTitleLabel().setBorder(new EmptyBorder(8, 90, 10, 0));

		JComponent c = (JComponent) getContentPane();
		c.setLayout(new MigLayout("insets 0, fill, hidemode 3", "95px[fill]", "fill"));

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
		TunedUtilities.installAction(this.getRootPane(), getKeyStroke(VK_DELETE, CTRL_MASK | SHIFT_MASK), new AbstractAction("Clear Cache") {

			@Override
			public void actionPerformed(ActionEvent e) {
				CacheManager.getInstance().clearAll();
				UILogger.info("Cache has been cleared");
			}
		});

		TunedUtilities.installAction(this.getRootPane(), getKeyStroke(VK_F5, 0), new AbstractAction("Run") {

			@Override
			public void actionPerformed(ActionEvent evt) {
				MainFrame.this.setVisible(false);
				GroovyPad.main(new String[0]);
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
			Analytics.trackView(panel.getClass(), selectedBuilder.getName());
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
				dragEnterTimer = TunedUtilities.invokeLater(SELECTDELAY_ON_DRAG_OVER, new Runnable() {

					@Override
					public void run() {
						selectEnabled = true;

						// bring window to front when on dnd
						SwingUtilities.getWindowAncestor(((DropTarget) dtde.getSource()).getComponent()).toFront();
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
