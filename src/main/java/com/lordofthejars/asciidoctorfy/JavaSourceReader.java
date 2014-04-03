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

import com.github.antlrjavaparser.ParseException;

public class JavaSourceReader {

	private static final String START_COMMENT = "/**";
    private static final String END_COMMENT = "*/";
    private static final String COMMENT_SYMBOL = "*";
    private static final String INCLUDE_JAVA = "include::[\\w/#]+\\.java\\[\\w*\\]";
    private static final String METHOD_KEY = "method";
    private static final String METHOD_SEPARATOR = "#";
    private static final String CLASS_KEY = "class";
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
		attributes.put(CLASS_KEY, "");

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
		
	    final int methodSeparator = fileName.lastIndexOf(METHOD_SEPARATOR);
		String classLocation = fileName.substring(0,
				methodSeparator);
		final int extensionSeparator = fileName
				.lastIndexOf(".");
		String method = fileName.substring(methodSeparator + 1,
				extensionSeparator);

		Map<String, Object> attributes = new HashMap<>();
		attributes.put(METHOD_KEY, method);

		InputStream fileInputStream = new FileInputStream(
				new File(baseDir, classLocation + ".java"));

		appendSourceCode(attributes, fileInputStream);
	}

	private boolean isIncludeWithMethod(String fileName) {
		return fileName.contains(METHOD_SEPARATOR);
	}

	private String getFilenamePath(final String asciidocLine) {
		return asciidocLine.substring(9,
				asciidocLine.lastIndexOf("["));
	}

	private boolean isAJavaIncludeSentence(final String asciidocLine) {
		return asciidocLine.matches(INCLUDE_JAVA);
	}

	private boolean isInsideComments(boolean insideCommentBlock,
			String trimedLine) {
		return trimedLine.startsWith(COMMENT_SYMBOL) && insideCommentBlock;
	}

	private boolean isEndingComments(boolean insideCommentBlock,
			String trimedLine) {
		return trimedLine.startsWith(END_COMMENT) && insideCommentBlock;
	}

	private boolean isStartingComments(String trimedLine) {
		return trimedLine.startsWith(START_COMMENT);
	}

}
