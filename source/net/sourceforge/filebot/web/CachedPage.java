
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;


public class CachedPage extends CachedResource<String> {
	
	public CachedPage(URL url) {
		super(url.toString(), String.class, 2 * 24 * 60 * 60 * 1000); // 48h update interval
	}
	
	
	@Override
	protected Cache getCache() {
		return CacheManager.getInstance().getCache("web-data-diskcache");
	}
	
	
	@Override
	public String process(ByteBuffer data) throws Exception {
		return Charset.forName("UTF-16BE").decode(data).toString();
	}
	
	
	@Override
	protected ByteBuffer fetchData(URL url, long lastModified) throws IOException {
		return Charset.forName("UTF-16BE").encode(readAll(openConnection(url)));
	}
	
	
	protected Reader openConnection(URL url) throws IOException {
		return getReader(url.openConnection());
	}
	
}
