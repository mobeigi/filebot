package net.filebot.mediainfo;

import static net.filebot.Logging.*;
import static net.filebot.similarity.Normalization.*;

import java.io.File;
import java.util.LinkedHashMap;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class ImageMetadata extends LinkedHashMap<String, String> {

	public ImageMetadata(File file) {
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file);

			for (Directory directory : metadata.getDirectories()) {
				for (Tag tag : directory.getTags()) {
					String v = tag.getDescription();
					if (v != null && v.length() > 0) {
						putIfAbsent(normalizeSpace(normalizePunctuation(tag.getTagName()), "_"), v);
					}
				}

				if (directory.hasErrors()) {
					for (String error : directory.getErrors()) {
						debug.warning(error);
					}
				}
			}
		} catch (Throwable e) {
			debug.warning(e::toString);
		}
	}

}
