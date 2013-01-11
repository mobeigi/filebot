
package net.sourceforge.filebot.ui.rename;


import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.AcoustID;
import net.sourceforge.filebot.web.AudioTrack;
import net.sourceforge.filebot.web.SortOrder;


class AudioFingerprintMatcher implements AutoCompleteMatcher {
	
	private AcoustID service;
	
	
	public AudioFingerprintMatcher(AcoustID service) {
		this.service = service;
	}
	
	
	@Override
	public List<Match<File, ?>> match(List<File> files, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		
		// check audio files against acoustid
		for (Entry<File, AudioTrack> it : service.lookup(filter(files, AUDIO_FILES)).entrySet()) {
			if (it.getKey().exists() && it.getValue() != null) {
				matches.add(new Match<File, AudioTrack>(it.getKey(), it.getValue()));
			}
		}
		
		return matches;
	}
	
}
