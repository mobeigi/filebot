package net.sourceforge.filebot.cli;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.List;

import net.sourceforge.filebot.RenameAction;

public interface CmdlineInterface {

	List<File> rename(Collection<File> files, RenameAction action, String conflict, String output, String format, String db, String query, String sortOrder, String filter, String lang, boolean strict) throws Exception;

	List<File> getSubtitles(Collection<File> files, String db, String query, String lang, String output, String encoding, String format, boolean strict) throws Exception;

	List<File> getMissingSubtitles(Collection<File> files, String db, String query, String lang, String output, String encoding, String format, boolean strict) throws Exception;

	boolean check(Collection<File> files) throws Exception;

	File compute(Collection<File> files, String output, String encoding) throws Exception;

	List<String> fetchEpisodeList(String query, String format, String db, String sortOrder, String lang) throws Exception;

	String getMediaInfo(File file, String format) throws Exception;

	List<File> extract(Collection<File> files, String output, String conflict, FileFilter filter, boolean forceExtractAll) throws Exception;

}
