
package net.sourceforge.filebot.ui.panel.subtitle;


import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.DownloadTask;


public class SubtitlePackage {
	
	private final SubtitleDescriptor subtitleDescriptor;
	
	private final ArchiveType archiveType;
	
	private final Icon archiveIcon;
	
	private final DownloadTask downloadTask;
	
	
	public SubtitlePackage(SubtitleDescriptor subtitleDescriptor) {
		this.subtitleDescriptor = subtitleDescriptor;
		
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
	

	public String getLanguageName() {
		return subtitleDescriptor.getLanguageName();
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
