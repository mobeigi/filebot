package net.filebot.mediainfo;

import static java.nio.charset.StandardCharsets.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.util.RegularExpressions.*;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

public class MediaInfo implements Closeable {

	private Pointer handle;

	public MediaInfo() {
		try {
			handle = MediaInfoLibrary.INSTANCE.New();
		} catch (LinkageError e) {
			throw new MediaInfoException(e);
		}
	}

	public synchronized MediaInfo open(File file) throws IOException, IllegalArgumentException {
		if (!file.isFile() || file.length() < 64 * 1024) {
			throw new IllegalArgumentException("Invalid media file: " + file);
		}

		String path = file.getCanonicalPath();

		// on Mac files that contain accents cannot be opened via JNA WString file paths due to encoding differences so we use the buffer interface instead for these files
		if (Platform.isMac() && !US_ASCII.newEncoder().canEncode(path)) {
			try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
				if (openViaBuffer(raf)) {
					return this;
				}
				throw new IOException("Failed to initialize media info buffer: " + path);
			}
		}

		// open via file path
		if (0 != MediaInfoLibrary.INSTANCE.Open(handle, new WString(path))) {
			return this;
		}
		throw new IOException("Failed to open media file: " + path);
	}

	private boolean openViaBuffer(RandomAccessFile f) throws IOException {
		byte[] buffer = new byte[4 * 1024 * 1024]; // use large buffer to reduce JNA calls
		int read = -1;

		if (0 == MediaInfoLibrary.INSTANCE.Open_Buffer_Init(handle, f.length(), 0)) {
			return false;
		}

		do {
			read = f.read(buffer);
			int result = MediaInfoLibrary.INSTANCE.Open_Buffer_Continue(handle, buffer, read);
			if ((result & 8) == 8) {
				break;
			}

			long gotoPos = MediaInfoLibrary.INSTANCE.Open_Buffer_Continue_GoTo_Get(handle);
			if (gotoPos >= 0) {
				f.seek(gotoPos);
				MediaInfoLibrary.INSTANCE.Open_Buffer_Init(handle, f.length(), gotoPos);
			}
		} while (read > 0);

		MediaInfoLibrary.INSTANCE.Open_Buffer_Finalize(handle);
		return true;
	}

	public synchronized String inform() {
		return MediaInfoLibrary.INSTANCE.Inform(handle).toString();
	}

	public String option(String option) {
		return option(option, "");
	}

	public synchronized String option(String option, String value) {
		return MediaInfoLibrary.INSTANCE.Option(handle, new WString(option), new WString(value)).toString();
	}

	public String get(StreamKind streamKind, int streamNumber, String parameter) {
		return get(streamKind, streamNumber, parameter, InfoKind.Text, InfoKind.Name);
	}

	public String get(StreamKind streamKind, int streamNumber, String parameter, InfoKind infoKind) {
		return get(streamKind, streamNumber, parameter, infoKind, InfoKind.Name);
	}

	public synchronized String get(StreamKind streamKind, int streamNumber, String parameter, InfoKind infoKind, InfoKind searchKind) {
		return MediaInfoLibrary.INSTANCE.Get(handle, streamKind.ordinal(), streamNumber, new WString(parameter), infoKind.ordinal(), searchKind.ordinal()).toString();
	}

	public String get(StreamKind streamKind, int streamNumber, int parameterIndex) {
		return get(streamKind, streamNumber, parameterIndex, InfoKind.Text);
	}

	public synchronized String get(StreamKind streamKind, int streamNumber, int parameterIndex, InfoKind infoKind) {
		return MediaInfoLibrary.INSTANCE.GetI(handle, streamKind.ordinal(), streamNumber, parameterIndex, infoKind.ordinal()).toString();
	}

	public synchronized int streamCount(StreamKind streamKind) {
		return MediaInfoLibrary.INSTANCE.Count_Get(handle, streamKind.ordinal(), -1);
	}

	public synchronized int parameterCount(StreamKind streamKind, int streamNumber) {
		return MediaInfoLibrary.INSTANCE.Count_Get(handle, streamKind.ordinal(), streamNumber);
	}

	public Map<StreamKind, List<Map<String, String>>> snapshot() {
		Map<StreamKind, List<Map<String, String>>> mediaInfo = new EnumMap<StreamKind, List<Map<String, String>>>(StreamKind.class);

		for (StreamKind streamKind : StreamKind.values()) {
			int streamCount = streamCount(streamKind);

			if (streamCount > 0) {
				List<Map<String, String>> streamInfoList = new ArrayList<Map<String, String>>(streamCount);

				for (int i = 0; i < streamCount; i++) {
					streamInfoList.add(snapshot(streamKind, i));
				}

				mediaInfo.put(streamKind, streamInfoList);
			}
		}

		return mediaInfo;
	}

	public Map<String, String> snapshot(StreamKind streamKind, int streamNumber) {
		Map<String, String> streamInfo = new LinkedHashMap<String, String>();

		for (int i = 0, count = parameterCount(streamKind, streamNumber); i < count; i++) {
			String value = get(streamKind, streamNumber, i, InfoKind.Text);

			if (value.length() > 0) {
				streamInfo.put(get(streamKind, streamNumber, i, InfoKind.Name), value);
			}
		}

		// MediaInfo does not support EXIF image metadata natively so we use the metadata-extractor library and implicitly merge that information in
		if (streamKind == StreamKind.Image && streamNumber == 0) {
			String path = get(StreamKind.General, 0, "CompleteName");
			try {
				Map<String, String> values = new ImageMetadata(new File(path)).snapshot(t -> {
					return Stream.of(t.getDirectoryName(), t.getTagName()).flatMap(NON_WORD::splitAsStream).distinct().collect(joining("_"));
				});
				streamInfo.putAll(values);
			} catch (Throwable e) {
				debug.warning(format("%s: %s", e, path));
			}
		}

		return streamInfo;
	}

	@Override
	public synchronized void close() {
		MediaInfoLibrary.INSTANCE.Close(handle);
	}

	public synchronized void dispose() {
		if (handle == null) {
			return;
		}

		// delete handle
		MediaInfoLibrary.INSTANCE.Delete(handle);
		handle = null;
	}

	@Override
	protected void finalize() {
		dispose();
	}

	public enum StreamKind {
		General, Video, Audio, Text, Chapters, Image, Menu;
	}

	public enum InfoKind {
		/**
		 * Unique name of parameter.
		 */
		Name,

		/**
		 * Value of parameter.
		 */
		Text,

		/**
		 * Unique name of measure unit of parameter.
		 */
		Measure,

		Options,

		/**
		 * Translated name of parameter.
		 */
		Name_Text,

		/**
		 * Translated name of measure unit.
		 */
		Measure_Text,

		/**
		 * More information about the parameter.
		 */
		Info,

		/**
		 * How this parameter is supported, could be N (No), B (Beta), R (Read only), W (Read/Write).
		 */
		HowTo,

		/**
		 * Domain of this piece of information.
		 */
		Domain;
	}

	public static String version() {
		return staticOption("Info_Version");
	}

	public static String parameters() {
		return staticOption("Info_Parameters");
	}

	public static String codecs() {
		return staticOption("Info_Codecs");
	}

	public static String capacities() {
		return staticOption("Info_Capacities");
	}

	public static String staticOption(String option) {
		return staticOption(option, "");
	}

	public static String staticOption(String option, String value) {
		try {
			return MediaInfoLibrary.INSTANCE.Option(null, new WString(option), new WString(value)).toString();
		} catch (LinkageError e) {
			throw new MediaInfoException(e);
		}
	}

	public static Map<StreamKind, List<Map<String, String>>> snapshot(File file) throws IOException {
		try (MediaInfo mi = new MediaInfo().open(file)) {
			return mi.snapshot();
		}
	}

}
