package net.filebot.similarity;

import static java.util.Collections.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;

public class SimilarityComparator<T, P> implements Comparator<T> {

	protected SimilarityMetric metric;
	protected Collection<P> paragon;

	protected Function<T, Collection<P>> mapper;

	public SimilarityComparator(SimilarityMetric metric, Collection<P> paragon, Function<T, Collection<P>> mapper) {
		this.metric = metric;
		this.paragon = paragon;
		this.mapper = mapper;
	}

	public SimilarityComparator(P paragon, Function<T, P> mapper) {
		this(new NameSimilarityMetric(), singleton(paragon), mapper.andThen(Collections::singleton));
	}

	@Override
	public int compare(T o1, T o2) {
		return Double.compare(getSimilarity(o2), getSimilarity(o1));
	}

	private static final double ZERO = 0;

	public double getSimilarity(T value) {
		return paragon.stream().mapToDouble((it) -> accumulateSimilarity(it, value)).average().orElse(ZERO);
	}

	private double accumulateSimilarity(P paragon, T value) {
		if (paragon == null) {
			return ZERO;
		}

		return mapper.apply(value).stream().mapToDouble((it) -> it == null ? ZERO : (double) metric.getSimilarity(paragon, it)).max().orElse(ZERO);
	}

}
