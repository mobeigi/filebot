package net.sourceforge.filebot.web;

import static java.util.Collections.*;
import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Icon;

import net.sourceforge.filebot.Cache;
import net.sourceforge.filebot.ResourceManager;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class AcoustID implements MusicIdentificationService {

	private static final FloodLimit REQUEST_LIMIT = new FloodLimit(3, 1, TimeUnit.SECONDS);

	private String apikey;

	public AcoustID(String apikey) {
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

	@Override
	public Map<File, AudioTrack> lookup(Iterable<File> files) throws Exception {
		Map<File, AudioTrack> results = new LinkedHashMap<File, AudioTrack>();

		for (Map<String, String> fp : fpcalc(files)) {
			results.put(new File(fp.get("FILE")), parse(lookup(fp.get("DURATION"), fp.get("FINGERPRINT"))));
		}
		return results;
	}

	public String lookup(File file) throws Exception {
		Map<String, String> fp = fpcalc(singleton(file)).get(0);
		return lookup(fp.get("DURATION"), fp.get("FINGERPRINT"));
	}

	public String lookup(String duration, String fingerprint) throws IOException, InterruptedException, ParseException {
		// http://api.acoustid.org/v2/lookup?client=8XaBELgH&meta=recordings+releasegroups+compress&duration=641&fingerprint=AQABz0qUkZK4oOfhL-CPc4e5C_wW2H2QH9uDL4cvoT8UNQ-eHtsE8cceeFJx-LiiHT-aPzhxoc-Opj_eI5d2hOFyMJRzfDk-QSsu7fBxqZDMHcfxPfDIoPWxv9C1o3yg44d_3Df2GJaUQeeR-cb2HfaPNsdxHj2PJnpwPMN3aPcEMzd-_MeB_Ej4D_CLP8ghHjkJv_jh_UDuQ8xnILwunPg6hF2R8HgzvLhxHVYP_ziJX0eKPnIE1UePMByDJyg7wz_6yELsB8n4oDmDa0Gv40hf6D3CE3_wH6HFaxCPUD9-hNeF5MfWEP3SCGym4-SxnXiGs0mRjEXD6fgl4LmKWrSChzzC33ge9PB3otyJMk-IVC6R8MTNwD9qKQ_CC8kPv4THzEGZS8GPI3x0iGVUxC1hRSizC5VzoamYDi-uR7iKPhGSI82PkiWeB_eHijvsaIWfBCWH5AjjCfVxZ1TQ3CvCTclGnEMfHbnZFA8pjD6KXwd__Cn-Y8e_I9cq6CR-4S9KLXqQcsxxoWh3eMxiHI6TIzyPv0M43YHz4yte-Cv-4D16Hv9F9C9SPUdyGtZRHV-OHEeeGD--BKcjVLOK_NCDXMfx44dzHEiOZ0Z44Rf6DH5R3uiPj4d_PKolJNyRJzyu4_CTD2WOvzjKH9GPb4cUP1Av9EuQd8fGCFee4JlRHi18xQh96NLxkCgfWFKOH6WGeoe4I3za4c5hTscTPEZTES1x8kE-9MQPjT8a8gh5fPgQZtqCFj9MDvp6fDx6NCd07bjx7MLR9AhtnFnQ70GjOcV0opmm4zpY3SOa7HiwdTtyHa6NC4e-HN-OfC5-OP_gLe2QDxfUCz_0w9l65HiPAz9-IaGOUA7-4MZ5CWFOlIfe4yUa6AiZGxf6w0fFxsjTOdC6Itbh4mGD63iPH9-RFy909XAMj7mC5_BvlDyO6kGTZKJxHUd4NDwuZUffw_5RMsde5CWkJAgXnDReNEaP6DTOQ65yaD88HoeX8fge-DSeHo9Qa8cTHc80I-_RoHxx_UHeBxrJw62Q34Kd7MEfpCcu6BLeB1ePw6OO4sOF_sHhmB504WWDZiEu8sKPpkcfCT9xfej0o0lr4T5yNJeOvjmu40w-TDmqHXmYgfFhFy_M7tD1o0cO_B2ms2j-ACEEQgQgAIwzTgAGmBIKIImNQAABwgQATAlhDGCCEIGIIM4BaBgwQBogEBIOESEIA8ARI5xAhxEFmAGAMCKAURKQQpQzRAAkCCBQEAKkQYIYIQQxCixCDADCABMAE0gpJIgyxhEDiCKCCIGAEIgJIQByAhFgGACCACMRQEyBAoxQiHiCBCFOECQFAIgAABR2QAgFjCDMA0AUMIoAIMChQghChASGEGeYEAIAIhgBSErnJPPEGWYAMgw05AhiiGHiBBBGGSCQcQgwRYJwhDDhgCSCSSEIQYwILoyAjAIigBFEUQK8gAYAQ5BCAAjkjCCAEEMZAUQAZQCjCCkpCgFMCCiIcVIAZZgilAQAiSHQECOcQAQIc4QClAHAjDDGkAGAMUoBgyhihgEChFCAAWEIEYwIJYwViAAlHCBIGEIEAEIQAoBwwgwiEBAEEEOoEwBY4wRwxAhBgAcKAESIQAwwIowRFhoBhAE
		URL url = new URL("http://api.acoustid.org/v2/lookup?client=" + apikey + "&meta=recordings+releasegroups+releases+tracks+compress&duration=" + duration + "&fingerprint=" + fingerprint);

		Cache cache = Cache.getCache("web-datasource");
		String response = cache.get(url, String.class);
		if (response != null) {
			return response;
		}

		// respect rate limit
		REQUEST_LIMIT.acquirePermit();
		response = readAll(getReader(url.openConnection()));
		cache.put(url, response);

		return response;
	}

	public AudioTrack parse(String json) throws IOException, InterruptedException, ParseException {
		Map<?, ?> data = (Map<?, ?>) new JSONParser().parse(json);

		if (!data.get("status").equals("ok")) {
			throw new IOException("acoustid responded with error: " + data.get("status"));
		}
		try {
			for (Object result : (List<?>) data.get("results")) {
				try {
					Map<?, ?> recording = (Map<?, ?>) ((List<?>) ((Map<?, ?>) result).get("recordings")).get(0);

					String artist = (String) ((Map<?, ?>) ((List<?>) recording.get("artists")).get(0)).get("name");
					String title = (String) recording.get("title");

					AudioTrack audioTrack = new AudioTrack(artist, title, null);
					try {
						Map<?, ?> releaseGroup = (Map<?, ?>) ((List<?>) recording.get("releasegroups")).get(0);
						List<?> releases = (List<?>) releaseGroup.get("releases");

						for (Object it : releases) {
							try {
								AudioTrack thisRelease = new AudioTrack(artist, title, null);
								Map<?, ?> release = (Map<?, ?>) it;
								Map<?, ?> date = (Map<?, ?>) release.get("date");
								try {
									thisRelease.albumReleaseDate = new Date(Integer.parseInt(date.get("year").toString()), Integer.parseInt(date.get("month").toString()), Integer.parseInt(date.get("day").toString()));
								} catch (Exception e) {
									// ignore
								}

								if (thisRelease.albumReleaseDate == null || thisRelease.albumReleaseDate.getTimeStamp() >= (audioTrack.albumReleaseDate == null ? Long.MAX_VALUE : audioTrack.albumReleaseDate.getTimeStamp())) {
									continue;
								}

								Map<?, ?> medium = (Map<?, ?>) ((List<?>) release.get("mediums")).get(0);
								thisRelease.mediumIndex = new Integer(medium.get("position").toString());
								thisRelease.mediumCount = new Integer(release.get("medium_count").toString());

								Map<?, ?> track = (Map<?, ?>) ((List<?>) medium.get("tracks")).get(0);
								thisRelease.trackIndex = new Integer(track.get("position").toString());
								thisRelease.trackCount = new Integer(medium.get("track_count").toString());

								try {
									thisRelease.album = release.get("title").toString();
								} catch (Exception e) {
									thisRelease.album = (String) releaseGroup.get("title");
								}
								try {
									thisRelease.albumArtist = (String) ((Map<?, ?>) ((List<?>) releaseGroup.get("artists")).get(0)).get("name");
								} catch (Exception e) {
									thisRelease.albumArtist = null;
								}
								thisRelease.trackTitle = (String) track.get("title");

								if (!"Various Artists".equalsIgnoreCase(thisRelease.albumArtist) && (thisRelease.album == null || !thisRelease.album.contains("Greatest Hits"))) {
									audioTrack = thisRelease;
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						// allow album to be null
					}
					return audioTrack;
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
		}
		return null;
	}

	public List<Map<String, String>> fpcalc(Iterable<File> files) throws IOException, InterruptedException {
		// use fpcalc executable path as specified by the cmdline or default to "fpcalc" and let the shell figure it out
		String fpcalc = System.getProperty("net.sourceforge.filebot.AcoustID.fpcalc", "fpcalc");

		List<String> command = new ArrayList<String>();
		command.add(fpcalc);
		for (File f : files) {
			command.add(f.toString());
		}

		Process process = null;
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(command);
			try {
				processBuilder.redirectError(Redirect.INHERIT);
			} catch (Throwable e) {
				Logger.getLogger(AcoustID.class.getName()).log(Level.WARNING, "Unable to inherit IO: " + e.getMessage());
			}
			process = processBuilder.start();
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
}
