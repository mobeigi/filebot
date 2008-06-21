
package net.sourceforge.tuned;


import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XPathUtil {
	
	public static Node selectNode(String xpath, Object node) {
		try {
			return (Node) getXPath(xpath).evaluate(node, XPathConstants.NODE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	public static Node selectFirstNode(String xpath, Object node) {
		try {
			NodeList nodeList = (NodeList) getXPath(xpath).evaluate(node, XPathConstants.NODESET);
			
			if (nodeList.getLength() <= 0)
				return null;
			
			return nodeList.item(0);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	public static List<Node> selectNodes(String xpath, Object node) {
		try {
			NodeList nodeList = (NodeList) getXPath(xpath).evaluate(node, XPathConstants.NODESET);
			
			ArrayList<Node> nodes = new ArrayList<Node>(nodeList.getLength());
			
			for (int i = 0; i < nodeList.getLength(); i++) {
				nodes.add(nodeList.item(i));
			}
			
			return nodes;
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
	

	public static int selectInteger(String xpath, Object node) {
		return Integer.parseInt(selectString(xpath, node));
	}
	

	private static XPathExpression getXPath(String xpath) throws XPathExpressionException {
		return XPathFactory.newInstance().newXPath().compile(xpath);
	}
	
}
