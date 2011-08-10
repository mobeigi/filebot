
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.MediaTypes.*;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.VideoHashSubtitleService;


abstract class SubtitleDropTarget extends JButton {
	
	private enum DropAction {
		Download,
		Upload,
		Cancel
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
		setDropAction(DropAction.Download);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		
		// install mouse listener
		addActionListener(clickHandler);
		
		// install drop target
		new DropTarget(this, dropHandler);
	}
	

	private void setDropAction(DropAction dropAction) {
		setIcon(getIcon(dropAction));
	}
	

	private Icon getIcon(DropAction dropAction) {
		switch (dropAction) {
			case Download:
				return ResourceManager.getIcon("subtitle.exact.download");
			case Upload:
				return ResourceManager.getIcon("subtitle.exact.upload");
			default:
				return ResourceManager.getIcon("message.error");
		}
	}
	

	public abstract VideoHashSubtitleService[] getServices();
	

	public abstract String getQueryLanguage();
	

	private boolean handleDownload(List<File> videoFiles) {
		VideoHashSubtitleDownloadDialog dialog = new VideoHashSubtitleDownloadDialog(getWindow(this));
		
		// initialize download parameters
		dialog.setVideoFiles(videoFiles.toArray(new File[0]));
		
		for (VideoHashSubtitleService service : getServices()) {
			dialog.addSubtitleService(service);
		}
		
		// start looking for subtitles
		dialog.startQuery(getQueryLanguage());
		
		// initialize window properties
		dialog.setIconImage(getImage(getIcon(DropAction.Download)));
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.pack();
		
		// show dialog
		dialog.setLocation(getOffsetLocation(dialog.getOwner()));
		dialog.setVisible(true);
		
		// now it's up to the user
		return true;
	}
	

	private boolean handleUpload(Map<File, File> videosMappedBySubtitle) {
		// TODO implement upload
		throw new UnsupportedOperationException("Not implemented yet");
	}
	

	private boolean handleDrop(List<File> files) {
		// perform a drop action depending on the given files
		if (containsOnly(files, VIDEO_FILES)) {
			return handleDownload(files);
		}
		
		if (containsOnly(files, FOLDERS)) {
			// collect all video files from the dropped folders 
			List<File> videoFiles = filter(listFiles(files, 0), VIDEO_FILES);
			
			if (videoFiles.size() > 0) {
				return handleDownload(videoFiles);
			}
		}
		
		if (containsOnly(files, SUBTITLE_FILES)) {
			// TODO implement upload
			throw new UnsupportedOperationException("Not implemented yet");
		}
		
		if (containsOnlyVideoSubtitleMatches(files)) {
			// TODO implement upload
			throw new UnsupportedOperationException("Not implemented yet");
		}
		
		return false;
	}
	

	private boolean containsOnlyVideoSubtitleMatches(List<File> files) {
		List<File> subtitles = filter(files, SUBTITLE_FILES);
		
		if (subtitles.isEmpty())
			return false;
		
		// number of subtitle files must match the number of video files
		return subtitles.size() == filter(files, VIDEO_FILES).size();
	}
	

	private DropAction getDropAction(List<File> files) {
		// video files only, or any folder, containing video files
		if (containsOnly(files, VIDEO_FILES) || (containsOnly(files, FOLDERS) && filter(listFiles(files, 0), VIDEO_FILES).size() > 0)) {
			return DropAction.Download;
		}
		
		// subtitle files only, or video/subtitle matches
		if (containsOnly(files, SUBTITLE_FILES) || containsOnlyVideoSubtitleMatches(files)) {
			return DropAction.Upload;
		}
		
		// unknown input		
		return DropAction.Cancel;
	}
	

	private final DropTargetAdapter dropHandler = new DropTargetAdapter() {
		
		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			DropAction dropAction = DropAction.Download;
			
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
		

		public void dragExit(DropTargetEvent dte) {
			// reset to default state
			setDropAction(DropAction.Download);
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
	
}
