package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.io.Serializable;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

public class Artwork implements Serializable {

	private String database;

	private String[] tags;
	private URL url;

	private String language;
	private double rating;

	public Artwork() {
		// used by serializer
	}

	public Artwork(Datasource database, Stream<?> tags, URL url, Locale language, Double rating) {
		this.database = database.getIdentifier();
		this.tags = tags.filter(Objects::nonNull).map(Object::toString).toArray(String[]::new);
		this.url = url;
		this.language = language == null || language.getLanguage().isEmpty() ? null : language.getLanguage();
		this.rating = rating == null ? 0 : rating;
	}

	public String getDatabase() {
		return database;
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
		return rating;
	}

	public boolean matches(Object... tags) {
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

}
