
package net.sourceforge.filebot.ui.panel.subtitle;


import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.DownloadTask;


public class SubtitlePackage {
	
	private final SubtitleDescriptor subtitleDescriptor;
	
	private final ArchiveType archiveType;
	
	private final ImageIcon archiveIcon;
	
	private final Language language;
	
	private final DownloadTask downloadTask;
	
	
	public SubtitlePackage(SubtitleDescriptor subtitleDescriptor) {
		this.subtitleDescriptor = subtitleDescriptor;
		
		language = new Language(subtitleDescriptor.getLanguageName());
		downloadTask = subtitleDescriptor.createDownloadTask();
		
		archiveType = ArchiveType.forName(subtitleDescriptor.getArchiveType());
		archiveIcon = ResourceManager.getArchiveIcon(archiveType.getExtension());
	}
	

	public SubtitleDescriptor getSubtitleDescriptor() {
		return subtitleDescriptor;
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
	

	public DownloadTask getDownloadTask() {
		return downloadTask;
	}
	
}
