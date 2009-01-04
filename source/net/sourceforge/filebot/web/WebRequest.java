
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


public final class WebRequest {
	
	public static Document getHtmlDocument(URL url) throws IOException, SAXException {
		return getHtmlDocument(url.openConnection());
	}
	

	public static Document getHtmlDocument(URLConnection connection) throws IOException, SAXException {
		return getHtmlDocument(getReader(connection));
	}
	

	public static Reader getReader(URLConnection connection) throws IOException {
		Charset charset = getCharset(connection.getContentType());
		String encoding = connection.getContentEncoding();
		InputStream inputStream = connection.getInputStream();
		
		if ((encoding != null) && encoding.equalsIgnoreCase("gzip"))
			inputStream = new GZIPInputStream(inputStream);
		
		return new InputStreamReader(inputStream, charset);
	}
	

	public static Document getHtmlDocument(Reader reader) throws SAXException, IOException {
		DOMParser parser = new DOMParser();
		parser.setFeature("http://xml.org/sax/features/namespaces", false);
		parser.parse(new InputSource(reader));
		
		return parser.getDocument();
	}
	

	public static Document getDocument(URL url) throws SAXException, IOException, ParserConfigurationException {
		return getDocument(url.toString());
	}
	

	public static Document getDocument(String url) throws SAXException, IOException, ParserConfigurationException {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(url);
	}
	

	public static Document getDocument(InputStream inputStream) throws SAXException, IOException, ParserConfigurationException {
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
	}
	

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
					Logger.getLogger("global").log(Level.WARNING, e.getMessage());
				}
			}
		}
		
		// use UTF-8 if charset cannot be determined
		return Charset.forName("UTF-8");
	}
	

	private WebRequest() {
		throw new UnsupportedOperationException();
	}
}
