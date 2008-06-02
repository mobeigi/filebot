
package net.sourceforge.filebot.ui.panel.subtitle;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.ImageIcon;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.DownloadTask;


public class SubtitlePackage {
	
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	
	private final SubtitleDescriptor subtitleDescriptor;
	
	private final ImageIcon archiveIcon;
	
	private final ImageIcon languageIcon;
	
	private DownloadTask downloadTask;
	
	
	public SubtitlePackage(SubtitleDescriptor subtitleDescriptor) {
		this.subtitleDescriptor = subtitleDescriptor;
		
		archiveIcon = ResourceManager.getArchiveIcon(subtitleDescriptor.getArchiveType());
		languageIcon = ResourceManager.getFlagIcon(LanguageResolver.getDefault().getLanguageCode(subtitleDescriptor.getLanguageName()));
	}
	

	public String getName() {
		return subtitleDescriptor.getName();
	}
	

	public ArchiveType getArchiveType() {
		return ArchiveType.forName(subtitleDescriptor.getArchiveType());
	}
	

	public ImageIcon getArchiveIcon() {
		return archiveIcon;
	}
	

	public String getLanguageName() {
		return subtitleDescriptor.getLanguageName();
	}
	

	public ImageIcon getLanguageIcon() {
		return languageIcon;
	}
	

	public synchronized void startDownload() {
		if (downloadTask != null)
			throw new IllegalStateException("Download has been started already");
		
		downloadTask = subtitleDescriptor.createDownloadTask();
		downloadTask.addPropertyChangeListener(new DownloadTaskPropertyChangeAdapter());
		
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
	
	
	private class DownloadTaskPropertyChangeAdapter implements PropertyChangeListener {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			propertyChangeSupport.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
		}
		
	};
	
}
