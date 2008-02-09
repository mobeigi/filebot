
package net.sourceforge.tuned;


import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XPathUtil {
	
	public static Node selectNode(String xpath, Object node) {
		try {
			XPath xp = XPathFactory.newInstance().newXPath();
			
			return (Node) xp.evaluate(xpath, node, XPathConstants.NODE);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	

	public static Node selectFirstNode(String xpath, Object node) {
		return selectNodes(xpath, node).get(0);
	}
	

	public static List<Node> selectNodes(String xpath, Object node) {
		try {
			XPath xp = XPathFactory.newInstance().newXPath();
			
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
	

	public static String selectString(String xpath, Object node) {
		try {
			XPath xp = XPathFactory.newInstance().newXPath();
			return ((String) xp.evaluate(xpath, node, XPathConstants.STRING)).trim();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
