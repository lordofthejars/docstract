package com.lordofthejars.asciidoctorfy;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlUtils {

    public static final String indenting(Node rootNode) throws XPathExpressionException, TransformerException {

        XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']", rootNode,
                XPathConstants.NODESET);

        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            node.getParentNode().removeChild(node);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter stringWriter = new StringWriter();
        StreamResult streamResult = new StreamResult(stringWriter);

        transformer.transform(new DOMSource(rootNode), streamResult);

        return stringWriter.toString();
    }

    public static final String[] executeXPathExpression(InputSource inputSource, String xpathExpression,
            String namespace) throws XPathExpressionException, SAXException, IOException, ParserConfigurationException, TransformerException {

        // optional namespace spec: xmlns:prefix:URI
        String nsPrefix = null;
        String nsUri = null;
        if (namespace != null && namespace.startsWith("xmlns:")) {
            String[] nsDef = namespace.substring("xmlns:".length()).split("=");
            if (nsDef.length == 2) {
                nsPrefix = nsDef[0];
                nsUri = nsDef[1];
            }
        }

        // Parse XML to DOM
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        Document doc = dbFactory.newDocumentBuilder().parse(inputSource);

        // Find nodes by XPATH
        XPathFactory xpFactory = XPathFactory.newInstance();
        XPath xpath = xpFactory.newXPath();

        // namespace?
        if (nsPrefix != null) {
            final String myPrefix = nsPrefix;
            final String myUri = nsUri;
            xpath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) {
                    return myPrefix.equals(prefix) ? myUri : null;
                }

                public String getPrefix(String namespaceURI) {
                    return null; // we are not using this.
                }

                public Iterator<?> getPrefixes(String namespaceURI) {
                    return null; // we are not using this.
                }
            });
        }

        XPathExpression expr = xpath.compile(xpathExpression);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        List<String> lines = new ArrayList<>();
        
        for (int i = 0; i < nodes.getLength(); i++) {
            lines.add((indenting(nodes.item(i))));
        }

        return lines.toArray(new String[lines.size()]);

    }

}
