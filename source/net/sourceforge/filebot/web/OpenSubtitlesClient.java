
package net.sourceforge.filebot.web;


import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;


/**
 * Client for the OpenSubtitles XML-RPC API.
 * 
 */
public class OpenSubtitlesClient {
	
	/**
	 * <table>
	 * <tr>
	 * <td>Main server:</td>
	 * <td>http://www.opensubtitles.org/xml-rpc</td>
	 * </tr>
	 * <tr>
	 * <td>Developing tests:</td>
	 * <td>http://dev.opensubtitles.org/xml-rpc</td>
	 * </tr>
	 * </table>
	 */
	private String url = "http://www.opensubtitles.org/xml-rpc";
	
	private String useragent;
	
	private String token = null;
	
	
	public OpenSubtitlesClient(String useragent) {
		this.useragent = useragent;
	}
	

	public void login(String username, String password) throws XmlRpcFault {
		login(username, password, "en");
	}
	

	/**
	 * This will login user. This method should be called always when starting talking with
	 * server.
	 * 
	 * @param username blank for anonymous user.
	 * @param password blank for anonymous user.
	 * @param language <a href="http://en.wikipedia.org/wiki/List_of_ISO_639-2_codes">ISO639</a>
	 *            2 letter codes as language and later communication will be done in this
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
	 */
	@SuppressWarnings("unchecked")
	public synchronized void logout() {
		
		// anonymous users will always get a 401 Unauthorized when trying to logout
		try {
			Map<String, String> response = (Map<String, String>) invoke("LogOut", token);
			checkStatus(response.get("status"));
		} catch (Exception e) {
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Exception while deactivating session", e);
		}
		
		token = null;
	}
	

	public synchronized boolean isLoggedOn() {
		return token != null;
	}
	

	/**
	 * Check status whether it is OK or not
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
			// will never happen
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Invalid xml-rpc url: " + url, e);
			return null;
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
	public List<OpenSubtitlesSubtitleDescriptor> searchSubtitles(int... imdbidArray) throws XmlRpcFault {
		
		List<Map<String, String>> imdbidList = new ArrayList<Map<String, String>>(imdbidArray.length);
		
		for (int imdbid : imdbidArray) {
			Map<String, String> map = new HashMap<String, String>(1);
			
			// pad id with zeros
			map.put("imdbid", String.format("%07d", imdbid));
			
			imdbidList.add(map);
		}
		
		Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("SearchSubtitles", token, imdbidList);
		
		ArrayList<OpenSubtitlesSubtitleDescriptor> subs = new ArrayList<OpenSubtitlesSubtitleDescriptor>();
		
		if (!(response.get("data") instanceof List))
			throw new XmlRpcException("Illegal response: " + response.toString());
		
		// if there was an error data may not be a list
		for (Map<String, String> subtitle : response.get("data")) {
			subs.add(new OpenSubtitlesSubtitleDescriptor(subtitle));
		}
		
		return subs;
	}
	

	@SuppressWarnings("unchecked")
	public List<MovieDescriptor> searchMoviesOnIMDB(String query) throws XmlRpcFault {
		
		Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("SearchMoviesOnIMDB", token, query);
		
		ArrayList<MovieDescriptor> movies = new ArrayList<MovieDescriptor>();
		
		for (Map<String, String> movie : response.get("data")) {
			movies.add(new MovieDescriptor(movie.get("title"), new Integer(movie.get("id"))));
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
