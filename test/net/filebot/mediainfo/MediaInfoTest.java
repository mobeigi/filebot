package net.filebot.mediainfo;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.mediainfo.MediaInfo.StreamKind;

public class MediaInfoTest {

	private static File getSampleFile(String name) throws Exception {
		File folder = new File(FileUtils.getTempDirectory(), MediaInfoTest.class.getName());
		File file = new File(folder, name + ".mp4");

		if (!file.exists()) {
			byte[] bytes = Cache.getCache(folder.getName(), CacheType.Persistent).bytes("video/mp4/720/big_buck_bunny_720p_1mb.mp4", n -> {
				return new URL("http://www.sample-videos.com/" + n);
			}).get();

			FileUtils.forceMkdir(folder);
			FileUtils.writeByteArrayToFile(file, bytes);
		}

		return file;
	}

	private static void testSampleFile(String name) throws Exception {
		MediaInfo mi = new MediaInfo().open(getSampleFile(name));

		assertEquals("MPEG-4", mi.get(StreamKind.General, 0, "Format"));
		assertEquals("AVC", mi.get(StreamKind.Video, 0, "Format"));
		assertEquals("AAC", mi.get(StreamKind.Audio, 0, "Format"));
	}

	@Test
	public void open() throws Exception {
		testSampleFile("English");
	}

	@Test
	public void openUnicode() throws Exception {
		testSampleFile("中文");
		testSampleFile("日本語");
	}

	@Test
	public void openDiacriticalMarks() throws Exception {
		testSampleFile("Español");
		testSampleFile("Österreichisch");
	}

}
