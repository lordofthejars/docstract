package com.lordofthejars.docstract;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.lordofthejars.docstract.XmlUtils;

public class XmlUtilsTest {

    private static final String separator = System.getProperty("line.separator");
    
    @Test
    public void should_execute_xpath_expressions_to_xml_files() throws XPathExpressionException, SAXException, IOException, ParserConfigurationException, TransformerException {
        
        InputStream content = Class.class.getResourceAsStream("/content.xml");
        
        String[] result = XmlUtils.executeXPathExpression(new InputSource(content), "/servers/name", null);
        assertThat(Arrays.asList(result), hasItems("<name>b</name>"+separator,"<name>c</name>"+separator));
        
    }
    
    @Test
    public void should_execute_xpath_expressions_to_namespaced_xml_files() throws XPathExpressionException, SAXException, IOException, ParserConfigurationException, TransformerException {
        
        InputStream content = Class.class.getResourceAsStream("/maven.xml");
        
        String[] result = XmlUtils.executeXPathExpression(new InputSource(content), "/mvn:project/mvn:dependencies/mvn:dependency[mvn:groupId[contains(., 'junit')]]", "xmlns:mvn=http://maven.apache.org/POM/4.0.0");
        
        assertThat(Arrays.asList(result), hasItems("<dependency xmlns=\"http://maven.apache.org/POM/4.0.0\">" + separator + 
                "    <groupId>junit</groupId>" + separator + 
                "    <artifactId>junit</artifactId>" + separator + 
                "    <version>${version.junit}</version>" + separator + 
                "    <scope>test</scope>" + separator + 
                "</dependency>" + separator));
        
    }
    
}
