package net.filebot.format;

import groovy.lang.GroovyObjectSupport;

import java.util.function.Function;

public class DynamicBindings extends GroovyObjectSupport {

	private Function<String, Object> map;

	public DynamicBindings(Function<String, Object> map) {
		this.map = map;
	}

	@Override
	public Object getProperty(String property) {
		return map.apply(property);
	}

}
