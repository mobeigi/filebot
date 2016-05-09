package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.io.Serializable;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class Artwork implements Serializable {

	protected String[] tags;
	protected URL url;

	protected String language;
	protected Double rating;

	public Artwork() {
		// used by serializer
	}

	public Artwork(Stream<?> tags, URL url, Locale language, Double rating) {
		this.tags = tags.filter(Objects::nonNull).map(Object::toString).toArray(String[]::new);
		this.url = url;
		this.language = language == null || language.getLanguage().isEmpty() ? null : language.getLanguage();
		this.rating = rating;
	}

	public List<String> getTags() {
		return unmodifiableList(asList(tags));
	}

	public URL getUrl() {
		return url;
	}

	public Locale getLanguage() {
		return language == null ? null : new Locale(language);
	}

	public double getRating() {
		return rating == null ? 0 : rating;
	}

	public boolean matches(Object... tags) {
		if (tags == null || tags.length == 0) {
			return true;
		}

		return stream(tags).filter(Objects::nonNull).map(Object::toString).allMatch(tag -> {
			return stream(this.tags).anyMatch(tag::equalsIgnoreCase) || tag.equalsIgnoreCase(language);
		});
	}

	@Override
	public int hashCode() {
		return url.hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Artwork) {
			Artwork artwork = (Artwork) other;
			return url.sameFile(artwork.url);
		}
		return false;
	}

	@Override
	public String toString() {
		return asList(String.join("/", tags), language, new DecimalFormat("0.##").format(rating), url).toString();
	}

	public static final Comparator<Artwork> RATING_ORDER = Comparator.comparing(Artwork::getRating, reverseOrder());

}
