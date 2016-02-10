package net.filebot.media;

import static org.junit.Assert.*;

import org.junit.Test;

public class MediaDetectionTest {

	@Test
	public void parseMovieYear() {
		assertEquals("[2009]", MediaDetection.parseMovieYear("Avatar 2009 2100").toString());
		assertEquals("[1955]", MediaDetection.parseMovieYear("1898 Sissi 1955").toString());
	}
}
