package net.filebot.media;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static net.filebot.similarity.Normalization.*;
import static net.filebot.util.FileUtilities.*;
import static net.filebot.web.EpisodeUtilities.*;

import java.util.Objects;

import net.filebot.web.AudioTrack;
import net.filebot.web.Episode;
import net.filebot.web.EpisodeFormat;
import net.filebot.web.Movie;
import net.filebot.web.MoviePart;

public enum NamingStandard {

	Plex;

	public String getPath(Object o) {
		if (o instanceof Episode)
			return getPath((Episode) o);
		if (o instanceof Movie)
			return getPath((Movie) o);
		if (o instanceof AudioTrack)
			return getPath((AudioTrack) o);

		return null;
	}

	public String getPath(Episode e) {
		// enforce title length limit by default
		String episodeTitle = truncateText(EpisodeFormat.SeasonEpisode.formatMultiTitle(getMultiEpisodeList(e)), TITLE_MAX_LENGTH);

		// Anime
		if (isAnime(e)) {
			String primaryTitle = e.getSeriesInfo().getName();
			String episode = String.join(" - ", primaryTitle, EpisodeFormat.SeasonEpisode.formatSxE(e), episodeTitle);
			return path(getAnimeFolder(), primaryTitle, episode);
		}

		// TV Series
		String episode = String.join(" - ", e.getSeriesName(), EpisodeFormat.SeasonEpisode.formatS00E00(e), episodeTitle);
		String season = e.getSeason() == null ? e.getSpecial() == null ? null : getSpecialFolder(e.getSeason()) : getSeasonFolder(e.getSeason());
		return path(getSeriesFolder(), e.getSeriesName(), season, episode);
	}

	public String getPath(Movie m) {
		// Movie
		String name = m.getNameWithYear();

		// Movie (multi-part)
		if (m instanceof MoviePart) {
			name = String.format("%s CD%d", name, ((MoviePart) m).getPartIndex());
		}

		return path(getMovieFolder(), m.getNameWithYear(), name);
	}

	public String getPath(AudioTrack a) {
		// Music
		String name = String.join(" - ", a.getArtist(), first(a.getTrackTitle(), a.getTitle()));

		// prepend track number
		if (a.getTrack() != null) {
			name = String.format("%02d. %s", a.getTrack(), name);
		}

		return path(getMusicFolder(), first(a.getAlbumArtist(), a.getArtist()), a.getAlbum(), name);
	}

	private static String path(String... name) {
		return stream(name).filter(Objects::nonNull).map(s -> {
			s = replacePathSeparators(s, " ");
			s = normalizeQuotationMarks(s);
			s = trimTrailingPunctuation(s);
			s = validateFileName(s);
			return s;
		}).filter(s -> s.length() > 0).collect(joining("/"));
	}

	private static String first(String... options) {
		return stream(options).filter(Objects::nonNull).findFirst().get();
	}

	public String getMovieFolder() {
		return "Movies";
	}

	public String getSeriesFolder() {
		return "TV Shows";
	}

	public String getAnimeFolder() {
		return "Anime";
	}

	public String getMusicFolder() {
		return "Music";
	}

	public String getSpecialFolder(Integer season) {
		return "Specials";
	}

	public String getSeasonFolder(Integer season) {
		return String.format("Season %02d", season == null ? 0 : season);
	}

	public static final int TITLE_MAX_LENGTH = 150;

}
