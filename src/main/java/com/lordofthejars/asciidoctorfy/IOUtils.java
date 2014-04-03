package com.lordofthejars.asciidoctorfy;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class IOUtils {

	public static final String NEW_LINE = System.getProperty("line.separator");
	
	public static ByteArrayInputStream copyInputStream(InputStream inputStream)
			throws IOException {

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		int nRead;
		byte[] data = new byte[16384];

		while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}

		return new ByteArrayInputStream(buffer.toByteArray());

	}
	
	public static String readFull(File file) throws IOException {
	    return new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
	}
	
	public static String[] readFull(InputStream inputStream) {
		List<String> lines = new ArrayList<String>();
		Scanner scanner = new Scanner(inputStream);

		while (scanner.hasNextLine()) {
			lines.add(scanner.nextLine());
		}

		scanner.close();

		return lines.toArray(new String[lines.size()]);

	}
	
	public static void writeFull(String content, OutputStream outputStream) throws IOException {
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
		bufferedWriter.write(content);
		bufferedWriter.flush();
		bufferedWriter.close();
	}

	public static String join(String[] content, int initialIndex) {
		StringBuilder stringBuilder = new StringBuilder();

		for (String string : content) {
			stringBuilder.append(string.substring(initialIndex)).append(
					NEW_LINE);
		}

		return stringBuilder.toString();

	}
	
}
