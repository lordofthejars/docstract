package com.lordofthejars.asciidoctorfy;

import static com.lordofthejars.asciidoctorfy.IOUtils.NEW_LINE;
import static com.lordofthejars.asciidoctorfy.IOUtils.readFull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.github.antlrjavaparser.ParseException;

public class JavaSourceReader {

    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
    private static final String START_COMMENT = "/**";
    private static final String END_COMMENT = "*/";
    private static final String COMMENT_SYMBOL = "*";
    private static final String INCLUDE_JAVA = "include::[\\w(),\\s/#]+\\.java\\[\\w*\\]";
    private static final String INCLUDE_XML = "include::[\\w/]+\\.xml\\[.*\\]";
    private static final String METHOD_KEY = "method";
    private static final String METHOD_SEPARATOR = "#";
    private static final String CLASS_KEY = "class";
    
    private StringBuilder content = new StringBuilder();

    private File baseDir = new File(".");

    public JavaSourceReader() {
        super();
    }

    public JavaSourceReader(File baseDir) {
        this.baseDir = baseDir;
    }

    public String generateDoc(InputStream javaFile) throws ParseException, IOException {

        String[] fullClass = readFull(javaFile);

        boolean insideCommentBlock = false;

        for (String line : fullClass) {

            String trimedLine = line.trim();

            if (isStartingComments(trimedLine)) {
                insideCommentBlock = true;
            }

            if (isEndingComments(insideCommentBlock, trimedLine)) {
                insideCommentBlock = false;
            }

            if (isInsideComments(insideCommentBlock, trimedLine)) {
                final String asciidocLine = trimedLine.substring(1).trim();
                resolveAsciiDocLine(asciidocLine);
            }

        }

        return content.toString();

    }

    private void resolveAsciiDocLine(final String asciidocLine) throws FileNotFoundException, IOException {
       
        if (isAJavaIncludeSentence(asciidocLine)) {
        
            resolveJavaInclude(asciidocLine);
        
        } else {
        
            if (isAnXmlIncludeSentence(asciidocLine)) {
            
                String fileName = getFilenamePath(asciidocLine);

                if (isIncludeWithXPath(asciidocLine)) {

                    String xmlContent = executeXpathExpression(asciidocLine, fileName);
                    appendXmlSourceCode(xmlContent);

                } else {
                    appendXmlSourceCode(IOUtils.readFull(new File(this.baseDir, fileName)));
                }
            } else {
               
                if (isEmptyCommentLine(asciidocLine)) {
                    content.append(NEW_LINE);
                } else {
                    content.append(asciidocLine).append(NEW_LINE);
                }
            }
        }
    }

    private void appendXmlSourceCode(String sourceCode) {
        content.append("[source, xml]").append(NEW_LINE);
        content.append("----").append(NEW_LINE);
        content.append(sourceCode.trim()).append(NEW_LINE);
        content.append("----").append(NEW_LINE);
    }
    
    private String executeXpathExpression(final String asciidocLine, String fileName) throws FileNotFoundException {

        StringBuilder xmlContent = new StringBuilder();

        XPath xpath = XPathFactory.newInstance().newXPath();
        String expression = getXpathExpression(asciidocLine);
        InputSource inputSource = new InputSource(new FileInputStream(new File(this.baseDir, fileName)));

        try {
            
            NodeList nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                xmlContent.append(printNode(nodes.item(i))).append(NEW_LINE);
            }

            return xmlContent.toString();

        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        } catch (TransformerException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getXpathExpression(String asciidocLine) {
        return asciidocLine.substring(asciidocLine.indexOf(OPEN_BRACKET) + 1, asciidocLine.lastIndexOf(CLOSE_BRACKET));
    }

    private boolean isEmptyCommentLine(final String asciidocLine) {
        return asciidocLine.length() == 0;
    }

    private void resolveJavaInclude(final String asciidocLine) throws FileNotFoundException, IOException {
        
        String fileName = getFilenamePath(asciidocLine);

        if (isIncludeWithMethod(fileName)) {
            includeJavaMethod(fileName);
        } else {
            includeJavaClass(fileName);
        }
    }

    private void includeJavaClass(String fileName) throws FileNotFoundException, IOException {
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(CLASS_KEY, "");

        InputStream fileInputStream = new FileInputStream(new File(this.baseDir, fileName));

        appendSourceCode(attributes, fileInputStream);
    }

    private void appendSourceCode(Map<String, Object> attributes, InputStream fileInputStream) throws IOException {
        
        Java7Parser java7Parser = new Java7Parser();
        ContentAndCallouts extractedContent = java7Parser.extract(fileInputStream, attributes);
        
        content.append("[source, java]").append(NEW_LINE);
        content.append("----").append(NEW_LINE);
        content.append(extractedContent.getContent().trim()).append(NEW_LINE);
        content.append("----").append(NEW_LINE);
        String callouts = extractedContent.getCallouts().trim();
        
        if(!"".equals(callouts)) {
            content.append(callouts).append(NEW_LINE);
        }
    }

    private void includeJavaMethod(String fileName) throws FileNotFoundException, IOException {

        final int methodSeparator = fileName.lastIndexOf(METHOD_SEPARATOR);
        String classLocation = fileName.substring(0, methodSeparator);
        final int extensionSeparator = fileName.lastIndexOf(".");
        String method = fileName.substring(methodSeparator + 1, extensionSeparator);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put(METHOD_KEY, method);

        InputStream fileInputStream = new FileInputStream(new File(baseDir, classLocation + ".java"));

        appendSourceCode(attributes, fileInputStream);
    }

    private boolean isIncludeWithXPath(String fileName) {
        // Inspect if there is content between []
        return fileName.indexOf(CLOSE_BRACKET) - fileName.indexOf(OPEN_BRACKET) > 1;
    }

    private String printNode(Node rootNode) throws TransformerException {

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

    private boolean isIncludeWithMethod(String fileName) {
        return fileName.contains(METHOD_SEPARATOR);
    }

    private String getFilenamePath(final String asciidocLine) {
        return asciidocLine.substring(9, asciidocLine.lastIndexOf(OPEN_BRACKET));
    }

    private boolean isAnXmlIncludeSentence(final String asciidocLine) {
        return asciidocLine.matches(INCLUDE_XML);
    }

    private boolean isAJavaIncludeSentence(final String asciidocLine) {
        return asciidocLine.matches(INCLUDE_JAVA);
    }

    private boolean isInsideComments(boolean insideCommentBlock, String trimedLine) {
        return trimedLine.startsWith(COMMENT_SYMBOL) && insideCommentBlock;
    }

    private boolean isEndingComments(boolean insideCommentBlock, String trimedLine) {
        return trimedLine.startsWith(END_COMMENT) && insideCommentBlock;
    }

    private boolean isStartingComments(String trimedLine) {
        return trimedLine.startsWith(START_COMMENT);
    }

}
