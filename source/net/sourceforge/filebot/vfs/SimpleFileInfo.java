package net.sourceforge.filebot.vfs;

import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.util.Arrays;

public class SimpleFileInfo implements FileInfo, Comparable<FileInfo> {

	private final String path;
	private final long length;

	public SimpleFileInfo(String path, long length) {
		this.path = path;
		this.length = length;
	}

	@Override
	public String getPath() {
		return path;
	}

	public String getName() {
		return getNameWithoutExtension(path);
	}

	@Override
	public String getType() {
		return getExtension(path);
	}

	public long getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { getPath(), getLength() });
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof FileInfo) {
			FileInfo other = (FileInfo) obj;
			return other.getLength() == getLength() && other.getPath().equals(getPath());
		}

		return false;
	}

	@Override
	public int compareTo(FileInfo other) {
		return getPath().compareTo(other.getPath());
	}

	@Override
	public String toString() {
		return getPath();
	}

	@Override
	public File toFile() {
		return new File(path);
	}

}
