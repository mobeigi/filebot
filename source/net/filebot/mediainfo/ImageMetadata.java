package net.filebot.mediainfo;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static net.filebot.Logging.*;
import static net.filebot.util.JsonUtilities.*;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.util.FileUtilities.ExtensionFileFilter;

public class ImageMetadata {

	private File file;
	private Metadata metadata;

	public ImageMetadata(File file) {
		this.file = file;
	}

	protected synchronized Metadata getMetadata() throws ImageProcessingException, IOException {
		if (metadata == null) {
			metadata = ImageMetadataReader.readMetadata(file);
		}
		return metadata;
	}

	protected boolean accept(Directory directory) {
		return !directory.getName().matches("JPEG|JFIF|Interoperability|Huffman|File");
	}

	public Map<String, String> snapshot(Function<Tag, String> key) throws ImageProcessingException, IOException {
		Map<String, String> values = new LinkedHashMap<String, String>();

		for (Directory directory : getMetadata().getDirectories()) {
			if (accept(directory)) {
				for (Tag tag : directory.getTags()) {
					String v = tag.getDescription();
					if (v != null && v.length() > 0) {
						values.put(key.apply(tag), v);
					}
				}
			}
		}

		return values;
	}

	public Optional<ZonedDateTime> getDateTaken() {
		return extract(m -> m.getFirstDirectoryOfType(ExifIFD0Directory.class).getDate(ExifSubIFDDirectory.TAG_DATETIME)).map(d -> {
			return d.toInstant().atZone(ZoneOffset.UTC);
		});
	}

	public Optional<List<String>> getLocationTaken() {
		return extract(m -> m.getFirstDirectoryOfType(GpsDirectory.class).getGeoLocation()).map(this::locate);
	}

	protected List<String> locate(GeoLocation location) {
		try {
			// e.g. https://maps.googleapis.com/maps/api/geocode/json?latlng=40.7470444,-073.9411611
			Cache cache = Cache.getCache("geocode", CacheType.Monthly);
			Object json = cache.json(location.getLatitude() + "," + location.getLongitude(), pos -> new URL("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + pos)).get();

			Map<AddressComponents, String> address = new EnumMap<AddressComponents, String>(AddressComponents.class);

			streamJsonObjects(json, "results").limit(1).forEach(r -> {
				streamJsonObjects(r, "address_components").forEach(a -> {
					String name = getString(a, "long_name");
					for (Object type : getArray(a, "types")) {
						stream(AddressComponents.values()).filter(c -> c.name().equals(type)).findFirst().ifPresent(c -> {
							address.putIfAbsent(c, name);
						});
					}
				});
			});

			return address.values().stream().filter(Objects::nonNull).collect(toList()); // enum set is always in natural order
		} catch (Exception e) {
			debug.warning(e::toString);
		}

		return emptyList();
	}

	private enum AddressComponents {
		country, administrative_area_level_1, administrative_area_level_2, administrative_area_level_3, administrative_area_level_4, sublocality, neighborhood;
	}

	protected <T> Optional<T> extract(Function<Metadata, T> extract) {
		if (SUPPORTED_FILE_TYPES.accept(file)) {
			try {
				return Optional.ofNullable(extract.apply(getMetadata()));
			} catch (Exception e) {
				debug.warning(e::toString);
			}
		}
		return Optional.empty();
	}

	public static final FileFilter SUPPORTED_FILE_TYPES = new ExtensionFileFilter("jpg", "jpeg", "png", "webp", "gif", "ico", "bmp", "tiff", "psd", "pcx", "raw", "crw", "cr2", "nef", "orf", "raf", "rw2", "rwl", "srw", "arw", "dng", "x3f", "mov", "mp4", "m4v", "3g2", "3gp", "3gp");

}
