package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import java.io.Serializable;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class Artwork implements Serializable {

	private String database;

	private String[] category;
	private URL url;

	private String language;
	private double rating;

	public Artwork() {
		// used by serializer
	}

	public Artwork(Datasource database, List<String> category, URL url, Locale language, double rating) {
		this.database = database.getIdentifier();
		this.category = category.toArray(new String[0]);
		this.url = url;
		this.language = language.getLanguage();
		this.rating = rating;
	}

	public String getDatabase() {
		return database;
	}

	public List<String> getCategory() {
		return unmodifiableList(asList(category));
	}

	public URL getUrl() {
		return url;
	}

	public String getLanguage() {
		return language;
	}

	public double getRating() {
		return rating;
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
		return asList(database, asList(category), url, language, rating).toString();
	}

}
