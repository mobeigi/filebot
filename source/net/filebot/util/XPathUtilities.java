package net.filebot.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XPathUtilities {

	public static Node selectNode(String xpath, Object node) {
		return (Node) evaluateXPath(xpath, node, XPathConstants.NODE);
	}

	public static List<Node> selectNodes(String xpath, Object node) {
		return new NodeListDecorator((NodeList) evaluateXPath(xpath, node, XPathConstants.NODESET));
	}

	public static String selectString(String xpath, Object node) {
		return ((String) evaluateXPath(xpath, node, XPathConstants.STRING)).trim();
	}

	public static List<String> selectStrings(String xpath, Object node) {
		List<String> values = new ArrayList<String>();
		for (Node it : selectNodes(xpath, node)) {
			String textContent = getTextContent(it);
			if (textContent.length() > 0) {
				values.add(textContent);
			}
		}
		return values;
	}

	/**
	 * @param nodeName
	 *            search for nodes with this name
	 * @param parentNode
	 *            search in the child nodes of this nodes
	 * @return text content of the child node or null if no child with the given name was found
	 */
	public static Node getChild(String nodeName, Node parentNode) {
		for (Node child : new NodeListDecorator(parentNode.getChildNodes())) {
			if (nodeName.equals(child.getNodeName()))
				return child;
		}

		return null;
	}

	public static List<Node> getChildren(String nodeName, Node parentNode) {
		List<Node> children = new ArrayList<Node>();

		if (parentNode != null) {
			for (Node child : new NodeListDecorator(parentNode.getChildNodes())) {
				if (nodeName.equals(child.getNodeName()))
					children.add(child);
			}
		}

		return children;
	}

	public static String getAttribute(String attribute, Node node) {
		Node attributeNode = node.getAttributes().getNamedItem(attribute);

		if (attributeNode != null)
			return attributeNode.getNodeValue().trim();

		return null;
	}

	/**
	 * Get text content of the first child node matching the given node name. Use this method instead of {@link #selectString(String, Object)} whenever xpath support is not required, because it is much faster, especially for large documents.
	 *
	 * @param childName
	 *            search for nodes with this name
	 * @param parentNode
	 *            search in the child nodes of this nodes
	 * @return text content of the child node or null if no child with the given name was found
	 */
	public static String getTextContent(String childName, Node parentNode) {
		Node child = getChild(childName, parentNode);

		if (child == null) {
			return null;
		}

		return getTextContent(child);
	}

	public static String getTextContent(Node node) {
		StringBuilder sb = new StringBuilder();

		for (Node textNode : getChildren("#text", node)) {
			sb.append(textNode.getNodeValue());
		}

		return sb.toString().trim();
	}

	public static List<String> getListContent(String childName, String delimiter, Node parentNode) {
		List<String> list = new ArrayList<String>();
		for (Node node : getChildren(childName, parentNode)) {
			String textContent = getTextContent(node);
			if (textContent != null && textContent.length() > 0) {
				if (delimiter == null) {
					list.add(textContent);
				} else {
					for (String it : textContent.split(delimiter)) {
						it = it.trim();
						if (it.length() > 0) {
							list.add(it);
						}
					}
				}
			}
		}
		return list;
	}

	public static Double getDecimal(String textContent) {
		try {
			return new Double(textContent);
		} catch (NumberFormatException | NullPointerException e) {
			return null;
		}
	}

	public static Object evaluateXPath(String xpath, Object item, QName returnType) {
		try {
			return XPathFactory.newInstance().newXPath().compile(xpath).evaluate(item, returnType);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Dummy constructor to prevent instantiation.
	 */
	private XPathUtilities() {
		throw new UnsupportedOperationException();
	}

	protected static class NodeListDecorator extends AbstractList<Node> {

		private final NodeList nodes;

		public NodeListDecorator(NodeList nodes) {
			this.nodes = nodes;
		}

		@Override
		public Node get(int index) {
			return nodes.item(index);
		}

		@Override
		public int size() {
			return nodes.getLength();
		}

	}

}
