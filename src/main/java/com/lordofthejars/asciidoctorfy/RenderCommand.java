package com.lordofthejars.asciidoctorfy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.tomitribe.crest.api.Command;
import org.tomitribe.crest.api.Default;
import org.tomitribe.crest.api.Option;
import org.tomitribe.crest.api.Required;

import com.github.antlrjavaparser.ParseException;

public class RenderCommand {
	
	
	@Command
	public void render(@Option("input") @Required File inputSourceFile, @Option("output") @Required File outputSourceFile, @Default(".") @Option("baseDir") File baseDir) {
		
		try {
			
		    JavaSourceReader javaSourceReader = new JavaSourceReader(baseDir);
			String content = javaSourceReader.generateDoc(new FileInputStream(inputSourceFile));
			
			if(content != null) {
				IOUtils.writeFull(content, new FileOutputStream(outputSourceFile));			
			} else {
				throw new IllegalStateException("No content has been provided in file "+inputSourceFile.getAbsolutePath());
			}
			
		} catch (ParseException e) {
			throw new IllegalStateException(e);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
