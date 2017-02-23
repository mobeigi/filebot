package net.filebot.cli;

import static net.filebot.Logging.*;
import static net.filebot.MediaTypes.*;
import static net.filebot.util.ExceptionUtilities.*;
import static net.filebot.util.FileUtilities.*;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

import javax.script.Bindings;
import javax.script.SimpleBindings;

public class ArgumentProcessor {

	public int run(ArgumentBean args) {
		try {
			// interactive mode enables basic selection and confirmation dialogs in the CLI
			CmdlineInterface cli = args.isInteractive() ? new CmdlineOperationsTextUI() : new CmdlineOperations();

			if (args.script == null) {
				// execute command
				return runCommand(cli, args);
			} else {
				// execute user script
				runScript(cli, args);

				// script finished successfully
				log.finest("Done ヾ(＠⌒ー⌒＠)ノ");
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

	public int runCommand(CmdlineInterface cli, ArgumentBean args) throws Exception {
		// sanity checks
		if (args.getSubtitles && args.recursive) {
			throw new CmdlineException("`filebot -get-subtitles -r` has been disabled due to abuse. Please see http://bit.ly/suball for details.");
		}

		// print episode info
		if (args.list) {
			return print(cli.fetchEpisodeList(args.getEpisodeListProvider(), args.getSearchQuery(), args.getExpressionFormat(), args.getExpressionFilter(), args.getSortOrder(), args.getLanguage().getLocale(), args.isStrict()));
		}

		// print media info
		if (args.mediaInfo) {
			return print(cli.getMediaInfo(args.getFiles(true), args.getExpressionFileFilter(), args.getExpressionFormat()));
		}

		// revert files
		if (args.revert) {
			return cli.revert(args.getFiles(false), args.getExpressionFileFilter(), args.getRenameAction()).isEmpty() ? 1 : 0;
		}

		// file operations
		Set<File> files = new LinkedHashSet<File>(args.getFiles(true));

		if (args.extract) {
			files.addAll(cli.extract(files, args.getOutputPath(), args.getConflictAction(), null, args.isStrict()));
		}

		if (args.getSubtitles) {
			files.addAll(cli.getMissingSubtitles(files, args.getSearchQuery(), args.getLanguage(), args.getSubtitleOutputFormat(), args.getEncoding(), args.getSubtitleNamingFormat(), args.isStrict()));
		}

		if (args.rename) {
			cli.rename(files, args.getRenameAction(), args.getConflictAction(), args.getAbsoluteOutputFolder(), args.getExpressionFormat(), args.getDatasource(), args.getSearchQuery(), args.getSortOrder(), args.getExpressionFilter(), args.getLanguage().getLocale(), args.isStrict());
		}

		if (args.check) {
			// check verification file
			if (containsOnly(files, VERIFICATION_FILES)) {
				if (!cli.check(files)) {
					throw new Exception("Data corruption detected"); // one or more hashes do not match
				}
			} else {
				cli.compute(files, args.getOutputPath(), args.getOutputHashType(), args.getEncoding());
			}
		}

		return 0;
	}

	private int print(Stream<?> values) {
		return values.mapToInt(v -> {
			System.out.println(v);
			return 1;
		}).sum() == 0 ? 1 : 0;
	}

	public void runScript(CmdlineInterface cli, ArgumentBean args) throws Throwable {
		Bindings bindings = new SimpleBindings();
		bindings.put(ScriptShell.SHELL_ARGS_BINDING_NAME, args);
		bindings.put(ScriptShell.ARGV_BINDING_NAME, args.getFiles(false));

		ScriptSource source = ScriptSource.findScriptProvider(args.script);
		ScriptShell shell = new ScriptShell(source.getScriptProvider(args.script), cli, args.defines);
		shell.runScript(source.accept(args.script), bindings);
	}

}
