package net.filebot.archive;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Arrays.*;
import static net.filebot.Logging.*;
import static net.filebot.util.RegularExpressions.*;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.filebot.util.ByteBufferOutputStream;
import net.filebot.vfs.FileInfo;
import net.filebot.vfs.SimpleFileInfo;

public class SevenZipExecutable implements ArchiveExtractor {

	// e.g. 2014-09-15 05:33:10 ....A 398536 625065 folder/file.txt
	final Pattern listFilesLinePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+\\S+\\s+(?<size>\\d+)\\s+\\d*\\s+(?<name>.+)$", Pattern.MULTILINE);

	final File archive;

	public SevenZipExecutable(File file) throws Exception {
		if (!file.exists()) {
			throw new FileNotFoundException(file.getAbsolutePath());
		}

		this.archive = file.getCanonicalFile();
	}

	protected String get7zCommand() {
		// use 7z executable path as specified by the cmdline or default to "7z" and let the shell figure it out
		return System.getProperty("net.filebot.Archive.7z", "7z");
	}

	protected CharSequence execute(String... command) throws IOException {
		Process process = new ProcessBuilder(command).redirectError(Redirect.INHERIT).start();

		try (ByteBufferOutputStream bb = new ByteBufferOutputStream(8 * 1024)) {
			bb.transferFully(process.getInputStream());

			int returnCode = process.waitFor();
			String output = UTF_8.decode(bb.getByteBuffer()).toString();

			// DEBUG
			debug.fine(format("Execute: %s", asList(command)));
			debug.finest(output);

			if (returnCode == 0) {
				return output;
			} else {
				throw new IOException(String.format("%s failed with exit code %d: %s", get7zCommand(), returnCode, SPACE.matcher(output).replaceAll(" ").trim()));
			}
		} catch (InterruptedException e) {
			throw new IOException(String.format("%s timed out", get7zCommand()), e);
		}
	}

	@Override
	public List<FileInfo> listFiles() throws IOException {
		List<FileInfo> paths = new ArrayList<FileInfo>();

		// e.g. 7z l -y archive.7z
		CharSequence output = execute(get7zCommand(), "l", "-y", archive.getPath());

		Matcher m = listFilesLinePattern.matcher(output);
		while (m.find()) {
			String path = m.group("name").trim();
			long size = Long.parseLong(m.group("size"));

			// ignore folders, e.g. 2015-03-26 02:37:24 D.... 0 0 folder
			if (size > 0 && path.length() > 0) {
				paths.add(new SimpleFileInfo(path, size));
			}
		}

		return paths;
	}

	@Override
	public void extract(File outputDir) throws IOException {
		// e.g. 7z x -y -aos archive.7z
		execute(get7zCommand(), "x", "-y", "-aos", archive.getPath(), "-o" + outputDir.getCanonicalPath());
	}

	@Override
	public void extract(File outputDir, FileFilter filter) throws IOException {
		// e.g. 7z x -y -aos archive.7z file.txt image.png info.nfo
		Stream<String> command = Stream.of(get7zCommand(), "x", "-y", "-aos", archive.getPath(), "-o" + outputDir.getCanonicalPath());
		Stream<String> selection = listFiles().stream().filter(f -> filter.accept(f.toFile())).map(f -> f.getPath());

		execute(Stream.concat(command, selection).toArray(String[]::new));
	}

}
