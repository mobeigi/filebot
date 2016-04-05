package net.filebot.media;

import static java.util.Collections.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Locale;

import org.junit.Test;

public class MediaDetectionTest {

	@Test
	public void parseMovieYear() {
		assertEquals("[2009]", MediaDetection.parseMovieYear("Avatar 2009 2100").toString());
		assertEquals("[1955]", MediaDetection.parseMovieYear("1898 Sissi 1955").toString());
	}

	@Test
	public void stripFormatInfo() throws Exception {
		assertEquals("3.Idiots.PAL.DVD..", MediaDetection.stripFormatInfo("3.Idiots.PAL.DVD.DD5.1.x264"));
	}

	@Test
	public void detectSeriesName() throws Exception {
		assertEquals("[]", MediaDetection.detectSeriesNames(singleton(new File("Movie/LOTR.2001.AVC-1080")), false, Locale.ENGLISH).toString());
	}

}
