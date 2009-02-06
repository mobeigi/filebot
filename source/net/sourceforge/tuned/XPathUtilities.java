
package net.sourceforge.tuned;


import java.util.AbstractList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public final class XPathUtilities {
	
	public static Node selectNode(String xpath, Object node) {
		try {
			return (Node) getXPath(xpath).evaluate(node, XPathConstants.NODE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	public static List<Node> selectNodes(String xpath, Object node) {
		try {
			return new NodeListDecorator((NodeList) getXPath(xpath).evaluate(node, XPathConstants.NODESET));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	public static String selectString(String xpath, Object node) {
		try {
			return ((String) getXPath(xpath).evaluate(node, XPathConstants.STRING)).trim();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	/**
	 * @param nodeName search for nodes with this name
	 * @param parentNode search in the child nodes of this nodes
	 * @return text content of the child node or null if no child with the given name was found
	 */
	public static Node getChild(String nodeName, Node parentNode) {
		for (Node child : new NodeListDecorator(parentNode.getChildNodes())) {
			if (nodeName.equals(child.getNodeName()))
				return child;
		}
		
		return null;
	}
	

	/**
	 * Get text content of the first child node matching the given node name. Use this method
	 * instead of {@link #selectString(String, Object)} whenever xpath support is not required,
	 * because it is much faster, especially for large documents.
	 * 
	 * @param nodeName search for nodes with this name
	 * @param parentNode search in the child nodes of this nodes
	 * @return text content of the child node or null if no child with the given name was found
	 */
	public static String getTextContent(String nodeName, Node parentNode) {
		Node child = getChild(nodeName, parentNode);
		
		if (child == null)
			return null;
		
		return child.getTextContent();
	}
	

	public static int selectInteger(String xpath, Object node) {
		return Integer.parseInt(selectString(xpath, node));
	}
	

	public static boolean exists(String xpath, Object node) {
		return selectNode(xpath, node) != null;
	}
	

	private static XPathExpression getXPath(String xpath) throws XPathExpressionException {
		return XPathFactory.newInstance().newXPath().compile(xpath);
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
