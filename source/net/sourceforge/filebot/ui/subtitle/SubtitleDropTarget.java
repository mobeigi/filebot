package net.sourceforge.filebot.ui.subtitle;

import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.filebot.media.MediaDetection.*;
import static net.sourceforge.filebot.ui.NotificationLogging.*;
import static net.sourceforge.filebot.ui.transfer.FileTransferable.*;
import static net.sourceforge.tuned.FileUtilities.*;
import static net.sourceforge.tuned.ui.TunedUtilities.*;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.OpenSubtitlesClient;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.filebot.web.VideoHashSubtitleService;
import net.sourceforge.tuned.FileUtilities;
import net.sourceforge.tuned.FileUtilities.ParentFilter;

abstract class SubtitleDropTarget extends JButton {

	private enum DropAction {
		Accept, Cancel
	}

	public SubtitleDropTarget() {
		setHorizontalAlignment(CENTER);

		setHideActionText(true);
		setBorderPainted(false);
		setContentAreaFilled(false);
		setFocusPainted(false);

		setBackground(Color.white);
		setOpaque(false);

		// initialize with default mode
		setDropAction(DropAction.Accept);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		// install mouse listener
		addActionListener(clickHandler);

		// install drop target
		new DropTarget(this, dropHandler);
	}

	protected void setDropAction(DropAction dropAction) {
		setIcon(getIcon(dropAction));
	}

	protected abstract boolean handleDrop(List<File> files);

	protected abstract DropAction getDropAction(List<File> files);

	protected abstract Icon getIcon(DropAction dropAction);

	private final DropTargetAdapter dropHandler = new DropTargetAdapter() {

		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			DropAction dropAction = DropAction.Accept;

			try {
				dropAction = getDropAction(getFilesFromTransferable(dtde.getTransferable()));
			} catch (Exception e) {
				// just accept the drag if we can't access the transferable,
				// because on some implementations we can't access transferable data before we accept the drag,
				// but accepting or rejecting the drag depends on the files dragged
			}

			// update visual representation
			setDropAction(dropAction);

			// accept or reject
			if (dropAction != DropAction.Cancel) {
				dtde.acceptDrag(DnDConstants.ACTION_REFERENCE);
			} else {
				dtde.rejectDrag();
			}
		}

		@Override
		public void dragExit(DropTargetEvent dte) {
			// reset to default state
			setDropAction(DropAction.Accept);
		};

		@Override
		public void drop(DropTargetDropEvent dtde) {
			dtde.acceptDrop(DnDConstants.ACTION_REFERENCE);

			try {
				dtde.dropComplete(handleDrop(getFilesFromTransferable(dtde.getTransferable())));
			} catch (Exception e) {
				UILogger.log(Level.WARNING, e.getMessage(), e);
			}

			// reset to default state
			dragExit(dtde);
		}

	};

	private final ActionListener clickHandler = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent evt) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setMultiSelectionEnabled(true);

			// collect media file extensions (video and subtitle files)
			List<String> extensions = new ArrayList<String>();
			Collections.addAll(extensions, VIDEO_FILES.extensions());
			Collections.addAll(extensions, SUBTITLE_FILES.extensions());
			chooser.setFileFilter(new FileNameExtensionFilter("Media files", extensions.toArray(new String[0])));

			if (chooser.showOpenDialog(getWindow(evt.getSource())) == JFileChooser.APPROVE_OPTION) {
				List<File> files = Arrays.asList(chooser.getSelectedFiles());

				if (getDropAction(files) != DropAction.Cancel) {
					handleDrop(Arrays.asList(chooser.getSelectedFiles()));
				}
			}
		}
	};

	public static abstract class Download extends SubtitleDropTarget {

		public abstract VideoHashSubtitleService[] getVideoHashSubtitleServices();

		public abstract SubtitleProvider[] getSubtitleProviders();

		public abstract String getQueryLanguage();

		@Override
		protected DropAction getDropAction(List<File> input) {
			// accept video files and folders
			return filter(input, VIDEO_FILES, FOLDERS).size() > 0 ? DropAction.Accept : DropAction.Cancel;
		}

		@Override
		protected boolean handleDrop(List<File> input) {
			if (getQueryLanguage() == null) {
				UILogger.info("Please select a specific subtitle language.");
				return false;
			}

			// perform a drop action depending on the given files
			final Collection<File> videoFiles = new TreeSet<File>();

			// video files only
			videoFiles.addAll(filter(input, VIDEO_FILES));
			videoFiles.addAll(filter(listFiles(filter(input, FOLDERS), 5, false), VIDEO_FILES));

			if (videoFiles.size() > 0) {
				// invoke later so we don't block the DnD operation with the download dialog
				invokeLater(0, new Runnable() {

					@Override
					public void run() {
						handleDownload(videoFiles);
					}
				});
				return true;
			}

			return false;
		}

		protected boolean handleDownload(Collection<File> videoFiles) {
			SubtitleAutoMatchDialog dialog = new SubtitleAutoMatchDialog(getWindow(this));

			// initialize download parameters
			dialog.setVideoFiles(videoFiles.toArray(new File[0]));

			for (VideoHashSubtitleService service : getVideoHashSubtitleServices()) {
				dialog.addSubtitleService(service);
			}

			for (SubtitleProvider service : getSubtitleProviders()) {
				dialog.addSubtitleService(service);
			}

			// start looking for subtitles
			dialog.startQuery(getQueryLanguage());

			// initialize window properties
			dialog.setIconImage(getImage(getIcon(DropAction.Accept)));
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setSize(850, 575);

			// show dialog
			dialog.setLocation(getOffsetLocation(dialog.getOwner()));
			dialog.setVisible(true);

			return true;
		}

		protected Icon getIcon(DropAction dropAction) {
			switch (dropAction) {
			case Accept:
				return ResourceManager.getIcon("subtitle.exact.download");
			default:
				return ResourceManager.getIcon("message.error");
			}
		}

	}

	public static abstract class Upload extends SubtitleDropTarget {

		public abstract OpenSubtitlesClient getSubtitleService();

		@Override
		protected DropAction getDropAction(List<File> input) {
			// accept video files and folders
			return (filter(input, VIDEO_FILES).size() > 0 && filter(input, SUBTITLE_FILES).size() > 0) || filter(input, FOLDERS).size() > 0 ? DropAction.Accept : DropAction.Cancel;
		}

		@Override
		protected boolean handleDrop(List<File> input) {
			if (getSubtitleService().isAnonymous()) {
				UILogger.info("Please login. Anonymous user is not allowed to upload subtitles.");
				return false;
			}

			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

			// perform a drop action depending on the given files
			final Collection<File> files = new TreeSet<File>();

			// video files only
			files.addAll(filter(input, FILES));
			files.addAll(listFiles(filter(input, FOLDERS), 5, false));

			final List<File> videos = filter(files, VIDEO_FILES);
			final List<File> subtitles = filter(files, SUBTITLE_FILES);

			final Map<File, File> uploadPlan = new LinkedHashMap<File, File>();
			for (File subtitle : subtitles) {
				File video = getVideoForSubtitle(subtitle, filter(videos, new ParentFilter(subtitle.getParentFile())));
				uploadPlan.put(subtitle, video);
			}

			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

			if (uploadPlan.size() > 0) {
				// invoke later so we don't block the DnD operation with the download dialog
				invokeLater(0, new Runnable() {

					@Override
					public void run() {
						handleUpload(uploadPlan);
					}
				});
				return true;
			}
			return false;
		}

		protected void handleUpload(Map<File, File> uploadPlan) {
			SubtitleUploadDialog dialog = new SubtitleUploadDialog(getSubtitleService(), getWindow(this));

			// initialize window properties
			dialog.setIconImage(getImage(getIcon(DropAction.Accept)));
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setSize(820, 575);

			// show dialog
			dialog.setLocation(getOffsetLocation(dialog.getOwner()));

			// start processing
			dialog.setUploadPlan(uploadPlan);
			dialog.startChecking();

			// show dialog
			dialog.setVisible(true);
		}

		protected File getVideoForSubtitle(File subtitle, List<File> videos) {
			String baseName = stripReleaseInfo(FileUtilities.getName(subtitle)).toLowerCase();

			// find corresponding movie file
			for (File it : videos) {
				if (!baseName.isEmpty() && stripReleaseInfo(FileUtilities.getName(it)).toLowerCase().startsWith(baseName)) {
					return it;
				}
			}

			return null;
		}

		protected Icon getIcon(DropAction dropAction) {
			if (dropAction == DropAction.Accept)
				return ResourceManager.getIcon("subtitle.exact.upload");

			return ResourceManager.getIcon("message.error");
		}
	}

}
