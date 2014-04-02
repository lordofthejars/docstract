package com.lordofthejars.asciidoctorfy;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.github.antlrjavaparser.ParseException;
import com.lordofthejars.asciidoctorfy.RenderCommand;

public class RenderCommandTest {

	private static final String EXPECTED_OUTPUT = "My name is *Alex*.\r\n" + 
			"I am 33 years _old_.\r\n" + 
			"\r\n" + 
			"Jump Jump +!!+.\r\n" + 
			"\r\n" + 
			"[source, java]\r\n" + 
			"----\r\n" + 
			"public interface MyInterface {\r\n" + 
			"\r\n" + 
			"	void loginSuccess(Object hash);\r\n" + 
			"	void loginSuccess();\r\n" + 
			"	void foo(int number, Object block);\r\n" + 
			"	\r\n" + 
			"}\r\n" + 
			"----\r\n" + 
			"\r\n" + 
			"[source, java]\r\n" + 
			"----\r\n" + 
			"public String getId() {\r\n" + 
			"	return id;\r\n" + 
			"}\r\n" + 
			"----";
	
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	
	@Test
	public void should_render_javadoc_content_as_AsciiDoc_file() throws ParseException, FileNotFoundException, IOException {
		
		RenderCommand renderCommand = new RenderCommand();
		
		File outputFile = temporaryFolder.newFile("output.adoc");
		renderCommand.render(new File("src/test/java/com/lordofthejars/asciidoctorfy/MM.java"), outputFile);
		
		
		assertThat(readFull(outputFile).trim(), is(EXPECTED_OUTPUT));
		
	}
	
	private String readFull(File file) throws IOException {
		byte[] content = new byte[(int) file.length()];
		
		FileInputStream fileInputStream = new FileInputStream(file);
		fileInputStream.read(content);
		fileInputStream.close();
		
		return new String(content);
		
	}
	
}
