
package net.sourceforge.filebot.archive;


import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IArchiveOpenCallback;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.ISevenZipInArchive;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.tuned.FileUtilities.ExtensionFileFilter;


public class Archive implements Closeable {
	
	private ISevenZipInArchive inArchive;
	private ArchiveOpenVolumeCallback openVolume;
	
	
	public Archive(File file) throws Exception {
		// initialize 7-Zip-JBinding
		if (!file.exists()) {
			throw new FileNotFoundException(file.getAbsolutePath());
		}
		
		// 7-Zip-JBinding NATIVE LIBS MUST BE LOADED WITH SYSTEM CLASSLOADER
		Class<?> clazz = ClassLoader.getSystemClassLoader().loadClass("net.sourceforge.filebot.archive.SevenZipLoader");
		Method m = clazz.getMethod("open", new Class[] { IInStream.class, IArchiveOpenCallback.class });
		
		try {
			openVolume = new ArchiveOpenVolumeCallback();
			inArchive = (ISevenZipInArchive) m.invoke(null, openVolume.getStream(file.getAbsolutePath()), openVolume);
		} catch (InvocationTargetException e) {
			throw (Exception) e.getTargetException();
		}
	}
	
	
	public int itemCount() throws SevenZipException {
		return inArchive.getNumberOfItems();
	}
	
	
	public Map<PropID, Object> getItem(int index) throws SevenZipException {
		Map<PropID, Object> item = new EnumMap<PropID, Object>(PropID.class);
		
		for (PropID prop : PropID.values()) {
			Object value = inArchive.getProperty(index, prop);
			if (value != null) {
				item.put(prop, value);
			}
		}
		
		return item;
	}
	
	
	public List<File> listFiles() throws SevenZipException {
		List<File> paths = new ArrayList<File>();
		
		for (int i = 0; i < inArchive.getNumberOfItems(); i++) {
			boolean isFolder = (Boolean) inArchive.getProperty(i, PropID.IS_FOLDER);
			if (!isFolder) {
				String path = (String) inArchive.getProperty(i, PropID.PATH);
				if (path != null) {
					paths.add(new File(path));
				}
			}
		}
		
		return paths;
	}
	
	
	public void extract(ExtractOutProvider outputMapper) throws SevenZipException {
		inArchive.extract(null, false, new ExtractCallback(inArchive, outputMapper));
	}
	
	
	public void extract(ExtractOutProvider outputMapper, FileFilter filter) throws SevenZipException {
		List<Integer> selection = new ArrayList<Integer>();
		
		for (int i = 0; i < inArchive.getNumberOfItems(); i++) {
			boolean isFolder = (Boolean) inArchive.getProperty(i, PropID.IS_FOLDER);
			if (!isFolder) {
				String path = (String) inArchive.getProperty(i, PropID.PATH);
				if (path != null && filter.accept(new File(path))) {
					selection.add(i);
				}
			}
		}
		
		int[] indices = new int[selection.size()];
		for (int i = 0; i < indices.length; i++) {
			indices[i] = selection.get(i);
		}
		inArchive.extract(indices, false, new ExtractCallback(inArchive, outputMapper));
	}
	
	
	@Override
	public void close() throws IOException {
		try {
			inArchive.close();
		} catch (SevenZipException e) {
			throw new IOException(e);
		} finally {
			openVolume.close();
		}
	}
	
	
	public static Set<String> getArchiveTypes() {
		Set<String> extensions = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
		
		// application data
		extensions.addAll(MediaTypes.getDefault().getExtensionList("archive"));
		
		// formats provided by the library
		for (ArchiveFormat it : ArchiveFormat.values()) {
			extensions.add(it.getMethodName());
		}
		
		return extensions;
	}
	
	public static final FileFilter VOLUME_ONE_FILTER = new FileFilter() {
		
		private Pattern volume = Pattern.compile("[.]r[0-9]+$|[.]part[0-9]+[.][0-9a-z]+$|[.][0-9]+$", Pattern.CASE_INSENSITIVE);
		private FileFilter archives = new ExtensionFileFilter(getArchiveTypes());
		
		
		@Override
		public boolean accept(File path) {
			if (!archives.accept(path) && !volume.matcher(path.getName()).find())
				return false;
			
			Matcher matcher = volume.matcher(path.getName());
			if (matcher.find()) {
				Scanner scanner = new Scanner(matcher.group()).useDelimiter("\\D+");
				if (!scanner.hasNext() || scanner.nextInt() != 1) {
					return false;
				}
			}
			
			return true;
		}
		
	};
	
}
