package net.sourceforge.filebot.cli;

import static net.sourceforge.filebot.Settings.*;
import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.filebot.util.ExceptionUtilities.*;
import static net.sourceforge.filebot.util.FileUtilities.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.StandardRenameAction;
import net.sourceforge.filebot.cli.ScriptShell.ScriptProvider;
import net.sourceforge.filebot.web.CachedResource;

public class ArgumentProcessor {

	public int process(ArgumentBean args, CmdlineInterface cli) {
		Analytics.trackView(ArgumentProcessor.class, "FileBot CLI");
		CLILogger.setLevel(args.getLogLevel());

		try {
			// print episode info
			if (args.list) {
				for (String eps : cli.fetchEpisodeList(args.query, args.format, args.db, args.order, args.lang)) {
					System.out.println(eps);
				}
				return 0;
			}

			// print media info
			if (args.mediaInfo) {
				for (File file : args.getFiles(true)) {
					System.out.println(cli.getMediaInfo(file, args.format));
				}
				return 0;
			}

			// execute CLI operations
			if (args.script == null) {
				// file operations
				Collection<File> files = new LinkedHashSet<File>(args.getFiles(true));

				if (args.extract) {
					files.addAll(cli.extract(files, args.output, args.conflict, null, true));
				}

				if (args.getSubtitles) {
					files.addAll(cli.getSubtitles(files, args.db, args.query, args.lang, args.output, args.encoding, args.format, !args.nonStrict));
				} else if (args.getMissingSubtitles) {
					files.addAll(cli.getMissingSubtitles(files, args.db, args.query, args.lang, args.output, args.encoding, args.format, !args.nonStrict));
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
			} else {
				// execute user script
				Bindings bindings = new SimpleBindings();
				bindings.put(ScriptShell.ARGV_BINDING_NAME, args.getFiles(false));

				DefaultScriptProvider scriptProvider = new DefaultScriptProvider();
				URI script = scriptProvider.getScriptLocation(args.script);

				if (!scriptProvider.isInlineScheme(script.getScheme())) {
					if (scriptProvider.getResourceTemplate(script.getScheme()) != null) {
						scriptProvider.setBaseScheme(new URI(script.getScheme(), "%s", null));
					} else if ("file".equals(script.getScheme())) {
						File base = new File(script).getParentFile();
						File template = new File(base, "%s.groovy");
						scriptProvider.setBaseScheme(template.toURI());
					} else {
						File base = new File(script.getPath()).getParentFile();
						String template = normalizePathSeparators(new File(base, "%s.groovy").getPath());
						scriptProvider.setBaseScheme(new URI(script.getScheme(), script.getHost(), template, script.getQuery(), script.getFragment()));
					}
				}

				ScriptShell shell = new ScriptShell(scriptProvider, args.defines);
				shell.runScript(script, bindings);
			}

			CLILogger.finest("Done ヾ(＠⌒ー⌒＠)ノ");
			return 0;
		} catch (Throwable e) {
			if (e.getClass() == Exception.class) {
				CLILogger.log(Level.SEVERE, String.format("%s: %s", getRootCause(e).getClass().getSimpleName(), getRootCauseMessage(e)));
			} else {
				CLILogger.log(Level.SEVERE, String.format("%s: %s", getRootCause(e).getClass().getSimpleName(), getRootCauseMessage(e)), getRootCause(e));
			}
			CLILogger.finest("Failure (°_°)");
			return -1;
		}
	}

	public static class DefaultScriptProvider implements ScriptProvider {

		private URI baseScheme;

		public void setBaseScheme(URI baseScheme) {
			this.baseScheme = baseScheme;
		}

		public String getResourceTemplate(String scheme) {
			try {
				return getApplicationProperty("script." + scheme);
			} catch (MissingResourceException e) {
				return null;
			}
		}

		public boolean isInlineScheme(String scheme) {
			return "g".equals(scheme);
		}

		@Override
		public URI getScriptLocation(String input) throws Exception {
			try {
				return new URL(input).toURI();
			} catch (Exception e) {
				// g:println 'hello world'
				if (input.startsWith("g:")) {
					return new URI("g", input.substring(2), null);
				}

				// fn:sortivo / svn:sortivo
				if (Pattern.matches("\\w+:.+", input)) {
					String scheme = input.substring(0, input.indexOf(':'));
					if (getResourceTemplate(scheme) != null) {
						return new URI(scheme, input.substring(scheme.length() + 1, input.length()), null);
					}
				}

				File file = new File(input);
				if (baseScheme != null && !file.isAbsolute()) {
					return new URI(baseScheme.getScheme(), String.format(baseScheme.getSchemeSpecificPart(), input), null);
				}

				// X:/foo/bar.groovy
				if (!file.isFile()) {
					throw new FileNotFoundException(file.getPath());
				}
				return file.getAbsoluteFile().toURI();
			}
		}

		@Override
		public String fetchScript(URI uri) throws IOException {
			if (uri.getScheme().equals("file")) {
				return readAll(new InputStreamReader(new FileInputStream(new File(uri)), "UTF-8"));
			}

			if (uri.getScheme().equals("g")) {
				return uri.getSchemeSpecificPart();
			}

			// remote script
			String resolver = getResourceTemplate(uri.getScheme());
			String url = (resolver != null) ? String.format(resolver, uri.getSchemeSpecificPart()) : uri.toString();

			// fetch remote script only if modified
			CachedResource<String> script = new CachedResource<String>(url, String.class, CachedResource.ONE_DAY) {

				@Override
				public String process(ByteBuffer data) {
					return Charset.forName("UTF-8").decode(data).toString();
				}
			};
			return script.get();
		}
	}

}
