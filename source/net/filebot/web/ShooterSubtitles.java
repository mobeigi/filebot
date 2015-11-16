package net.filebot.web;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Icon;

import net.filebot.Cache;
import net.filebot.ResourceManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ShooterSubtitles implements VideoHashSubtitleService {

	private static final String LANGUAGE_CHINESE = "Chinese";
	private static final String LANGUAGE_ENGLISH = "English";

	@Override
	public String getName() {
		return "射手网";
	}

	@Override
	public URI getLink() {
		return URI.create("http://shooter.cn");
	}

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.shooter");
	}

	@Override
	public Map<File, List<SubtitleDescriptor>> getSubtitleList(File[] videoFiles, String languageName) throws Exception {
		Map<File, List<SubtitleDescriptor>> result = new HashMap<File, List<SubtitleDescriptor>>();
		for (File it : videoFiles) {
			result.put(it, getSubtitleList(it, languageName));
		}
		return result;
	}

	protected URL getSubApiUrl() {
		try {
			return new URL(System.getProperty("net.filebot.web.ShooterSubtitles.url", "https://www.shooter.cn/api/subapi.php"));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @see https://docs.google.com/document/d/1ufdzy6jbornkXxsD-OGl3kgWa4P9WO5NZb6_QYZiGI0/preview
	 */
	public synchronized List<SubtitleDescriptor> getSubtitleList(File file, String languageName) throws Exception {
		if (!LANGUAGE_CHINESE.equals(languageName) && !LANGUAGE_ENGLISH.equals(languageName)) {
			throw new IllegalArgumentException("Language not supported: " + languageName);
		}
		if (file.length() < 8192) {
			return emptyList();
		}

		URL endpoint = getSubApiUrl();
		Map<String, String> param = new LinkedHashMap<String, String>();
		param.put("filehash", computeFileHash(file));
		param.put("pathinfo", file.getPath());
		param.put("format", "json");
		param.put("lang", LANGUAGE_CHINESE.equals(languageName) ? "Chn" : "Eng");

		Cache cache = Cache.getCache("web-datasource");
		String key = endpoint.toString() + param.toString();
		SubtitleDescriptor[] value = cache.get(key, SubtitleDescriptor[].class);
		if (value != null) {
			return asList(value);
		}

		ByteBuffer post = WebRequest.post(endpoint, param, null);
		Object response = JSONValue.parse(StandardCharsets.UTF_8.decode(post).toString());

		List<SubtitleDescriptor> results = new ArrayList<SubtitleDescriptor>();
		String name = getNameWithoutExtension(file.getName());

		JSON: for (JSONObject result : jsonList(response)) {
			if (result == null)
				continue;

			List<JSONObject> files = jsonList(result.get("Files"));
			if (files.size() == 1) {
				for (JSONObject fd : jsonList(result.get("Files"))) {
					String type = (String) fd.get("Ext");
					String link = (String) fd.get("Link");
					results.add(new ShooterSubtitleDescriptor(name, type, link, languageName));

					// use the first best option and ignore the rest
					break JSON;
				}
			}
		}

		cache.put(key, results.toArray(new SubtitleDescriptor[0]));
		return results;
	}

	@Override
	public CheckResult checkSubtitle(File videoFile, File subtitleFile) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void uploadSubtitle(Object identity, Locale locale, File videoFile, File subtitleFile) throws Exception {
		throw new UnsupportedOperationException();
	}

	protected static List<JSONObject> jsonList(final Object array) {
		return new AbstractList<JSONObject>() {

			@Override
			public JSONObject get(int index) {
				return (JSONObject) ((JSONArray) array).get(index);
			}

			@Override
			public int size() {
				return array == null ? 0 : ((JSONArray) array).size();
			}
		};
	}

	/**
	 *
	 * @see https://docs.google.com/document/d/1w5MCBO61rKQ6hI5m9laJLWse__yTYdRugpVyz4RzrmM/preview
	 */
	protected static String computeFileHash(File file) throws IOException {
		List<String> hashes = new ArrayList<String>();
		long fileSize = file.length();
		if (fileSize < 8192)
			return "";

		long[] offset = new long[4];
		offset[3] = fileSize - 8192;
		offset[2] = fileSize / 3;
		offset[1] = fileSize / 3 * 2;
		offset[0] = 4096;

		try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
			byte[] buffer = new byte[4096];
			for (int i = 0; i < 4; i++) {
				f.seek(offset[i]);
				int read = f.read(buffer, 0, buffer.length);
				hashes.add(md5(buffer, 0, read));
			}
		}

		return String.join(";", hashes);
	}

	protected static String md5(byte[] input, int offset, int len) {
		try {
			MessageDigest hash = MessageDigest.getInstance("MD5");
			hash.update(input, offset, len);
			return String.format("%032x", new BigInteger(1, hash.digest())); // as hex string
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static class ShooterSubtitleDescriptor implements SubtitleDescriptor, Serializable {

		private String name;
		private String type;
		private String link;
		private String language;

		public ShooterSubtitleDescriptor(String name, String type, String link, String language) {
			this.name = name;
			this.type = type;
			this.link = link;
			this.language = language;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getLanguageName() {
			return language;
		}

		@Override
		public String getType() {
			return type;
		}

		@Override
		public ByteBuffer fetch() throws Exception {
			return WebRequest.fetch(new URL(link));
		}

		@Override
		public String getPath() {
			return getName() + "." + getType();
		}

		@Override
		public long getLength() {
			return -1;
		}

		@Override
		public File toFile() {
			return new File(getPath());
		}

		@Override
		public String toString() {
			return String.format("%s [%s]", getName(), getLanguageName());
		}
	}

}
