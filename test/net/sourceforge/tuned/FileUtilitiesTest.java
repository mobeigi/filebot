
package net.sourceforge.tuned;


import static org.junit.Assert.*;

import org.junit.Test;


public class FileUtilitiesTest {
	
	@Test
	public void hasExtension() {
		assertTrue(FileUtilities.hasExtension("abc.txt", "txt"));
		assertFalse(FileUtilities.hasExtension(".hidden", "txt"));
	}
	
	
	@Test
	public void getExtension() {
		assertEquals("txt", FileUtilities.getExtension("abc.txt"));
		assertEquals("out", FileUtilities.getExtension("a.out"));
		assertEquals(null, FileUtilities.getExtension(".hidden"));
		assertEquals(null, FileUtilities.getExtension("a."));
		
		assertEquals("r00", FileUtilities.getExtension("archive.r00"));
		assertEquals(null, FileUtilities.getExtension("archive.r??"));
		assertEquals(null, FileUtilities.getExtension("archive.invalid extension"));
	}
	
	
	@Test
	public void getNameWithoutExtension() {
		assertEquals("abc", FileUtilities.getNameWithoutExtension("abc.txt"));
		assertEquals("a", FileUtilities.getNameWithoutExtension("a.out"));
		assertEquals(".hidden", FileUtilities.getNameWithoutExtension(".hidden"));
		assertEquals("a.", FileUtilities.getNameWithoutExtension("a."));
		
		assertEquals("archive", FileUtilities.getNameWithoutExtension("archive.r00"));
		assertEquals("archive.r??", FileUtilities.getNameWithoutExtension("archive.r??"));
		assertEquals("archive.invalid extension", FileUtilities.getNameWithoutExtension("archive.invalid extension"));
	}
	
}
