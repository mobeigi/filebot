package net.filebot.web;

import java.io.Serializable;
import java.util.Map;

public class Trailer implements Serializable {

	protected String type;
	protected String name;
	protected Map<String, String> sources;

	public Trailer() {
		// used by serializer
	}

	public Trailer(String type, String name, Map<String, String> sources) {
		this.type = type;
		this.name = name;
		this.sources = sources;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public Map<String, String> getSources() {
		return sources;
	}

	public String getSource(String size) {
		return sources.containsKey(size) ? sources.get(size) : sources.values().iterator().next();
	}

	@Override
	public String toString() {
		return String.format("%s %s (%s)", name, sources.keySet(), type);
	}

}
