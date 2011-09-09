
package net.sourceforge.filebot.cli;


import static java.util.Collections.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptException;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.WebServices;
import net.sourceforge.filebot.format.ExpressionFormat;
import net.sourceforge.filebot.ui.Language;
import net.sourceforge.filebot.web.EpisodeListProvider;
import net.sourceforge.filebot.web.MovieIdentificationService;


public class ArgumentBean {
	
	@Option(name = "-rename-series", usage = "Rename episodes", metaVar = "folder")
	public boolean renameSeries;
	
	@Option(name = "-rename-movie", usage = "Rename movie", metaVar = "folder")
	public boolean renameMovie;
	
	@Option(name = "-get-subtitles", usage = "Fetch subtitles", metaVar = "folder")
	public boolean getSubtitles;
	
	@Option(name = "--format", usage = "Episode naming scheme", metaVar = "expression")
	public String format = "{n} - {s+'x'}{e.pad(2)} - {t}";
	
	@Option(name = "--q", usage = "Search query", metaVar = "name")
	public String query = null;
	
	@Option(name = "--db", usage = "Episode database")
	public String db = null;
	
	@Option(name = "--lang", usage = "Language", metaVar = "language code")
	public String lang = "en";
	
	@Option(name = "-help", usage = "Print this help message")
	public boolean help = false;
	
	@Option(name = "-open", usage = "Open file", metaVar = "<file>")
	public boolean open = false;
	
	@Option(name = "-clear", usage = "Clear application settings")
	public boolean clear = false;
	
	@Argument
	public List<String> arguments = new ArrayList<String>();
	

	public boolean runCLI() {
		return getSubtitles || renameSeries || renameMovie;
	}
	

	public boolean printHelp() {
		return help;
	}
	

	public boolean openSFV() {
		return open && containsOnly(getFiles(false), MediaTypes.getDefaultFilter("verification"));
	}
	

	public boolean clearUserData() {
		return clear;
	}
	

	public List<File> getFiles(boolean resolveFolders) {
		List<File> files = new ArrayList<File>();
		
		// resolve given paths
		for (String argument : arguments) {
			try {
				File file = new File(argument).getCanonicalFile();
				
				// resolve folders
				files.addAll(resolveFolders && file.isDirectory() ? listFiles(singleton(file), 0) : singleton(file));
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		return files;
	}
	

	public ExpressionFormat getEpisodeFormat() throws ScriptException {
		return new ExpressionFormat(format);
	}
	

	public Language getLanguage() {
		Language language = Language.getLanguage(lang);
		
		if (language == null)
			throw new IllegalArgumentException("Illegal language code: " + lang);
		
		return language;
	}
	

	public EpisodeListProvider getEpisodeListProvider() throws Exception {
		if (db == null)
			return WebServices.TVRage;
		
		return (EpisodeListProvider) WebServices.class.getField(db).get(null);
	}
	

	public MovieIdentificationService getMovieIdentificationService() throws Exception {
		if (db == null)
			return WebServices.OpenSubtitles;
		
		return (MovieIdentificationService) WebServices.class.getField(db).get(null);
	}
	
}
