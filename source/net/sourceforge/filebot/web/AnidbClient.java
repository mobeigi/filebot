
package net.sourceforge.filebot.web;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.resources.ResourceManager;
import net.sourceforge.tuned.FunctionIterator;
import net.sourceforge.tuned.ProgressIterator;
import net.sourceforge.tuned.XPathUtil;
import net.sourceforge.tuned.FunctionIterator.Function;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;


public class AnidbClient extends EpisodeListClient {
	
	private final SearchResultCache cache = new SearchResultCache();
	
	private final String host = "anidb.info";
	
	
	public AnidbClient() {
		super("AniDB", ResourceManager.getIcon("search.anidb"), false);
	};
	

	@Override
	public List<SearchResult> search(String searchterm) throws IOException, SAXException {
		if (cache.containsKey(searchterm)) {
			return Collections.singletonList(cache.get(searchterm));
		}
		
		Document dom = HtmlUtil.getHtmlDocument(getSearchUrl(searchterm));
		
		List<Node> nodes = XPathUtil.selectNodes("//TABLE[@class='animelist']//TR/TD/ancestor::TR", dom);
		
		List<SearchResult> searchResults = new ArrayList<SearchResult>(nodes.size());
		
		if (!nodes.isEmpty())
			for (Node node : nodes) {
				Node titleNode = XPathUtil.selectNode("./TD[@class='name']/A", node);
				
				String title = XPathUtil.selectString(".", titleNode);
				String href = XPathUtil.selectString("@href", titleNode);
				
				String file = "/perl-bin/" + href;
				
				try {
					URL url = new URL("http", host, file);
					
					searchResults.add(new HyperLink(title, url));
				} catch (MalformedURLException e) {
					Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, "Invalid href: " + href);
				}
			}
		else {
			// we might have been redirected to the episode list page directly
			List<Node> list = XPathUtil.selectNodes("//TABLE[@class='eplist']", dom);
			
			if (!list.isEmpty()) {
				// get show's name from the document
				String header = XPathUtil.selectString("id('layout-content')//H1[1]", dom);
				String title = header.replaceFirst("Anime:\\s*", "");
				
				searchResults.add(new HyperLink(title, getSearchUrl(searchterm)));
			}
		}
		
		cache.addAll(searchResults);
		
		return searchResults;
	}
	

	@Override
	public ProgressIterator<Episode> getEpisodeList(SearchResult searchResult, int season) throws IOException, SAXException {
		
		Document dom = HtmlUtil.getHtmlDocument(getEpisodeListLink(searchResult, season));
		
		List<Node> nodes = XPathUtil.selectNodes("id('eplist')//TR/TD/SPAN/ancestor::TR", dom);
		
		return new FunctionIterator<Node, Episode>(nodes, new EpisodeFunction(searchResult, nodes.size()));
	}
	
	
	private static class EpisodeFunction implements Function<Node, Episode> {
		
		private final SearchResult searchResult;
		private final NumberFormat numberFormat;
		
		
		public EpisodeFunction(SearchResult searchResult, int nodeCount) {
			this.searchResult = searchResult;
			
			numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
			numberFormat.setMinimumIntegerDigits(Math.max(Integer.toString(nodeCount).length(), 2));
			numberFormat.setGroupingUsed(false);
		}
		

		@Override
		public Episode evaluate(Node node) {
			String number = XPathUtil.selectString("./TD[contains(@class,'id')]/A", node);
			String title = XPathUtil.selectString("./TD[@class='title']/LABEL/text()", node);
			
			if (title.startsWith("recap"))
				title = title.replaceFirst("recap", "");
			
			try {
				// try to format number of episode
				number = numberFormat.format(Integer.parseInt(number));
			} catch (NumberFormatException ex) {
				// leave it be
			}
			
			// no seasons for anime
			return new Episode(searchResult.getName(), null, number, title);
		}
		
	}
	
	
	@Override
	public URI getEpisodeListLink(SearchResult searchResult, int season) {
		return ((HyperLink) searchResult).getUri();
	}
	

	private URL getSearchUrl(String searchterm) throws UnsupportedEncodingException, MalformedURLException {
		String qs = URLEncoder.encode(searchterm, "UTF-8");
		
		// type=2 -> only TV Series
		String file = "/perl-bin/animedb.pl?type=2&show=animelist&orderby.name=0.1&orderbar=0&noalias=1&do.search=Search&adb.search=" + qs;
		
		return new URL("http", host, file);
	}
	
}
