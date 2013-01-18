
package net.sourceforge.filebot.web;


import static net.sourceforge.filebot.web.WebRequest.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
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

import com.cedarsoftware.util.io.JsonReader;


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
			results.put(new File(fp.get("FILE")), lookup(fp.get("DURATION"), fp.get("FINGERPRINT")));
		}
		return results;
	}
	
	
	public AudioTrack lookup(String duration, String fingerprint) throws IOException, InterruptedException {
		// http://api.acoustid.org/v2/lookup?client=8XaBELgH&meta=recordings+releasegroups+compress&duration=641&fingerprint=AQABz0qUkZK4oOfhL-CPc4e5C_wW2H2QH9uDL4cvoT8UNQ-eHtsE8cceeFJx-LiiHT-aPzhxoc-Opj_eI5d2hOFyMJRzfDk-QSsu7fBxqZDMHcfxPfDIoPWxv9C1o3yg44d_3Df2GJaUQeeR-cb2HfaPNsdxHj2PJnpwPMN3aPcEMzd-_MeB_Ej4D_CLP8ghHjkJv_jh_UDuQ8xnILwunPg6hF2R8HgzvLhxHVYP_ziJX0eKPnIE1UePMByDJyg7wz_6yELsB8n4oDmDa0Gv40hf6D3CE3_wH6HFaxCPUD9-hNeF5MfWEP3SCGym4-SxnXiGs0mRjEXD6fgl4LmKWrSChzzC33ge9PB3otyJMk-IVC6R8MTNwD9qKQ_CC8kPv4THzEGZS8GPI3x0iGVUxC1hRSizC5VzoamYDi-uR7iKPhGSI82PkiWeB_eHijvsaIWfBCWH5AjjCfVxZ1TQ3CvCTclGnEMfHbnZFA8pjD6KXwd__Cn-Y8e_I9cq6CR-4S9KLXqQcsxxoWh3eMxiHI6TIzyPv0M43YHz4yte-Cv-4D16Hv9F9C9SPUdyGtZRHV-OHEeeGD--BKcjVLOK_NCDXMfx44dzHEiOZ0Z44Rf6DH5R3uiPj4d_PKolJNyRJzyu4_CTD2WOvzjKH9GPb4cUP1Av9EuQd8fGCFee4JlRHi18xQh96NLxkCgfWFKOH6WGeoe4I3za4c5hTscTPEZTES1x8kE-9MQPjT8a8gh5fPgQZtqCFj9MDvp6fDx6NCd07bjx7MLR9AhtnFnQ70GjOcV0opmm4zpY3SOa7HiwdTtyHa6NC4e-HN-OfC5-OP_gLe2QDxfUCz_0w9l65HiPAz9-IaGOUA7-4MZ5CWFOlIfe4yUa6AiZGxf6w0fFxsjTOdC6Itbh4mGD63iPH9-RFy909XAMj7mC5_BvlDyO6kGTZKJxHUd4NDwuZUffw_5RMsde5CWkJAgXnDReNEaP6DTOQ65yaD88HoeX8fge-DSeHo9Qa8cTHc80I-_RoHxx_UHeBxrJw62Q34Kd7MEfpCcu6BLeB1ePw6OO4sOF_sHhmB504WWDZiEu8sKPpkcfCT9xfej0o0lr4T5yNJeOvjmu40w-TDmqHXmYgfFhFy_M7tD1o0cO_B2ms2j-ACEEQgQgAIwzTgAGmBIKIImNQAABwgQATAlhDGCCEIGIIM4BaBgwQBogEBIOESEIA8ARI5xAhxEFmAGAMCKAURKQQpQzRAAkCCBQEAKkQYIYIQQxCixCDADCABMAE0gpJIgyxhEDiCKCCIGAEIgJIQByAhFgGACCACMRQEyBAoxQiHiCBCFOECQFAIgAABR2QAgFjCDMA0AUMIoAIMChQghChASGEGeYEAIAIhgBSErnJPPEGWYAMgw05AhiiGHiBBBGGSCQcQgwRYJwhDDhgCSCSSEIQYwILoyAjAIigBFEUQK8gAYAQ5BCAAjkjCCAEEMZAUQAZQCjCCkpCgFMCCiIcVIAZZgilAQAiSHQECOcQAQIc4QClAHAjDDGkAGAMUoBgyhihgEChFCAAWEIEYwIJYwViAAlHCBIGEIEAEIQAoBwwgwiEBAEEEOoEwBY4wRwxAhBgAcKAESIQAwwIowRFhoBhAE
		URL url = new URL("http://api.acoustid.org/v2/lookup?client=" + apikey + "&meta=recordings+releasegroups+compress&duration=" + duration + "&fingerprint=" + fingerprint);
		
		Cache cache = Cache.getCache("web-datasource");
		AudioTrack audioTrack = cache.get(url, AudioTrack.class);
		if (audioTrack != null)
			return audioTrack;
		
		// respect rate limit
		REQUEST_LIMIT.acquirePermit();
		
		String response = readAll(getReader(url.openConnection()));
		Map<?, ?> data = JsonReader.jsonToMaps(response);
		
		if (!data.get("status").equals("ok")) {
			throw new IOException("acoustid responded with error: " + data.get("status"));
		}
		try {
			Map<?, ?> recording = (Map<?, ?>) ((List<?>) ((Map<?, ?>) ((List<?>) data.get("results")).get(0)).get("recordings")).get(0);
			String artist = (String) ((Map<?, ?>) ((List<?>) recording.get("artists")).get(0)).get("name");
			String title = (String) recording.get("title");
			String album = null;
			try {
				album = (String) ((Map<?, ?>) ((List<?>) recording.get("releasegroups")).get(0)).get("title");
			} catch (Exception e) {
				// allow album to be null
			}
			
			audioTrack = new AudioTrack(artist, title, album);
			cache.put(url, audioTrack);
			return audioTrack;
		} catch (Exception e) {
			// no results
			return null;
		}
	}
	
	
	public List<Map<String, String>> fpcalc(Iterable<File> files) throws IOException, InterruptedException {
		List<String> command = new ArrayList<String>();
		command.add("fpcalc");
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
		
		Scanner scanner = new Scanner(process.getInputStream());
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
