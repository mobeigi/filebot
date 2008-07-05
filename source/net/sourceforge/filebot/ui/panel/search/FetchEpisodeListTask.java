
package net.sourceforge.filebot.ui.panel.search;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingWorker;

import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.EpisodeListClient;
import net.sourceforge.filebot.web.SearchResult;


class FetchEpisodeListTask extends SwingWorker<List<Episode>, Void> {
	
	private final SearchResult searchResult;
	private final EpisodeListClient searchEngine;
	private final int numberOfSeason;
	
	private long duration = -1;
	
	
	public FetchEpisodeListTask(EpisodeListClient searchEngine, SearchResult searchResult, int numberOfSeason) {
		this.searchEngine = searchEngine;
		this.searchResult = searchResult;
		this.numberOfSeason = numberOfSeason;
	}
	

	@Override
	protected List<Episode> doInBackground() throws Exception {
		long start = System.currentTimeMillis();
		
		Iterator<Episode> itr = searchEngine.getEpisodeList(searchResult, numberOfSeason).iterator();
		
		ArrayList<Episode> list = new ArrayList<Episode>();
		
		while (itr.hasNext())
			list.add(itr.next());
		
		duration = System.currentTimeMillis() - start;
		return list;
	}
	

	public EpisodeListClient getSearchEngine() {
		return searchEngine;
	}
	

	public SearchResult getSearchResult() {
		return searchResult;
	}
	

	public int getNumberOfSeason() {
		return numberOfSeason;
	}
	

	public long getDuration() {
		return duration;
	}
	
}
