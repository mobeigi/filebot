
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public class HtmlUtil {
	
	private static Charset getCharset(String contentType) {
		if (contentType != null) {
			// e.g. Content-Type: text/html; charset=iso-8859-1
			Pattern pattern = Pattern.compile(".*;\\s*charset=(\\S+).*", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(contentType);
			
			if (matcher.matches()) {
				String charsetName = matcher.group(1);
				
				try {
					return Charset.forName(charsetName);
				} catch (Exception e) {
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, e.getMessage());
				}
			}
		}
		
		// use UTF-8 if charset cannot be determined
		return Charset.forName("UTF-8");
	}
	

	public static Document getHtmlDocument(URI uri) throws IOException, SAXException {
		return getHtmlDocument(uri.toURL());
	}
	

	public static Document getHtmlDocument(URL url) throws IOException, SAXException {
		return getHtmlDocument(url.openConnection());
	}
	

	public static Document getHtmlDocument(URLConnection connection) throws IOException, SAXException {
		Charset charset = getCharset(connection.getContentType());
		String encoding = connection.getContentEncoding();
		InputStream inputStream = connection.getInputStream();
		
		if ((encoding != null) && encoding.equalsIgnoreCase("gzip"))
			inputStream = new GZIPInputStream(inputStream);
		
		return getHtmlDocument(new InputStreamReader(inputStream, charset));
	}
	

	public static Document getHtmlDocument(Reader reader) throws SAXException, IOException {
		DOMParser parser = new DOMParser();
		parser.setFeature("http://xml.org/sax/features/namespaces", false);
		parser.parse(new InputSource(reader));
		
		return parser.getDocument();
	}
	
}
