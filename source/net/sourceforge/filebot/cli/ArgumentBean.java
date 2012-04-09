
package net.sourceforge.filebot.cli;


import static java.util.Collections.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

import net.sourceforge.filebot.MediaTypes;


public class ArgumentBean {
	
	@Option(name = "-rename", usage = "Rename episode/movie files", metaVar = "fileset")
	public boolean rename = false;
	
	@Option(name = "--db", usage = "Episode/Movie database", metaVar = "[TVRage, AniDB, TheTVDB] or [OpenSubtitles, IMDb, TheMovieDB]")
	public String db;
	
	@Option(name = "--order", usage = "Episode order", metaVar = "[Airdate, Absolute, DVD]")
	public String order = "Airdate";
	
	@Option(name = "--action", usage = "Rename action", metaVar = "[move, copy, keeplink, symlink, hardlink, test]")
	public String action = "move";
	
	@Option(name = "--conflict", usage = "Conflict resolution", metaVar = "[override, skip, fail]")
	public String conflict = "skip";
	
	@Option(name = "--filter", usage = "Episode filter", metaVar = "expression")
	public String filter = null;
	
	@Option(name = "--format", usage = "Episode/Movie naming scheme", metaVar = "expression")
	public String format;
	
	@Option(name = "-non-strict", usage = "Use less strict matching")
	public boolean nonStrict = false;
	
	@Option(name = "-get-subtitles", usage = "Fetch subtitles", metaVar = "fileset")
	public boolean getSubtitles;
	
	@Option(name = "-get-missing-subtitles", usage = "Fetch missing subtitles", metaVar = "fileset")
	public boolean getMissingSubtitles;
	
	@Option(name = "--q", usage = "Search query", metaVar = "title")
	public String query;
	
	@Option(name = "--lang", usage = "Language", metaVar = "2-letter language code")
	public String lang = "en";
	
	@Option(name = "-check", usage = "Create/Check verification file", metaVar = "fileset")
	public boolean check;
	
	@Option(name = "--output", usage = "Output path / format", metaVar = "Output options")
	public String output;
	
	@Option(name = "--encoding", usage = "Output character encoding", metaVar = "[UTF-8, windows-1252, GB18030, etc]")
	public String encoding;
	
	@Option(name = "-list", usage = "Fetch episode list")
	public boolean list = false;
	
	@Option(name = "-mediainfo", usage = "Get media info")
	public boolean mediaInfo = false;
	
	@Option(name = "-extract", usage = "Extract archives")
	public boolean extract = false;
	
	@Option(name = "-script", usage = "Run Groovy script", metaVar = "robot.groovy")
	public String script = null;
	
	@Option(name = "-trust-script", usage = "Lift scripting restrictions")
	public boolean trustScript = false;
	
	@Option(name = "--log", usage = "Log level", metaVar = "[all, config, info, warning]")
	public String log = "all";
	
	@Option(name = "-open", usage = "Open file in GUI", metaVar = "file")
	public boolean open = false;
	
	@Option(name = "-clear", usage = "Clear cache and application settings")
	public boolean clear = false;
	
	@Option(name = "-no-analytics", usage = "Disable analytics")
	public boolean disableAnalytics = false;
	
	@Option(name = "-version", usage = "Print version identifier")
	public boolean version = false;
	
	@Option(name = "-help", usage = "Print this help message")
	public boolean help = false;
	
	@Argument
	public List<String> arguments = new ArrayList<String>();
	
	
	public boolean runCLI() {
		return rename || getSubtitles || getMissingSubtitles || check || list || mediaInfo || extract || script != null;
	}
	
	
	public boolean openSFV() {
		return open && containsOnly(getFiles(false), MediaTypes.getDefaultFilter("verification"));
	}
	
	
	public boolean printVersion() {
		return version;
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
			// resolve folders
			File file = new File(argument).getAbsoluteFile();
			files.addAll(resolveFolders && file.isDirectory() ? listFiles(singleton(file), 0, false) : singleton(file));
		}
		
		return files;
	}
	
	
	public URI getScriptLocation() {
		try {
			return new URL(script).toURI();
		} catch (Exception eu) {
			if (script.startsWith("script://")) {
				try {
					return new URI("script", script.substring(9), null, null, null);
				} catch (URISyntaxException e) {
					throw new IllegalArgumentException(e);
				}
			}
			try {
				File file = new File(script);
				if (!file.exists()) {
					throw new FileNotFoundException(file.getPath());
				}
				return file.toURI();
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
	
	
	public Locale getLocale() {
		return new Locale(lang);
	}
	
	
	public Level getLogLevel() {
		return Level.parse(log.toUpperCase());
	}
	
}
