
package net.filebot.media;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import org.junit.Test;

public class ReleaseInfoTest {

	ReleaseInfo info = new ReleaseInfo();

	@Test
	public void getVideoSource() {
		assertEquals("DVDRip", info.getVideoSource("Jurassic.Park[1993]DvDrip-aXXo"));
	}

	@Test
	public void getReleaseGroup() throws Exception {
		assertEquals("aXXo", info.getReleaseGroup("Jurassic.Park[1993]DvDrip-aXXo"));
	}

	@Test
	public void getClutterBracketPattern() throws Exception {
		assertEquals("John [2016]  (ENG)", clean(info.getClutterBracketPattern(true), "John [2016] [Action, Drama] (ENG)"));
		assertEquals("John [2016]  ", clean(info.getClutterBracketPattern(false), "John [2016] [Action, Drama] (ENG)"));
	}

	private static String clean(Pattern p, String s) {
		return p.matcher(s).replaceAll("");
	}

}
