package net.filebot.format;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import groovy.lang.GroovyObjectSupport;

public class DynamicBindings extends GroovyObjectSupport {

	private Supplier<Collection<?>> keys;
	private Function<String, Object> properties;

	public DynamicBindings(Supplier<Collection<?>> keys, Function<String, Object> properties) {
		this.keys = keys;
		this.properties = properties;
	}

	@Override
	public Object getProperty(String property) {
		return properties.apply(property);
	}

	@Override
	public String toString() {
		return keys.get().toString();
	}

}
