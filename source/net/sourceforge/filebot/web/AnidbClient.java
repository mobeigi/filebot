
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.XPathUtilities.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import net.sourceforge.filebot.ResourceManager;


public class AnidbClient implements EpisodeListProvider {
	
	private static final String host = "anidb.net";
	

	@Override
	public String getName() {
		return "AniDB";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.anidb");
	}
	

	@Override
	public List<SearchResult> search(String query) throws IOException, SAXException {
		// Air Status: ignore
		// Anime Type: TV Series, TV Special, OVA
		// Hide Synonyms: true
		URL searchUrl = new URL("http", host, "/perl-bin/animedb.pl?type.tvspecial=1&type.tvseries=1&type.ova=1&show=animelist&orderby.name=0.1&noalias=1&do.update=update&adb.search=" + URLEncoder.encode(query, "UTF-8"));
		
		Document dom = getHtmlDocument(searchUrl);
		
		List<Node> nodes = selectNodes("//TABLE[@class='animelist']//TR/TD/ancestor::TR", dom);
		
		List<SearchResult> results = new ArrayList<SearchResult>(nodes.size());
		
		for (Node node : nodes) {
			Node link = selectNode("./TD[@class='name']/A", node);
			
			// prefer title that is similar to the search query
			String title = selectString("./following-sibling::*[@class='match']", link);
			
			// remove leading and trailing parenthesis
			title = title.replaceAll("(^\\()|(\\)$)", "");
			
			if (title.isEmpty()) {
				// fallback: use main title
				title = getTextContent(link);
			}
			
			// anime page
			String href = getAttribute("href", link);
			
			try {
				results.add(new HyperLink(title, new URL("http", host, "/perl-bin/" + href)));
			} catch (MalformedURLException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid href: " + href);
			}
		}
		
		// we might have been redirected to the episode list page
		if (results.isEmpty()) {
			// get anime information from document
			String title = selectTitle(dom);
			String link = selectString("//*[@class='data']//A[@class='short_link']/@href", dom);
			
			try {
				// insert single entry
				results.add(new HyperLink(title, new URL(link)));
			} catch (MalformedURLException e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Invalid location: " + link);
			}
		}
		
		return results;
	}
	

	protected String selectTitle(Document animePage) {
		// extract name from header (e.g. "Anime: Naruto")
		return selectString("//H1", animePage).replaceFirst("Anime:\\s*", "");
	}
	

	protected String selectOfficialTitle(Document animePage, String languageName) {
		// create xpath query for official title of the given language
		// e.g. //*[@class='data']//*[contains(@class, 'official') and .//*[contains(@title, 'english')]]//LABEL
		
		String condition = String.format(".//*[contains(@title, '%s')]", languageName.toLowerCase());
		String xpath = String.format("//*[@class='data']//*[contains(@class, 'official') and %s]//LABEL", condition);
		
		return selectString(xpath, animePage);
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult) throws IOException, SAXException {
		
		Document dom = getHtmlDocument(getEpisodeListLink(searchResult).toURL());
		
		// use title from anime page
		String animeTitle = selectTitle(dom);
		
		List<Node> nodes = selectNodes("id('eplist')//TR/TD/SPAN/ancestor::TR", dom);
		
		List<Episode> episodes = new ArrayList<Episode>(nodes.size());
		
		for (Node node : nodes) {
			List<Node> columns = getChildren("TD", node);
			
			String number = getTextContent("A", columns.get(0));
			String title = getTextContent("LABEL", columns.get(1));
			
			// if number does not match, episode is probably some kind of special (S1, S2, ...)
			if (number.matches("\\d+")) {
				// no seasons for anime
				episodes.add(new Episode(animeTitle, null, number, title));
			}
		}
		
		return episodes;
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult) {
		return ((HyperLink) searchResult).toURI();
	}
	

	@Override
	public boolean hasSingleSeasonSupport() {
		return false;
	}
	

	@Override
	public List<Episode> getEpisodeList(SearchResult searchResult, int season) throws Exception {
		throw new UnsupportedOperationException();
	}
	

	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return null;
	}
	
}
