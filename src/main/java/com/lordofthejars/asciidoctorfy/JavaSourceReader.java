package com.lordofthejars.asciidoctorfy;

import static com.lordofthejars.asciidoctorfy.IOUtils.NEW_LINE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.github.antlrjavaparser.ParseException;

public class JavaSourceReader {

	private Java7Parser java7Parser = new Java7Parser();
	private StringBuilder content = new StringBuilder();
	
	private File baseDir = new File(".");
	
	public JavaSourceReader() {
	    super();
	}
	
	public JavaSourceReader(File baseDir){
	    this.baseDir = baseDir;
	}
	
	public String generateDoc(InputStream javaFile) throws ParseException,
			IOException {

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

	private void resolveAsciiDocLine(final String asciidocLine) throws FileNotFoundException,
			IOException {
		if (isAJavaIncludeSentence(asciidocLine)) {
			resolveJavaInclude(asciidocLine);
		} else {
			if (isEmptyCommentLine(asciidocLine)) {
				content.append(NEW_LINE);
			} else {
				content.append(asciidocLine).append(NEW_LINE);
			}
		}
	}

	private boolean isEmptyCommentLine(final String asciidocLine) {
		return asciidocLine.length() == 0;
	}

	private void resolveJavaInclude(final String asciidocLine) throws FileNotFoundException,
			IOException {
		String fileName = getFilenamePath(asciidocLine);

		if (isIncludeWithMethod(fileName)) {
			includeJavaMethod(fileName);
		} else {
			includeJavaClass(fileName);
		}
	}

	private void includeJavaClass(String fileName)
			throws FileNotFoundException, IOException {
		Map<String, Object> attributes = new HashMap<>();
		attributes.put("class", "");

		InputStream fileInputStream = new FileInputStream(
				new File(this.baseDir, fileName));

		appendSourceCode(attributes, fileInputStream);
	}

	private void appendSourceCode(Map<String, Object> attributes, InputStream fileInputStream)
			throws IOException {
		String extractedContent = java7Parser.extract(
				fileInputStream, attributes);
		content.append("[source, java]").append(NEW_LINE);
		content.append("----").append(NEW_LINE);
		content.append(extractedContent.trim()).append(NEW_LINE);
		content.append("----").append(NEW_LINE);
	}

	private void includeJavaMethod(String fileName)
			throws FileNotFoundException, IOException {
		
	    final int methodSeparator = fileName.lastIndexOf("#");
		String classLocation = fileName.substring(0,
				methodSeparator);
		final int extensionSeparator = fileName
				.lastIndexOf(".");
		String method = fileName.substring(methodSeparator + 1,
				extensionSeparator);

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("method", method);

		InputStream fileInputStream = new FileInputStream(
				new File(baseDir, classLocation + ".java"));

		appendSourceCode(attributes, fileInputStream);
	}

	private boolean isIncludeWithMethod(String fileName) {
		return fileName.contains("#");
	}

	private String getFilenamePath(final String asciidocLine) {
		return asciidocLine.substring(9,
				asciidocLine.lastIndexOf("["));
	}

	private boolean isAJavaIncludeSentence(final String asciidocLine) {
		return asciidocLine.matches("include::[\\w/#]+\\.java\\[\\w*\\]");
	}

	private boolean isInsideComments(boolean insideCommentBlock,
			String trimedLine) {
		return trimedLine.startsWith("*") && insideCommentBlock;
	}

	private boolean isEndingComments(boolean insideCommentBlock,
			String trimedLine) {
		return trimedLine.startsWith("*/") && insideCommentBlock;
	}

	private boolean isStartingComments(String trimedLine) {
		return trimedLine.startsWith("/**");
	}

	private static String[] readFull(InputStream inputStream) {
		List<String> lines = new ArrayList<String>();
		Scanner scanner = new Scanner(inputStream);

		while (scanner.hasNextLine()) {
			lines.add(scanner.nextLine());
		}

		scanner.close();

		return lines.toArray(new String[lines.size()]);

	}

}
