
package net.sourceforge.filebot.ui.panel.rename.metric;


import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public interface SimilarityMetric {
	
	public float getSimilarity(ListEntry a, ListEntry b);
	

	public String getDescription();
	

	public String getName();
}
