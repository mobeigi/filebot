
package net.sourceforge.filebot.subtitle;


import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.junit.Test;


public class SubRipReaderTest {
	
	@Test
	public void parse() throws Exception {
		List<SubtitleElement> list = new ArrayList<SubtitleElement>();
		
		URL resource = new URL("http://www.opensubtitles.org/en/download/file/1951733951.gz");
		InputStream source = new GZIPInputStream(resource.openStream());
		
		SubRipReader reader = new SubRipReader(new InputStreamReader(source, "UTF-8"));
		
		try {
			while (reader.hasNext()) {
				list.add(reader.next());
			}
		} finally {
			reader.close();
		}
		
		assertEquals(501, list.size(), 0);
	}
}
