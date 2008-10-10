
package net.sourceforge.filebot.ui.panel.subtitle;


import java.beans.PropertyChangeEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker.StateValue;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.DownloadTask;
import net.sourceforge.tuned.ui.SwingWorkerPropertyChangeAdapter;

import org.jdesktop.beans.AbstractBean;


public class SubtitlePackage extends AbstractBean {
	
	private final SubtitleDescriptor subtitleDescriptor;
	
	private final ArchiveType archiveType;
	
	private final ImageIcon archiveIcon;
	
	private final Language language;
	
	private DownloadTask downloadTask;
	
	private StateValue downloadState = StateValue.PENDING;
	
	private float downloadProgress = 0;
	
	
	public SubtitlePackage(SubtitleDescriptor subtitleDescriptor) {
		this.subtitleDescriptor = subtitleDescriptor;
		
		this.language = new Language(subtitleDescriptor.getLanguageName());
		
		this.archiveType = ArchiveType.forName(subtitleDescriptor.getArchiveType());
		this.archiveIcon = ResourceManager.getArchiveIcon(archiveType.getExtension());
	}
	

	public String getName() {
		return subtitleDescriptor.getName();
	}
	

	public Language getLanguage() {
		return language;
	}
	

	public ArchiveType getArchiveType() {
		return archiveType;
	}
	

	public Icon getArchiveIcon() {
		return archiveIcon;
	}
	

	@Override
	public String toString() {
		return getName();
	}
	

	public synchronized void startDownload() {
		if (downloadTask != null)
			throw new IllegalStateException("Download has already been started");
		
		downloadTask = subtitleDescriptor.createDownloadTask();
		downloadTask.addPropertyChangeListener(new DownloadTaskPropertyChangeAdapter());
		
		downloadTask.execute();
	}
	

	public StateValue getDownloadState() {
		return downloadState;
	}
	

	private void setDownloadState(StateValue downloadState) {
		this.downloadState = downloadState;
		firePropertyChange("downloadState", null, downloadState);
	}
	

	public float getDownloadProgress() {
		return downloadProgress;
	}
	

	private void setDownloadProgress(float downloadProgress) {
		this.downloadProgress = downloadProgress;
		firePropertyChange("downloadProgress", null, downloadProgress);
	}
	
	
	private class DownloadTaskPropertyChangeAdapter extends SwingWorkerPropertyChangeAdapter {
		
		@Override
		public void started(PropertyChangeEvent evt) {
			setDownloadState(StateValue.STARTED);
		}
		

		@Override
		public void done(PropertyChangeEvent evt) {
			setDownloadState(StateValue.DONE);
		}
		

		@Override
		public void progress(PropertyChangeEvent evt) {
			setDownloadProgress((Float) evt.getNewValue() / 100);
		}
	};
	
}
