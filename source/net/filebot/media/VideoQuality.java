package net.filebot.media;

import static net.filebot.util.StringUtilities.*;
import static java.util.Comparator.*;
import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;

import java.io.File;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.filebot.format.MediaBindingBean;

public class VideoQuality implements Comparator<File> {

	public static boolean isBetter(File f1, File f2) {
		return new VideoQuality().compare(f1, f2) > 0;
	}

	@Override
	public int compare(File f1, File f2) {
		ToDoubleFunction<File> repack = this::isRepack;
		ToDoubleFunction<File> resolution = this::getResolution;
		ToDoubleFunction<File> size = this::getSize;

		return Stream.of(repack, resolution, size).mapToInt(c -> {
			try {
				return comparingDouble(c).compare(f1, f2);
			} catch (Throwable e) {
				debug.warning(format("Failed to read media info: %s", e.getMessage()));
				return 0;
			}
		}).filter(i -> i != 0).findFirst().orElse(0);
	}

	private Optional<MediaBindingBean> media(File f) {
		if (VIDEO_FILES.accept(f) || SUBTITLE_FILES.accept(f)) {
			return Optional.of(new MediaBindingBean(null, f, null));
		}
		return Optional.empty();
	}

	private static final Pattern REPACK = Pattern.compile("(?<!\\p{Alnum})(PROPER|REPACK)(?!\\p{Alnum})", Pattern.CASE_INSENSITIVE);
	private static final double ZERO = 0;

	public double isRepack(File f) {
		return media(f).map(it -> {
			return find(it.getFileName(), REPACK) || find(it.getOriginalFileName(), REPACK) ? 1 : ZERO;
		}).orElse(ZERO);
	}

	public double getResolution(File f) {
		return media(f).map(it -> {
			return it.getDimension().stream().mapToDouble(Number::doubleValue).reduce((a, b) -> a * b).orElse(ZERO);
		}).orElse(ZERO);
	}

	public double getSize(File f) {
		return media(f).map(it -> {
			return it.getInferredMediaFile().length();
		}).orElseGet(f::length);
	}

}
