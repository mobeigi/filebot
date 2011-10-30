
package net.sourceforge.filebot.cli;


import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import net.sourceforge.filebot.Analytics;
import net.sourceforge.filebot.MediaTypes;


public class ArgumentProcessor {
	
	public ArgumentBean parse(String[] args) throws CmdLineException {
		final ArgumentBean bean = new ArgumentBean();
		
		if (args != null && args.length > 0) {
			CmdLineParser parser = new CmdLineParser(bean);
			parser.parseArgument(args);
		}
		
		return bean;
	}
	

	public int process(ArgumentBean args, CmdlineInterface cli) throws Exception {
		Analytics.trackView(ArgumentProcessor.class, "FileBot CLI");
		CLILogger.setLevel(args.getLogLevel());
		
		try {
			// print operations
			if (args.list) {
				for (String eps : cli.fetchEpisodeList(args.query, args.format, args.db, args.lang)) {
					System.out.println(eps);
				}
				return 0;
			}
			
			if (args.script == null) {
				// file operations
				Set<File> files = new LinkedHashSet<File>(args.getFiles(true));
				
				if (args.getSubtitles) {
					List<File> subtitles = cli.getSubtitles(files, args.query, args.lang, args.output, args.encoding);
					files.addAll(subtitles);
				}
				
				if (args.rename) {
					cli.rename(files, args.query, args.format, args.db, args.lang, !args.nonStrict);
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
				String script = readAll(new InputStreamReader(args.getScriptLocation().openStream(), "UTF-8"));
				
				Bindings bindings = new SimpleBindings();
				bindings.put("args", args.getFiles(false));
				
				Analytics.trackEvent("CLI", "ExecuteScript", args.getScriptLocation().getProtocol());
				ScriptShell shell = new ScriptShell(cli, args, AccessController.getContext());
				shell.evaluate(script, bindings);
			}
			
			CLILogger.finest("Done ヾ(＠⌒ー⌒＠)ノ");
			return 0;
		} catch (Exception e) {
			CLILogger.severe(e.toString());
			CLILogger.finest("Failure (°_°)");
			return -1;
		}
	}
	

	public void printHelp(ArgumentBean argumentBean) {
		new CmdLineParser(argumentBean).printUsage(System.out);
	}
	
}
