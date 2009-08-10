
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import net.sourceforge.tuned.ByteBufferOutputStream;


public final class WebRequest {
	
	public static Document getHtmlDocument(URL url) throws IOException, SAXException {
		return getHtmlDocument(url.openConnection());
	}
	

	public static Document getHtmlDocument(URLConnection connection) throws IOException, SAXException {
		return getHtmlDocument(getReader(connection));
	}
	

	public static Reader getReader(URLConnection connection) throws IOException {
		try {
			connection.addRequestProperty("Accept-Encoding", "gzip,deflate");
			connection.addRequestProperty("Accept-Charset", "UTF-8,ISO-8859-1");
		} catch (IllegalStateException e) {
			// too bad, can't request gzipped document anymore
		}
		
		Charset charset = getCharset(connection.getContentType());
		String encoding = connection.getContentEncoding();
		
		InputStream inputStream = connection.getInputStream();
		
		if ("gzip".equalsIgnoreCase(encoding))
			inputStream = new GZIPInputStream(inputStream);
		else if ("deflate".equalsIgnoreCase(encoding)) {
			inputStream = new InflaterInputStream(inputStream, new Inflater(true));
		}
		
		return new InputStreamReader(inputStream, charset);
	}
	

	public static Document getHtmlDocument(Reader reader) throws SAXException, IOException {
		DOMParser parser = new DOMParser();
		parser.setFeature("http://xml.org/sax/features/namespaces", false);
		parser.parse(new InputSource(reader));
		
		return parser.getDocument();
	}
	

	public static Document getDocument(URL url) throws IOException, SAXException {
		return getDocument(new InputSource(getReader(url.openConnection())));
	}
	

	public static Document getDocument(InputStream inputStream) throws SAXException, IOException, ParserConfigurationException {
		return getDocument(new InputSource(inputStream));
	}
	

	public static Document getDocument(InputSource source) throws IOException, SAXException {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
		} catch (ParserConfigurationException e) {
			// will never happen
			throw new RuntimeException(e);
		}
	}
	

	public static ByteBuffer fetch(URL resource) throws IOException {
		return fetch(resource, null);
	}
	

	public static ByteBuffer fetch(URL url, Map<String, String> requestParameters) throws IOException {
		URLConnection connection = url.openConnection();
		
		if (requestParameters != null) {
			for (Entry<String, String> parameter : requestParameters.entrySet()) {
				connection.addRequestProperty(parameter.getKey(), parameter.getValue());
			}
		}
		
		int contentLength = connection.getContentLength();
		
		InputStream in = connection.getInputStream();
		ByteBufferOutputStream buffer = new ByteBufferOutputStream(contentLength >= 0 ? contentLength : 32 * 1024);
		
		try {
			// read all
			buffer.transferFully(in);
		} catch (IOException e) {
			// if the content length is not known in advance an IOException (Premature EOF) 
			// is always thrown after all the data has been read
			if (contentLength >= 0) {
				throw e;
			}
		} finally {
			in.close();
		}
		
		return buffer.getByteBuffer();
	}
	

	private static Charset getCharset(String contentType) {
		if (contentType != null) {
			// e.g. Content-Type: text/html; charset=iso-8859-1
			Matcher matcher = Pattern.compile("charset=(\\p{Graph}+)").matcher(contentType);
			
			if (matcher.find()) {
				try {
					return Charset.forName(matcher.group(1));
				} catch (IllegalArgumentException e) {
					Logger.getLogger(WebRequest.class.getName()).log(Level.WARNING, e.getMessage());
				}
			}
		}
		
		// use http default encoding if charset cannot be determined
		return Charset.forName("ISO-8859-1");
	}
	

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private WebRequest() {
		throw new UnsupportedOperationException();
	}
}
