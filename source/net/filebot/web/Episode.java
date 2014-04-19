package net.filebot.web;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Episode implements Serializable {

	private String seriesName;
	private SimpleDate seriesStartDate;

	private Integer season;
	private Integer episode;
	private String title;

	// absolute episode number
	private Integer absolute;

	// special number
	private Integer special;

	// episode airdate
	private SimpleDate airdate;

	// original series descriptor
	private SearchResult series;

	protected Episode() {
		// used by serializer
	}

	public Episode(Episode obj) {
		this(obj.seriesName, obj.seriesStartDate, obj.season, obj.episode, obj.title, obj.absolute, obj.special, obj.airdate, obj.series);
	}

	public Episode(String seriesName, SimpleDate seriesStartDate, Integer season, Integer episode, String title, SearchResult series) {
		this(seriesName, seriesStartDate, season, episode, title, null, null, null, series);
	}

	public Episode(String seriesName, SimpleDate seriesStartDate, Integer season, Integer episode, String title, Integer absolute, Integer special, SimpleDate airdate, SearchResult series) {
		this.seriesName = seriesName;
		this.seriesStartDate = (seriesStartDate == null ? null : seriesStartDate.clone());
		this.season = season;
		this.episode = episode;
		this.title = title;
		this.absolute = absolute;
		this.special = special;
		this.airdate = (airdate == null ? null : airdate.clone());
		this.series = (series == null ? null : series.clone());
	}

	public String getSeriesName() {
		return seriesName;
	}

	public SimpleDate getSeriesStartDate() {
		return seriesStartDate;
	}

	public Integer getEpisode() {
		return episode;
	}

	public Integer getSeason() {
		return season;
	}

	public String getTitle() {
		return title;
	}

	public Integer getAbsolute() {
		return absolute;
	}

	public Integer getSpecial() {
		return special;
	}

	public SimpleDate getAirdate() {
		return airdate;
	}

	public SearchResult getSeries() {
		return series;
	}

	public List<Integer> getNumbers() {
		return Arrays.asList(season, episode, special);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Episode) {
			Episode other = (Episode) obj;
			return equals(season, other.season) && equals(episode, other.episode) && equals(seriesName, other.seriesName) && equals(title, other.title) && equals(special, other.special);
		}

		return false;
	}

	private boolean equals(Object o1, Object o2) {
		if (o1 == null || o2 == null)
			return o1 == o2;

		return o1.equals(o2);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(new Object[] { seriesName, season, episode, title, special });
	}

	@Override
	public Episode clone() {
		return new Episode(this);
	}

	@Override
	public String toString() {
		return EpisodeFormat.SeasonEpisode.format(this);
	}

}
