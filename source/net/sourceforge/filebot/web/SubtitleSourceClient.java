
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class SubtitleSourceClient implements SubtitleProvider {
	
	private static final String host = "www.subtitlesource.org";
	
	private static final int pageSize = 20;
	
	
	@Override
	public String getName() {
		return "SubtitleSource";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.subtitlesource");
	}
	

	@Override
	public List<SearchResult> search(String query) throws Exception {
		return search(query, "all");
	}
	

	public List<SearchResult> search(String query, String language) throws Exception {
		// e.g. http://www.subtitlesource.org/api/xmlsearch/firefly/all/0
		URL url = new URL("http", host, "/api/xmlsearch/" + URLEncoder.encode(query, "utf-8") + "/" + language + "/0");
		
		Document dom = getDocument(url);
		
		Map<Integer, MovieDescriptor> movieMap = new LinkedHashMap<Integer, MovieDescriptor>();
		
		for (Node node : selectNodes("//sub", dom)) {
			Integer imdb = Integer.valueOf(getTextContent("imdb", node));
			
			if (!movieMap.containsKey(imdb)) {
				String title = getTextContent("title", node);
				
				movieMap.put(imdb, new MovieDescriptor(title, imdb));
			}
		}
		
		return new ArrayList<SearchResult>(movieMap.values());
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, Locale language) throws Exception {
		// english language name or null
		String languageFilter = (language == null || language == Locale.ROOT) ? null : language.getDisplayLanguage(Locale.ENGLISH);
		
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		
		for (SubtitleDescriptor subtitle : getSubtitleList(searchResult)) {
			if (languageFilter == null || languageFilter.equalsIgnoreCase(subtitle.getLanguageName())) {
				subtitles.add(subtitle);
			}
		}
		
		return subtitles;
	}
	

	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult) throws Exception {
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		
		for (int offset = 0; true; offset += pageSize) {
			List<SubtitleDescriptor> page = getSubtitleList(searchResult, offset);
			
			// add new subtitles
			subtitles.addAll(page);
			
			if (page.size() < pageSize) {
				// last page reached
				return subtitles;
			}
		}
	}
	

	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, int offset) throws Exception {
		int imdb = ((MovieDescriptor) searchResult).getImdbId();
		
		// e.g. http://www.subtitlesource.org/api/xmlsearch/0303461/imdb/0
		URL url = new URL("http", host, "/api/xmlsearch/" + imdb + "/imdb/" + offset);
		
		Document dom = getDocument(url);
		
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		
		for (Node node : selectNodes("//sub", dom)) {
			int id = Integer.parseInt(getTextContent("id", node));
			String releaseName = getTextContent("releasename", node);
			String language = getTextContent("language", node);
			String title = getTextContent("title", node);
			int season = Integer.parseInt(getTextContent("season", node));
			int episode = Integer.parseInt(getTextContent("episode", node));
			
			// e.g. http://www.subtitlesource.org/download/zip/760
			URL downloadLink = new URL("http", host, "/download/zip/" + id);
			
			subtitles.add(new SubtitleSourceSubtitleDescriptor(releaseName, language, title, season, episode, downloadLink));
		}
		
		return subtitles;
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, Locale language) {
		int imdb = ((MovieDescriptor) searchResult).getImdbId();
		
		try {
			return new URI("http://" + host + "/title/" + String.format("tt%07d", imdb));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
}
