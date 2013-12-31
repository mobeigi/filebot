package net.sourceforge.filebot.ui.subtitle;

import static java.util.Collections.*;
import static net.sourceforge.filebot.MediaTypes.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.SwingWorker;
import javax.swing.event.SwingPropertyChangeSupport;

import net.sourceforge.filebot.Language;
import net.sourceforge.filebot.vfs.ArchiveType;
import net.sourceforge.filebot.vfs.MemoryFile;
import net.sourceforge.filebot.web.SubtitleDescriptor;
import net.sourceforge.filebot.web.SubtitleProvider;
import net.sourceforge.tuned.FileUtilities;

public class SubtitlePackage {

	private final SubtitleProvider provider;
	private final SubtitleDescriptor subtitle;
	private final Language language;
	private Download download;

	public SubtitlePackage(SubtitleProvider provider, SubtitleDescriptor subtitle) {
		this.provider = provider;
		this.subtitle = subtitle;

		// resolve language name
		this.language = new Language(languageCodeByName.get(subtitle.getLanguageName()), Language.getISO3LanguageCodeByName(subtitle.getLanguageName()), subtitle.getLanguageName());

		// initialize download worker
		download = new Download(subtitle);

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

	public SubtitleProvider getProvider() {
		return provider;
	}

	public String getName() {
		return subtitle.getName();
	}

	public Language getLanguage() {
		return language;
	}

	public String getType() {
		return subtitle.getType();
	}

	public Download getDownload() {
		return download;
	}

	public void reset() {
		// cancel old download
		download.cancel(false);

		// create new download
		Download old = download;
		download = new Download(subtitle);

		// transfer listeners
		for (PropertyChangeListener listener : old.getPropertyChangeSupport().getPropertyChangeListeners()) {
			old.removePropertyChangeListener(listener);
			download.addPropertyChangeListener(listener);
		}

		pcs.firePropertyChange("download.phase", old.getPhase(), download.getPhase());
	}

	@Override
	public String toString() {
		return subtitle.getName();
	}

	private final PropertyChangeSupport pcs = new SwingPropertyChangeSupport(this, true);

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public static class Download extends SwingWorker<List<MemoryFile>, Void> {

		enum Phase {
			PENDING, WAITING, DOWNLOADING, EXTRACTING, DONE
		}

		private final SubtitleDescriptor subtitle;

		private Phase current = Phase.PENDING;

		private Download(SubtitleDescriptor descriptor) {
			this.subtitle = descriptor;
		}

		public void start() {
			setPhase(Phase.WAITING);

			// enqueue worker
			execute();
		}

		@Override
		protected List<MemoryFile> doInBackground() throws Exception {
			setPhase(Phase.DOWNLOADING);

			// fetch archive
			ByteBuffer data = subtitle.fetch();

			// abort if download has been cancelled
			if (isCancelled())
				return null;

			setPhase(Phase.EXTRACTING);

			ArchiveType archiveType = ArchiveType.forName(subtitle.getType());

			if (archiveType == ArchiveType.UNKOWN) {
				// cannot extract files from archive
				return singletonList(new MemoryFile(subtitle.getPath(), data));
			}

			// extract contents of the archive
			List<MemoryFile> vfs = extract(archiveType, data);

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

		private List<MemoryFile> extract(ArchiveType archiveType, ByteBuffer data) throws IOException {
			List<MemoryFile> vfs = new ArrayList<MemoryFile>();

			for (MemoryFile file : archiveType.fromData(data)) {
				if (SUBTITLE_FILES.accept(file.getName())) {
					// add subtitle files, ignore non-subtitle files
					vfs.add(file);
				} else {
					// check if file is a supported archive
					ArchiveType type = ArchiveType.forName(FileUtilities.getExtension(file.getName()));

					if (type != ArchiveType.UNKOWN) {
						// extract nested archives recursively
						vfs.addAll(extract(type, file.getData()));
					}
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

		public boolean isStarted() {
			return current != Phase.PENDING;
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
