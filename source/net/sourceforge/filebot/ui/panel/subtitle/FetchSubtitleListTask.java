
package net.sourceforge.filebot.ui.panel.subtitle;


import java.util.List;

import javax.swing.SwingWorker;

import net.sourceforge.filebot.web.MovieDescriptor;
import net.sourceforge.filebot.web.SubtitleClient;
import net.sourceforge.filebot.web.SubtitleDescriptor;


class FetchSubtitleListTask extends SwingWorker<List<? extends SubtitleDescriptor>, Void> {
	
	private final SubtitleClient client;
	private final MovieDescriptor descriptor;
	
	private long duration = -1;
	
	
	public FetchSubtitleListTask(MovieDescriptor descriptor, SubtitleClient client) {
		this.descriptor = descriptor;
		this.client = client;
	}
	

	@Override
	protected List<? extends SubtitleDescriptor> doInBackground() throws Exception {
		long start = System.currentTimeMillis();
		
		List<? extends SubtitleDescriptor> list = client.getSubtitleList(descriptor);
		
		duration = System.currentTimeMillis() - start;
		return list;
	}
	

	public SubtitleClient getClient() {
		return client;
	}
	

	public MovieDescriptor getDescriptor() {
		return descriptor;
	}
	

	public long getDuration() {
		return duration;
	}
}
