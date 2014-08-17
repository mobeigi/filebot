package net.filebot.web;

import static net.filebot.util.FileUtilities.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.filebot.util.ByteBufferOutputStream;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class WebRequest {

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

	public static Document getDocument(URL url) throws IOException, SAXException {
		return getDocument(url.openConnection());
	}

	public static Document getDocument(URLConnection connection) throws IOException, SAXException {
		return getDocument(new InputSource(getReader(connection)));
	}

	public static Document getDocument(String xml) throws IOException, SAXException {
		return getDocument(new InputSource(new StringReader(xml)));
	}

	public static Document getDocument(InputSource source) throws IOException, SAXException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setFeature("http://xml.org/sax/features/namespaces", false);
			factory.setFeature("http://xml.org/sax/features/validation", false);
			return factory.newDocumentBuilder().parse(source);
		} catch (ParserConfigurationException e) {
			// will never happen
			throw new RuntimeException(e);
		}
	}

	public static ByteBuffer fetch(URL resource) throws IOException {
		return fetch(resource, 0, null, null);
	}

	public static ByteBuffer fetchIfModified(URL resource, long ifModifiedSince) throws IOException {
		return fetch(resource, ifModifiedSince, null, null);
	}

	public static ByteBuffer fetch(URL url, long ifModifiedSince, Map<String, String> requestParameters, Map<String, List<String>> responseParameters) throws IOException {
		URLConnection connection = url.openConnection();
		if (ifModifiedSince > 0) {
			connection.setIfModifiedSince(ifModifiedSince);
		}

		try {
			connection.addRequestProperty("Accept-Encoding", "gzip,deflate");
			connection.addRequestProperty("Accept-Charset", "UTF-8");
		} catch (IllegalStateException e) {
			// too bad, can't request gzipped data
		}

		if (requestParameters != null) {
			for (Entry<String, String> parameter : requestParameters.entrySet()) {
				connection.addRequestProperty(parameter.getKey(), parameter.getValue());
			}
		}

		int contentLength = connection.getContentLength();
		String encoding = connection.getContentEncoding();

		InputStream in = connection.getInputStream();
		if ("gzip".equalsIgnoreCase(encoding))
			in = new GZIPInputStream(in);
		else if ("deflate".equalsIgnoreCase(encoding)) {
			in = new InflaterInputStream(in, new Inflater(true));
		}

		// store response headers
		if (responseParameters != null) {
			responseParameters.putAll(connection.getHeaderFields());
		}

		ByteBufferOutputStream buffer = new ByteBufferOutputStream(contentLength >= 0 ? contentLength : 4 * 1024);
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

		// no data, e.g. If-Modified-Since requests
		if (contentLength < 0 && buffer.getByteBuffer().remaining() == 0)
			return null;

		return buffer.getByteBuffer();
	}

	public static ByteBuffer post(URL url, Map<String, ?> parameters, Map<String, String> requestParameters) throws IOException {
		byte[] postData = encodeParameters(parameters, true).getBytes("UTF-8");
		if (requestParameters != null && "gzip".equals(requestParameters.get("Content-Encoding"))) {
			postData = gzip(postData);
		}
		return post(url, postData, "application/x-www-form-urlencoded", requestParameters);
	}

	public static ByteBuffer post(URL url, byte[] postData, String contentType, Map<String, String> requestParameters) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		connection.addRequestProperty("Content-Length", String.valueOf(postData.length));
		connection.addRequestProperty("Content-Type", contentType);
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);

		if (requestParameters != null) {
			for (Entry<String, String> parameter : requestParameters.entrySet()) {
				connection.addRequestProperty(parameter.getKey(), parameter.getValue());
			}
		}

		// write post data
		OutputStream out = connection.getOutputStream();
		out.write(postData);
		out.close();

		// read response
		int contentLength = connection.getContentLength();
		String encoding = connection.getContentEncoding();

		InputStream in = connection.getInputStream();
		if ("gzip".equalsIgnoreCase(encoding))
			in = new GZIPInputStream(in);
		else if ("deflate".equalsIgnoreCase(encoding)) {
			in = new InflaterInputStream(in, new Inflater(true));
		}

		ByteBufferOutputStream buffer = new ByteBufferOutputStream(contentLength >= 0 ? contentLength : BUFFER_SIZE);
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

	public static int head(URL url) throws IOException {
		HttpURLConnection c = (HttpURLConnection) url.openConnection();
		c.setRequestMethod("HEAD");
		return c.getResponseCode();
	}

	public static String encodeParameters(Map<String, ?> parameters, boolean unicode) {
		StringBuilder sb = new StringBuilder();

		for (Entry<String, ?> entry : parameters.entrySet()) {
			if (sb.length() > 0) {
				sb.append("&");
			}

			sb.append(entry.getKey());
			if (entry.getValue() != null) {
				sb.append("=");
				sb.append(encode(entry.getValue().toString(), unicode));
			}
		}

		return sb.toString();
	}

	private static byte[] gzip(byte[] data) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
		GZIPOutputStream gzip = new GZIPOutputStream(out);
		gzip.write(data);
		gzip.close();
		return out.toByteArray();
	}

	public static String encode(String string, boolean unicode) {
		try {
			return URLEncoder.encode(string, unicode ? "UTF-8" : "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static Charset getCharset(String contentType) {
		if (contentType != null) {
			// e.g. Content-Type: text/html; charset=iso-8859-1
			Matcher matcher = Pattern.compile("charset=[\"]?([\\p{Graph}&&[^\"]]+)[\"]?").matcher(contentType);

			if (matcher.find()) {
				try {
					return Charset.forName(matcher.group(1));
				} catch (IllegalArgumentException e) {
					Logger.getLogger(WebRequest.class.getName()).log(Level.WARNING, "Illegal charset: " + contentType);
				}
			}

			// use http default encoding only for text/html
			if (contentType.equals("text/html")) {
				return Charset.forName("ISO-8859-1");
			}
		}

		// use UTF-8 if we don't know any better
		return Charset.forName("UTF-8");
	}

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private WebRequest() {
		throw new UnsupportedOperationException();
	}
}
