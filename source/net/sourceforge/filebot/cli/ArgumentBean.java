
package net.sourceforge.filebot.cli;


import static java.util.Collections.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import net.sourceforge.filebot.MediaTypes;


public class ArgumentBean {
	
	@Option(name = "-rename", usage = "Rename episode/movie files", metaVar = "fileset")
	public boolean rename = false;
	
	@Option(name = "--db", usage = "Episode/Movie database", metaVar = "[TVRage, AniDB, TheTVDB] or [OpenSubtitles, TheMovieDB]")
	public String db;
	
	@Option(name = "--format", usage = "Episode naming scheme", metaVar = "expression")
	public String format;
	
	@Option(name = "-non-strict", usage = "Use less strict matching")
	public boolean nonStrict = false;
	
	@Option(name = "-get-subtitles", usage = "Fetch subtitles", metaVar = "fileset")
	public boolean getSubtitles;
	
	@Option(name = "--q", usage = "Search query", metaVar = "title")
	public String query;
	
	@Option(name = "--lang", usage = "Language", metaVar = "2-letter language code")
	public String lang = "en";
	
	@Option(name = "-check", usage = "Create/Check verification file", metaVar = "fileset")
	public boolean check;
	
	@Option(name = "--output", usage = "Output options", metaVar = "[sfv, md5, sha1] or [srt]")
	public String output;
	
	@Option(name = "--encoding", usage = "Character encoding", metaVar = "[UTF-8, windows-1252, GB18030, etc]")
	public String encoding;
	
	@Option(name = "-list", usage = "Fetch episode list")
	public boolean list = false;
	
	@Option(name = "-mediainfo", usage = "Get media info")
	public boolean mediaInfo = false;
	
	@Option(name = "-script", usage = "Run Groovy script")
	public String script = null;
	
	@Option(name = "--log", usage = "Log level", metaVar = "[all, config, info, warning]")
	public String log = "all";
	
	@Option(name = "-open", usage = "Open file in GUI", metaVar = "file")
	public boolean open = false;
	
	@Option(name = "-clear", usage = "Clear cache and application settings")
	public boolean clear = false;
	
	@Option(name = "-no-analytics", usage = "Disable analytics")
	public boolean disableAnalytics = false;
	
	@Option(name = "-help", usage = "Print this help message")
	public boolean help = false;
	
	@Argument
	public List<String> arguments = new ArrayList<String>();
	

	public boolean runCLI() {
		return rename || getSubtitles || check || list || mediaInfo || script != null;
	}
	

	public boolean openSFV() {
		return open && containsOnly(getFiles(false), MediaTypes.getDefaultFilter("verification"));
	}
	

	public boolean printHelp() {
		return help;
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
				files.addAll(resolveFolders && file.isDirectory() ? listFiles(singleton(file), 0, false) : singleton(file));
			} catch (IOException e) {
				throw new IllegalArgumentException(e);
			}
		}
		
		return files;
	}
	

	public URL getScriptLocation() {
		try {
			return new URL(script);
		} catch (MalformedURLException eu) {
			try {
				File file = new File(script);
				if (!file.exists())
					throw new FileNotFoundException(file.getPath());
				
				return file.toURI().toURL();
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
	

	public Level getLogLevel() {
		return Level.parse(log.toUpperCase());
	}
	
}
