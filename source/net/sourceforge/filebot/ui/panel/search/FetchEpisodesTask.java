
package net.sourceforge.filebot.ui.panel.search;


import java.util.List;

import javax.swing.SwingWorker;

import net.sourceforge.filebot.web.Episode;
import net.sourceforge.filebot.web.SearchEngine;


public class FetchEpisodesTask extends SwingWorker<List<Episode>, Object> {
	
	private String showName;
	private SearchEngine searchEngine;
	private int numberOfSeason;
	private long duration;
	
	
	public FetchEpisodesTask(SearchEngine searchEngine, String showname, int numberOfSeason) {
		showName = showname;
		this.searchEngine = searchEngine;
		this.numberOfSeason = numberOfSeason;
	}
	

	@Override
	protected List<Episode> doInBackground() throws Exception {
		long start = System.currentTimeMillis();
		
		List<Episode> episodes = searchEngine.getEpisodeList(showName, numberOfSeason);
		
		duration = System.currentTimeMillis() - start;
		return episodes;
	}
	

	public String getShowName() {
		return showName;
	}
	

	public int getNumberOfSeason() {
		return numberOfSeason;
	}
	

	public long getDuration() {
		return duration;
	}
	

	public SearchEngine getSearchEngine() {
		return searchEngine;
	}
	
}
