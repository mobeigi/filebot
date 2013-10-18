package net.sourceforge.filebot.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Movie extends SearchResult {

	protected int year;
	protected int imdbId;
	protected int tmdbId;

	protected Movie() {
		// used by serializer
	}

	public Movie(Movie obj) {
		this(obj.name, obj.aliasNames, obj.year, obj.imdbId, obj.tmdbId);
	}

	public Movie(String name, int year, int imdbId, int tmdbId) {
		this(name, new String[0], year, imdbId, tmdbId);
	}

	public Movie(String name, String[] aliasNames, int year, int imdbId, int tmdbId) {
		super(name, aliasNames);
		this.year = year;
		this.imdbId = imdbId;
		this.tmdbId = tmdbId;
	}

	public int getYear() {
		return year;
	}

	public int getImdbId() {
		return imdbId;
	}

	public int getTmdbId() {
		return tmdbId;
	}

	@Override
	public List<String> getEffectiveNames() {
		if (aliasNames == null || aliasNames.length == 0) {
			return Collections.singletonList(toString(name, year));
		}

		List<String> names = new ArrayList<String>(1 + aliasNames.length);
		names.add(toString(name, year));
		for (String alias : aliasNames) {
			names.add(toString(alias, year));
		}
		return names;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Movie) {
			Movie other = (Movie) object;
			if (imdbId > 0 && other.imdbId > 0) {
				return imdbId == other.imdbId;
			} else if (tmdbId > 0 && other.tmdbId > 0) {
				return tmdbId == other.tmdbId;
			}

			return year == other.year && name.equals(other.name);
		}

		return false;
	}

	@Override
	public Movie clone() {
		return new Movie(this);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { name.toLowerCase(), year });
	}

	@Override
	public String toString() {
		return toString(name, year);
	}

	private static String toString(String name, int year) {
		return String.format("%s (%04d)", name, year < 0 ? 0 : year);
	}

}
