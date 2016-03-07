package net.filebot;

@FunctionalInterface
public interface Resource<R> {

	R get() throws Exception;

}
