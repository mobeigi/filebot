
package net.sourceforge.filebot.subtitle;


import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import org.junit.Test;


public class SubRipReaderTest {
	
	@Test
	public void parse() throws Exception {
		LinkedList<SubtitleElement> list = new LinkedList<SubtitleElement>();
		
		URL resource = new URL("http://www.opensubtitles.org/en/download/file/1951733951.gz");
		InputStream stream = new GZIPInputStream(resource.openStream());
		
		SubRipReader reader = new SubRipReader(new Scanner(stream, "UTF-8"));
		
		try {
			while (reader.hasNext()) {
				list.add(reader.next());
			}
		} finally {
			reader.close();
		}
		
		assertEquals(499, list.size(), 0);
		
		assertEquals(3455, list.getFirst().getStart(), 0);
		assertEquals(6799, list.getFirst().getEnd(), 0);
		
		assertEquals("Come with me if you want to live.", list.get(253).getText());
	}
	
}
