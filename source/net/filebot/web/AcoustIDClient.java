package net.filebot.web;

import static net.filebot.web.WebRequest.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.ResourceManager;

import com.cedarsoftware.util.io.JsonObject;
import com.cedarsoftware.util.io.JsonReader;

public class AcoustIDClient implements MusicIdentificationService {

	private static final FloodLimit REQUEST_LIMIT = new FloodLimit(3, 1, TimeUnit.SECONDS);

	private String apikey;

	public AcoustIDClient(String apikey) {
		this.apikey = apikey;
	}

	@Override
	public String getName() {
		return "AcoustID";
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.acoustid");
	}

	public Cache getCache() {
		return Cache.getCache("web-datasource-lv3");
	}

	@Override
	public Map<File, AudioTrack> lookup(Collection<File> files) throws Exception {
		Map<File, AudioTrack> results = new LinkedHashMap<File, AudioTrack>();

		if (files.size() > 0) {
			for (Map<String, String> fp : fpcalc(files)) {
				File file = new File(fp.get("FILE"));
				int duration = Integer.parseInt(fp.get("DURATION"));
				String fingerprint = fp.get("FINGERPRINT");

				if (duration > 10 && fingerprint != null) {
					String response = lookup(duration, fingerprint);
					if (response != null && response.length() > 0) {
						results.put(file, parseResult(lookup(duration, fingerprint), duration));
					}
				}
			}
		}

		return results;
	}

	public String lookup(int duration, String fingerprint) throws IOException, InterruptedException {
		Map<String, String> postParam = new LinkedHashMap<String, String>();
		postParam.put("duration", String.valueOf(duration));
		postParam.put("fingerprint", fingerprint);

		String cacheKey = postParam.toString();
		Cache cache = getCache();
		String response = cache.get(cacheKey, String.class);
		if (response != null) {
			return response;
		}

		// respect rate limit
		REQUEST_LIMIT.acquirePermit();

		// http://api.acoustid.org/v2/lookup?client=8XaBELgH&meta=recordings+releasegroups+compress&duration=641&fingerprint=AQABz0qUkZK4oOfhL-CPc4e5C_wW2H2QH9uDL4cvoT8UNQ-eHtsE8cceeFJx-LiiHT-aPzhxoc-Opj_eI5d2hOFyMJRzfDk-QSsu7fBxqZDMHcfxPfDIoPWxv9C1o3yg44d_3Df2GJaUQeeR-cb2HfaPNsdxHj2PJnpwPMN3aPcEMzd-_MeB_Ej4D_CLP8ghHjkJv_jh_UDuQ8xnILwunPg6hF2R8HgzvLhxHVYP_ziJX0eKPnIE1UePMByDJyg7wz_6yELsB8n4oDmDa0Gv40hf6D3CE3_wH6HFaxCPUD9-hNeF5MfWEP3SCGym4-SxnXiGs0mRjEXD6fgl4LmKWrSChzzC33ge9PB3otyJMk-IVC6R8MTNwD9qKQ_CC8kPv4THzEGZS8GPI3x0iGVUxC1hRSizC5VzoamYDi-uR7iKPhGSI82PkiWeB_eHijvsaIWfBCWH5AjjCfVxZ1TQ3CvCTclGnEMfHbnZFA8pjD6KXwd__Cn-Y8e_I9cq6CR-4S9KLXqQcsxxoWh3eMxiHI6TIzyPv0M43YHz4yte-Cv-4D16Hv9F9C9SPUdyGtZRHV-OHEeeGD--BKcjVLOK_NCDXMfx44dzHEiOZ0Z44Rf6DH5R3uiPj4d_PKolJNyRJzyu4_CTD2WOvzjKH9GPb4cUP1Av9EuQd8fGCFee4JlRHi18xQh96NLxkCgfWFKOH6WGeoe4I3za4c5hTscTPEZTES1x8kE-9MQPjT8a8gh5fPgQZtqCFj9MDvp6fDx6NCd07bjx7MLR9AhtnFnQ70GjOcV0opmm4zpY3SOa7HiwdTtyHa6NC4e-HN-OfC5-OP_gLe2QDxfUCz_0w9l65HiPAz9-IaGOUA7-4MZ5CWFOlIfe4yUa6AiZGxf6w0fFxsjTOdC6Itbh4mGD63iPH9-RFy909XAMj7mC5_BvlDyO6kGTZKJxHUd4NDwuZUffw_5RMsde5CWkJAgXnDReNEaP6DTOQ65yaD88HoeX8fge-DSeHo9Qa8cTHc80I-_RoHxx_UHeBxrJw62Q34Kd7MEfpCcu6BLeB1ePw6OO4sOF_sHhmB504WWDZiEu8sKPpkcfCT9xfej0o0lr4T5yNJeOvjmu40w-TDmqHXmYgfFhFy_M7tD1o0cO_B2ms2j-ACEEQgQgAIwzTgAGmBIKIImNQAABwgQATAlhDGCCEIGIIM4BaBgwQBogEBIOESEIA8ARI5xAhxEFmAGAMCKAURKQQpQzRAAkCCBQEAKkQYIYIQQxCixCDADCABMAE0gpJIgyxhEDiCKCCIGAEIgJIQByAhFgGACCACMRQEyBAoxQiHiCBCFOECQFAIgAABR2QAgFjCDMA0AUMIoAIMChQghChASGEGeYEAIAIhgBSErnJPPEGWYAMgw05AhiiGHiBBBGGSCQcQgwRYJwhDDhgCSCSSEIQYwILoyAjAIigBFEUQK8gAYAQ5BCAAjkjCCAEEMZAUQAZQCjCCkpCgFMCCiIcVIAZZgilAQAiSHQECOcQAQIc4QClAHAjDDGkAGAMUoBgyhihgEChFCAAWEIEYwIJYwViAAlHCBIGEIEAEIQAoBwwgwiEBAEEEOoEwBY4wRwxAhBgAcKAESIQAwwIowRFhoBhAE
		URL url = new URL("http://api.acoustid.org/v2/lookup?client=" + apikey + "&meta=recordings+releases+releasegroups+tracks+compress");

		// enable compression for request and response
		Map<String, String> requestParam = new HashMap<String, String>();
		requestParam.put("Content-Encoding", "gzip");
		requestParam.put("Accept-Encoding", "gzip");

		// submit
		response = Charset.forName("UTF-8").decode(post(url, postParam, requestParam)).toString();

		// DEBUG
		// System.out.println(response);

		cache.put(cacheKey, response);
		return response;
	}

	private static Object[] array(Object node, String key) {
		Object value = ((Map<?, ?>) node).get(key);
		return value == null ? null : ((JsonObject<?, ?>) value).getArray();
	}

	private static Map<?, ?> firstMap(Object node, String key) {
		Object[] values = array(node, key);
		return values == null || values.length == 0 ? null : (Map<?, ?>) values[0];
	}

	private static Integer integer(Object node, String key) {
		Object value = ((Map<?, ?>) node).get(key);
		return value == null ? null : new Integer(value.toString());
	}

	private static String string(Object node, String key) {
		Object value = ((Map<?, ?>) node).get(key);
		return value == null ? null : value.toString();
	}

	public AudioTrack parseResult(String json, final int targetDuration) throws IOException {
		Map<?, ?> data = JsonReader.jsonToMaps(json);

		if (!data.get("status").equals("ok")) {
			throw new IOException("acoustid responded with error: " + data.get("status"));
		}

		try {
			for (Object result : array(data, "results")) {
				try {
					// pick most likely matching recording
					return Stream.of(array(result, "recordings")).sorted((Object o1, Object o2) -> {
						Integer i1 = integer(o1, "duration");
						Integer i2 = integer(o2, "duration");
						return Double.compare(i1 == null ? Double.NaN : Math.abs(i1 - targetDuration), i2 == null ? Double.NaN : Math.abs(i2 - targetDuration));
					}).map((Object o) -> {
						Map<?, ?> recording = (Map<?, ?>) o;
						try {
							Map<?, ?> releaseGroup = firstMap(recording, "releasegroups");
							if (releaseGroup == null) {
								return null;
							}

							String artist = (String) firstMap(recording, "artists").get("name");
							String title = (String) recording.get("title");

							AudioTrack audioTrack = new AudioTrack(artist, title, null);
							audioTrack.mbid = string(result, "id");

							String type = string(releaseGroup, "type");
							Object[] secondaryTypes = array(releaseGroup, "secondarytypes");
							Object[] releases = array(releaseGroup, "releases");

							if (releases == null || secondaryTypes != null || (!"Album".equals(type))) {
								return audioTrack; // default to simple music info if album data is undesirable
							}

							for (Object it : releases) {
								AudioTrack thisRelease = audioTrack.clone();
								Map<?, ?> release = (Map<?, ?>) it;
								Map<?, ?> date = (Map<?, ?>) release.get("date");
								try {
									thisRelease.albumReleaseDate = new SimpleDate(integer(date, "year"), integer(date, "month"), integer(date, "day"));
								} catch (Exception e) {
									thisRelease.albumReleaseDate = null;
								}

								if (thisRelease.albumReleaseDate == null || thisRelease.albumReleaseDate.getTimeStamp() >= (audioTrack.albumReleaseDate == null ? Long.MAX_VALUE : audioTrack.albumReleaseDate.getTimeStamp())) {
									continue;
								}

								Map<?, ?> medium = firstMap(release, "mediums");
								thisRelease.mediumIndex = integer(medium, "position");
								thisRelease.mediumCount = integer(release, "medium_count");

								Map<?, ?> track = firstMap(medium, "tracks");
								thisRelease.trackIndex = integer(track, "position");
								thisRelease.trackCount = integer(medium, "track_count");

								try {
									thisRelease.album = release.get("title").toString();
								} catch (Exception e) {
									thisRelease.album = (String) releaseGroup.get("title");
								}
								try {
									thisRelease.albumArtist = (String) firstMap(releaseGroup, "artists").get("name");
								} catch (Exception e) {
									thisRelease.albumArtist = null;
								}
								thisRelease.trackTitle = (String) track.get("title");

								if (!"Various Artists".equalsIgnoreCase(thisRelease.albumArtist) && (thisRelease.album == null || !thisRelease.album.contains("Greatest Hits"))) {
									// full info audio track
									return thisRelease;
								}
							}

							// default to simple music info if extended info is not available
							return audioTrack;
						} catch (Exception e) {
							Logger.getLogger(AcoustIDClient.class.getName()).log(Level.WARNING, e.toString(), e);
							return null;
						}
					}).filter(o -> o != null).sorted(new MostFieldsNotNull()).findFirst().get();
				} catch (Exception e) {
					// ignore
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public String getChromaprintCommand() {
		// use fpcalc executable path as specified by the cmdline or default to "fpcalc" and let the shell figure it out
		return System.getProperty("net.filebot.AcoustID.fpcalc", "fpcalc");
	}

	public List<Map<String, String>> fpcalc(Collection<File> files) throws IOException, InterruptedException {
		List<String> command = new ArrayList<String>();
		command.add(getChromaprintCommand());
		for (File f : files) {
			command.add(f.toString());
		}

		Process process = null;
		try {
			process = new ProcessBuilder(command).redirectError(Redirect.INHERIT).start();
		} catch (Exception e) {
			throw new IOException("Failed to exec fpcalc: " + e.getMessage());
		}

		Scanner scanner = new Scanner(new InputStreamReader(process.getInputStream(), "UTF-8"));
		LinkedList<Map<String, String>> results = new LinkedList<Map<String, String>>();

		try {
			while (scanner.hasNextLine()) {
				String[] value = scanner.nextLine().split("=", 2);

				if (value.length != 2)
					continue;

				if (results.isEmpty() || results.getLast().containsKey(value[0])) {
					results.addLast(new HashMap<String, String>(3));
				}
				results.getLast().put(value[0], value[1]);
			}
		} finally {
			scanner.close();
		}

		if (process.waitFor() != 0) {
			throw new IOException("Failed to exec fpcalc: Exit code " + process.exitValue());
		}

		return results;
	}

	private static class MostFieldsNotNull implements Comparator<Object> {

		@Override
		public int compare(Object o1, Object o2) {
			return Integer.compare(count(o2), count(o1));
		}

		public int count(Object o) {
			int n = 0;
			try {
				for (Field field : o.getClass().getDeclaredFields()) {
					if (field.get(o) != null) {
						n++;
					}
				}
			} catch (Exception e) {
				Logger.getLogger(AcoustIDClient.class.getName()).log(Level.WARNING, e.toString(), e);
			}
			return n;
		}
	}

}
