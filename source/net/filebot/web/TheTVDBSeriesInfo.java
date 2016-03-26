package net.filebot.web;

import java.io.Serializable;
import java.net.URL;
import java.util.Locale;

public class TheTVDBSeriesInfo extends SeriesInfo implements Serializable {

	protected String imdbId;
	protected String overview;

	protected String airsDayOfWeek;
	protected String airTime;

	protected String bannerUrl;
	protected String fanartUrl;
	protected String posterUrl;

	protected TheTVDBSeriesInfo() {

	}

	public TheTVDBSeriesInfo(TheTVDBSeriesInfo other) {
		super(other);
		this.imdbId = other.imdbId;
		this.overview = other.overview;
		this.airsDayOfWeek = other.airsDayOfWeek;
		this.airTime = other.airTime;
		this.bannerUrl = other.bannerUrl;
		this.fanartUrl = other.fanartUrl;
		this.posterUrl = other.posterUrl;
	}

	public TheTVDBSeriesInfo(Datasource database, SortOrder order, Locale language, Integer id) {
		super(database, order, language, id);
	}

	public SimpleDate getFirstAired() {
		return getStartDate();
	}

	public String getContentRating() {
		return getCertification();
	}

	public String getImdbId() {
		return imdbId;
	}

	public void setImdbId(String imdbId) {
		this.imdbId = imdbId;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String overview) {
		this.overview = overview;
	}

	public String getAirsDayOfWeek() {
		return airsDayOfWeek;
	}

	public void setAirsDayOfWeek(String airsDayOfWeek) {
		this.airsDayOfWeek = airsDayOfWeek;
	}

	public String getAirTime() {
		return airTime;
	}

	public void setAirTime(String airTime) {
		this.airTime = airTime;
	}

	public String getBannerUrl() {
		return bannerUrl;
	}

	public void setBannerUrl(URL bannerUrl) {
		this.bannerUrl = bannerUrl.toString();
	}

	public URL getFanartUrl() {
		try {
			return new URL(fanartUrl);
		} catch (Exception e) {
			return null;
		}
	}

	public void setFanartUrl(URL fanartUrl) {
		this.fanartUrl = fanartUrl.toString();
	}

	public URL getPosterUrl() {
		try {
			return new URL(posterUrl);
		} catch (Exception e) {
			return null;
		}
	}

	public void setPosterUrl(URL posterUrl) {
		this.posterUrl = posterUrl.toString();
	}

	@Override
	public TheTVDBSeriesInfo clone() {
		return new TheTVDBSeriesInfo(this);
	}

}
