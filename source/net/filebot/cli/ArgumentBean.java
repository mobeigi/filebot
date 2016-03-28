package net.filebot.cli;

import static java.util.Collections.*;
import static net.filebot.Logging.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.ExplicitBooleanOptionHandler;

import net.filebot.Language;

public class ArgumentBean {

	@Option(name = "--mode", usage = "Open GUI in single panel mode", metaVar = "[Rename, Subtitles, SFV]")
	public String mode = null;

	@Option(name = "-rename", usage = "Rename media files")
	public boolean rename = false;

	@Option(name = "--db", usage = "Database", metaVar = "[TheTVDB, AniDB] or [TheMovieDB] or [AcoustID, ID3] or [xattr]")
	public String db;

	@Option(name = "--order", usage = "Episode order", metaVar = "[Airdate, Absolute, DVD]")
	public String order = "Airdate";

	@Option(name = "--action", usage = "Rename action", metaVar = "[move, copy, keeplink, symlink, hardlink, test]")
	public String action = "move";

	@Option(name = "--conflict", usage = "Conflict resolution", metaVar = "[skip, override, auto, index, fail]")
	public String conflict = "skip";

	@Option(name = "--filter", usage = "Filter expression", metaVar = "expression")
	public String filter = null;

	@Option(name = "--format", usage = "Format expression", metaVar = "expression")
	public String format;

	@Option(name = "-non-strict", usage = "Enable advanced matching and more aggressive guessing")
	public boolean nonStrict = false;

	@Option(name = "-get-subtitles", usage = "Fetch subtitles")
	public boolean getSubtitles;

	@Option(name = "--q", usage = "Force lookup query", metaVar = "series/movie title")
	public String query;

	@Option(name = "--lang", usage = "Language", metaVar = "3-letter language code")
	public String lang = "en";

	@Option(name = "-check", usage = "Create/Check verification files")
	public boolean check;

	@Option(name = "--output", usage = "Output path", metaVar = "/path")
	public String output;

	@Option(name = "--encoding", usage = "Output character encoding", metaVar = "[UTF-8, Windows-1252]")
	public String encoding;

	@Option(name = "-list", usage = "Fetch episode list")
	public boolean list = false;

	@Option(name = "-mediainfo", usage = "Get media info")
	public boolean mediaInfo = false;

	@Option(name = "-revert", usage = "Revert files")
	public boolean revert = false;

	@Option(name = "-extract", usage = "Extract archives")
	public boolean extract = false;

	@Option(name = "-script", usage = "Run Groovy script", metaVar = "[fn:name] or [dev:name] or [/path/to/script.groovy]")
	public String script = null;

	@Option(name = "--log", usage = "Log level", metaVar = "[all, fine, info, warning, off]")
	public String log = "all";

	@Option(name = "--log-file", usage = "Log file", metaVar = "/path/to/log.txt")
	public String logFile = null;

	@Option(name = "--log-lock", usage = "Lock log file", metaVar = "[yes, no]", handler = ExplicitBooleanOptionHandler.class)
	public boolean logLock = true;

	@Option(name = "-r", usage = "Resolve folders recursively")
	public boolean recursive = false;

	@Option(name = "-clear-cache", usage = "Clear cached and temporary data")
	public boolean clearCache = false;

	@Option(name = "-clear-prefs", usage = "Clear application settings")
	public boolean clearPrefs = false;

	@Option(name = "-unixfs", usage = "Do not strip invalid characters from file paths")
	public boolean unixfs = false;

	@Option(name = "-no-xattr", usage = "Disable extended attributes")
	public boolean disableExtendedAttributes = false;

	@Option(name = "-version", usage = "Print version identifier")
	public boolean version = false;

	@Option(name = "-help", usage = "Print this help message")
	public boolean help = false;

	@Option(name = "--def", usage = "Define script variables", handler = BindingsHandler.class)
	public Map<String, String> defines = new LinkedHashMap<String, String>();

	@Argument
	public List<String> arguments = new ArrayList<String>();

	public boolean runCLI() {
		return rename || getSubtitles || check || list || mediaInfo || revert || extract || script != null;
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
				debug.warning(format("Illegal Argument: %s (%s)", e, argument));
			}

			if (resolveFolders && file.isDirectory()) {
				if (recursive) {
					files.addAll(listFiles(file));
				} else {
					files.addAll(filter(getChildren(file, FILES), NOT_HIDDEN));
				}
			} else {
				files.add(file);
			}
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

	private ArgumentBean(String... array) {
		this.array = array;
	}

	public String[] getArray() {
		return array.clone();
	}

	public static ArgumentBean parse(String[] args) throws CmdLineException {
		ArgumentBean bean = new ArgumentBean(args);
		CmdLineParser parser = new CmdLineParser(bean);
		parser.parseArgument(args);
		return bean;
	}

	public static void printHelp(ArgumentBean argumentBean, OutputStream out) {
		new CmdLineParser(argumentBean, ParserProperties.defaults().withShowDefaults(false).withOptionSorter(null)).printUsage(out);
	}

}
