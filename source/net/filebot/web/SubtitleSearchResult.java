package net.filebot.web;

import java.util.Locale;

public class SubtitleSearchResult extends Movie {

	enum Kind {
		Movie, Series, Other, Unkown;

		public static Kind forName(String s) {
			if (s == null || s.isEmpty())
				return Unkown;
			else if (s.equalsIgnoreCase("m") || s.equalsIgnoreCase("movie"))
				return Movie;
			if (s.equalsIgnoreCase("s") || s.equalsIgnoreCase("tv series"))
				return Series;
			else
				return Other;
		}
	}

	private Kind kind;
	private int score;

	public SubtitleSearchResult(int imdbId, String name, int year, String kind, int score) {
		this(name, null, year, imdbId, -1, Locale.ENGLISH, Kind.forName(kind), score);
	}

	public SubtitleSearchResult(String name, String[] aliasNames, int year, int imdbId, int tmdbId, Locale locale, Kind kind, int score) {
		super(name, aliasNames, year, imdbId, tmdbId, locale);

		this.kind = kind;
		this.score = score;
	}

	public Kind getKind() {
		return kind;
	}

	public int getScore() {
		return score;
	}

	public boolean isMovie() {
		return kind == Kind.Movie;
	}

	public boolean isSeries() {
		return kind == Kind.Series;
	}

}
