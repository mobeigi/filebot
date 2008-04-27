
package net.sourceforge.filebot.ui.panel.rename.similarity;


import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public abstract class SimilarityMetric {
	
	public abstract float getSimilarity(ListEntry a, ListEntry b);
	

	public abstract String getDescription();
	

	public abstract String getName();
}
