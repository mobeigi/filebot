package net.filebot.cli;

import static net.filebot.Logging.*;
import static net.filebot.Settings.*;
import static net.filebot.util.ExceptionUtilities.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import net.filebot.Cache;
import net.filebot.CacheType;
import net.filebot.MediaTypes;
import net.filebot.StandardRenameAction;
import net.filebot.WebServices;
import net.filebot.cli.ScriptShell.ScriptProvider;

public class ArgumentProcessor {

	public int run(ArgumentBean args) {
		try {
			if (args.script == null) {
				// execute command
				return runCommand(args);
			} else {
				// execute user script
				runScript(args);

				// script finished successfully
				log.finest("Done ヾ(＠⌒ー⌒＠)ノ" + System.lineSeparator());
				return 0;
			}
		} catch (Throwable e) {
			if (findCause(e, CmdlineException.class) != null) {
				log.log(Level.WARNING, findCause(e, CmdlineException.class).getMessage());
			} else if (findCause(e, ScriptDeath.class) != null) {
				log.log(Level.WARNING, findCause(e, ScriptDeath.class).getMessage());
			} else {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		// script failed with exception -> exit with non-zero exit code (and use positive code to avoid issues with launch4j launcher)
		log.finest("Failure (°_°)");
		return 1;
	}

	public int runCommand(ArgumentBean args) throws Exception {
		CmdlineInterface cli = new CmdlineOperations();

		// sanity checks
		if (args.getSubtitles && args.recursive) {
			throw new CmdlineException("`filebot -get-subtitles -r` has been disabled due to abuse. Please see http://bit.ly/suball for details.");
		}

		// print episode info
		if (args.list) {
			List<String> lines = cli.fetchEpisodeList(args.query, args.format, args.db, args.order, args.filter, args.lang);
			lines.forEach(System.out::println);
			return lines.isEmpty() ? 1 : 0;
		}

		// print media info
		if (args.mediaInfo) {
			List<String> lines = cli.getMediaInfo(args.getFiles(true), args.format, args.filter);
			lines.forEach(System.out::println);
			return lines.isEmpty() ? 1 : 0;
		}

		// revert files
		if (args.revert) {
			List<File> files = cli.revert(args.getFiles(false), args.filter, "TEST".equalsIgnoreCase(args.action));
			return files.isEmpty() ? 1 : 0;
		}

		// file operations
		Collection<File> files = new LinkedHashSet<File>(args.getFiles(true));

		if (args.extract) {
			files.addAll(cli.extract(files, args.output, args.conflict, null, true));
		}

		if (args.getSubtitles) {
			files.addAll(cli.getMissingSubtitles(files, WebServices.OpenSubtitles.getName(), args.query, args.lang, args.output, args.encoding, args.format, !args.nonStrict));
		}

		if (args.rename) {
			cli.rename(files, StandardRenameAction.forName(args.action), args.conflict, args.output, args.format, args.db, args.query, args.order, args.filter, args.lang, !args.nonStrict);
		}

		if (args.check) {
			// check verification file
			if (containsOnly(files, MediaTypes.getDefaultFilter("verification"))) {
				if (!cli.check(files)) {
					throw new Exception("Data corruption detected"); // one or more hashes mismatch
				}
			} else {
				cli.compute(files, args.output, args.encoding);
			}
		}

		return 0;
	}

	public void runScript(ArgumentBean args) throws Throwable {
		Bindings bindings = new SimpleBindings();
		bindings.put(ScriptShell.SHELL_ARGV_BINDING_NAME, args.getArray());
		bindings.put(ScriptShell.ARGV_BINDING_NAME, args.getFiles(false));

		DefaultScriptProvider scriptProvider = new DefaultScriptProvider();
		URI script = scriptProvider.getScriptLocation(args.script);

		if (!scriptProvider.isInline(script)) {
			if (scriptProvider.resolveTemplate(script) != null) {
				scriptProvider.setBaseScheme(new URI(script.getScheme(), "%s", null));
			} else if (scriptProvider.isFile(script)) {
				File parent = new File(script).getParentFile();
				File template = new File(parent, "%s.groovy");
				scriptProvider.setBaseScheme(template.toURI());
			} else {
				File parent = new File(script.getPath()).getParentFile();
				String template = normalizePathSeparators(new File(parent, "%s.groovy").getPath());
				scriptProvider.setBaseScheme(new URI(script.getScheme(), script.getHost(), template, script.getQuery(), script.getFragment()));
			}
		}

		ScriptShell shell = new ScriptShell(scriptProvider, args.defines);
		shell.runScript(script, bindings);
	}

	public static class DefaultScriptProvider implements ScriptProvider {

		public static final String SCHEME_REMOTE_STABLE = "fn";
		public static final String SCHEME_REMOTE_DEVEL = "dev";
		public static final String SCHEME_INLINE_GROOVY = "g";
		public static final String SCHEME_LOCAL_FILE = "file";

		public static final Pattern TEMPLATE_SCHEME = Pattern.compile("([a-z]{1,5}):(.+)");

		public static final String NAME = "script";

		private URI baseScheme;

		public void setBaseScheme(URI baseScheme) {
			this.baseScheme = baseScheme;
		}

		public String resolveTemplate(URI uri) {
			try {
				String template = getApplicationProperty(NAME + '.' + uri.getScheme());
				return String.format(template, uri.getSchemeSpecificPart());
			} catch (MissingResourceException e) {
				return null;
			}
		}

		public boolean isInline(URI r) {
			return SCHEME_INLINE_GROOVY.equals(r.getScheme());
		}

		public boolean isFile(URI r) {
			return SCHEME_LOCAL_FILE.equals(r.getScheme());
		}

		@Override
		public URI getScriptLocation(String input) throws Exception {
			// e.g. dev:amc
			Matcher template = TEMPLATE_SCHEME.matcher(input);
			if (template.matches()) {
				URI uri = new URI(template.group(1), template.group(2), null);

				// 1. fn:amc
				// 2. dev:sysinfo
				// 3. g:println 'hello world'
				switch (uri.getScheme()) {
				case SCHEME_REMOTE_STABLE:
				case SCHEME_REMOTE_DEVEL:
				case SCHEME_INLINE_GROOVY:
					return uri;
				default:
					return new URL(input).toURI();
				}
			}

			File file = new File(input);
			if (baseScheme != null && !file.isAbsolute()) {
				return new URI(baseScheme.getScheme(), String.format(baseScheme.getSchemeSpecificPart(), input), null);
			}

			// e.g. /path/to/script.groovy
			if (!file.isFile()) {
				throw new FileNotFoundException(file.getPath());
			}
			return file.getCanonicalFile().toURI();
		}

		@Override
		public String fetchScript(URI uri) throws Exception {
			if (isFile(uri)) {
				return readTextFile(new File(uri));
			}

			if (isInline(uri)) {
				return uri.getSchemeSpecificPart();
			}

			// check remote script for updates (weekly for stable and daily for devel branches)
			Cache cache = Cache.getCache(NAME, CacheType.Persistent);
			return cache.text(uri.toString(), s -> {
				return new URL(resolveTemplate(uri));
			}).expire(SCHEME_REMOTE_DEVEL.equals(uri.getScheme()) ? Cache.ONE_DAY : Cache.ONE_WEEK).get();
		}
	}

}
