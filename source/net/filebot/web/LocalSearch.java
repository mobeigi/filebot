package net.filebot.web;

import static java.util.Collections.*;
import static java.util.Comparator.*;
import static java.util.stream.Collectors.*;
import static net.filebot.similarity.Normalization.*;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

import com.ibm.icu.text.Transliterator;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

public class LocalSearch<T> {

	private AbstractStringMetric metric = new QGramsDistance();
	private float resultMinimumSimilarity = 0.5f;
	private int resultSetSize = 20;

	private Transliterator transliterator = Transliterator.getInstance("Any-Latin;Latin-ASCII;[:Diacritic:]remove");

	private List<T> objects;
	private List<Set<String>> fields;

	public LocalSearch(Collection<? extends T> data) {
		objects = new ArrayList<T>(data);
		fields = objects.stream().map(this::getFields).collect(toList());
	}

	public List<T> search(String q) throws ExecutionException, InterruptedException {
		String query = normalize(q);

		return IntStream.range(0, objects.size()).mapToObj(i -> {
			T object = objects.get(i);
			Set<String> field = fields.get(i);

			boolean match = field.stream().anyMatch(it -> it.contains(query));
			double similarity = field.stream().mapToDouble(it -> metric.getSimilarity(query, it)).max().orElse(0);

			return match || similarity > resultMinimumSimilarity ? new SimpleImmutableEntry<T, Double>(object, similarity) : null;
		}).filter(Objects::nonNull).sorted(reverseOrder(comparing(Entry::getValue))).limit(resultSetSize).map(Entry::getKey).collect(toList());
	}

	public void setResultMinimumSimilarity(float resultMinimumSimilarity) {
		this.resultMinimumSimilarity = resultMinimumSimilarity;
	}

	public void setResultSetSize(int resultSetSize) {
		this.resultSetSize = resultSetSize;
	}

	protected Set<String> getFields(T object) {
		return set(singleton(object.toString()));
	}

	protected Set<String> set(Collection<String> values) {
		Set<String> set = new HashSet<String>(values.size());
		for (String value : values) {
			if (value != null) {
				set.add(normalize(value));
			}
		}
		return set;
	}

	protected String normalize(String value) {
		// normalize separator, normalize case and trim
		return normalizePunctuation(transliterator.transform(value)).toLowerCase();
	}

}
