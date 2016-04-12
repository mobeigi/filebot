package net.filebot.cli;

import static net.filebot.Logging.*;
import static net.filebot.util.ExceptionUtilities.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import net.filebot.MediaTypes;
import net.filebot.StandardRenameAction;
import net.filebot.WebServices;

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
		log.finest("Failure (°_°)" + System.lineSeparator());
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

		ScriptSource source = ScriptSource.findScriptProvider(args.script);
		ScriptShell shell = new ScriptShell(source.getScriptProvider(args.script), args.defines);
		shell.runScript(source.accept(args.script), bindings);
	}

}
