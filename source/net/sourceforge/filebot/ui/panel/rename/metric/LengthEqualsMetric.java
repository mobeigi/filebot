
package net.sourceforge.filebot.ui.panel.rename.metric;


import net.sourceforge.filebot.ui.panel.rename.entry.AbstractFileEntry;
import net.sourceforge.filebot.ui.panel.rename.entry.ListEntry;


public class LengthEqualsMetric implements SimilarityMetric {
	
	@Override
	public float getSimilarity(ListEntry a, ListEntry b) {
		if ((a instanceof AbstractFileEntry) && (b instanceof AbstractFileEntry)) {
			long lengthA = ((AbstractFileEntry) a).getLength();
			long lengthB = ((AbstractFileEntry) b).getLength();
			
			if (lengthA == lengthB)
				return 1;
		}
		
		return 0;
	}
	

	@Override
	public String getDescription() {
		return "Check whether file size is equal or not";
	}
	

	@Override
	public String getName() {
		return "Length";
	}
	
}
