
package net.sourceforge.filebot.ui.panel.subtitle;


import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.swing.SwingWorker;

import net.sourceforge.filebot.web.SubtitleDescriptor;


public class SubtitlePackage {
	
	private final SubtitleDescriptor subtitleDescriptor;
	
	private final Language language;
	
	private final SwingWorker<ByteBuffer, ?> download;
	

	public SubtitlePackage(SubtitleDescriptor subtitleDescriptor) {
		this.subtitleDescriptor = subtitleDescriptor;
		
		this.language = new Language(languageCodeByName.get(subtitleDescriptor.getLanguageName()), subtitleDescriptor.getLanguageName());
		this.download = subtitleDescriptor.createDownloadTask();
	}
	

	public String getName() {
		return subtitleDescriptor.getName();
	}
	

	public Language getLanguage() {
		return language;
	}
	

	public ArchiveType getArchiveType() {
		return ArchiveType.forName(subtitleDescriptor.getArchiveType());
	}
	

	public SwingWorker<ByteBuffer, ?> getDownload() {
		return download;
	}
	

	@Override
	public String toString() {
		return subtitleDescriptor.getName();
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
