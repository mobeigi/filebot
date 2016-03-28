package net.filebot.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Movie extends SearchResult {

	protected int year;
	protected int imdbId;
	protected int tmdbId;

	// optional movie name language hint
	protected String language;

	protected Movie() {
		// used by serializer
	}

	public Movie(int imdbId) {
		this(null, 0, imdbId, -1);
	}

	public Movie(String name, int year, int imdbId, int tmdbId) {
		this(name, null, year, imdbId, tmdbId, null);
	}

	public Movie(String name, String[] aliasNames, int year, int imdbId, int tmdbId, Locale locale) {
		super(tmdbId > 0 ? tmdbId : imdbId > 0 ? imdbId : -1, name, aliasNames);
		this.year = year;
		this.imdbId = imdbId;
		this.tmdbId = tmdbId;
		this.language = (locale == null ? null : locale.getLanguage());
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

	public Locale getLanguage() {
		return language == null ? null : new Locale(language);
	}

	public String getNameWithYear() {
		return toString(name, year);
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

	public List<String> getEffectiveNamesWithoutYear() {
		return super.getEffectiveNames();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Movie) {
			Movie other = (Movie) object;

			if (tmdbId > 0 && other.tmdbId > 0) {
				return tmdbId == other.tmdbId;
			}
			if (imdbId > 0 && other.imdbId > 0) {
				return imdbId == other.imdbId;
			}
			if (year != other.year) {
				return false;
			}

			Set<String> intersection = new HashSet<String>(getEffectiveNames());
			intersection.retainAll(other.getEffectiveNames());
			return intersection.size() > 0;
		}

		return false;
	}

	@Override
	public Movie clone() {
		return new Movie(name, aliasNames, year, imdbId, tmdbId, getLanguage());
	}

	@Override
	public String toString() {
		return toString(name, year);
	}

	private static String toString(String name, int year) {
		return String.format("%s (%04d)", name, year < 0 ? 0 : year);
	}

}
