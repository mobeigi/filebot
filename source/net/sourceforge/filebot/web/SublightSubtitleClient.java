
package net.sourceforge.filebot.web;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.Timer;
import net.sublight.webservice.ArrayOfGenre;
import net.sublight.webservice.ArrayOfIMDB;
import net.sublight.webservice.ArrayOfRelease;
import net.sublight.webservice.ArrayOfString;
import net.sublight.webservice.ArrayOfSubtitle;
import net.sublight.webservice.ArrayOfSubtitleLanguage;
import net.sublight.webservice.ClientInfo;
import net.sublight.webservice.Genre;
import net.sublight.webservice.IMDB;
import net.sublight.webservice.Release;
import net.sublight.webservice.Subtitle;
import net.sublight.webservice.SubtitleLanguage;
import net.sublight.webservice.SubtitlesAPI2;
import net.sublight.webservice.SubtitlesAPI2Soap;


public class SublightSubtitleClient implements SubtitleProvider {
	
	private static final String iid = "42cc1701-3752-49e2-a148-332960073452";
	
	private final ClientInfo clientInfo = new ClientInfo();
	
	private SubtitlesAPI2Soap webservice;
	
	private String session;
	

	public SublightSubtitleClient(String clientIdentity, String apikey) {
		clientInfo.setClientId(clientIdentity);
		clientInfo.setApiKey(apikey);
	}
	

	@Override
	public String getName() {
		return "Sublight";
	}
	

	@Override
	public Icon getIcon() {
		return ResourceManager.getIcon("search.sublight");
	}
	

	@Override
	public List<SearchResult> search(String query) throws WebServiceException {
		// require login
		login();
		
		Holder<ArrayOfIMDB> response = new Holder<ArrayOfIMDB>();
		Holder<String> error = new Holder<String>();
		
		webservice.findIMDB(query, null, null, response, error);
		
		// abort if something went wrong
		checkError(error);
		
		List<SearchResult> results = new ArrayList<SearchResult>();
		
		if (response.value != null) {
			for (IMDB imdb : response.value.getIMDB()) {
				// remove classifier (e.g. tt0436992 -> 0436992) 
				int id = Integer.parseInt(imdb.getId().substring(2));
				
				results.add(new MovieDescriptor(imdb.getTitle(), imdb.getYear(), id));
			}
		}
		
		return results;
	}
	

	@Override
	public List<SubtitleDescriptor> getSubtitleList(SearchResult searchResult, String languageName) throws WebServiceException {
		MovieDescriptor movie = (MovieDescriptor) searchResult;
		
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		
		// retrieve subtitles by name and year
		for (Subtitle subtitle : getSubtitleList(null, movie.getName(), movie.getYear(), languageName)) {
			subtitles.add(new SublightSubtitleDescriptor(subtitle, this));
		}
		
		return subtitles;
	}
	

	public List<SubtitleDescriptor> getSubtitleList(File videoFile, String languageName) throws WebServiceException, IOException {
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		
		// retrieve subtitles by video hash
		for (Subtitle subtitle : getSubtitleList(SublightVideoHasher.computeHash(videoFile), null, null, languageName)) {
			// only keep linked subtitles
			if (subtitle.isIsLinked()) {
				subtitles.add(new SublightSubtitleDescriptor(subtitle, this));
			}
		}
		
		return subtitles;
	}
	

	protected List<Subtitle> getSubtitleList(String videoHash, String name, Integer year, String languageName) throws WebServiceException {
		// require login
		login();
		
		// given language or all languages
		ArrayOfSubtitleLanguage languages = new ArrayOfSubtitleLanguage();
		
		if (languageName != null) {
			// given language
			languages.getSubtitleLanguage().add(getSubtitleLanguage(languageName));
		} else {
			// all languages
			Collections.addAll(languages.getSubtitleLanguage(), SubtitleLanguage.values());
		}
		
		// all genres
		ArrayOfGenre genres = new ArrayOfGenre();
		Collections.addAll(genres.getGenre(), Genre.values());
		
		// response holders
		Holder<ArrayOfSubtitle> subtitles = new Holder<ArrayOfSubtitle>();
		Holder<ArrayOfRelease> releases = new Holder<ArrayOfRelease>();
		Holder<String> error = new Holder<String>();
		
		webservice.searchSubtitles3(session, videoHash, name, year, null, null, languages, genres, null, null, null, subtitles, releases, null, error);
		
		// abort if something went wrong
		checkError(error);
		
		// return empty list if response is empty
		if (subtitles.value == null) {
			return Collections.emptyList();
		}
		
		// map all release names by subtitle id
		if (releases.value != null) {
			Map<String, String> releaseNameBySubtitleID = new HashMap<String, String>();
			
			// map release names by subtitle id
			for (Release release : releases.value.getRelease()) {
				releaseNameBySubtitleID.put(release.getSubtitleID(), release.getName());
			}
			
			// set release names
			for (Subtitle subtitle : subtitles.value.getSubtitle()) {
				subtitle.setRelease(releaseNameBySubtitleID.get(subtitle.getSubtitleID()));
			}
		}
		
		return subtitles.value.getSubtitle();
	}
	

	@SuppressWarnings("unchecked")
	private static final Entry<SubtitleLanguage, String>[] aliasList = new Entry[] {
			new SimpleEntry(SubtitleLanguage.PORTUGUESE_BRAZIL, "Brazilian"), 
			new SimpleEntry(SubtitleLanguage.BOSNIAN_LATIN, "Bosnian"), 
			new SimpleEntry(SubtitleLanguage.SERBIAN_LATIN, "Serbian")
	};
	

	protected SubtitleLanguage getSubtitleLanguage(String languageName) {
		// check subtitle language enum
		for (SubtitleLanguage language : SubtitleLanguage.values()) {
			if (language.value().equalsIgnoreCase(languageName))
				return language;
		}
		
		// check alias list
		for (Entry<SubtitleLanguage, String> alias : aliasList) {
			if (alias.getValue().equalsIgnoreCase(languageName))
				return alias.getKey();
		}
		
		// illegal language name
		throw new IllegalArgumentException("Illegal language: " + languageName);
	}
	

	protected String getLanguageName(SubtitleLanguage language) {
		// check alias list
		for (Entry<SubtitleLanguage, String> alias : aliasList) {
			if (language == alias.getKey())
				return alias.getValue();
		}
		
		// use language value by default
		return language.value();
	}
	

	protected byte[] getZipArchive(Subtitle subtitle) throws WebServiceException {
		// require login
		login();
		
		Holder<String> ticket = new Holder<String>();
		Holder<byte[]> data = new Holder<byte[]>();
		Holder<String> error = new Holder<String>();
		
		webservice.getDownloadTicket(session, null, subtitle.getSubtitleID(), null, ticket, null, error);
		
		// abort if something went wrong
		checkError(error);
		
		webservice.downloadByID4(session, subtitle.getSubtitleID(), -1, false, ticket.value, null, data, null, error);
		
		// abort if something went wrong
		checkError(error);
		
		// return zip file bytes
		return data.value;
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
		return null;
	}
	

	protected synchronized void login() throws WebServiceException {
		if (webservice == null) {
			// lazy initialize because all the JAX-WS class loading can take quite some time
			webservice = new SubtitlesAPI2().getSubtitlesAPI2Soap();
		}
		
		if (session == null) {
			// args contains only iid
			ArrayOfString args = new ArrayOfString();
			args.getString().add(iid);
			
			Holder<String> session = new Holder<String>();
			Holder<String> error = new Holder<String>();
			
			webservice.logInAnonymous4(clientInfo, args, session, null, error);
			
			// abort if something went wrong
			checkError(error);
			
			// start session
			this.session = session.value;
		}
		
		// reset timer
		logoutTimer.set(10, TimeUnit.MINUTES, true);
	}
	

	protected synchronized void logout() throws WebServiceException {
		if (session != null) {
			Holder<String> error = new Holder<String>();
			
			webservice.logOut(session, null, error);
			
			// abort if something went wrong
			checkError(error);
			
			// stop session
			this.session = null;
			
			// cancel timer
			logoutTimer.cancel();
		}
	}
	

	protected void checkError(Holder<?> error) throws WebServiceException {
		if (error.value != null) {
			throw new WebServiceException("Response indicates error: " + error.value);
		}
	}
	

	protected final Timer logoutTimer = new Timer() {
		
		@Override
		public void run() {
			logout();
		}
	};
	
}
