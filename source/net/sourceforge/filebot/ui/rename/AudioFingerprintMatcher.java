package net.sourceforge.filebot.ui.rename;

import static net.sourceforge.filebot.MediaTypes.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.awt.Component;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import net.sourceforge.filebot.similarity.Match;
import net.sourceforge.filebot.web.AudioTrack;
import net.sourceforge.filebot.web.ID3Lookup;
import net.sourceforge.filebot.web.MusicIdentificationService;
import net.sourceforge.filebot.web.SortOrder;

class AudioFingerprintMatcher implements AutoCompleteMatcher {

	private MusicIdentificationService service;

	public AudioFingerprintMatcher(MusicIdentificationService service) {
		this.service = service;
	}

	@Override
	public List<Match<File, ?>> match(List<File> files, SortOrder order, Locale locale, boolean autodetection, Component parent) throws Exception {
		List<Match<File, ?>> matches = new ArrayList<Match<File, ?>>();
		List<File> audioFiles = filter(files, AUDIO_FILES, VIDEO_FILES);

		// check audio files against acoustid
		if (audioFiles.size() > 0) {
			for (Entry<File, AudioTrack> it : service.lookup(audioFiles).entrySet()) {
				if (it.getKey().exists() && it.getValue() != null) {
					AudioTrack track = it.getValue().clone();

					// use AcoustID as default but prefer with ID3 data if available
					if (!autodetection) {
						AudioTrack id3 = new ID3Lookup().lookup(Collections.singleton(it.getKey())).get(it.getKey());
						for (Field field : AudioTrack.class.getDeclaredFields()) {
							if (!field.isAccessible()) {
								field.setAccessible(true);
							}
							Object id3value = field.get(id3);
							if (id3value != null && !id3value.toString().isEmpty()) {
								field.set(track, id3value);
							}
						}
					}

					matches.add(new Match<File, AudioTrack>(it.getKey(), track));
				}
			}
		}

		return matches;
	}

}
