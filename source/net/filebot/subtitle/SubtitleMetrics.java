package net.filebot.subtitle;

import static java.util.Collections.*;
import static net.filebot.media.MediaDetection.*;
import static net.filebot.similarity.EpisodeMetrics.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.filebot.mediainfo.MediaInfo;
import net.filebot.mediainfo.MediaInfo.StreamKind;
import net.filebot.similarity.CrossPropertyMetric;
import net.filebot.similarity.EpisodeMetrics;
import net.filebot.similarity.MetricAvg;
import net.filebot.similarity.MetricCascade;
import net.filebot.similarity.NameSimilarityMetric;
import net.filebot.similarity.NumericSimilarityMetric;
import net.filebot.similarity.SimilarityMetric;
import net.filebot.web.OpenSubtitlesSubtitleDescriptor;
import net.filebot.web.SubtitleDescriptor;

public enum SubtitleMetrics implements SimilarityMetric {

	// subtitle verification metric specifically excluding SxE mismatches
	AbsoluteSeasonEpisode(new SimilarityMetric() {

		@Override
		public float getSimilarity(Object o1, Object o2) {
			float f = SeasonEpisode.getSimilarity(o1, o2);
			if (f == 0 && (getEpisodeIdentifier(o1.toString(), true) == null) == (getEpisodeIdentifier(o2.toString(), true) == null)) {
				return 0;
			}
			return f < 1 ? -1 : 1;
		}
	}),

	DiskNumber(new NumericSimilarityMetric() {

		private final Pattern CDNO = Pattern.compile("(?:CD|DISK)(\\d+)", Pattern.CASE_INSENSITIVE);

		@Override
		public float getSimilarity(Object o1, Object o2) {
			int c1 = getDiskNumber(o1);
			int c2 = getDiskNumber(o2);

			if (c1 == 0 && c2 == 0) // undefined
				return 0;

			return c1 == c2 ? 1 : -1; // positive or negative match
		}

		public int getDiskNumber(Object o) {
			int cd = 0;
			Matcher matcher = CDNO.matcher(o.toString());
			while (matcher.find()) {
				cd = Integer.parseInt(matcher.group(1));
			}
			return cd;
		}
	}),

	VideoProperties(new CrossPropertyMetric() {

		private final String FPS = "FPS";
		private final String SECONDS = "SECS";

		public float getSimilarity(Object o1, Object o2) {
			return o1 instanceof SubtitleDescriptor ? super.getSimilarity(o1, o2) : super.getSimilarity(o2, o1); // make sure that SubtitleDescriptor is o1
		};

		protected Map<String, Object> getProperties(Object object) {
			if (object instanceof OpenSubtitlesSubtitleDescriptor) {
				return getSubtitleProperties((OpenSubtitlesSubtitleDescriptor) object);
			} else if (object instanceof File) {
				return getVideoProperties((File) object);
			}
			return emptyMap();
		};

		private Map<String, Object> getSubtitleProperties(OpenSubtitlesSubtitleDescriptor subtitle) {
			Map<String, Object> props = new HashMap<String, Object>();

			try {
				float fps = Math.round(subtitle.getMovieFPS()); // round because most FPS values in the database are bad anyway
				if (fps > 0) {
					props.put(FPS, fps);
				}
				long seconds = (long) Math.floor(subtitle.getMovieTimeMS() / (double) 1000);
				if (seconds > 0) {
					props.put(SECONDS, seconds);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return props;
		}

		private final Map<File, Map<String, Object>> mediaInfoCache = new WeakHashMap<File, Map<String, Object>>(64);

		private Map<String, Object> getVideoProperties(File file) {
			synchronized (mediaInfoCache) {
				return mediaInfoCache.computeIfAbsent(file, (f) -> {
					try {
						Map<String, Object> props = new HashMap<String, Object>();

						MediaInfo mediaInfo = new MediaInfo();
						if (mediaInfo.open(file)) {
							float fps = Math.round(Float.parseFloat(mediaInfo.get(StreamKind.Video, 0, "FrameRate")));
							if (fps > 0) {
								props.put(FPS, fps);
							}
							long seconds = (long) Math.floor(Long.parseLong(mediaInfo.get(StreamKind.Video, 0, "Duration")) / (double) 1000);
							if (seconds > 0) {
								props.put(SECONDS, seconds);
							}
						}

						return props;
					} catch (Exception e) {
						return emptyMap();
					}
				});
			}
		}
	});

	// inner metric
	private final SimilarityMetric metric;

	private SubtitleMetrics(SimilarityMetric metric) {
		this.metric = metric;
	}

	@Override
	public float getSimilarity(Object o1, Object o2) {
		return metric.getSimilarity(o1, o2);
	}

	public static SimilarityMetric[] defaultSequence() {
		return new SimilarityMetric[] { EpisodeFunnel, EpisodeBalancer, NameSubstringSequence, new MetricCascade(NameSubstringSequence, Name), Numeric, FileName, DiskNumber, VideoProperties, new NameSimilarityMetric() };
	}

	public static SimilarityMetric verificationMetric() {
		return EpisodeMetrics.verificationMetric();
	}

	public static SimilarityMetric sanityMetric() {
		return new MetricCascade(AbsoluteSeasonEpisode, AirDate, new MetricAvg(NameSubstringSequence, Name), getMovieMatchMetric());
	}

}
