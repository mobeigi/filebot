package net.filebot.format;

import groovy.lang.GroovyObjectSupport;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

public class DynamicBindings extends GroovyObjectSupport {

	private Function<String, Object> map;
	private String[] keys;

	public DynamicBindings(Function<String, Object> map, Stream<String> keys) {
		this.map = map;
		this.keys = keys.toArray(String[]::new);
	}

	@Override
	public Object getProperty(String property) {
		return map.apply(property);
	}

	@Override
	public String toString() {
		return Arrays.toString(keys);
	}

}
