package com.lordofthejars.asciidoctorfy;

import static com.lordofthejars.asciidoctorfy.IOUtils.NEW_LINE;
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

	private static final String EXPECTED_OUTPUT = "My name is *Alex*." +NEW_LINE+ 
			"I am 33 years _old_." + NEW_LINE + 
			NEW_LINE + 
			"Jump Jump +!!+." + NEW_LINE + 
			NEW_LINE + 
			"[source, java]" + NEW_LINE + 
			"----" + NEW_LINE + 
			"public interface MyInterface {" + NEW_LINE + 
			NEW_LINE + 
			"	void loginSuccess(Object hash);" + NEW_LINE + 
			"	void loginSuccess();" + NEW_LINE + 
			"	void foo(int number, Object block);" + NEW_LINE + 
			"	" + NEW_LINE + 
			"}" + NEW_LINE + 
			"----" + NEW_LINE + 
			NEW_LINE + 
			"[source, java]" + NEW_LINE + 
			"----" + NEW_LINE + 
			"public String getId() {" + NEW_LINE+ 
			"	return id;" + NEW_LINE + 
			"}" + NEW_LINE + 
			"----";
	
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	
	@Test
	public void should_render_javadoc_content_as_AsciiDoc_file() throws ParseException, FileNotFoundException, IOException {
		
		RenderCommand renderCommand = new RenderCommand();
		
		File outputFile = temporaryFolder.newFile("output.adoc");
		renderCommand.render(new File("src/test/java/com/lordofthejars/asciidoctorfy/MM.java"), outputFile, new File("."));
		
		
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
