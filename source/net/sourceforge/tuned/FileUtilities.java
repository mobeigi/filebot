package net.sourceforge.tuned;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

public final class FileUtilities {

	public static File moveRename(File source, File destination) throws IOException {
		// resolve destination
		destination = resolveDestination(source, destination, true);

		if (source.isDirectory()) {
			// move folder
			org.apache.commons.io.FileUtils.moveDirectory(source, destination);
		} else {
			// move file
			try {
				// * On Windows ATOMIC_MOVE allows us to rename files even if only lower/upper-case changes (without ATOMIC_MOVE the operation would be ignored)
				java.nio.file.Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (LinkageError e) {
				org.apache.commons.io.FileUtils.moveFile(source, destination); // use "copy and delete" as fallback if standard rename fails
			}
		}

		return destination;
	}

	public static File copyAs(File source, File destination) throws IOException {
		// resolve destination
		destination = resolveDestination(source, destination, true);

		if (source.isDirectory()) {
			// copy folder
			org.apache.commons.io.FileUtils.copyDirectory(source, destination);
		} else {
			// copy file
			try {
				java.nio.file.Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			} catch (LinkageError e) {
				org.apache.commons.io.FileUtils.copyFile(source, destination);
			}
		}

		return destination;
	}

	public static File resolveDestination(File source, File destination, boolean mkdirs) throws IOException {
		// resolve destination
		if (!destination.isAbsolute()) {
			// same folder, different name
			destination = new File(source.getParentFile(), destination.getPath());
		}

		// make sure we that we can create the destination folder structure
		File destinationFolder = destination.getParentFile();

		// create parent folder if necessary
		if (mkdirs && !destinationFolder.isDirectory() && !destinationFolder.mkdirs()) {
			throw new IOException("Failed to create folder: " + destinationFolder);
		}

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
		return java.nio.file.Files.createSymbolicLink(link.toPath(), target.toPath()).toFile();
	}

	public static boolean delete(File file) {
		// delete files or files
		return org.apache.commons.io.FileUtils.deleteQuietly(file);
	}

	public static byte[] readFile(File source) throws IOException {
		InputStream in = new FileInputStream(source);

		try {
			long size = source.length();
			if (size < 0 || size > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Unable to read file: " + source);
			}

			byte[] data = new byte[(int) size];

			int position = 0;
			int read = 0;

			while (position < data.length && (read = in.read(data, position, data.length - position)) >= 0) {
				position += read;
			}

			return data;
		} finally {
			in.close();
		}
	}

	public static String readAll(Reader source) throws IOException {
		StringBuilder text = new StringBuilder();
		char[] buffer = new char[2048];

		int read = 0;
		while ((read = source.read(buffer)) >= 0) {
			text.append(buffer, 0, read);
		}

		return text.toString();
	}

	public static void writeFile(ByteBuffer data, File destination) throws IOException {
		FileChannel fileChannel = new FileOutputStream(destination).getChannel();

		try {
			fileChannel.write(data);
		} finally {
			fileChannel.close();
		}
	}

	public static List<String[]> readCSV(InputStream source, String charsetName, String separatorPattern) {
		Scanner scanner = new Scanner(source, charsetName);
		Pattern separator = Pattern.compile(separatorPattern);
		List<String[]> rows = new ArrayList<String[]>(65536);

		while (scanner.hasNextLine()) {
			rows.add(separator.split(scanner.nextLine()));
		}

		return rows;
	}

	public static Reader createTextReader(File file) throws IOException {
		CharsetDetector detector = new CharsetDetector();
		detector.setDeclaredEncoding("UTF-8"); // small boost for UTF-8 as default encoding
		detector.setText(new BufferedInputStream(new FileInputStream(file)));

		CharsetMatch charset = detector.detect();
		if (charset != null)
			return charset.getReader();

		// assume UTF-8 by default
		return new InputStreamReader(new FileInputStream(file), "UTF-8");
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
		return Charset.forName("UTF-8").decode(data).toString();
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

	public static List<File> flatten(Iterable<File> roots, int maxDepth, boolean listHiddenFiles) {
		List<File> files = new ArrayList<File>();

		// unfold/flatten file tree
		for (File root : roots) {
			if (root.isDirectory()) {
				listFiles(root, 0, files, maxDepth, listHiddenFiles);
			} else {
				files.add(root);
			}
		}

		return files;
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

	public static List<File> listFiles(Iterable<File> folders, int maxDepth, boolean listHiddenFiles) {
		List<File> files = new ArrayList<File>();

		// collect files from directory tree
		for (File folder : folders) {
			listFiles(folder, 0, files, maxDepth, listHiddenFiles);
		}

		return files;
	}

	private static void listFiles(File folder, int depth, List<File> files, int maxDepth, boolean listHiddenFiles) {
		if (depth > maxDepth)
			return;

		for (File file : folder.listFiles()) {
			if (!listHiddenFiles && file.isHidden()) // ignore hidden files
				continue;

			if (file.isDirectory()) {
				listFiles(file, depth + 1, files, maxDepth, listHiddenFiles);
			} else {
				files.add(file);
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
	 * Invalid file name characters: \, /, :, *, ?, ", <, >, |, \r and \n
	 */
	public static final Pattern ILLEGAL_CHARACTERS = Pattern.compile("[\\\\/:*?\"<>|\\r\\n]|[ ]+$|(?<=[^.])[.]+$");

	/**
	 * Strip file name of invalid characters
	 * 
	 * @param filename
	 *            original filename
	 * @return valid file name stripped of invalid characters
	 */
	public static String validateFileName(CharSequence filename) {
		// strip invalid characters from file name
		return ILLEGAL_CHARACTERS.matcher(filename).replaceAll("").replaceAll("\\s+", " ").trim();
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
		return Pattern.compile("\\s*[\\\\/]+\\s*").matcher(path).replaceAll(replacement);
	}

	public static String getXmlString(Document dom) throws TransformerException {
		Transformer tr = TransformerFactory.newInstance().newTransformer();
		tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		tr.setOutputProperty(OutputKeys.INDENT, "yes");

		// create string from dom
		StringWriter buffer = new StringWriter();
		tr.transform(new DOMSource(dom), new StreamResult(buffer));
		return buffer.toString();
	}

	public static final long KILO = 1024;
	public static final long MEGA = 1024 * KILO;
	public static final long GIGA = 1024 * MEGA;

	public static String formatSize(long size) {
		if (size >= MEGA)
			return String.format("%,d MB", size / MEGA);
		else if (size >= KILO)
			return String.format("%,d KB", size / KILO);
		else
			return String.format("%,d Byte", size);
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

	public static class ExtensionFileFilter implements FileFilter {

		private final String[] extensions;

		public ExtensionFileFilter(String... extensions) {
			this.extensions = extensions;
		}

		public ExtensionFileFilter(Collection<String> extensions) {
			this.extensions = extensions.toArray(new String[0]);
		}

		@Override
		public boolean accept(File file) {
			return hasExtension(file, extensions);
		}

		public boolean accept(String name) {
			return hasExtension(name, extensions);
		}

		public boolean acceptExtension(String extension) {
			for (String other : extensions) {
				if (other.equalsIgnoreCase(extension))
					return true;
			}

			return false;
		}

		public String extension() {
			return extensions[0];
		}

		public String[] extensions() {
			return extensions.clone();
		}
	}

	public static class RegexFileFilter implements FileFilter, FilenameFilter {

		private final Pattern pattern;

		public RegexFileFilter(Pattern pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean accept(File dir, String name) {
			return pattern.matcher(name).find();
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

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private FileUtilities() {
		throw new UnsupportedOperationException();
	}

}
