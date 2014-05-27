package com.lordofthejars.docstract;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.antlrjavaparser.ParseException;
import com.lordofthejars.docstract.IOUtils;
import com.lordofthejars.docstract.RenderCommand;

public class RenderCommandTest {

    private static final String SEPARATOR = System.getProperty("line.separator");
    
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	
	@Test
    public void should_render_javadoc_content_with_autonumerical_callouts_as_AsciiDoc_file() throws IOException {
        
        RenderCommand renderCommand = new RenderCommand();
        
        File outputFile = temporaryFolder.newFile("output.adoc");
        renderCommand.render(new File("src/test/java/com/lordofthejars/docstract/AutonumericalCallout.java"), outputFile, new File("."));
        
        String output = readFull(outputFile).trim();
        
        assertThat(output, is("[source, java]"+SEPARATOR+"----"+SEPARATOR+"public void code() {"+SEPARATOR+"        "+SEPARATOR+"        System.out.println(\"Hello World\"); //  <1>"+SEPARATOR+"        "+SEPARATOR+"    }"+SEPARATOR+"----"+SEPARATOR+"<1> Prints Hello World"));
    }
	
	@Test
	public void should_render_javadoc_content_with_callouts_as_AsciiDoc_file() throws IOException {
	    
	    RenderCommand renderCommand = new RenderCommand();
        
        File outputFile = temporaryFolder.newFile("output.adoc");
        renderCommand.render(new File("src/test/java/com/lordofthejars/docstract/Callouts.java"), outputFile, new File("."));
        
        String output = readFull(outputFile).trim();
        
        assertThat(output, containsString("// <1>"));
        assertThat(output, containsString("<1> Prints Hello World"));
        
	}
	
	@Test
	public void should_render_javadoc_content_as_AsciiDoc_file() throws ParseException, FileNotFoundException, IOException {
		
		RenderCommand renderCommand = new RenderCommand();
		
		File outputFile = temporaryFolder.newFile("output.adoc");
		renderCommand.render(new File("src/test/java/com/lordofthejars/docstract/MM.java"), outputFile, new File("."));
		
		String output = readFull(outputFile).trim();
		
		assertThat(output, containsString("My name is *Alex*."));
		assertThat(output, containsString("public interface MyInterface"));
		assertThat(output, containsString("public String getId()"));
		assertThat(output, containsString("public void computeSomething(int a, int b)"));
		
	}
	
	@Test
    public void should_render_javadoc_with_xml_files_content_as_AsciiDoc_file() throws ParseException, FileNotFoundException, IOException {
        
        RenderCommand renderCommand = new RenderCommand();
        
        File outputFile = temporaryFolder.newFile("output.adoc");
        renderCommand.render(new File("src/test/java/com/lordofthejars/docstract/Xml.java"), outputFile, new File("."));
        
        String output = IOUtils.readFull(outputFile);
        
       
        assertThat(output, containsString("<1> defines the server name"));
        assertThat(output, containsString("<!--1-->"));
        assertThat(output, containsString("<name>b</name>"));
        assertThat(output, containsString("<name>c</name>"));
        
    }
	
	@Test
    public void should_render_javadoc_with_autonumerical_xml_files_content_as_AsciiDoc_file() throws ParseException, FileNotFoundException, IOException {
        
        RenderCommand renderCommand = new RenderCommand();
        
        File outputFile = temporaryFolder.newFile("output.adoc");
        renderCommand.render(new File("src/test/java/com/lordofthejars/docstract/XmlAutonumerical.java"), outputFile, new File("."));
        
        String output = IOUtils.readFull(outputFile);
        
       
        assertThat(output, containsString("<1> defines the server name"));
        assertThat(output, containsString("<!--1-->"));
        assertThat(output, containsString("<name>b</name>"));
        assertThat(output, containsString("<name>c</name>"));
        
    }
	
	@Test
    public void should_render_javadoc_with_xml_namespace_files_content_as_AsciiDoc_file() throws ParseException, FileNotFoundException, IOException {
        
        RenderCommand renderCommand = new RenderCommand();
        
        File outputFile = temporaryFolder.newFile("output.adoc");
        renderCommand.render(new File("src/test/java/com/lordofthejars/docstract/XmlNamespace.java"), outputFile, new File("."));
        
        String output = IOUtils.readFull(outputFile);
        
        assertThat(output, containsString("<groupId>junit</groupId>"));
        assertThat(output, containsString("<artifactId>junit</artifactId>"));
        assertThat(output, containsString("<version>${version.junit}</version>"));
        assertThat(output, containsString("xmlns=\"http://maven.apache.org/POM/4.0.0\""));
        
    }
	
	private String readFull(File file) throws IOException {
		byte[] content = new byte[(int) file.length()];
		
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read(content);
		fileInputStream.close();
		
		return new String(content);
		
	}
	
}
