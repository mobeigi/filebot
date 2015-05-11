package net.filebot.web;

import java.util.Locale;

public class OpenSubtitlesSearchResult extends Movie {

	public static final char KIND_MOVIE = 'm';
	public static final char KIND_SERIES = 's';

	private char kind;
	private int score;

	public OpenSubtitlesSearchResult(int imdbId, String name, int year, char kind, int score) {
		super(name, null, year, imdbId, -1, Locale.ENGLISH);

		this.kind = kind;
		this.score = score;
	}

	public char getKind() {
		return kind;
	}

	public int getScore() {
		return score;
	}

}
