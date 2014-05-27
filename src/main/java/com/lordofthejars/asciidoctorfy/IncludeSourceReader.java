package com.lordofthejars.asciidoctorfy;

import static com.lordofthejars.asciidoctorfy.IOUtils.NEW_LINE;
import static com.lordofthejars.asciidoctorfy.IOUtils.readFull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.antlrjavaparser.ParseException;

public class IncludeSourceReader {

    private static final String WHITE_SPACE = " ";
    private static final String XMLNS = "xmlns";
    private static final String OPEN_BRACKET = "[";
    private static final String CLOSE_BRACKET = "]";
    private static final String START_COMMENT = "/**";
    private static final String END_COMMENT = "*/";
    private static final String COMMENT_SYMBOL = "*";
    private static final String INCLUDE_JAVA = "include::[\\w(),\\s/#]+\\.java\\[\\w*\\]";
    private static final String INCLUDE_XML = "include::[\\w/]+\\.xml\\[.*\\]";
    private static final String XML_CALLOUT_PATTERN_LINE = ".*<!--\\s+[\\d+#]\\s+.*-->\\s*";
    private static final Pattern XML_CALLOUT_INDEX = Pattern.compile("<!--\\s+[\\d+#]\\s+");
    private static final String METHOD_KEY = "method";
    private static final String METHOD_SEPARATOR = "#";
    private static final String CLASS_KEY = "class";

    private StringBuilder content = new StringBuilder();

    private File baseDir = new File(".");

    public IncludeSourceReader() {
        super();
    }

    public IncludeSourceReader(File baseDir) {
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

                resolveXmlInclude(asciidocLine);

            } else {

                if (isEmptyCommentLine(asciidocLine)) {
                    content.append(NEW_LINE);
                } else {
                    content.append(asciidocLine).append(NEW_LINE);
                }
            }
        }
    }

    private void resolveXmlInclude(final String asciidocLine) throws FileNotFoundException, IOException {
        String fileName = getFilenamePath(asciidocLine);

        if (isIncludeWithXPath(asciidocLine)) {

            String[] xmlContent = executeXpathExpression(asciidocLine, fileName);
            appendXmlSourceCode(xmlContent);

        } else {
            appendXmlSourceCode(IOUtils.readFull(new FileInputStream(new File(this.baseDir, fileName))));
        }
    }

    private void appendXmlSourceCode(String[] sourceCodeLines) {

        StringBuilder callouts = new StringBuilder();

        int autonumericalCallout = 1;

        content.append("[source, xml]").append(NEW_LINE);
        content.append("----").append(NEW_LINE);

        for (String line : sourceCodeLines) {
            if (line.matches(XML_CALLOUT_PATTERN_LINE)) {

                Matcher matcher = XML_CALLOUT_INDEX.matcher(line);

                if (matcher.find()) {

                    int startCalloutIndex = matcher.start();
                    int endCalloutIndex = matcher.end();

                    String calloutNumber = line.substring(startCalloutIndex + 5, endCalloutIndex).trim();
                    String calloutComment = line.substring(endCalloutIndex, line.lastIndexOf(">") - 2).trim();

                    if ("#".equals(calloutNumber)) {
                        callouts.append("<").append(autonumericalCallout).append("> ").append(calloutComment)
                                .append(NEW_LINE);
                        calloutNumber = Integer.toString(autonumericalCallout);
                        autonumericalCallout++;
                    }
                    callouts.append("<").append(calloutNumber).append("> ").append(calloutComment).append(NEW_LINE);

                    content.append(line.substring(0, startCalloutIndex)).append("<!--").append(calloutNumber)
                            .append("-->").append(NEW_LINE);

                } else {
                    content.append(line).append(NEW_LINE);
                }

            } else {
                content.append(line).append(NEW_LINE);
            }
        }

        content.append("----").append(NEW_LINE);
        content.append(callouts.toString()).append(NEW_LINE);
    }

    private String[] executeXpathExpression(final String asciidocLine, String fileName) throws FileNotFoundException {

        String fullXpath = fullXpathExpression(asciidocLine);

        String expression = getXpathExpression(fullXpath);
        String namespace = getNamespace(fullXpath);

        InputSource inputSource = new InputSource(new FileInputStream(new File(this.baseDir, fileName)));

        try {
            return XmlUtils.executeXPathExpression(inputSource, expression, namespace);
        } catch (XPathExpressionException | SAXException | IOException | ParserConfigurationException
                | TransformerException e) {
            throw new IllegalArgumentException(e);
        }

    }

    private String fullXpathExpression(String asciidocLine) {
        return asciidocLine.substring(asciidocLine.indexOf(OPEN_BRACKET) + 1, asciidocLine.lastIndexOf(CLOSE_BRACKET))
                .trim();
    }

    private String getNamespace(String fullXpathExpression) {

        if (fullXpathExpression.startsWith(XMLNS)) {
            return fullXpathExpression.substring(0, fullXpathExpression.indexOf(WHITE_SPACE)).trim();
        }

        return null;

    }

    private String getXpathExpression(String fullXpathExpression) {

        if (fullXpathExpression.startsWith(XMLNS)) {
            return fullXpathExpression.substring(fullXpathExpression.indexOf(WHITE_SPACE) + 1,
                    fullXpathExpression.length()).trim();
        }

        return fullXpathExpression.trim();
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

        if (!"".equals(callouts)) {
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

    private boolean isIncludeWithMethod(String fileName) {
        return fileName.contains(METHOD_SEPARATOR);
    }

    private String getFilenamePath(final String asciidocLine) {
        return asciidocLine.substring(9, asciidocLine.indexOf(OPEN_BRACKET));
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
