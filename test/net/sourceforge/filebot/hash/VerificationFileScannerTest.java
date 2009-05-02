
package net.sourceforge.filebot.hash;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.Scanner;
import java.util.Map.Entry;

import org.junit.Test;


public class VerificationFileScannerTest {
	
	@Test
	public void nextLine() {
		// trim lines, skip empty lines
		String text = String.format("%s   %n  %n%n%n    %s%n%s", "line 1", "line 2", "line 3");
		
		VerificationFileScanner lines = new VerificationFileScanner(new Scanner(text));
		
		assertEquals("line 1", lines.nextLine());
		assertEquals("line 2", lines.nextLine());
		assertEquals("line 3", lines.nextLine());
		
		assertFalse(lines.hasNext());
	}
	

	@Test
	public void parseLine() {
		VerificationFileScanner reader = new VerificationFileScanner(new Scanner("null"));
		
		// md5 
		Entry<File, String> md5 = reader.parseLine("50e85fe18e17e3616774637a82968f4c *folder/file.txt");
		
		assertEquals("file.txt", md5.getKey().getName());
		assertEquals("folder", md5.getKey().getParent());
		assertEquals("50e85fe18e17e3616774637a82968f4c", md5.getValue());
		
		// sha1
		Entry<File, String> sha1 = reader.parseLine("1a02a7c1e9ac91346d08829d5037b240f42ded07 ?SHA1*folder/file.txt");
		
		assertEquals("file.txt", sha1.getKey().getName());
		assertEquals("folder", sha1.getKey().getParent());
		assertEquals("1a02a7c1e9ac91346d08829d5037b240f42ded07", sha1.getValue());
	}
	
}
