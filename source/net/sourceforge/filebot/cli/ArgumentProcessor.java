
package net.sourceforge.filebot.cli;


import static net.sourceforge.filebot.cli.CLILogging.*;
import static net.sourceforge.tuned.ExceptionUtilities.*;
import static net.sourceforge.tuned.FileUtilities.*;

import java.io.File;
import java.security.AccessController;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.logging.Level;

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
					files.addAll(cli.extract(files, args.output));
				}
				
				if (args.getSubtitles) {
					files.addAll(cli.getSubtitles(files, args.query, args.lang, args.output, args.encoding, !args.nonStrict));
				} else if (args.getMissingSubtitles) {
					files.addAll(cli.getMissingSubtitles(files, args.query, args.lang, args.output, args.encoding, !args.nonStrict));
				}
				
				if (args.rename) {
					cli.rename(files, args.action, args.output, args.format, args.db, args.query, args.order, args.lang, !args.nonStrict);
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
				
				Analytics.trackEvent("CLI", "ExecuteScript", args.getScriptLocation().getProtocol());
				ScriptShell shell = new ScriptShell(cli, args, args.trustScript, AccessController.getContext());
				shell.run(args.getScriptLocation(), bindings);
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
	}
	
}
