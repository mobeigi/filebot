package net.filebot.ui.rename;

import static net.filebot.Logging.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import net.filebot.ResourceManager;
import net.filebot.mediainfo.ImageMetadata;
import net.filebot.similarity.Match;
import net.filebot.web.Datasource;
import net.filebot.web.SortOrder;

public class PhotoFileMatcher implements Datasource, AutoCompleteMatcher {

	public static final PhotoFileMatcher INSTANCE = new PhotoFileMatcher();

	@Override
	public String getIdentifier() {
		return "exif";
	}

	@Override
	public String getName() {
		return "Exif Metadata";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.exif");
	}

	@Override
	public List<Match<File, ?>> match(Collection<File> files, boolean strict, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();

		for (File f : filter(files, ImageMetadata.SUPPORTED_FILE_TYPES)) {
			try {
				ImageMetadata metadata = new ImageMetadata(f);
				if (metadata.getDateTaken().isPresent()) {
					matches.add(new Match<File, File>(f, f)); // photo mode is the same as generic file mode (but only select photo files)
				}
			} catch (Exception e) {
				debug.warning(format("%s [%s]", e, f));
			}
		}

		return matches;
	}

}
