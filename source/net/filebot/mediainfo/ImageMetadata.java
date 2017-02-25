package net.filebot.mediainfo;

import static net.filebot.Logging.*;
import static net.filebot.similarity.Normalization.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import net.filebot.util.FileUtilities.ExtensionFileFilter;

public class ImageMetadata extends LinkedHashMap<String, String> {

	public ImageMetadata(File file) throws ImageProcessingException, IOException {
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
	}

	public static ZonedDateTime getDateTaken(File file) {
		if (SUPPORTED_FILE_TYPES.accept(file)) {
			try {
				Metadata metadata = ImageMetadataReader.readMetadata(file);
				ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
				Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME);

				if (date != null) {
					return date.toInstant().atZone(ZoneOffset.UTC);
				}
			} catch (Exception e) {
				debug.warning(e::toString);
			}
		}

		return null;
	}

	public static final FileFilter SUPPORTED_FILE_TYPES = new ExtensionFileFilter("jpg", "jpeg", "png", "webp", "gif", "ico", "bmp", "tiff", "psd", "pcx", "raw", "crw", "cr2", "nef", "orf", "raf", "rw2", "rwl", "srw", "arw", "dng", "x3f", "mov", "mp4", "m4v", "3g2", "3gp", "3gp");

}
