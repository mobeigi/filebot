package net.filebot.web;

import java.net.URI;
import java.util.List;

import javax.swing.Icon;

public interface SubtitleProvider {

	public List<SubtitleSearchResult> search(String query) throws Exception;

	public List<SubtitleSearchResult> guess(String tag) throws Exception;

	public List<SubtitleDescriptor> getSubtitleList(SubtitleSearchResult searchResult, int[][] episodeFilter, String languageName) throws Exception;

	public URI getSubtitleListLink(SubtitleSearchResult searchResult, String languageName);

	public String getName();

	public URI getLink();

	public Icon getIcon();

}
