
package net.sourceforge.filebot.media;


import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;


public class ReleaseInfoTest {
	
	@Test
	public void getVideoSource() {
		ReleaseInfo info = new ReleaseInfo();
		File f = new File("Jurassic.Park[1993]DvDrip-aXXo.avi");
		
		assertEquals("DVDRip", info.getVideoSource(f.getName()));
	}
	
	
	@Test
	public void getReleaseGroup() throws Exception {
		ReleaseInfo info = new ReleaseInfo();
		File f = new File("Jurassic.Park[1993]DvDrip-aXXo.avi");
		
		assertEquals("aXXo", info.getReleaseGroup(f.getName()));
	}
	
}
