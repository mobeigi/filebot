
package net.sourceforge.tuned;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XPathUtil {
	
	public static Node selectNode(String xpath, Object node, String namespacePrefix, String namespace) {
		try {
			XPath xp = createXPath(namespacePrefix, namespace);
			
			return (Node) xp.evaluate(xpath, node, XPathConstants.NODE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	public static List<Node> selectNodes(String xpath, Object node, String namespacePrefix, String namespace) {
		try {
			XPath xp = createXPath(namespacePrefix, namespace);
			
			NodeList nodeList = (NodeList) xp.evaluate(xpath, node, XPathConstants.NODESET);
			
			ArrayList<Node> nodes = new ArrayList<Node>(nodeList.getLength());
			
			for (int i = 0; i < nodeList.getLength(); i++) {
				nodes.add(nodeList.item(i));
			}
			
			return nodes;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	public static String selectString(String xpath, Object node, String namespacePrefix, String namespace) {
		try {
			XPath xp = createXPath(namespacePrefix, namespace);
			return (String) xp.evaluate(xpath, node, XPathConstants.STRING);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	private static XPath createXPath(String namespacePrefix, String namespace) {
		XPath xp = XPathFactory.newInstance().newXPath();
		
		if (namespacePrefix != null && namespace != null) {
			xp.setNamespaceContext(new NamespaceContextProvider(namespacePrefix, namespace));
		}
		
		return xp;
	}
	
	
	private static class NamespaceContextProvider implements NamespaceContext {
		
		String boundPrefix;
		String boundURI;
		
		
		NamespaceContextProvider(String prefix, String URI) {
			boundPrefix = prefix;
			boundURI = URI;
		}
		

		public String getNamespaceURI(String prefix) {
			if (prefix.equals(boundPrefix)) {
				return boundURI;
			} else if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
				return XMLConstants.XML_NS_URI;
			} else if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
				return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
			} else {
				return XMLConstants.NULL_NS_URI;
			}
		}
		

		public String getPrefix(String namespaceURI) {
			if (namespaceURI.equals(boundURI)) {
				return boundPrefix;
			} else if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
				return XMLConstants.XML_NS_PREFIX;
			} else if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
				return XMLConstants.XMLNS_ATTRIBUTE;
			} else {
				return null;
			}
		}
		

		@SuppressWarnings("unchecked")
		public Iterator getPrefixes(String namespaceURI) {
			return null;
		}
	}
	
}
