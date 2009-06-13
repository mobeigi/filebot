
package net.sourceforge.filebot.ui.panel.subtitle;


import static net.sourceforge.filebot.FileBotUtilities.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import javax.swing.SwingWorker;
import javax.swing.event.SwingPropertyChangeSupport;

import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.tuned.FileUtilities;


public class SubtitlePackage {
	
	private final String name;
	
	private final Language language;
	
	private final ArchiveType archiveType;
	
	private Download download;
	

	public SubtitlePackage(SubtitleDescriptor descriptor) {
		name = descriptor.getName();
		language = new Language(languageCodeByName.get(descriptor.getLanguageName()), descriptor.getLanguageName());
		archiveType = ArchiveType.forName(descriptor.getArchiveType());
		download = new Download(descriptor.getDownloadFunction(), archiveType);
		
		// forward phase events
		download.addPropertyChangeListener(new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("phase")) {
					pcs.firePropertyChange("download.phase", evt.getOldValue(), evt.getNewValue());
				}
			}
		});
	}
	

	public String getName() {
		return name;
	}
	

	public Language getLanguage() {
		return language;
	}
	

	public ArchiveType getArchiveType() {
		return archiveType;
	}
	

	public Download getDownload() {
		return download;
	}
	

	public void reset() {
		Download old = download;
		download = new Download(old.function, old.archiveType);
		
		// transfer listeners
		for (PropertyChangeListener listener : old.getPropertyChangeSupport().getPropertyChangeListeners()) {
			old.removePropertyChangeListener(listener);
			download.addPropertyChangeListener(listener);
		}
		
		pcs.firePropertyChange("download.phase", old.getPhase(), download.getPhase());
	}
	

	@Override
	public String toString() {
		return name;
	}
	

	private final PropertyChangeSupport pcs = new SwingPropertyChangeSupport(this, true);
	

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}
	

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}
	

	public static class Download extends SwingWorker<Map<File, ByteBuffer>, Void> {
		
		enum Phase {
			PENDING,
			DOWNLOADING,
			EXTRACTING,
			DONE
		}
		

		private final Callable<ByteBuffer> function;
		private final ArchiveType archiveType;
		
		private Phase current = Phase.PENDING;
		

		private Download(Callable<ByteBuffer> function, ArchiveType archiveType) {
			this.function = function;
			this.archiveType = archiveType;
		}
		

		@Override
		protected Map<File, ByteBuffer> doInBackground() throws Exception {
			setPhase(Phase.DOWNLOADING);
			
			// fetch archive
			ByteBuffer data = function.call();
			
			setPhase(Phase.EXTRACTING);
			
			// extract contents of the archive
			Map<File, ByteBuffer> vfs = extract(archiveType, data);
			
			// if we can't extract files from a rar archive, it might actually be a zip file with the wrong extension
			if (vfs.isEmpty() && archiveType != ArchiveType.ZIP) {
				vfs = extract(ArchiveType.ZIP, data);
			}
			
			if (vfs.isEmpty()) {
				throw new IOException("Cannot extract files from archive");
			}
			
			// return file contents
			return vfs;
		}
		

		private Map<File, ByteBuffer> extract(ArchiveType archiveType, ByteBuffer data) throws IOException {
			Map<File, ByteBuffer> vfs = new LinkedHashMap<File, ByteBuffer>();
			
			for (Entry<File, ByteBuffer> entry : archiveType.fromData(data).extract().entrySet()) {
				String filename = entry.getKey().getName();
				
				if (SUBTITLE_FILES.accept(filename)) {
					// keep only subtitle files
					vfs.put(entry.getKey(), entry.getValue());
				} else if (ARCHIVE_FILES.accept(filename)) {
					// extract recursively if archive contains another archive
					vfs.putAll(extract(ArchiveType.forName(FileUtilities.getExtension(filename)), entry.getValue()));
				}
			}
			
			return vfs;
		}
		

		@Override
		protected void done() {
			setPhase(Phase.DONE);
		}
		

		private void setPhase(Phase phase) {
			Phase old = current;
			current = phase;
			
			firePropertyChange("phase", old, phase);
		}
		

		public Phase getPhase() {
			return current;
		}
	}
	

	/**
	 * Map english language name to language code.
	 */
	private static final Map<String, String> languageCodeByName = mapLanguageCodeByName();
	

	private static Map<String, String> mapLanguageCodeByName() {
		ResourceBundle bundle = ResourceBundle.getBundle(Language.class.getName(), Locale.ENGLISH);
		
		Map<String, String> map = new HashMap<String, String>();
		
		for (String code : bundle.keySet()) {
			map.put(bundle.getString(code), code);
		}
		
		return map;
	}
	
}
