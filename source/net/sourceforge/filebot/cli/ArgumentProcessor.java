
package net.sourceforge.filebot.cli;


import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.tuned.ExceptionUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.MediaTypes;
import net.sourceforge.filebot.cli.ScriptShell.ScriptProvider;
import net.sourceforge.filebot.web.CachedResource;


public class ArgumentProcessor {
	
	public ArgumentBean parse(String[] args) throws CmdLineException {
		final ArgumentBean bean = new ArgumentBean();
		
		if (args != null && args.length > 0) {
			List<String> arguments = new ArrayList<String>();
			Map<String, String> parameters = new HashMap<String, String>();
			
			for (String it : args) {
				if (it.startsWith("-X")) {
					String[] pair = it.substring(2).split("=", 2);
					if (pair.length == 2) {
						parameters.put(pair[0], pair[1]);
					}
				} else {
					arguments.add(it);
				}
			}
			
			CmdLineParser parser = new CmdLineParser(bean);
			parser.parseArgument(arguments);
			bean.parameters = parameters;
		}
		
		return bean;
	}
	
	
	public int process(ArgumentBean args, CmdlineInterface cli) throws Exception {
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
					files.addAll(cli.extract(files, args.output, args.conflict));
				}
				
				if (args.getSubtitles) {
					files.addAll(cli.getSubtitles(files, args.db, args.query, args.lang, args.output, args.encoding, !args.nonStrict));
				} else if (args.getMissingSubtitles) {
					files.addAll(cli.getMissingSubtitles(files, args.db, args.query, args.lang, args.output, args.encoding, !args.nonStrict));
				}
				
				if (args.rename) {
					cli.rename(files, args.action, args.conflict, args.output, args.format, args.db, args.query, args.order, args.filter, args.lang, !args.nonStrict);
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
				bindings.put("args", args.getFiles(false));
				
				ScriptProvider scriptProvider = new DefaultScriptProvider();
				Analytics.trackEvent("CLI", "ExecuteScript", scriptProvider.getScriptLocation(args.script).getScheme());
				
				ScriptShell shell = new ScriptShell(cli, args, args.parameters, args.trustScript, AccessController.getContext(), scriptProvider);
				shell.runScript(args.script, bindings);
			}
			
			CLILogger.finest("Done ヾ(＠⌒ー⌒＠)ノ");
			return 0;
		} catch (Throwable e) {
			CLILogger.log(Level.SEVERE, String.format("%s: %s", getRootCause(e).getClass().getSimpleName(), getRootCauseMessage(e)), e.getClass() == Exception.class ? null : getRootCause(e));
			CLILogger.finest("Failure (°_°)");
			return -1;
		}
	}
	
	
	public void printHelp(ArgumentBean argumentBean) {
		new CmdLineParser(argumentBean).printUsage(System.out);
		System.out.println(" -X<name>=<value>                       : Define script variable");
	}
	
	
	public static class DefaultScriptProvider implements ScriptProvider {
		
		@Override
		public URI getScriptLocation(String input) {
			try {
				return new URL(input).toURI();
			} catch (Exception eu) {
				try {
					// fn:sortivo
					if (input.startsWith("fn:")) {
						return new URI("http", "filebot.sourceforge.net", "/scripts/" + input.substring(3) + ".groovy", null);
					}
					
					// script:println 'hello world'
					if (input.startsWith("script:")) {
						return new URI("script", input.substring(7), null, null, null);
					}
					
					// system:in
					if (input.equals("system:in")) {
						return new URI("system", "in", null, null, null);
					}
					
					// X:/foo/bar.groovy
					File file = new File(input);
					if (!file.isFile()) {
						throw new FileNotFoundException(file.getPath());
					}
					return file.toURI();
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
		
		
		@Override
		public String fetchScript(URI uri) throws IOException {
			if (uri.getScheme().equals("file")) {
				return readAll(new InputStreamReader(new FileInputStream(new File(uri)), "UTF-8"));
			}
			
			if (uri.getScheme().equals("system")) {
				return readAll(new InputStreamReader(System.in));
			}
			
			if (uri.getScheme().equals("script")) {
				return uri.getAuthority();
			}
			
			// fetch remote script only if modified
			CachedResource<String> script = new CachedResource<String>(uri.toString(), String.class, 24 * 60 * 60 * 1000) {
				
				@Override
				public String process(ByteBuffer data) {
					return Charset.forName("UTF-8").decode(data).toString();
				}
			};
			return script.get();
		}
	}
	
}
