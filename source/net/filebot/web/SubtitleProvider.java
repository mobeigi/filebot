package net.filebot.web;

import java.net.URI;
import java.util.List;

public interface SubtitleProvider extends Datasource {

	public List<SubtitleSearchResult> search(String query) throws Exception;

	public List<SubtitleSearchResult> guess(String tag) throws Exception;

	public List<SubtitleDescriptor> getSubtitleList(SubtitleSearchResult searchResult, int[][] episodeFilter, String languageName) throws Exception;

	public URI getSubtitleListLink(SubtitleSearchResult searchResult, String languageName);

	public URI getLink();

}
