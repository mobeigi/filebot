package net.filebot;

import java.util.function.Function;

@FunctionalInterface
public interface Resource<R> {

	R get() throws Exception;

	default Resource<R> memoize() {
		return new MemoizedResource<R>(this);
	}

	default <T> Resource<T> transform(Function<R, T> function) {
		return new TransformedResource<R, T>(this, function);
	}

	static <T> Resource<T> lazy(Resource<T> resource) {
		return resource.memoize();
	}

}

class MemoizedResource<R> implements Resource<R> {

	private final Resource<R> resource;
	private R value;

	public MemoizedResource(Resource<R> resource) {
		this.resource = resource;
	}

	@Override
	public synchronized R get() throws Exception {
		if (value == null) {
			value = resource.get();
		}
		return value;
	}
}

class TransformedResource<R, T> implements Resource<T> {

	private final Resource<R> resource;
	private final Function<R, T> function;

	public TransformedResource(Resource<R> resource, Function<R, T> function) {
		this.resource = resource;
		this.function = function;
	}

	@Override
	public T get() throws Exception {
		return function.apply(resource.get());
	}

}
