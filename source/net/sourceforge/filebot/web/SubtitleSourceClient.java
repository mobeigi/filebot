
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.getDocument;
import static net.sourceforge.tuned.XPathUtilities.getTextContent;
import static net.sourceforge.tuned.XPathUtilities.selectNodes;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Icon;

import net.sourceforge.filebot.ResourceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Node;


public class SubtitleSourceClient implements SubtitleClient {
	
	protected static final String HOST = "www.subtitlesource.org";
	
	private static final int PAGE_SIZE = 20;
	
	
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
		URL url = new URL("http", HOST, "/api/xmlsearch/" + URLEncoder.encode(query, "utf-8") + "/" + language + "/0");
		
		Document dom = getDocument(url);
		
		Map<Integer, String> movieMap = new LinkedHashMap<Integer, String>();
		
		for (Node node : selectNodes("//sub", dom)) {
			Integer imdb = Integer.valueOf(getTextContent("imdb", node));
			
			if (!movieMap.containsKey(imdb)) {
				String title = getTextContent("title", node);
				movieMap.put(imdb, title);
			}
		}
		
		// create SearchResult collection
		List<SearchResult> result = new ArrayList<SearchResult>();
		
		for (Entry<Integer, String> movie : movieMap.entrySet()) {
			result.add(new MovieDescriptor(movie.getValue(), movie.getKey()));
		}
		
		return result;
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
		
		for (int offset = 0; true; offset += PAGE_SIZE) {
			List<SubtitleDescriptor> page = getSubtitleList(searchResult, offset);
			
			// add new subtitles
			subtitles.addAll(page);
			
			if (page.size() < PAGE_SIZE) {
				// last page reached
				return subtitles;
			}
		}
	}
	

	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, int offset) throws Exception {
		int imdb = ((MovieDescriptor) searchResult).getImdbId();
		
		// e.g. http://www.subtitlesource.org/api/xmlsearch/0303461/imdb/0
		URL url = new URL("http", HOST, "/api/xmlsearch/" + imdb + "/imdb/" + offset);
		
		Document dom = getDocument(url);
		
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		
		for (Node node : selectNodes("//sub", dom)) {
			int id = Integer.parseInt(getTextContent("id", node));
			String releaseName = getTextContent("releasename", node);
			String language = getTextContent("language", node);
			String title = getTextContent("title", node);
			int season = Integer.parseInt(getTextContent("season", node));
			int episode = Integer.parseInt(getTextContent("episode", node));
			
			subtitles.add(new SubtitleSourceSubtitleDescriptor(id, releaseName, language, title, season, episode));
		}
		
		return subtitles;
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, Locale language) {
		int imdb = ((MovieDescriptor) searchResult).getImdbId();
		
		try {
			return new URI("http://" + HOST + "/title/" + String.format("tt%07d", imdb));
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
	
}
