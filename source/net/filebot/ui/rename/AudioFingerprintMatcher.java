package net.filebot.ui.rename;

import static net.filebot.MediaTypes.*;
import static net.filebot.util.FileUtilities.*;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import net.filebot.similarity.Match;
import net.filebot.web.AudioTrack;
import net.filebot.web.MusicIdentificationService;
import net.filebot.web.SortOrder;

class AudioFingerprintMatcher implements AutoCompleteMatcher {

	private MusicIdentificationService service;

	public AudioFingerprintMatcher(MusicIdentificationService service) {
		this.service = service;
	}

	@Override
	public List<Match<File, ?>> match(List<File> files, boolean strict, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		List<File> audioFiles = filter(files, AUDIO_FILES, VIDEO_FILES);

		// check audio files against AcoustID
		if (audioFiles.size() > 0) {
			for (Entry<File, AudioTrack> it : service.lookup(audioFiles).entrySet()) {
				if (it.getKey().exists() && it.getValue() != null) {
					AudioTrack track = it.getValue().clone();
					matches.add(new Match<File, AudioTrack>(it.getKey(), track));
				}
			}
		}

		return matches;
	}

}
