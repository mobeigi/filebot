
package net.sourceforge.filebot.ui.panel.rename.similarity;


import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public class StringEqualsMetric extends SimilarityMetric {
	
	@Override
	public float getSimilarity(ListEntry<?> a, ListEntry<?> b) {
		if (a.toString().equalsIgnoreCase(b.toString())) {
			return 1;
		}
		
		return 0;
	}
	

	@Override
	public String getDescription() {
		return "Check whether names are equal or not";
	}
	

	@Override
	public String getName() {
		return "String";
	}
	
}
