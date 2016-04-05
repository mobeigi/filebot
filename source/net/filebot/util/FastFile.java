package net.filebot.util;

import java.io.File;
import java.io.IOException;

public class FastFile extends File {

	private String name;
	private Long length;
	private Long lastModified;
	private Boolean isDirectory;
	private Boolean isFile;
	private Boolean isHidden;

	private String[] list;
	private File[] listFiles;

	private File canonicalFile;
	private File parentFile;

	public FastFile(File file) {
		super(file.getPath());
	}

	public FastFile(File parent, String child) {
		super(parent, child);
	}

	@Override
	public String getName() {
		return name != null ? name : (name = super.getName());
	}

	@Override
	public long length() {
		return length != null ? length : (length = super.length());
	}

	@Override
	public boolean isDirectory() {
		return isDirectory != null ? isDirectory : (isDirectory = super.isDirectory());
	}

	@Override
	public boolean isFile() {
		return isFile != null ? isFile : (isFile = super.isFile());
	}

	@Override
	public boolean isHidden() {
		return isHidden != null ? isHidden : (isHidden = super.isHidden());
	}

	@Override
	public long lastModified() {
		return lastModified != null ? lastModified : (lastModified = super.lastModified());
	}

	@Override
	public File getCanonicalFile() throws IOException {
		return canonicalFile != null ? canonicalFile : (canonicalFile = get(super.getCanonicalFile()));
	}

	@Override
	public File getParentFile() {
		return parentFile != null ? parentFile : (parentFile = get(super.getParentFile()));
	}

	@Override
	public String[] list() {
		if (list != null) {
			return list;
		}

		String[] names = super.list();
		if (names == null) {
			names = new String[0];
		}

		return (list = names);
	}

	@Override
	public File[] listFiles() {
		if (listFiles != null) {
			return listFiles;
		}

		String[] names = list();
		File[] files = new File[names.length];
		for (int i = 0; i < names.length; i++) {
			files[i] = new FastFile(this, names[i]);
		}

		return (listFiles = files);
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public boolean canRead() {
		return true;
	}

	@Override
	public boolean canWrite() {
		return false;
	}

	@Override
	public boolean canExecute() {
		return false;
	}

	@Override
	public boolean createNewFile() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean delete() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteOnExit() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean mkdir() {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean mkdirs() {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean renameTo(File dest) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setLastModified(long time) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setReadOnly() {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean setWritable(boolean writable, boolean ownerOnly) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setWritable(boolean writable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean setReadable(boolean readable, boolean ownerOnly) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean setReadable(boolean readable) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean setExecutable(boolean executable, boolean ownerOnly) {
		throw new UnsupportedOperationException();

	}

	@Override
	public boolean setExecutable(boolean executable) {
		throw new UnsupportedOperationException();

	}

	@Override
	public long getTotalSpace() {
		throw new UnsupportedOperationException();

	}

	@Override
	public long getFreeSpace() {
		throw new UnsupportedOperationException();

	}

	@Override
	public long getUsableSpace() {
		throw new UnsupportedOperationException();
	}

	public static FastFile get(File f) {
		return f == null ? null : new FastFile(f);
	}

}
