package net.filebot.web;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import javax.xml.parsers.SAXParserFactory;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class CachedXmlResource extends AbstractCachedResource<String, String> {

	public CachedXmlResource(String resource) {
		super(resource, String.class, ONE_WEEK, 2, 1000);
	}

	public CachedXmlResource(String resource, long expirationTime, int retryCountLimit, long retryWaitTime) {
		super(resource, String.class, expirationTime, retryCountLimit, retryWaitTime);
	}

	@Override
	protected Cache getCache() {
		return CacheManager.getInstance().getCache("web-datasource-lv3");
	}

	public Document getDocument() throws IOException {
		try {
			return WebRequest.getDocument(get());
		} catch (Exception e) {
			throw new IOException("Error while loading XML resource: " + getResourceLocation(resource));
		}
	}

	@Override
	public String process(String data) throws Exception {
		// make sure xml data is valid and well-formed before caching it
		SAXParserFactory sax = SAXParserFactory.newInstance();
		sax.setValidating(false);
		sax.setNamespaceAware(false);

		XMLReader reader = sax.newSAXParser().getXMLReader();
		reader.setErrorHandler(new DefaultHandler()); // unwind on error
		try {
			reader.parse(new InputSource(new StringReader(data)));
		} catch (SAXException e) {
			throw new IOException("Malformed XML: " + getResourceLocation(resource));
		}

		return data;
	}

	@Override
	protected String fetchData(URL url, long lastModified) throws IOException {
		ByteBuffer data = WebRequest.fetchIfModified(url, lastModified);

		if (data == null)
			return null; // not modified

		return Charset.forName("UTF-8").decode(data).toString();
	}

}
