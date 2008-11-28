
package net.sourceforge.filebot.web;


import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;


public class OpenSubtitlesHasherTest {
	
	@Test
	public void computeHash() throws Exception {
		assertEquals("8e245d9679d31e12", OpenSubtitlesHasher.computeHash(new File("breakdance.avi")));
	}
}
