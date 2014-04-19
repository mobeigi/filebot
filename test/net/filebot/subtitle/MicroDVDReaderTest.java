
package net.sourceforge.filebot.subtitle;


import static org.junit.Assert.*;

import java.io.StringReader;

import org.junit.Test;


public class MicroDVDReaderTest {
	
	@Test
	public void parse() throws Exception {
		MicroDVDReader reader = new MicroDVDReader(new StringReader("{856}{900}what's the plan?"));
		
		SubtitleElement element = reader.next();
		
		assertEquals(856 * 23.976, element.getStart(), 1);
		assertEquals(900 * 23.976, element.getEnd(), 1);
		assertEquals("what's the plan?", element.getText());
	}
	

	@Test
	public void fps() throws Exception {
		MicroDVDReader reader = new MicroDVDReader(new StringReader("{1}{1}100\n{300}{400} trim me "));
		
		SubtitleElement element = reader.next();
		
		assertEquals(300 * 100, element.getStart(), 0);
		assertEquals(400 * 100, element.getEnd(), 0);
		assertEquals("trim me", element.getText());
	}
	

	@Test
	public void newline() throws Exception {
		MicroDVDReader reader = new MicroDVDReader(new StringReader("\n\n{300}{400} l1|l2|l3| \n\n"));
		
		String[] lines = reader.next().getText().split("\\n");
		
		assertEquals(3, lines.length);
		assertEquals("l1", lines[0]);
		assertEquals("l2", lines[1]);
		assertEquals("l3", lines[2]);
	}
}
