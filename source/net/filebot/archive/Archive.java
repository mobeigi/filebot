package net.filebot.archive;

import static net.filebot.util.StringUtilities.*;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.filebot.MediaTypes;
import net.filebot.Settings;
import net.filebot.util.FileUtilities.ExtensionFileFilter;
import net.filebot.vfs.FileInfo;

public class Archive implements Closeable {

	public static enum Extractor {
		SevenZipNativeBindings, SevenZipExecutable, ApacheVFS;

		public ArchiveExtractor newInstance(File archive) throws Exception {
			switch (this) {
			case SevenZipNativeBindings:
				return new SevenZipNativeBindings(archive);
			case SevenZipExecutable:
				return new SevenZipExecutable(archive);
			case ApacheVFS:
				return new ApacheVFS(archive);
			}
			return null;
		}
	}

	public static Archive open(File archive) throws Exception {
		return new Archive(Settings.getPreferredArchiveExtractor().newInstance(archive));
	}

	private final ArchiveExtractor extractor;

	public Archive(ArchiveExtractor extractor) throws Exception {
		this.extractor = extractor;
	}

	public List<FileInfo> listFiles() throws Exception {
		return extractor.listFiles();
	}

	public void extract(File outputDir) throws Exception {
		extractor.extract(outputDir);
	}

	public void extract(File outputDir, FileFilter filter) throws Exception {
		extractor.extract(outputDir, filter);
	}

	@Override
	public void close() throws IOException {
		if (extractor instanceof Closeable) {
			((Closeable) extractor).close();
		}
	}

	public static Set<String> getArchiveTypes() {
		Set<String> extensions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

		// application data
		extensions.addAll(MediaTypes.getDefault().getExtensionList("archive"));

		// formats provided by the library
		extensions.addAll(SevenZipNativeBindings.getArchiveTypes());

		return extensions;
	}

	private static final Pattern multiPartIndex = Pattern.compile("[.][0-9]{3}$");

	public static boolean hasMultiPartIndex(File file) {
		return multiPartIndex.matcher(file.getName()).find();
	}

	public static final FileFilter VOLUME_ONE_FILTER = new FileFilter() {

		private final Pattern volume = Pattern.compile("[.]r[0-9]+$|[.]part[0-9]+|[.][0-9]+$", Pattern.CASE_INSENSITIVE);
		private final FileFilter archives = new ExtensionFileFilter(getArchiveTypes());

		@Override
		public boolean accept(File path) {
			if (!archives.accept(path) && !hasMultiPartIndex(path)) {
				return false;
			}

			Matcher matcher = volume.matcher(path.getName());
			if (matcher.find()) {
				Integer i = matchInteger(matcher.group());
				if (i == null || i != 1) {
					return false;
				}
			}

			return true;
		}

	};

}
