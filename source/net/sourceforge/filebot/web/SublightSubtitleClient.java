
package net.sourceforge.filebot.web;


import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import net.sourceforge.filebot.ResourceManager;
import net.sourceforge.tuned.Timer;
import net.sublight.webservice.ArrayOfGenre;
import net.sublight.webservice.ArrayOfIMDB;
import net.sublight.webservice.ArrayOfRelease;
import net.sublight.webservice.ArrayOfSubtitle;
import net.sublight.webservice.ArrayOfSubtitleLanguage;
import net.sublight.webservice.Genre;
import net.sublight.webservice.IMDB;
import net.sublight.webservice.Release;
import net.sublight.webservice.Subtitle;
import net.sublight.webservice.SubtitleLanguage;
import net.sublight.webservice.SubtitlesAPI2;
import net.sublight.webservice.SubtitlesAPI2Soap;


public class SublightSubtitleClient implements SubtitleProvider {
	
	private static final String iid = "42cc1701-3752-49e2-a148-332960073452";
	
	private final String clientInfo;
	
	private final SubtitlesAPI2Soap webservice;
	
	private String session;
	

	public SublightSubtitleClient(String clientInfo) {
		this.clientInfo = clientInfo;
		this.webservice = new SubtitlesAPI2().getSubtitlesAPI2Soap();
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
			subtitles.add(new SublightSubtitleDescriptor(subtitle));
		}
		
		return subtitles;
	}
	

	public List<SubtitleDescriptor> getSubtitleList(File videoFile, String languageName) throws WebServiceException, IOException {
		List<SubtitleDescriptor> subtitles = new ArrayList<SubtitleDescriptor>();
		
		// retrieve subtitles by video hash
		for (Subtitle subtitle : getSubtitleList(SublightVideoHasher.computeHash(videoFile), null, null, languageName)) {
			// only keep linked subtitles
			if (subtitle.isIsLinked()) {
				subtitles.add(new SublightSubtitleDescriptor(subtitle));
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
	

	protected SubtitleLanguage getSubtitleLanguage(String languageName) {
		for (SubtitleLanguage language : SubtitleLanguage.values()) {
			if (language.value().equalsIgnoreCase(languageName))
				return language;
		}
		
		// special language name handling
		if (languageName.equalsIgnoreCase("Brazilian"))
			return SubtitleLanguage.PORTUGUESE_BRAZIL;
		if (languageName.equalsIgnoreCase("Bosnian"))
			return SubtitleLanguage.BOSNIAN_LATIN;
		if (languageName.equalsIgnoreCase("Serbian"))
			return SubtitleLanguage.SERBIAN_LATIN;
		
		// unkown language
		throw new IllegalArgumentException("Illegal language: " + languageName);
	}
	

	@Override
	public URI getSubtitleListLink(SearchResult searchResult, String languageName) {
		return null;
	}
	

	protected synchronized void login() throws WebServiceException {
		if (session == null) {
			Holder<String> session = new Holder<String>();
			Holder<String> error = new Holder<String>();
			
			webservice.logInAnonymous3(clientInfo, iid, session, null, error);
			
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
			throw new WebServiceException("Login failed: " + error.value);
		}
	}
	

	protected final Timer logoutTimer = new Timer() {
		
		@Override
		public void run() {
			logout();
		}
	};
	
}
