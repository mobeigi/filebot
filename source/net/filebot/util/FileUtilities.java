package net.filebot.util;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.util.RegularExpressions.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public final class FileUtilities {

	public static File moveRename(File source, File destination) throws IOException {
		// resolve destination
		destination = resolveDestination(source, destination);

		if (source.isDirectory()) {
			// move folder
			FileUtils.moveDirectory(source, destination);
			return destination;
		}

		// on Windows, use ATOMIC_MOVE which allows us to rename files even if only lower/upper-case changes (without ATOMIC_MOVE the operation would be ignored)
		// but ATOMIC_MOVE can only work for files on the same drive, if that is not the case there is no point trying move with ATOMIC_MOVE
		if (File.separator.equals("\\") && source.equals(destination)) {
			try {
				return Files.move(source.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE).toFile();
			} catch (AtomicMoveNotSupportedException e) {
				debug.warning(e.getMessage());
			}
		}

		// Linux and Mac OS X
		return Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING).toFile();
	}

	public static File copyAs(File source, File destination) throws IOException {
		// resolve destination
		destination = resolveDestination(source, destination);

		if (source.isDirectory()) {
			// copy folder
			FileUtils.copyDirectory(source, destination);
			return destination;
		}

		// copy file
		return Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING).toFile();
	}

	public static File resolve(File source, File destination) {
		// resolve destination
		if (!destination.isAbsolute()) {
			// same folder, different name
			destination = new File(source.getParentFile(), destination.getPath());
		}
		return destination;
	}

	public static File resolveDestination(File source, File destination) throws IOException {
		// resolve destination
		destination = resolve(source, destination);

		// create parent folder if necessary and make sure that the folder structure is created, and throw exception if the folder structure can't be created
		Files.createDirectories(destination.getParentFile().toPath());

		return destination;
	}

	public static File createRelativeSymlink(File link, File target, boolean relativize) throws IOException {
		if (relativize) {
			// make sure we're working with the full path
			link = link.getCanonicalFile();
			target = target.getCanonicalFile();

			try {
				target = link.getParentFile().toPath().relativize(target.toPath()).toFile();
			} catch (Throwable e) {
				// unable to relativize link target
			}
		}

		// create symlink via NIO.2
		return Files.createSymbolicLink(link.toPath(), target.toPath()).toFile();
	}

	public static File createHardLinkStructure(File link, File target) throws IOException {
		if (target.isFile()) {
			return Files.createLink(link.toPath(), target.toPath()).toFile();
		}

		// if the target is a directory, recreate the structure and hardlink each file item
		final Path source = target.getCanonicalFile().toPath();
		final Path destination = link.getCanonicalFile().toPath();

		Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), FILE_WALK_MAX_DEPTH, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path linkFile = destination.resolve(source.relativize(file));
				Files.createDirectories(linkFile.getParent());
				Files.createLink(linkFile, file);
				return FileVisitResult.CONTINUE;
			}
		});

		return destination.toFile();
	}

	public static boolean delete(File file) {
		// delete files or files
		return FileUtils.deleteQuietly(file);
	}

	public static File createFolders(File folder) throws IOException {
		return Files.createDirectories(folder.toPath()).toFile();
	}

	public static byte[] readFile(File source) throws IOException {
		return Files.readAllBytes(source.toPath());
	}

	public static Stream<String> streamLines(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new UnicodeReader(new BufferedInputStream(new FileInputStream(file)), false, UTF_8), BUFFER_SIZE);
		return reader.lines().onClose(() -> {
			try {
				reader.close();
			} catch (Exception e) {
				debug.log(Level.SEVERE, "Failed to close file: " + file, e);
			}
		});
	}

	public static String readTextFile(File file) throws IOException {
		return streamLines(file).collect(joining(System.lineSeparator()));
	}

	public static File writeFile(ByteBuffer data, File destination) throws IOException {
		try (FileChannel channel = FileChannel.open(destination.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			channel.write(data);
		}
		return destination;
	}

	public static Reader createTextReader(File file) throws IOException {
		CharsetDetector detector = new CharsetDetector();
		detector.setDeclaredEncoding("UTF-8"); // small boost for UTF-8 as default encoding
		detector.setText(new BufferedInputStream(new FileInputStream(file)));

		CharsetMatch charset = detector.detect();
		if (charset != null)
			return charset.getReader();

		// assume UTF-8 by default
		return new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
	}

	public static String getText(ByteBuffer data) throws IOException {
		CharsetDetector detector = new CharsetDetector();
		detector.setDeclaredEncoding("UTF-8"); // small boost for UTF-8 as default encoding
		detector.setText(new ByteBufferInputStream(data));

		CharsetMatch charset = detector.detect();
		if (charset != null) {
			try {
				return charset.getString();
			} catch (RuntimeException e) {
				throw new IOException("Failed to read text", e);
			}
		}

		// assume UTF-8 by default
		return UTF_8.decode(data).toString();
	}

	/**
	 * Pattern used for matching file extensions.
	 *
	 * e.g. "file.txt" -> match "txt", ".hidden" -> no match
	 */
	public static final Pattern EXTENSION = Pattern.compile("(?<=.[.])\\p{Alnum}+$");
	public static final String UNC_PREFIX = "\\\\";

	public static String getExtension(File file) {
		if (file.isDirectory())
			return null;

		return getExtension(file.getName());
	}

	public static String getExtension(String name) {
		Matcher matcher = EXTENSION.matcher(name);

		if (matcher.find()) {
			// extension without leading '.'
			return matcher.group();
		}

		// no extension
		return null;
	}

	public static boolean hasExtension(File file, String... extensions) {
		// avoid native call for speed, if possible
		return hasExtension(file.getName(), extensions) && !file.isDirectory();
	}

	public static boolean hasExtension(String filename, String... extensions) {
		for (String it : extensions) {
			if (filename.length() - it.length() >= 2 && filename.charAt(filename.length() - it.length() - 1) == '.') {
				String tail = filename.substring(filename.length() - it.length(), filename.length());
				if (tail.equalsIgnoreCase(it)) {
					return true;
				}
			}
		}

		return false;
	}

	public static String getNameWithoutExtension(String name) {
		Matcher matcher = EXTENSION.matcher(name);

		if (matcher.find()) {
			return name.substring(0, matcher.start() - 1);
		}

		// no extension, return given name
		return name;
	}

	public static String getName(File file) {
		if (file == null)
			return null;
		if (file.getName().isEmpty() || UNC_PREFIX.equals(file.getParent()))
			return getFolderName(file);

		return getNameWithoutExtension(file.getName());
	}

	public static String getFolderName(File file) {
		if (UNC_PREFIX.equals(file.getParent()))
			return file.toString();

		if (file.getName().length() > 0)
			return file.getName();

		// file might be a drive (only has a path, but no name)
		return replacePathSeparators(file.toString(), "");
	}

	public static boolean isDerived(File derivate, File prime) {
		return isDerived(getName(derivate), prime);
	}

	public static boolean isDerived(String derivate, File prime) {
		String base = getName(prime).trim().toLowerCase();
		derivate = derivate.trim().toLowerCase();
		return derivate.startsWith(base);
	}

	public static boolean isDerivedByExtension(File derivate, File prime) {
		return isDerivedByExtension(getName(derivate), prime);
	}

	public static boolean isDerivedByExtension(String derivate, File prime) {
		String base = getName(prime).trim().toLowerCase();
		derivate = derivate.trim().toLowerCase();

		if (derivate.equals(base))
			return true;

		while (derivate.length() > base.length() && getExtension(derivate) != null) {
			derivate = getNameWithoutExtension(derivate);

			if (derivate.equals(base))
				return true;
		}

		return false;
	}

	public static boolean containsOnly(Collection<File> files, FileFilter filter) {
		if (files.isEmpty()) {
			return false;
		}
		for (File file : files) {
			if (!filter.accept(file))
				return false;
		}
		return true;
	}

	public static List<File> sortByUniquePath(Collection<File> files) {
		// sort by unique lower-case paths
		TreeSet<File> sortedSet = new TreeSet<File>(CASE_INSENSITIVE_PATH);
		sortedSet.addAll(files);

		return new ArrayList<File>(sortedSet);
	}

	public static List<File> filter(Iterable<File> files, FileFilter... filters) {
		List<File> accepted = new ArrayList<File>();

		for (File file : files) {
			for (FileFilter filter : filters) {
				if (filter.accept(file)) {
					accepted.add(file);
					break;
				}
			}
		}

		return accepted;
	}

	public static FileFilter not(FileFilter filter) {
		return new NotFileFilter(filter);
	}

	public static List<File> listPath(File file) {
		return listPathTail(file, Integer.MAX_VALUE, false);
	}

	public static List<File> listPathTail(File file, int tailSize, boolean reverse) {
		LinkedList<File> nodes = new LinkedList<File>();

		File node = file;
		for (int i = 0; node != null && i < tailSize && !UNC_PREFIX.equals(node.toString()); i++, node = node.getParentFile()) {
			if (reverse) {
				nodes.addLast(node);
			} else {
				nodes.addFirst(node);
			}
		}

		return nodes;
	}

	public static File getRelativePathTail(File file, int tailSize) {
		File f = null;
		for (File it : listPathTail(file, tailSize, false)) {
			if (it.getParentFile() != null) {
				f = new File(f, it.getName());
			}
		}
		return f;
	}

	public static List<File> getFileSystemRoots() {
		File[] roots = File.listRoots();

		// roots array may be null if folder permissions do not allow listing of files
		if (roots == null) {
			roots = new File[0];
		}

		return asList(roots);
	}

	public static List<File> getChildren(File folder) {
		return getChildren(folder, null, null);
	}

	public static List<File> getChildren(File folder, FileFilter filter) {
		return getChildren(folder, filter, null);
	}

	public static List<File> getChildren(File folder, FileFilter filter, Comparator<File> sorter) {
		File[] files = filter == null ? folder.listFiles() : folder.listFiles(filter);

		// children array may be null if folder permissions do not allow listing of files
		if (files == null) {
			files = new File[0];
		} else if (sorter != null) {
			sort(files, sorter);
		}

		return asList(files);
	}

	public static final int FILE_WALK_MAX_DEPTH = 32;

	public static List<File> listFiles(File... folders) {
		return listFiles(asList(folders));
	}

	public static List<File> listFiles(Iterable<File> folders) {
		return listFiles(folders, FILE_WALK_MAX_DEPTH, false, true, false);
	}

	public static List<File> listFolders(Iterable<File> folders) {
		return listFiles(folders, FILE_WALK_MAX_DEPTH, false, false, true);
	}

	public static List<File> listFiles(Iterable<File> folders, int maxDepth, boolean addHidden, boolean addFiles, boolean addFolders) {
		List<File> files = new ArrayList<File>();

		// collect files from directory tree
		for (File it : folders) {
			if (!addHidden && it.isHidden()) // ignore hidden files
				continue;

			if (it.isDirectory()) {
				if (addFolders) {
					files.add(it);
				}
				listFiles(it, files, 0, maxDepth, addHidden, addFiles, addFolders);
			} else if (addFiles && it.isFile()) {
				files.add(it);
			}
		}

		return files;
	}

	private static void listFiles(File folder, List<File> files, int depth, int maxDepth, boolean addHidden, boolean addFiles, boolean addFolders) {
		if (depth > maxDepth)
			return;

		for (File it : getChildren(folder)) {
			if (!addHidden && it.isHidden()) // ignore hidden files
				continue;

			if (it.isDirectory()) {
				if (addFolders) {
					files.add(it);
				}
				listFiles(it, files, depth + 1, maxDepth, addHidden, addFiles, addFolders);
			} else if (addFiles) {
				files.add(it);
			}
		}
	}

	public static SortedMap<File, List<File>> mapByFolder(Iterable<File> files) {
		SortedMap<File, List<File>> map = new TreeMap<File, List<File>>();

		for (File file : files) {
			File key = file.getParentFile();
			if (key == null) {
				throw new IllegalArgumentException("Parent is null: " + file);
			}

			List<File> valueList = map.get(key);
			if (valueList == null) {
				valueList = new ArrayList<File>();
				map.put(key, valueList);
			}

			valueList.add(file);
		}

		return map;
	}

	public static Map<String, List<File>> mapByExtension(Iterable<File> files) {
		Map<String, List<File>> map = new HashMap<String, List<File>>();

		for (File file : files) {
			String key = getExtension(file);
			if (key != null) {
				key = key.toLowerCase();
			}

			List<File> valueList = map.get(key);
			if (valueList == null) {
				valueList = new ArrayList<File>();
				map.put(key, valueList);
			}

			valueList.add(file);
		}

		return map;
	}

	/**
	 * Invalid file name characters: \, /, :, *, ?, ", <, >, |, \r, \n and excessive characters
	 */
	public static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\r\\n]|\\p{Cntrl}|\\s+$|(?<=[^.])[.]+$|(?<=.{250})(.+)(?=[.]\\p{Alnum}{3}$)");

	/**
	 * Strip file name of invalid characters
	 *
	 * @param filename
	 *            original filename
	 * @return valid file name stripped of invalid characters
	 */
	public static String validateFileName(CharSequence filename) {
		// strip invalid characters from file name
		return SPACE.matcher(ILLEGAL_CHARACTERS.matcher(filename).replaceAll("")).replaceAll(" ").trim();
	}

	public static boolean isInvalidFileName(CharSequence filename) {
		// check if file name contains any illegal characters
		return ILLEGAL_CHARACTERS.matcher(filename).find();
	}

	public static File validateFileName(File file) {
		// windows drives (e.g. c:, d:, etc.) are never invalid because name will be an empty string
		if (!isInvalidFileName(file.getName()))
			return file;

		// validate file name only
		return new File(file.getParentFile(), validateFileName(file.getName()));
	}

	public static File validateFilePath(File path) {
		Iterator<File> nodes = listPath(path).iterator();

		// initialize with root node, keep original root object if possible (so we don't loose the drive on windows)
		File validatedPath = validateFileName(nodes.next());

		// validate the rest of the path
		while (nodes.hasNext()) {
			validatedPath = new File(validatedPath, validateFileName(nodes.next().getName()));
		}

		return validatedPath;
	}

	public static boolean isInvalidFilePath(File path) {
		// check if file name contains any illegal characters
		for (File node = path; node != null; node = node.getParentFile()) {
			if (isInvalidFileName(node.getName()))
				return true;
		}

		return false;
	}

	public static String normalizePathSeparators(String path) {
		// special handling for UNC paths
		if (path.startsWith(UNC_PREFIX) && path.length() > 2) {
			return UNC_PREFIX + path.substring(2).replace('\\', '/');
		}
		return path.replace('\\', '/');
	}

	public static String replacePathSeparators(CharSequence path) {
		return replacePathSeparators(path, " ");
	}

	public static String replacePathSeparators(CharSequence path, String replacement) {
		return SLASH.matcher(path).replaceAll(replacement);
	}

	public static String md5(String string) {
		return md5(StandardCharsets.UTF_8.encode(string));
	}

	public static String md5(byte[] data) {
		return md5(ByteBuffer.wrap(data));
	}

	public static String md5(ByteBuffer data) {
		try {
			MessageDigest hash = MessageDigest.getInstance("MD5");
			hash.update(data);
			return String.format("%032x", new BigInteger(1, hash.digest())); // as hex string
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<File> asFileList(Object... paths) {
		List<File> files = new ArrayList<File>(paths.length);
		for (Object it : paths) {
			if (it instanceof CharSequence) {
				files.add(new File(it.toString()));
			} else if (it instanceof File) {
				files.add((File) it);
			} else if (it instanceof Path) {
				files.add(((Path) it).toFile());
			} else if (it instanceof Collection<?>) {
				files.addAll(asFileList(((Collection<?>) it).toArray())); // flatten object structure
			}
		}
		return files;
	}

	public static final int BUFFER_SIZE = 64 * 1024;

	public static final long KILO = 1024;
	public static final long MEGA = 1024 * KILO;
	public static final long GIGA = 1024 * MEGA;

	public static String formatSize(long size) {
		if (size >= GIGA)
			return String.format("%,d GB", size / GIGA);
		else if (size >= MEGA)
			return String.format("%,d MB", size / MEGA);
		else if (size >= KILO)
			return String.format("%,d KB", size / KILO);
		else
			return String.format("%,d bytes", size);
	}

	public static final FileFilter FOLDERS = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return file.isDirectory();
		}
	};

	public static final FileFilter FILES = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return file.isFile();
		}
	};

	public static final FileFilter TEMPORARY = new FileFilter() {

		private final String tmpdir = System.getProperty("java.io.tmpdir");

		@Override
		public boolean accept(File file) {
			return file.getAbsolutePath().startsWith(tmpdir);
		}
	};

	public static final FileFilter NOT_HIDDEN = new FileFilter() {

		@Override
		public boolean accept(File file) {
			return !file.isHidden();
		}
	};

	public static class ParentFilter implements FileFilter {

		private final File folder;

		public ParentFilter(File folder) {
			this.folder = folder;
		}

		@Override
		public boolean accept(File file) {
			return listPath(file).contains(folder);
		}
	}

	public static class ExtensionFileFilter implements FileFilter, FilenameFilter {

		public static final List<String> WILDCARD = singletonList("*");

		private final String[] extensions;

		public ExtensionFileFilter(String... extensions) {
			this.extensions = extensions.clone();
		}

		public ExtensionFileFilter(Collection<String> extensions) {
			this.extensions = extensions.toArray(new String[0]);
		}

		@Override
		public boolean accept(File dir, String name) {
			return accept(name);
		}

		@Override
		public boolean accept(File file) {
			return accept(file.getName());
		}

		public boolean accept(String name) {
			return acceptAny() || hasExtension(name, extensions);
		}

		public boolean acceptAny() {
			return extensions.length == 1 && WILDCARD.get(0).equals(extensions[0]);
		}

		public boolean acceptExtension(String extension) {
			if (acceptAny()) {
				return true;
			}

			for (String other : extensions) {
				if (other.equalsIgnoreCase(extension)) {
					return true;
				}
			}

			return false;
		}

		public String extension() {
			return extensions[0];
		}

		public String[] extensions() {
			return extensions.clone();
		}

		@Override
		public String toString() {
			StringBuilder s = new StringBuilder();
			for (String it : extensions) {
				if (s.length() > 0) {
					s.append(", ");
				}
				s.append("*.").append(it);
			}
			return s.toString();
		}
	}

	public static class RegexFileFilter implements FileFilter, FilenameFilter {

		private final Pattern pattern;

		public RegexFileFilter(Pattern pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean accept(File dir, String name) {
			return pattern.matcher(name).matches();
		}

		@Override
		public boolean accept(File file) {
			return accept(file.getParentFile(), file.getName());
		}
	}

	public static class NotFileFilter implements FileFilter {

		public FileFilter filter;

		public NotFileFilter(FileFilter filter) {
			this.filter = filter;
		}

		@Override
		public boolean accept(File file) {
			return !filter.accept(file);
		}
	}

	public static final Comparator<File> CASE_INSENSITIVE_PATH = new Comparator<File>() {

		@Override
		public int compare(File o1, File o2) {
			return o1.getPath().compareToIgnoreCase(o2.getPath());
		}
	};

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private FileUtilities() {
		throw new UnsupportedOperationException();
	}

}
