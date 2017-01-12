package com.vti.guardian.web.bean;

import java.util.HashSet;
import java.util.Set;

public class Node {
	
	private String name;
	
	private Set<Node> children = new HashSet<Node>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<Node> getChildren() {
		return children;
	}

	public void setChildren(Set<Node> children) {
		this.children = children;
	}
	
}
