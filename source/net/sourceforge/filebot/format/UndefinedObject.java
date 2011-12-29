
package net.sourceforge.filebot.format;


import groovy.lang.GroovyObjectSupport;


public class UndefinedObject extends GroovyObjectSupport {
	
	private String value;
	
	
	private UndefinedObject(String value) {
		this.value = value;
	}
	
	
	@Override
	public Object getProperty(String property) {
		return this;
	}
	
	
	@Override
	public Object invokeMethod(String name, Object args) {
		return this;
	}
	
	
	@Override
	public void setProperty(String property, Object newValue) {
		// ignore
	}
	
	
	@Override
	public String toString() {
		return value;
	}
	
}
