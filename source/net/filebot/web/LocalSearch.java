package net.sourceforge.filebot.web;

import static java.util.Collections.*;
import static net.sourceforge.filebot.similarity.Normalization.*;

import java.util.AbstractList;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import uk.ac.shef.wit.simmetrics.similaritymetrics.AbstractStringMetric;
import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

import com.ibm.icu.text.Transliterator;

public class LocalSearch<T> {

	private final AbstractStringMetric metric = new QGramsDistance();
	private float resultMinimumSimilarity = 0.5f;
	private int resultSetSize = 20;

	private final Transliterator transliterator = Transliterator.getInstance("Any-Latin;Latin-ASCII;[:Diacritic:]remove");

	private final List<T> objects;
	private final List<Set<String>> fields;

	public LocalSearch(Collection<? extends T> data) {
		objects = new ArrayList<T>(data);
		fields = new ArrayList<Set<String>>(objects.size());

		for (int i = 0; i < objects.size(); i++) {
			fields.add(i, getFields(objects.get(i)));
		}
	}

	public List<T> search(String query) throws ExecutionException, InterruptedException {
		final String q = normalize(query);
		List<Callable<Entry<T, Float>>> tasks = new ArrayList<Callable<Entry<T, Float>>>(objects.size());

		for (int i = 0; i < objects.size(); i++) {
			final int index = i;
			tasks.add(new Callable<Entry<T, Float>>() {

				@Override
				public Entry<T, Float> call() throws Exception {
					float similarity = 0;
					boolean match = false;

					for (String field : fields.get(index)) {
						match |= field.contains(q);
						similarity = Math.max(metric.getSimilarity(q, field), similarity);
					}

					return match || similarity > resultMinimumSimilarity ? new SimpleEntry<T, Float>(objects.get(index), similarity) : null;
				}
			});
		}

		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final List<Entry<T, Float>> resultSet = new ArrayList<Entry<T, Float>>(objects.size());

		try {
			for (Future<Entry<T, Float>> entry : executor.invokeAll(tasks)) {
				if (entry.get() != null) {
					resultSet.add(entry.get());
				}

				if (Thread.interrupted()) {
					throw new InterruptedException();
				}
			}
		} finally {
			executor.shutdownNow();
		}

		// sort by similarity descending (best matches first)
		sort(resultSet, new Comparator<Entry<T, Float>>() {

			@Override
			public int compare(Entry<T, Float> o1, Entry<T, Float> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});

		// view for the first 20 search results
		return new AbstractList<T>() {

			@Override
			public T get(int index) {
				return resultSet.get(index).getKey();
			}

			@Override
			public int size() {
				return Math.min(resultSetSize, resultSet.size());
			}
		};
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
