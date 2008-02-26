
package net.sourceforge.filebot.web;


import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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
	
	private String username;
	private String password;
	private String language;
	
	private String useragent;
	
	private String token = null;
	
	private Timer keepAliveDaemon = null;
	
	/**
	 * Interval to call NoOperation to keep the session from expiring
	 */
	private static final int KEEP_ALIVE_INTERVAL = 12 * 60 * 1000; // 12 minutes
	
	
	public OpenSubtitlesClient(String useragent) {
		this.useragent = useragent;
	}
	

	public boolean isLoggedOn() {
		return username != null;
	}
	

	/**
	 * login as anonymous user
	 */
	public synchronized void login() throws XmlRpcFault {
		this.login("", "", "en");
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
	public synchronized void login(String username, String password, String language) throws XmlRpcFault {
		if (isLoggedOn())
			throw new IllegalStateException("User is already logged on");
		
		if ((username == null) || (password == null) || (language == null))
			throw new IllegalArgumentException("Username, password and language must not be null");
		
		this.username = username;
		this.password = password;
		this.language = language;
	}
	

	public synchronized void logout() {
		if (!isLoggedOn())
			throw new IllegalStateException("User is not logged on");
		
		deactivate();
		
		username = null;
		password = null;
		language = null;
	}
	

	@SuppressWarnings("unchecked")
	private synchronized void activate() throws XmlRpcFault {
		if (isActive())
			return;
		
		if (!isLoggedOn())
			throw new IllegalStateException("User is not logged on");
		
		Map<String, String> response = (Map<String, String>) invoke("LogIn", username, password, language, useragent);
		checkStatus(response.get("status"));
		
		token = response.get("token");
		
		keepAliveDaemon = new Timer(getClass().getSimpleName() + " Keepalive", true);
		keepAliveDaemon.schedule(new KeepAliveTimerTask(), KEEP_ALIVE_INTERVAL, KEEP_ALIVE_INTERVAL);
	}
	

	@SuppressWarnings("unchecked")
	private synchronized void deactivate() {
		if (!isActive())
			return;
		
		// anonymous users will always get a 401 Unauthorized when trying to logout
		if (!username.isEmpty()) {
			try {
				Map<String, String> response = (Map<String, String>) invoke("LogOut", token);
				checkStatus(response.get("status"));
			} catch (Exception e) {
				Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, "Exception while deactivating connection", e);
			}
		}
		
		token = null;
		
		keepAliveDaemon.cancel();
		keepAliveDaemon = null;
	}
	

	private boolean isActive() {
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
		activate();
		
		return (Map<String, String>) invoke("ServerInfo", token);
	}
	

	@SuppressWarnings("unchecked")
	public List<OpenSubtitleDescriptor> searchSubtitles(int... imdbidArray) throws XmlRpcFault {
		activate();
		
		List<Map<String, String>> imdbidList = new ArrayList<Map<String, String>>(imdbidArray.length);
		
		for (int imdbid : imdbidArray) {
			Map<String, String> map = new HashMap<String, String>(1);
			
			// pad id with zeros
			map.put("imdbid", String.format("%07d", imdbid));
			
			imdbidList.add(map);
		}
		
		Map<String, List<Map<String, String>>> response = (Map<String, List<Map<String, String>>>) invoke("SearchSubtitles", token, imdbidList);
		
		ArrayList<OpenSubtitleDescriptor> subs = new ArrayList<OpenSubtitleDescriptor>();
		
		if (!(response.get("data") instanceof List))
			throw new XmlRpcException("Illegal response: " + response.toString());
		
		// if there was an error data may not be a list
		for (Map<String, String> subtitle : response.get("data")) {
			subs.add(new OpenSubtitleDescriptor(subtitle));
		}
		
		return subs;
	}
	

	@SuppressWarnings("unchecked")
	public List<MovieDescriptor> searchMoviesOnIMDB(String query) throws XmlRpcFault {
		activate();
		
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
			activate();
			
			Map<String, String> response = (Map<String, String>) invoke("NoOperation", token);
			checkStatus(response.get("status"));
			
			return true;
		} catch (Exception e) {
			deactivate();
			return false;
		}
	}
	
	
	private class KeepAliveTimerTask extends TimerTask {
		
		@Override
		public void run() {
			Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
			
			if (!noOperation()) {
				logger.log(Level.INFO, "Connection lost");
				deactivate();
			} else {
				logger.log(Level.INFO, "Connection is OK");
			}
		};
	};
	
}
