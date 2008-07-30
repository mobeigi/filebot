
package net.sourceforge.filebot.ui.panel.rename.metric;


import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public abstract class AbstractNameSimilarityMetric implements SimilarityMetric {
	
	@Override
	public float getSimilarity(ListEntry a, ListEntry b) {
		return getSimilarity(normalize(a.getName()), normalize(b.getName()));
	}
	

	protected String normalize(String name) {
		name = stripChecksum(name);
		name = normalizeSeparators(name);
		
		return name.trim().toLowerCase();
	}
	

	protected String normalizeSeparators(String name) {
		return name.replaceAll("[\\._ ]+", " ");
	}
	

	protected String stripChecksum(String name) {
		return name.replaceAll("\\[\\p{XDigit}{8}\\]", "");
	}
	

	public abstract float getSimilarity(String a, String b);
	
}
