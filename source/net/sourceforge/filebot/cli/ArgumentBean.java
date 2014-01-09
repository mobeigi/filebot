package net.sourceforge.filebot.cli;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sourceforge.filebot.Language;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.ExplicitBooleanOptionHandler;

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

	@Option(name = "-non-strict", usage = "Enable advanced matching and more aggressive guess work")
	public boolean nonStrict = false;

	@Option(name = "-get-subtitles", usage = "Fetch subtitles", metaVar = "fileset")
	public boolean getSubtitles;

	@Option(name = "-get-missing-subtitles", usage = "Fetch missing subtitles", metaVar = "fileset")
	public boolean getMissingSubtitles;

	@Option(name = "--q", usage = "Force lookup query", metaVar = "series/movie title")
	public String query;

	@Option(name = "--lang", usage = "Language", metaVar = "2-letter language code")
	public String lang = "en";

	@Option(name = "-check", usage = "Create/Check verification file", metaVar = "fileset")
	public boolean check;

	@Option(name = "--output", usage = "Output path", metaVar = "folder")
	public String output;

	@Option(name = "--encoding", usage = "Output character encoding", metaVar = "[UTF-8, windows-1252, GB18030, etc]")
	public String encoding;

	@Option(name = "-list", usage = "Fetch episode list")
	public boolean list = false;

	@Option(name = "-mediainfo", usage = "Get media info")
	public boolean mediaInfo = false;

	@Option(name = "-extract", usage = "Extract archives")
	public boolean extract = false;

	@Option(name = "-script", usage = "Run Groovy script", metaVar = "path/to/script.groovy")
	public String script = null;

	@Option(name = "-trust-script", usage = "Lift scripting restrictions")
	public boolean trustScript = false;

	@Option(name = "--log", usage = "Log level", metaVar = "[all, config, info, warning]")
	public String log = "all";

	@Option(name = "--log-file", usage = "Log file", metaVar = "path/to/log.txt")
	public String logFile = null;

	@Option(name = "--log-lock", usage = "Lock log file", metaVar = "[yes, no]", handler = ExplicitBooleanOptionHandler.class)
	public boolean logLock = true;

	@Option(name = "-r", usage = "Resolve folders recursively")
	public boolean recursive = false;

	@Option(name = "--mode", usage = "Open GUI with the specified mode only", metaVar = "[rename, sfv, etc]")
	public String mode = null;

	@Option(name = "-clear-cache", usage = "Clear cached and temporary data")
	public boolean clearCache = false;

	@Option(name = "-clear-prefs", usage = "Clear application settings")
	public boolean clearPrefs = false;

	@Option(name = "-unixfs", usage = "Do not strip invalid characters from file paths")
	public boolean unixfs = false;

	@Option(name = "-no-xattr", usage = "Disable extended attributes")
	public boolean disableExtendedAttributes = false;

	@Option(name = "-no-analytics", usage = "Disable analytics")
	public boolean disableAnalytics = false;

	@Option(name = "-version", usage = "Print version identifier")
	public boolean version = false;

	@Option(name = "-help", usage = "Print this help message")
	public boolean help = false;

	@Option(name = "--def", usage = "Define script variables", handler = BindingsHandler.class)
	public List<Entry<String, String>> bindings;

	@Argument
	public List<String> arguments;

	public boolean runCLI() {
		return rename || getSubtitles || getMissingSubtitles || check || list || mediaInfo || extract || script != null;
	}

	public boolean printVersion() {
		return version;
	}

	public boolean printHelp() {
		return help;
	}

	public boolean clearCache() {
		return clearCache;
	}

	public boolean clearUserData() {
		return clearPrefs;
	}

	public List<File> getFiles(boolean resolveFolders) {
		if (arguments == null || arguments.isEmpty()) {
			return emptyList();
		}

		// resolve given paths
		List<File> files = new ArrayList<File>();

		for (String argument : arguments) {
			File file = new File(argument).getAbsoluteFile();

			// resolve relative paths
			try {
				file = file.getCanonicalFile();
			} catch (Exception e) {
				Logger.getLogger(getClass().getName()).log(Level.WARNING, e.getMessage());
			}

			// resolve folders
			files.addAll(resolveFolders && file.isDirectory() ? listFiles(singleton(file), recursive ? 10 : 0, false) : singleton(file));
		}

		return files;
	}

	public Locale getLocale() {
		return new Locale(lang);
	}

	public Language getLanguage() {
		return Language.findLanguage(lang);
	}

	public Level getLogLevel() {
		return Level.parse(log.toUpperCase());
	}

	private final String[] array;

	public ArgumentBean(String... array) {
		this.array = array;
	}

	public List<String> getArray() {
		return unmodifiableList(asList(array));
	}

}
