
package net.sourceforge.filebot.web;


import static java.util.Collections.*;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;

import net.sourceforge.filebot.web.OpenSubtitlesSubtitleDescriptor.Property;


/**
 * Client for the OpenSubtitles XML-RPC API.
 */
public class OpenSubtitlesXmlRpc {
	
	private static final String url = "http://www.opensubtitles.org/xml-rpc";
	
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
	

	private Object invoke(String method, Object... arguments) throws XmlRpcFault {
		try {
			XmlRpcClient rpc = new XmlRpcClient(url, false);
			return rpc.invoke(method, arguments);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	

	/**
	 * This simple function returns basic server info.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, String> getServerInfo() throws XmlRpcFault {
		return (Map<String, String>) invoke("ServerInfo", token);
	}
	

	@SuppressWarnings("unchecked")
	public List<OpenSubtitlesSubtitleDescriptor> searchSubtitles(int imdbid, String languageName) throws XmlRpcFault {
		Map<String, String> query = new HashMap<String, String>(2);
		
		query.put("imdbid", String.format("%07d", imdbid));
		query.put("sublanguageid", getSubLanguageID(languageName));
		
		Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("SearchSubtitles", token, singletonList(query));
		
		List<OpenSubtitlesSubtitleDescriptor> subs = new ArrayList<OpenSubtitlesSubtitleDescriptor>();
		
		try {
			for (Map<String, String> subtitleData : response.get("data")) {
				subs.add(new OpenSubtitlesSubtitleDescriptor(Property.asEnumMap(subtitleData)));
			}
		} catch (ClassCastException e) {
			// if the response is an error message, generic types won't match 
			throw new XmlRpcException("Illegal response: " + response.toString(), e);
		}
		
		return subs;
	}
	

	private final Map<String, String> languageMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	

	@SuppressWarnings("unchecked")
	public String getSubLanguageID(String languageName) throws XmlRpcFault {
		synchronized (languageMap) {
			if (languageMap.isEmpty()) {
				Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("GetSubLanguages", "en");
				
				for (Map<String, String> language : response.get("data")) {
					languageMap.put(language.get("LanguageName"), language.get("SubLanguageID"));
				}
			}
		}
		
		return languageMap.get(languageName);
	}
	

	@SuppressWarnings("unchecked")
	public List<MovieDescriptor> searchMoviesOnIMDB(String query) throws XmlRpcFault {
		
		Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("SearchMoviesOnIMDB", token, query);
		
		List<MovieDescriptor> movies = new ArrayList<MovieDescriptor>();
		Pattern moviePattern = Pattern.compile("(.+) \\((\\d{4})\\).*");
		
		for (Map<String, String> movie : response.get("data")) {
			try {
				// get non-aka title (aka titles are separated by Â)
				Scanner titleScanner = new Scanner(movie.get("title")).useDelimiter("\u00C2");
				
				Matcher matcher = moviePattern.matcher(titleScanner.next().trim());
				
				if (!matcher.matches())
					throw new InputMismatchException("Cannot parse movie: " + movie);
				
				String title = matcher.group(1);
				String year = matcher.group(2);
				
				movies.add(new MovieDescriptor(title, Integer.parseInt(year), Integer.parseInt(movie.get("id"))));
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		
		return movies;
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
	
}
