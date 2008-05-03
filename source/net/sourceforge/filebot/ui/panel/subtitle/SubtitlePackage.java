
package net.sourceforge.filebot.ui.panel.subtitle;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.Icon;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.DownloadTask;


public class SubtitlePackage {
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	private DownloadTask downloadTask = null;
	
	private final SubtitleDescriptor subtitleDescriptor;
	
	
	public SubtitlePackage(SubtitleDescriptor subtitleDescriptor) {
		this.subtitleDescriptor = subtitleDescriptor;
	}
	

	public String getName() {
		return subtitleDescriptor.getName();
	}
	

	public ArchiveType getArchiveType() {
		return ArchiveType.forName(subtitleDescriptor.getArchiveType());
	}
	

	public Icon getArchiveIcon() {
		return ResourceManager.getArchiveIcon(getArchiveType().toString());
	}
	

	public String getLanguageName() {
		return subtitleDescriptor.getLanguageName();
	}
	

	public Icon getLanguageIcon() {
		return ResourceManager.getFlagIcon(LanguageResolver.getDefault().getLanguageCode(getLanguageName()));
	}
	

	public synchronized void startDownload() {
		if (downloadTask != null)
			throw new IllegalStateException("Download has been started already");
		
		downloadTask = subtitleDescriptor.createDownloadTask();
		downloadTask.addPropertyChangeListener(new DownloadTaskPropertyChangeRelay());
		
		downloadTask.execute();
	}
	

	public DownloadTask getDownloadTask() {
		if (downloadTask == null)
			throw new IllegalStateException("Download has not been started");
		
		return downloadTask;
	}
	

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}
	
	
	private class DownloadTaskPropertyChangeRelay implements PropertyChangeListener {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			propertyChangeSupport.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
		}
		
	};
	
}
