package net.filebot.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Episode implements Serializable {

	protected String seriesName;
	protected Integer season;
	protected Integer episode;
	protected String title;

	// absolute episode number
	protected Integer absolute;

	// special number
	protected Integer special;

	// episode airdate
	protected SimpleDate airdate;

	// extended series metadata
	protected SeriesInfo seriesInfo;

	protected Episode() {

	}

	public Episode(Episode obj) {
		this(obj.seriesName, obj.season, obj.episode, obj.title, obj.absolute, obj.special, obj.airdate, obj.seriesInfo);
	}

	public Episode(String seriesName, Integer season, Integer episode, String title) {
		this(seriesName, season, episode, title, null, null, null, null);
	}

	public Episode(String seriesName, Integer season, Integer episode, String title, Integer absolute, Integer special, SimpleDate airdate, SeriesInfo seriesInfo) {
		this.seriesName = seriesName;
		this.season = season;
		this.episode = episode;
		this.title = title;
		this.absolute = absolute;
		this.special = special;
		this.airdate = (airdate == null ? null : airdate.clone());
		this.seriesInfo = (seriesInfo == null ? null : seriesInfo.clone());
	}

	public String getSeriesName() {
		return seriesName;
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

	public SeriesInfo getSeriesInfo() {
		return seriesInfo;
	}

	public List<Integer> getNumbers() {
		return Arrays.asList(season, episode, special, absolute);
	}

	public List<String> getSeriesNames() {
		Set<String> names = new LinkedHashSet<String>();
		if (seriesName != null) {
			names.add(seriesName);
		}
		if (seriesInfo != null) {
			if (seriesInfo.name != null) {
				names.add(seriesInfo.name);
			}
			if (seriesInfo.aliasNames != null) {
				for (String it : seriesInfo.aliasNames) {
					names.add(it);
				}
			}
		}
		return new ArrayList<String>(names);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Episode) {
			Episode other = (Episode) obj;
			return equals(season, other.season) && equals(episode, other.episode) && equals(absolute, other.absolute) && equals(special, other.special) && equals(seriesName, other.seriesName) && equals(title, other.title);
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
		return Arrays.hashCode(new Object[] { season, episode, absolute, special, seriesName, title });
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
