
package net.sourceforge.filebot.ui.panel.episodelist;


import java.util.ArrayList;
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
		
		List<Episode> list = new ArrayList<Episode>();
		
		if (numberOfSeason == SeasonSpinnerModel.ALL_SEASONS) {
			list.addAll(searchEngine.getEpisodeList(searchResult));
		} else {
			list.addAll(searchEngine.getEpisodeList(searchResult, numberOfSeason));
		}
		
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
