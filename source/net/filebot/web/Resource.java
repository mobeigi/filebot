package net.filebot.web;

@FunctionalInterface
public interface Resource<R> {

	R get() throws Exception;

}
