
package net.sourceforge.filebot.web;


import static java.util.Collections.*;
import static net.sourceforge.tuned.StringUtilities.*;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.util.Base64;

import net.sourceforge.filebot.web.OpenSubtitlesSubtitleDescriptor.Property;


/**
 * Client for the OpenSubtitles XML-RPC API.
 */
public class OpenSubtitlesXmlRpc {
	
	private final String url = "http://www.opensubtitles.org/xml-rpc";
	
	private final String useragent;
	
	private String token;
	

	public OpenSubtitlesXmlRpc(String useragent) {
		this.useragent = useragent;
	}
	

	/**
	 * Login as anonymous user
	 * 
	 * @throws XmlRpcFault
	 */
	public void loginAnonymous() throws XmlRpcFault {
		login("", "");
	}
	

	/**
	 * Login as user and use English as language
	 * 
	 * @param username
	 * @param password
	 * @throws XmlRpcFault
	 */
	public void login(String username, String password) throws XmlRpcFault {
		login(username, password, "en");
	}
	

	/**
	 * This will login user. This method should be called always when starting talking with
	 * server.
	 * 
	 * @param username username (blank for anonymous user)
	 * @param password password (blank for anonymous user)
	 * @param language <a
	 *            href="http://en.wikipedia.org/wiki/List_of_ISO_639-2_codes">ISO639</a>
	 *            2-letter codes as language and later communication will be done in this
	 *            language if applicable (error codes and so on).
	 */
	@SuppressWarnings("unchecked")
	public synchronized void login(String username, String password, String language) throws XmlRpcFault {
		Map<String, String> response = (Map<String, String>) invoke("LogIn", username, password, language, useragent);
		checkStatus(response.get("status"));
		
		token = response.get("token");
	}
	

	/**
	 * This will logout user (ends session id). Good call this function is before ending
	 * (closing) clients program.
	 * 
	 * @throws XmlRpcFault
	 */
	public synchronized void logout() throws XmlRpcFault {
		try {
			invoke("LogOut", token);
			
			// anonymous users will always get a 401 Unauthorized when trying to logout
			// do not check status for logout response
			// checkStatus(response.get("status"));
		} finally {
			token = null;
		}
	}
	

	public synchronized boolean isLoggedOn() {
		return token != null;
	}
	

	@SuppressWarnings("unchecked")
	public Map<String, String> getServerInfo() throws XmlRpcFault {
		return (Map<String, String>) invoke("ServerInfo", token);
	}
	

	public List<OpenSubtitlesSubtitleDescriptor> searchSubtitles(int imdbid, String... sublanguageids) throws XmlRpcFault {
		return searchSubtitles(null, null, imdbid, sublanguageids);
	}
	

	public List<OpenSubtitlesSubtitleDescriptor> searchSubtitles(String moviehash, long moviebytesize, String... sublanguageids) throws XmlRpcFault {
		return searchSubtitles(moviehash, moviebytesize, null, sublanguageids);
	}
	

	@SuppressWarnings("unchecked")
	protected List<OpenSubtitlesSubtitleDescriptor> searchSubtitles(String moviehash, Long moviebytesize, Integer imdbid, String... sublanguageids) throws XmlRpcFault {
		ParameterMap query = new ParameterMap(4);
		
		// put parameters, but ignore null or empty values
		query.put("moviehash", moviehash);
		query.put("moviebytesize", moviebytesize);
		query.put("imdbid", imdbid);
		query.put("sublanguageid", join(sublanguageids, ","));
		
		Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("SearchSubtitles", token, singletonList(query));
		
		List<OpenSubtitlesSubtitleDescriptor> subtitles = new ArrayList<OpenSubtitlesSubtitleDescriptor>();
		
		try {
			for (Map<String, String> subtitleData : response.get("data")) {
				subtitles.add(new OpenSubtitlesSubtitleDescriptor(Property.asEnumMap(subtitleData)));
			}
		} catch (ClassCastException e) {
			// error response, no subtitles, ignore
		}
		
		return subtitles;
	}
	

	@SuppressWarnings("unchecked")
	public List<MovieDescriptor> searchMoviesOnIMDB(String query) throws XmlRpcFault {
		Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("SearchMoviesOnIMDB", token, query);
		
		List<MovieDescriptor> movies = new ArrayList<MovieDescriptor>();
		
		for (Map<String, String> movie : response.get("data")) {
			try {
				// get non-aka title (aka titles are separated by Â)
				Scanner titleScanner = new Scanner(movie.get("title")).useDelimiter("\u00C2");
				
				movies.add(new MovieDescriptor(titleScanner.next(), Integer.parseInt(movie.get("id"))));
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		return movies;
	}
	

	@SuppressWarnings("unchecked")
	public Collection<String> detectLanguage(String text) throws XmlRpcFault {
		try {
			// base64 encoded text
			String parameter = new String(Base64.encode(text.getBytes("utf-8")));
			
			Map<String, Map<String, String>> response = (Map<String, Map<String, String>>) invoke("DetectLanguage", token, new Object[] { parameter });
			
			if (response.containsKey("data")) {
				return response.get("data").values();
			}
			
			return Collections.emptySet();
		} catch (UnsupportedEncodingException e) {
			// will not happen
			throw new RuntimeException(e);
		}
	}
	

	public Map<String, String> getSubLanguages() throws XmlRpcFault {
		return getSubLanguages("en");
	}
	

	@SuppressWarnings("unchecked")
	public Map<String, String> getSubLanguages(String languageCode) throws XmlRpcFault {
		Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("GetSubLanguages", languageCode);
		
		Map<String, String> subLanguageMap = new HashMap<String, String>();
		
		for (Map<String, String> language : response.get("data")) {
			subLanguageMap.put(language.get("SubLanguageID"), language.get("LanguageName"));
		}
		
		return subLanguageMap;
	}
	

	@SuppressWarnings("unchecked")
	public boolean noOperation() {
		try {
			Map<String, String> response = (Map<String, String>) invoke("NoOperation", token);
			checkStatus(response.get("status"));
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	

	private Object invoke(String method, Object... arguments) throws XmlRpcFault {
		try {
			XmlRpcClient rpc = new XmlRpcClient(url, false);
			return rpc.invoke(method, arguments);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	

	/**
	 * Check whether status is OK or not
	 * 
	 * @param status status code and message (e.g. 200 OK, 401 Unauthorized, ...)
	 * @throws XmlRpcFault thrown if status code is not OK
	 */
	private void checkStatus(String status) throws XmlRpcFault {
		if (status.equals("200 OK"))
			return;
		
		Matcher m = Pattern.compile("(\\d+).*").matcher(status);
		
		if (!m.matches())
			throw new XmlRpcException("Illegal status code: " + status);
		
		throw new XmlRpcFault(Integer.parseInt(m.group(1)), status);
	}
	

	private static class ParameterMap extends HashMap<String, String> {
		
		public ParameterMap(int initialCapacity) {
			super(initialCapacity);
		}
		

		@Override
		public String put(String key, String value) {
			if (value != null && !value.isEmpty()) {
				return super.put(key, value);
			}
			
			return null;
		}
		

		public String put(String key, Object value) {
			if (value != null) {
				return put(key, value.toString());
			}
			
			return null;
		}
	}
	
}
