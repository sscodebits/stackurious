package com.shikha.stackoverflow.util;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Parses xml input files using Javax xml parser
 * @author shikha
 *
 */
public class ParseUtil {
	public static final String rowTag = "row";
	
	public static Element parseString(String xml) {
		Document d = parseDocument(xml);
		if (d != null) {
			NodeList nList = d.getElementsByTagName(rowTag);
			if (nList != null && nList.getLength() > 0) {
				Node node = nList.item(0);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) node;
					return e;
				}
			}
		}
		return null;
	}
	
	public static Document parseDocument(String xml) {
		try {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
	}

	public static long parseLong(Element e, String attr) {
		if (e.getAttribute(attr).isEmpty()) {
			return 0;
		} else {
			return Long.parseLong(e.getAttribute(attr));
		}
	}
}
