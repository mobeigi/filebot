
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import net.sourceforge.tuned.XPathUtil;

import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


class HtmlUtil {
	
	private static Charset getCharset(String contentType) {
		if (contentType != null) {
			// e.g. Content-Type: text/html; charset=iso-8859-1
			Pattern pattern = Pattern.compile(".*;\\s*charset=(\\S+).*", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(contentType);
			
			if (matcher.matches()) {
				String charsetName = matcher.group(1);
				return Charset.forName(charsetName);
			}
		}
		
		// use UTF-8 if charset cannot be determined
		return Charset.forName("UTF-8");
	}
	

	public static Document getHtmlDocument(URL url) throws IOException, SAXException {
		URLConnection connection = url.openConnection();
		
		Charset charset = getCharset(connection.getContentType());
		String encoding = connection.getContentEncoding();
		InputStream inputStream = connection.getInputStream();
		
		if (encoding != null && encoding.equalsIgnoreCase("gzip"))
			inputStream = new GZIPInputStream(inputStream);
		
		return getHtmlDocument(new InputStreamReader(inputStream, charset));
	}
	

	public static Document getHtmlDocument(Reader reader) throws SAXException, IOException {
		DOMParser parser = new DOMParser();
		parser.parse(new InputSource(reader));
		
		return parser.getDocument();
	}
	

	public static String selectString(String xpath, Node node) {
		return XPathUtil.selectString(xpath, node, "html", getNameSpace(node)).trim();
	}
	

	public static List<Node> selectNodes(String xpath, Node node) {
		return XPathUtil.selectNodes(xpath, node, "html", getNameSpace(node));
	}
	

	public static Node selectNode(String xpath, Node node) {
		return XPathUtil.selectNode(xpath, node, "html", getNameSpace(node));
	}
	

	private static String getNameSpace(Node node) {
		if (node instanceof Document) {
			// select root element
			return XPathUtil.selectNode("/*", node, null, null).getNamespaceURI();
		}
		
		return node.getNamespaceURI();
	}
	
}
