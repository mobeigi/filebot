package net.filebot.web;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Episode implements Serializable {

	protected String seriesName;
	protected SimpleDate seriesStartDate;

	protected Integer season;
	protected Integer episode;
	protected String title;

	// absolute episode number
	protected Integer absolute;

	// special number
	protected Integer special;

	// optional episode number order hint & episode name / title language hint
	protected String order;
	protected String language;

	// episode airdate
	protected SimpleDate airdate;

	// original series descriptor
	protected SearchResult series;

	protected Episode() {
		// used by serializer
	}

	public Episode(Episode obj) {
		this(obj.seriesName, obj.seriesStartDate, obj.season, obj.episode, obj.title, obj.absolute, obj.special, obj.getOrder(), obj.getLanguage(), obj.airdate, obj.series);
	}

	public Episode(String seriesName, SimpleDate seriesStartDate, Integer season, Integer episode, String title, SearchResult series) {
		this(seriesName, seriesStartDate, season, episode, title, null, null, null, null, null, series);
	}

	public Episode(String seriesName, SimpleDate seriesStartDate, Integer season, Integer episode, String title, Integer absolute, Integer special, SortOrder order, Locale locale, SimpleDate airdate, SearchResult series) {
		this.seriesName = seriesName;
		this.seriesStartDate = (seriesStartDate == null ? null : seriesStartDate.clone());
		this.season = season;
		this.episode = episode;
		this.title = title;
		this.absolute = absolute;
		this.special = special;
		this.order = (order == null ? null : order.name());
		this.language = (locale == null ? null : locale.getLanguage());
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

	public SortOrder getOrder() {
		return order == null ? null : SortOrder.forName(order);
	}

	public Locale getLanguage() {
		return language == null ? null : new Locale(language);
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
