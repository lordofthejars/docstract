package com.lordofthejars.asciidoctorfy;

import java.net.URL;

import javax.xml.ws.Action;

public class Project {

	private String id;
	private String name;
	private URL location;
	
	public Project(String id, String name, URL location) {
		super();
		this.id = id;
		this.name = name;
		this.location = location;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public URL getLocation() {
		return location;
	}
	
	
}
