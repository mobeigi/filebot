package net.filebot.mac;

import static javax.swing.BorderFactory.*;
import static net.filebot.UserFiles.*;
import static net.filebot.mac.MacAppUtilities.*;
import static net.filebot.ui.NotificationLogging.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.util.ui.SwingUI.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog.ModalExclusionType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;

import net.filebot.ResourceManager;
import net.filebot.media.MediaDetection;
import net.filebot.ui.HeaderPanel;
import net.filebot.ui.transfer.DefaultTransferHandler;
import net.filebot.ui.transfer.FileTransferable;
import net.filebot.ui.transfer.TransferablePolicy;
import net.miginfocom.swing.MigLayout;

public class DropToUnlock extends JList<File> {

	public static List<File> getParentFolders(Collection<File> files) {
		return files.stream().map(f -> f.isDirectory() ? f : f.getParentFile()).sorted().distinct().filter(f -> !f.exists() || isLockedFolder(f)).map(f -> {
			try {
				File file = f.getCanonicalFile();
				File root = MediaDetection.getStructureRoot(file);

				// if structure root doesn't work just grab first existing parent folder
				if (root == null || root.getParentFile() == null || root.getName().isEmpty()) {
					for (File it : listPathTail(file, Integer.MAX_VALUE, true)) {
						if (it.isDirectory()) {
							return it;
						}
					}
				}
				return root;
			} catch (Exception e) {
				return null;
			}
		}).filter(f -> f != null && isLockedFolder(f)).sorted().distinct().collect(Collectors.toList());
	}

	public static boolean showUnlockDialog(Window owner, Collection<File> files) {
		final List<File> model = getParentFolders(files);

		// TODO store secure bookmarks and auto-unlock folders if possible

		// check if we even need to unlock anything
		if (model.isEmpty() || model.stream().allMatch(f -> !isLockedFolder(f)))
			return true;

		final JDialog dialog = new JDialog(owner);
		final AtomicBoolean dialogCancelled = new AtomicBoolean(true);
		DropToUnlock d = new DropToUnlock(model) {

			@Override
			public void updateLockStatus(File... folders) {
				super.updateLockStatus(folders);

				// UI feedback for unlocked folders
				for (File it : folders) {
					if (!isLockedFolder(it)) {
						UILogger.log(Level.INFO, "Folder " + it.getName() + " has been unlocked");
					}
				}

				// if all folders have been unlocked auto-close dialog
				if (model.stream().allMatch(f -> !isLockedFolder(f))) {
					dialogCancelled.set(false);
					dialog.setVisible(false);
				}
			};
		};
		d.setBorder(createEmptyBorder(5, 15, 120, 15));

		JComponent c = (JComponent) dialog.getContentPane();
		c.setLayout(new MigLayout("insets 0, fill"));

		HeaderPanel h = new HeaderPanel();
		h.getTitleLabel().setText("Folder Permissions Required");
		h.getTitleLabel().setIcon(ResourceManager.getIcon("file.lock"));
		h.getTitleLabel().setBorder(createEmptyBorder(0, 0, 0, 64));
		c.add(h, "wmin 150px, hmin 75px, growx, dock north");
		c.add(d, "wmin 150px, hmin 150px, grow");

		dialog.setModal(true);
		dialog.setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);
		dialog.setSize(new Dimension(540, 420));
		dialog.setResizable(false);
		dialog.setLocationByPlatform(true);
		dialog.setAlwaysOnTop(true);

		// open required folders for easy drag and drop
		invokeLater(750, new Runnable() {

			@Override
			public void run() {
				model.stream().map(f -> f.getParentFile()).distinct().forEach(f -> {
					try {
						Desktop.getDesktop().open(f);
					} catch (Exception e) {
						Logger.getLogger(DropToUnlock.class.getName()).log(Level.WARNING, e.toString());
					}
				});
			}
		});

		// show and wait
		dialog.setVisible(true);

		return !dialogCancelled.get();
	}

	public DropToUnlock(Collection<File> model) {
		super(model.toArray(new File[0]));

		setLayoutOrientation(JList.HORIZONTAL_WRAP);
		setVisibleRowCount(-1);

		setCellRenderer(new FolderLockCellRenderer());

		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new FileChooserAction());

		setTransferHandler(new DefaultTransferHandler(new FolderDropPolicy(), null));
	}

	public void updateLockStatus(File... folder) {
		repaint();
	}

	private final RoundRectangle2D dropArea = new RoundRectangle2D.Double(0, 0, 0, 0, 20, 20);
	private final BasicStroke dashedStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[] { 5.0f }, 0.0f);

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// draw dashed bounding box
		int w = 300;
		int h = 70;
		int pad = 20;

		g2d.setColor(Color.lightGray);
		dropArea.setFrameFromCenter(getWidth() / 2, getHeight() - (h / 2) - pad - 10, (getWidth() - w) / 2, getHeight() - h - 2 * pad);
		g2d.setStroke(dashedStroke);
		g2d.draw(dropArea);

		// draw text
		g2d.setColor(Color.gray);
		g2d.setFont(g2d.getFont().deriveFont(Font.ITALIC, 36));
		g2d.drawString("Drop 'em", (int) dropArea.getMinX() + 15, (int) dropArea.getMinY() + 40);
		g2d.drawString("to Unlock 'em", (int) dropArea.getMinX() + 45, (int) dropArea.getMinY() + 40 + 35);
	}

	protected class FolderDropPolicy extends TransferablePolicy {

		@Override
		public boolean accept(Transferable tr) throws Exception {
			return true;
		}

		public void handleTransferable(Transferable tr, TransferAction action) throws Exception {
			List<File> files = FileTransferable.getFilesFromTransferable(tr);
			if (files != null) {
				List<File> folders = filter(files, FOLDERS);
				if (folders.size() > 0) {
					updateLockStatus(folders.toArray(new File[0]));
				}
			}
		}
	}

	protected static class FolderLockCellRenderer extends DefaultListCellRenderer {

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(100, 100);
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			File folder = (File) value;
			JLabel c = (JLabel) super.getListCellRendererComponent(list, folder.getName(), index, false, false);

			c.setIcon(ResourceManager.getIcon(isLockedFolder(folder) ? "folder.locked" : "folder.open"));
			c.setHorizontalTextPosition(JLabel.CENTER);
			c.setVerticalTextPosition(JLabel.BOTTOM);

			return c;
		}
	}

	protected static class FileChooserAction extends MouseAdapter {

		public void mouseClicked(MouseEvent evt) {
			DropToUnlock list = (DropToUnlock) evt.getSource();
			if (evt.getClickCount() > 0) {
				int index = list.locationToIndex(evt.getPoint());
				if (index >= 0 && list.getCellBounds(index, index).contains(evt.getPoint())) {
					File folder = list.getModel().getElementAt(index);
					if (isLockedFolder(folder)) {
						if (null != showOpenDialogSelectFolder(folder, "Grant Permission", getWindow(list))) {
							list.updateLockStatus(folder);
						}
					}
				}
			}
		}
	}

}
